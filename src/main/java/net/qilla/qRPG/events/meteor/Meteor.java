package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import com.mojang.math.Transformation;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.qilla.qRPG.events.general.MountEntity;
import net.qilla.qlibrary.util.tools.BlockUtil;
import net.qilla.qlibrary.util.tools.CurveUtil;
import net.qilla.qlibrary.util.tools.PlayerUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Meteor {

    private static final List<CraftBlockState> METEOR_BLOCKS = BlockUtil.getCraftState(List.of(
            Material.MAGMA_BLOCK,
            Material.NETHERRACK,
            Material.FIRE_CORAL_BLOCK,
            Material.NETHER_WART_BLOCK,
            Material.BLACKSTONE,
            Material.OBSIDIAN,
            Material.HONEYCOMB_BLOCK,
            Material.HONEY_BLOCK,
            Material.RESIN_BLOCK,
            Material.RAW_COPPER_BLOCK,
            Material.PRISMARINE,
            Material.BLUE_ICE,
            Material.RAW_GOLD_BLOCK,
            Material.GLOWSTONE,
            Material.CRYING_OBSIDIAN,
            Material.AMETHYST_BLOCK,
            Material.DRAGON_EGG
    ));

    private static final int MAX_SPAWN_RADIUS = 512;
    private static final int MIN_SPAWN_RADIUS = 256;
    private static final int SPAWN_HEIGHT = 512;
    private static final int CRATER_SIZE = 15;
    private static final float TRAIL_PUFF_CHANCE = 0.33f;
    private static final float MAX_SCALE = RandomUtil.between(10, 15);
    private static final int MAX_DEBRIS = 2500;

    private final Plugin plugin;
    private final CraftServer craftServer;
    private final ServerLevel level;
    private final World world;

    private final BlockPosition startPos;
    private final Vector startDelta;
    private final Vector endDelta;
    private final BlockPosition endPos;
    private final MeteorTrail trail;
    private MountEntity meteorMount;
    private MeteorEntity meteorDisplay;

    private final List<Chunk> loadedChunks = new ArrayList<>();
    private final int lifespan = RandomUtil.between(512, 640);
    private int tickCount = 0;
    private float meteorScale = 1f;
    private Position curPosition;

    public Meteor(@NotNull Plugin plugin, @NotNull MeteorTrail trail, @NotNull BlockPosition originPos, @NotNull World world) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(originPos, "Location cannot be null");
        Preconditions.checkNotNull(world, "World cannot be null");

        this.plugin = plugin;
        this.trail = trail;
        this.craftServer = (CraftServer) plugin.getServer();
        this.level = ((CraftWorld) world).getHandle();
        this.world = world;

        this.startPos = calcStartPos(originPos);
        this.startDelta = new Vector();
        this.endDelta = calcEndDelta(startPos, startDelta, world);
        this.endPos = startPos.offset(endDelta.getBlockX(), endDelta.getBlockY(), endDelta.getBlockZ());

        this.curPosition = startPos;
    }

    public void initAirborne(@NotNull CompletableFuture<Boolean> onCrash) {
        Preconditions.checkNotNull(onCrash, "Future cannot be null");

        meteorMount = new MountEntity(craftServer, level, startPos, lifespan);
        meteorMount.create();
        meteorDisplay = new MeteorEntity(craftServer, level, startPos, calcMeteorType().getHandle(), lifespan);
        meteorDisplay.create();

        this.meteorDisplay.startRiding(meteorMount, true);
        this.fallLoop(onCrash);
    }

    private void fallLoop(@NotNull CompletableFuture<Boolean> onCrash) {
        if(endPos == null) {
            onCrash.complete(false);
            trail.endCleanup();
            return;
        }

        new BukkitRunnable() {
            private BlockPosition oldPosition = startPos;
            @Override
            public void run() {
                Collection<Player> playersInvolved = meteorDisplay.getCraft().getTrackedPlayers();
                float oldNormalizedTick = (tickCount - 5) / (float) lifespan;
                float normalizedTick = tickCount / (float) lifespan;

                if(tickCount >= lifespan) {
                    crash(playersInvolved);
                    trail.endCleanup();
                    this.cancel();
                    onCrash.complete(true);
                    return;
                }

                Vector oldPosDelta = CurveUtil.linearBezierVector(startDelta, endDelta, oldNormalizedTick);
                Vector posDelta = CurveUtil.linearBezierVector(startDelta, endDelta, normalizedTick);
                System.out.println(posDelta);
                curPosition = startPos.offset(posDelta.getX(), posDelta.getY(), posDelta.getZ());
                oldPosition = startPos.offset(oldPosDelta.getBlockX(), oldPosDelta.getBlockY(), oldPosDelta.getBlockZ());

                Chunk chunk = world.getChunkAt(curPosition.blockX() >> 4, curPosition.blockZ() >> 4, false);
                if(!chunk.isLoaded()) {
                    world.loadChunk(chunk);
                    loadedChunks.add(chunk);
                }

                meteorMount.setPos(curPosition.x(), curPosition.y(), curPosition.z());

                if(meteorScale < MAX_SCALE) {
                    meteorScale += (MAX_SCALE / (lifespan - 75));
                    meteorDisplay.setTransformation(new Transformation(
                            new Vector3f(-(meteorScale / 2), -(meteorScale / 2), -(meteorScale / 2)),
                            new Quaternionf(),
                            new Vector3f(meteorScale, meteorScale, meteorScale),
                            new Quaternionf()
                    ));
                }

                if(Math.random() < TRAIL_PUFF_CHANCE && tickCount < (lifespan - 5)) {
                    trail.tickSmoke(playersInvolved, oldPosition, 0.25f);
                }

                if((tickCount % 5) == 0) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(new Location(world, curPosition.x(), curPosition.y(), curPosition.z()), Sound.ENTITY_PARROT_IMITATE_BREEZE, 40, RandomUtil.between(0f, 0.5f));
                    });
                }

                if(tickCount >= (lifespan / 2) && !trail.isCleaning()) {
                    trail.trailCleanup(playersInvolved, world);
                }

                tickCount++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void crash(@NotNull Collection<Player> playersInvolved) {
        Preconditions.checkNotNull(playersInvolved, "Collection cannot be null");
        List<BlockPosition> craterList = getCrater(endPos);
        List<CraftBlockData> blownBlocks = new ArrayList<>();

        this.carveCrater(playersInvolved, craterList);
        for(BlockPosition curPos : craterList) {
            if(blownBlocks.size() > MAX_DEBRIS) break;

            Block block = world.getBlockAt(curPos.blockX(), curPos.blockY(), curPos.blockZ());
            if(block.getType().isSolid() && Math.random() < 0.05) {
                blownBlocks.add((CraftBlockData) block.getBlockData());
            }
        }
        MeteorDebris debris = new MeteorDebris(plugin, level, craftServer);
        debris.burst(blownBlocks, playersInvolved, endPos);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(new Location(world, curPosition.x(), curPosition.y(), curPosition.z()),
                    Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 100, RandomUtil.between(0f, 0.5f));
        });

        loadedChunks.forEach(world::unloadChunk);
    }

    private List<BlockPosition> getCrater(@NotNull BlockPosition centerPos) {
        List<BlockPosition> craterList = new ArrayList<>();

        for(int x = -CRATER_SIZE; x < CRATER_SIZE; x++) {
            for(int y = -CRATER_SIZE; y < CRATER_SIZE; y++) {
                for(int z = -CRATER_SIZE; z < CRATER_SIZE; z++) {
                    if(x * x + y * y + z * z > CRATER_SIZE * CRATER_SIZE) continue;
                    craterList.add(centerPos.offset(x, y, z));
                }
            }
        }
        return craterList;
    }

    private void carveCrater(@NotNull Collection<Player> playersInvolved, @NotNull List<BlockPosition> craterList) {
        CraftBlockState blockState = BlockUtil.getCraftState(Material.AIR);

        for(BlockPosition blockPos : craterList) {
            PlayerUtil.sendPacket(playersInvolved,
                    new ClientboundBlockUpdatePacket(new BlockPos(
                            blockPos.blockX(), blockPos.blockY(), blockPos.blockZ()),
                            blockState.getHandle()));
        }
    }

    public @NotNull BlockPosition getStartPos() {
        return this.startPos;
    }

    public @NotNull BlockPosition getEndPos() {
        return this.endPos;
    }

    public @NotNull Position getCurPos() {
        return this.curPosition;
    }

    public float getProgress() {
        return this.tickCount / (float) this.lifespan;
    }

    private CraftBlockState calcMeteorType() {
        int index = 0;
        while(index < METEOR_BLOCKS.size()) {
            if(Math.random() < 0.50) {
                return METEOR_BLOCKS.get(index);
            } else index++;
        }
        return METEOR_BLOCKS.getFirst();
    }

    public static @NotNull BlockPosition calcStartPos(BlockPosition originPos) {
        double angle = Math.toRadians(RandomUtil.between(0f, 360f));

        int xDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.cos(angle));
        int yDelta = 0;
        int deltaZ = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.sin(angle));

        return Position.block(originPos.blockX() + xDelta, SPAWN_HEIGHT + yDelta, originPos.blockZ() + deltaZ);
    }

    public static @NotNull Vector calcEndDelta(@NotNull BlockPosition startPos, @NotNull Vector startDelta, @NotNull World world) {
        double angle = Math.toRadians(RandomUtil.between(0f, 360f));

        int xDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.cos(Math.toRadians(angle)));
        int zDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.sin(Math.toRadians(angle)));
        int yDelta = world.getHighestBlockYAt(startPos.blockX() + xDelta, startPos.blockZ() + zDelta) - startPos.blockY();

        return startDelta.clone().add(new Vector(xDelta, yDelta, zDelta));
    }
}