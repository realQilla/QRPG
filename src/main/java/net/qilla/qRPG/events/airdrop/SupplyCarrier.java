package net.qilla.qRPG.events.airdrop;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.server.level.ServerLevel;
import net.qilla.qlibrary.util.tools.CurveUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SupplyCarrier {

    private static final int MAX_SPAWN_RADIUS = 1024;
    private static final int MIN_SPAWN_RADIUS = 256;
    private static final int SPAWN_HEIGHT = RandomUtil.between(180, 240);
    private static final int HEIGHT_OFFSET = 48;
    private final int lifespan = RandomUtil.between(3072, 5120);
    private final float dropPoint = lifespan * RandomUtil.between(0.35f, 0.65f);

    private final Plugin plugin;
    private final CraftServer craftServer;
    private final ServerLevel level;
    private final World world;

    private final Vector startDelta;
    private final BlockPosition startPos;
    private final Vector endDelta;
    private final BlockPosition endPos;
    private final Vector ranMidDelta;
    private final Vector dropDelta;
    private final BlockPosition dropPos;

    private final List<Chunk> loadedChunks = new ArrayList<>();
    private int tickCount = 0;
    private Position curPosition;
    private VehicleEntity vehicleEntity;
    private AirdropEntity airdropEntity;

    public SupplyCarrier(@NotNull Plugin plugin, @NotNull World world, @NotNull BlockPosition originPos) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(world, "World cannot be null");
        Preconditions.checkNotNull(originPos, "Origin position cannot be null");

        this.plugin = plugin;
        this.world = world;
        this.craftServer = (CraftServer) plugin.getServer();
        this.level = ((CraftWorld) world).getHandle();

        this.startPos = calcStartPos(originPos);
        this.startDelta = new Vector();
        this.endDelta = calcEndDelta(startDelta);
        this.endPos = startPos.offset(endDelta.getBlockX(), endDelta.getBlockY(), endDelta.getBlockZ());
        this.ranMidDelta = calcRandomMidDelta(startDelta, endDelta);
        this.dropDelta = calcDropDelta(startDelta, ranMidDelta, endDelta, dropPoint / lifespan);
        this.dropPos = startPos.offset(dropDelta.getBlockX(), dropDelta.getBlockY(), dropDelta.getBlockZ());

        this.curPosition = startPos;
    }

    public void init(@NotNull CompletableFuture<Boolean> onEject, @NotNull CompletableFuture<Boolean> onLand) {
        vehicleEntity = new VehicleEntity(craftServer, level, this.getStartPos(), lifespan);
        vehicleEntity.create();
        airdropEntity = new AirdropEntity(craftServer, level, this.getStartPos(), lifespan);
        airdropEntity.create();

        airdropEntity.startRiding(vehicleEntity, true);

        new BukkitRunnable() {
            private boolean isEjected = false;

            @Override
            public void run() {
                float normalizedTick = (float) tickCount / lifespan;
                float nextNormalizedTick = (float) (tickCount + 1) / lifespan;

                if(!isEjected && tickCount >= dropPoint) {
                    isEjected = true;
                    airdropEntity.remove();
                    onEject.complete(true);
                    SupplyDrop supplyDrop = new SupplyDrop(plugin, world, getDropPos());
                    supplyDrop.init(onLand);
                }
                if(tickCount >= lifespan) {
                    loadedChunks.forEach(world::unloadChunk);
                    this.cancel();
                    return;
                }

                Vector posDelta = CurveUtil.quadraticBezierVector(startDelta, ranMidDelta, endDelta, normalizedTick);
                Vector nextPosDelta = CurveUtil.quadraticBezierVector(startDelta, ranMidDelta, endDelta, nextNormalizedTick);
                Vector direction = nextPosDelta.clone().subtract(posDelta).normalize();

                float vehicleYaw = (float) Math.toDegrees(Math.atan2(direction.getZ(), direction.getX())) + 90;
                float dropYaw = (float) Math.toDegrees(Math.atan2(direction.getZ(), direction.getX())) - 90;

                curPosition = startPos.offset(posDelta.getX(), posDelta.getY(), posDelta.getZ());

                Chunk chunk = world.getChunkAt(curPosition.blockX() >> 4, curPosition.blockZ() >> 4, false);
                if(!chunk.isLoaded()) {
                    world.loadChunk(chunk);
                    loadedChunks.add(chunk);
                }

                vehicleEntity.moveTo(curPosition.x(), curPosition.y(), curPosition.z(), vehicleYaw, 0);
                airdropEntity.moveTo(curPosition.x(), curPosition.y(), curPosition.z(), dropYaw, 0);

                tickCount++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public BlockPosition getStartPos() {
        return this.startPos;
    }

    public BlockPosition getEndPos() {
        return this.endPos;
    }

    public BlockPosition getDropPos() {
        return this.dropPos;
    }

    public Position getCurPos() {
        return this.curPosition;
    }

    public float getDropProgress() {
        return this.tickCount / this.dropPoint;
    }

    private static @NotNull BlockPosition calcStartPos(BlockPosition originPos) {
        double angle = Math.toRadians(RandomUtil.between(0f, 360f));

        int xDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.cos(angle));
        int yDelta = RandomUtil.offsetFrom(0, HEIGHT_OFFSET);
        int zDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.sin(angle));

        return Position.block(originPos.blockX() + xDelta, SPAWN_HEIGHT + yDelta, originPos.blockZ() + zDelta);
    }

    private static @NotNull Vector calcEndDelta(@NotNull Vector startDelta) {
        Preconditions.checkNotNull(startDelta, "Start delta cannot be null");

        double angle = Math.toRadians(RandomUtil.between(0f, 360f));

        int xDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.cos(angle));
        int yDelta = RandomUtil.offsetFrom(0, HEIGHT_OFFSET);
        int zDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.sin(angle));

        return startDelta.clone().add(new Vector(xDelta, yDelta, zDelta));
    }

    private static @NotNull Vector calcRandomMidDelta(@NotNull Vector startDelta, @NotNull Vector endDelta) {
        Preconditions.checkNotNull(startDelta, "Start delta cannot be null");
        Preconditions.checkNotNull(endDelta, "End delta cannot be null");

        Vector midDelta = startDelta.clone().add(endDelta).multiply(0.5);

        double angle = Math.toRadians(RandomUtil.between(0f, 360f));

        int xDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.cos(angle));
        int yDelta = RandomUtil.offsetFrom(0, HEIGHT_OFFSET);
        int zDelta = (int) (RandomUtil.between(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS) * Math.sin(angle));

        return midDelta.add(new Vector(xDelta, yDelta, zDelta));
    }

    private static @NotNull Vector calcDropDelta(@NotNull Vector startDelta, @NotNull Vector midDelta, @NotNull Vector endDelta, float normalizedTick) {
        Preconditions.checkNotNull(startDelta, "Start delta cannot be null");
        Preconditions.checkNotNull(midDelta, "Mid delta cannot be null");
        Preconditions.checkNotNull(endDelta, "End delta cannot be null");

        return CurveUtil.quadraticBezierVector(startDelta, midDelta, endDelta, normalizedTick);
    }
}