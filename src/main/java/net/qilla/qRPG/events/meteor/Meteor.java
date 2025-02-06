package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import com.mojang.math.Transformation;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.qilla.qRPG.events.general.MountEntity;
import net.qilla.qlibrary.util.tools.BlockUtil;
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

    private static final int CRATER_SIZE = 15;
    private static final float TRAIL_PUFF_CHANCE = 0.33f;
    private static final float MIN_SCALE = 4;
    private static final float MAX_SCALE = 12;
    private static final int MAX_DEBRIS = 2500;

    private final Plugin plugin;
    private final CraftServer craftServer;
    private final ServerLevel level;
    private final World world;
    private final BlockPosition originPos;
    private final BlockPosition crashPos;
    private final MountEntity meteorMount;
    private final MeteorEntity meteorDisplay;
    private final MeteorTrail trail;

    private final int lifespan = RandomUtil.between(225, 325);
    private int tickCount = 0;
    private float meteorScale = 0.5f;
    private Position curPosition;

    public Meteor(@NotNull Plugin plugin, @NotNull MeteorTrail trail, @NotNull BlockPosition blockPos, @NotNull World world) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(blockPos, "Location cannot be null");

        this.plugin = plugin;
        this.trail = trail;
        this.craftServer = (CraftServer) plugin.getServer();
        this.level = ((CraftWorld) world).getHandle();
        this.world = world;
        this.originPos = MeteorPathUtil.getOriginPos(blockPos);
        this.crashPos = MeteorPathUtil.getCrashPos(blockPos, world);
        this.meteorMount = new MountEntity(craftServer, level, originPos, lifespan + 5);
        this.meteorMount.create();
        this.meteorDisplay = new MeteorEntity(craftServer, level, originPos, calcMeteorBlock().getHandle(), lifespan + 5);
        level.addFreshEntity(meteorDisplay);
        this.curPosition = originPos;
    }

    public void initAirborne(@NotNull CompletableFuture<Boolean> onCrash) {
        Preconditions.checkNotNull(onCrash, "Future cannot be null");

        this.meteorDisplay.startRiding(this.meteorMount, true);
        this.fallLoop(onCrash);
    }

    private void fallLoop(@NotNull CompletableFuture<Boolean> onCrash) {
        if(crashPos == null) {
            onCrash.complete(false);
            trail.endCleanup();
            return;
        }

        final double xDif = crashPos.x() - originPos.x();
        final double yDif = crashPos.y() - originPos.y();
        final double zDif = crashPos.z() - originPos.z();

        final float maximumScale = RandomUtil.between(MIN_SCALE, MAX_SCALE);

        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<Player> playersInvolved = meteorDisplay.getCraft().getChunk().getPlayersSeeingChunk();

                if(tickCount >= (lifespan + 10)) {
                    crash(playersInvolved);
                    trail.endCleanup();
                    this.cancel();
                    onCrash.complete(true);
                    return;
                }

                Vec3 delta = new Vec3(xDif / lifespan, yDif / lifespan, zDif / lifespan);
                curPosition = curPosition.offset(delta.x(), delta.y(), delta.z());

                meteorMount.setPos(curPosition.x(), curPosition.y(), curPosition.z());

                meteorDisplay.setPos(curPosition.x(), curPosition.y(), curPosition.z());

                if(meteorScale < maximumScale) {
                    meteorScale += (maximumScale / (lifespan - 75));
                    meteorDisplay.setTransformation(new Transformation(
                            new Vector3f(-(meteorScale / 2), -(meteorScale / 2), -(meteorScale / 2)),
                            new Quaternionf(),
                            new Vector3f(meteorScale, meteorScale, meteorScale),
                            new Quaternionf()
                    ));
                }

                if(Math.random() < TRAIL_PUFF_CHANCE && tickCount < (lifespan - 5)) {
                    Position smokeOffset = curPosition.offset(delta.x() * -4, delta.y() * -4, delta.z() * -4);

                    trail.tickSmoke(playersInvolved, smokeOffset, 0.25f);
                }

                if((tickCount % 5) == 0) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(new Location(world, curPosition.x(), curPosition.y(), curPosition.z()), Sound.ENTITY_PARROT_IMITATE_BREEZE, 25f, RandomUtil.between(0f, 0.5f));
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
        List<BlockPosition> craterList = getCrater(crashPos);
        List<CraftBlockData> blownBlocks = new ArrayList<>();

        this.carveCrater(playersInvolved, craterList);
        for(BlockPosition curPos : craterList) {
            if(blownBlocks.size() > MAX_DEBRIS) break;

            Block block = world.getBlockAt(curPos.blockX(), curPos.blockY(), curPos.blockZ());
            if(block.getType().isSolid() && Math.random() < 0.05) {
                blownBlocks.add((CraftBlockData) block.getBlockData());
            }
        }
        new MeteorDebris(this).burst(blownBlocks, meteorDisplay.getCraft().getTrackedPlayers());

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(new Location(world, crashPos.blockX(), crashPos.blockY(), crashPos.blockZ()),
                    Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 100, RandomUtil.between(0f, 0.5f));
        });

    }

    private List<BlockPosition> getCrater(@NotNull BlockPosition center) {
        List<BlockPosition> craterList = new ArrayList<>();

        for(int x = -CRATER_SIZE; x < CRATER_SIZE; x++) {
            for(int y = -CRATER_SIZE; y < CRATER_SIZE; y++) {
                for(int z = -CRATER_SIZE; z < CRATER_SIZE; z++) {
                    if(x * x + y * y + z * z > CRATER_SIZE * CRATER_SIZE) continue;
                    craterList.add(center.offset(x, y, z));
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

    public @NotNull Plugin getPlugin() {
        return this.plugin;
    }

    public @NotNull CraftServer getCraftServer() {
        return this.craftServer;
    }

    public @NotNull ServerLevel getLevel() {
        return this.level;
    }

    public @NotNull BlockPosition getOriginPos() {
        return this.originPos;
    }

    public @NotNull BlockPosition getCrashPos() {
        return this.crashPos;
    }

    private CraftBlockState calcMeteorBlock() {
        int index = 0;
        while(index < METEOR_BLOCKS.size()) {
            if(Math.random() < 0.50) {
                return METEOR_BLOCKS.get(index);
            } else index++;
        }
        return METEOR_BLOCKS.getFirst();
    }
}