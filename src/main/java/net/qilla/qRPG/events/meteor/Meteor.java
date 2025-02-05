package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import com.mojang.math.Transformation;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
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

    private final Plugin plugin;
    private final CraftServer craftServer;
    private final ServerLevel level;
    private final World world;
    private final Position originPos;
    private final Position crashPos;
    private final MountEntity meteorMount;
    private final MeteorEntity meteorDisplay;
    private final MeteorTrail trail;

    private final int lifespan = RandomUtil.between(225, 325);
    private int tickCount = 0;
    private float meteorScale = 0.5f;
    private final List<CraftBlockData> blownBlocks = new ArrayList<>();
    private Position curPosition;

    public Meteor(@NotNull Plugin plugin, @NotNull MeteorTrail trail, @NotNull Location loc) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(loc, "Location cannot be null");

        this.plugin = plugin;
        this.trail = trail;
        this.craftServer = (CraftServer) plugin.getServer();
        this.level = ((CraftWorld) loc.getWorld()).getHandle();
        this.world = loc.getWorld();
        this.originPos = MeteorPathUtil.getOriginPos(loc);
        this.crashPos = MeteorPathUtil.getCrashPos(loc);
        this.meteorMount = new MountEntity(craftServer, level, originPos);
        level.addFreshEntity(meteorMount);
        meteorMount.setLifespan(lifespan + 5);
        this.meteorDisplay = new MeteorEntity(craftServer, level, originPos, calcMeteorBlock().getHandle());
        level.addFreshEntity(meteorDisplay);
        meteorDisplay.setLifespan(lifespan + 5);
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

        new BukkitRunnable() {
            final double xDif = crashPos.x() - originPos.x();
            final double yDif = crashPos.y() - originPos.y();
            final double zDif = crashPos.z() - originPos.z();

            final float maximumScale = RandomUtil.between(MIN_SCALE, MAX_SCALE);

            @Override
            public void run() {
                Collection<Player> playersInvolved = meteorDisplay.getCraft().getChunk().getPlayersSeeingChunk();
                Vec3 delta = new Vec3(xDif / lifespan, yDif / lifespan, zDif / lifespan);

                curPosition = curPosition.offset(delta.x(), delta.y(), delta.z());

                meteorMount.setPos(curPosition.x(), curPosition.y(), curPosition.z());
                meteorMount.setDeltaMovement(delta);

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

                if(tickCount >= (lifespan / 2)) {
                    trail.trailCleanup(playersInvolved, world);
                }

                if(tickCount >= (lifespan + 5)) {
                    crash(playersInvolved, crashPos);
                    trail.endCleanup();
                    this.cancel();
                    onCrash.complete(true);
                    return;
                }

                tickCount++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void crash(@NotNull Collection<Player> playersInvolved, @NotNull Position position) {
        Preconditions.checkNotNull(position, "Position cannot be null");
        Preconditions.checkNotNull(playersInvolved, "Collection cannot be null");
        CraftBlockState blockState = BlockUtil.getCraftState(Material.BARRIER);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for(int x = -CRATER_SIZE; x < CRATER_SIZE; x++) {
                for(int y = -CRATER_SIZE; y < CRATER_SIZE; y++) {
                    for(int z = -CRATER_SIZE; z < CRATER_SIZE; z++) {
                        if(x * x + y * y + z * z > CRATER_SIZE * CRATER_SIZE) continue;
                        Position curPos = position.offset(x, y, z);
                        Block block = world.getBlockAt(curPos.blockX(), curPos.blockY(), curPos.blockZ());
                        if(block.getType().isAir()) continue;

                        PlayerUtil.sendPacket(playersInvolved,
                                new ClientboundBlockUpdatePacket(new BlockPos(
                                        curPos.blockX(), curPos.blockY(), curPos.blockZ()),
                                        blockState.getHandle()));
                        if(block.getType().isSolid() && Math.random() < 0.075) {
                            blownBlocks.add((CraftBlockData) block.getBlockData());
                        }
                    }
                }
            }
            new MeteorDebris(this).burst(blownBlocks, meteorDisplay.getCraft().getTrackedPlayers());
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.playSound(new Location(world, position.x(), position.y(), position.z()), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 100, RandomUtil.between(0f, 0.5f));
            });
        });
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

    public @NotNull Position getOriginPos() {
        return this.originPos;
    }

    public @NotNull Position getCrashPos() {
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