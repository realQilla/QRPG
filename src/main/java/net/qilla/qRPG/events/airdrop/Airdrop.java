package net.qilla.qRPG.events.airdrop;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.server.level.ServerLevel;
import net.qilla.qRPG.events.general.MountEntity;
import net.qilla.qlibrary.util.tools.CurveUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class Airdrop {

    private static final int POSITION_RANGE = 256;
    private static final int SPAWN_HEIGHT = 180;

    private final Plugin plugin;
    private final CraftServer craftServer;
    private final ServerLevel level;
    private final World world;

    private final int vehicleLifespan = RandomUtil.between(768, 1024);
    private final int airDropLifespan = RandomUtil.between(250, 380);
    private final float dropPoint = RandomUtil.between(0.35f, 0.65f);

    private final BlockPosition pointA;
    private final BlockPosition pointB;
    private final BlockPosition pointC;
    private final BlockPosition pointD;
    private MountEntity vehicleMountEntity;
    private VehicleEntity vehicleEntity;
    private MountEntity airdropMountEntity;
    private AirdropEntity airdropEntity;
    private final List<ParachuteEntity> parachuteEntities = new ArrayList<>();

    public Airdrop(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(blockPos, "Location cannot be null");

        this.plugin = plugin;
        this.craftServer = (CraftServer) plugin.getServer();
        this.level = ((CraftWorld) world).getHandle();
        this.world = world;

        this.pointA = this.createPointA(blockPos);
        this.pointB = this.createPointB(pointA);
        this.pointC = this.createPointC(pointA(), pointB());
        this.pointD = this.createPointD(pointC());
    }

    public void initAirborne(@NotNull CompletableFuture<Boolean> onEject, @NotNull CompletableFuture<Boolean> onLand) {
        vehicleMountEntity = new MountEntity(craftServer, level, pointA, vehicleLifespan);
        vehicleMountEntity.create();
        vehicleEntity = new VehicleEntity(craftServer, level, pointA, vehicleLifespan);
        vehicleEntity.create();
        airdropMountEntity = new MountEntity(craftServer, level, pointA, vehicleLifespan);
        airdropMountEntity.create();
        airdropEntity = new AirdropEntity(craftServer, level, pointA, vehicleLifespan);
        airdropEntity.create();

        vehicleEntity.startRiding(vehicleMountEntity, true);
        airdropEntity.startRiding(airdropMountEntity, true);
        airdropMountEntity.startRiding(vehicleEntity, true);

        final double deltaX = pointB.x() - pointA.x();
        final double deltaY = pointB.y() - pointA.y();
        final double deltaZ = pointB.z() - pointA.z();

        new BukkitRunnable() {
            private Position vehiclePos = pointA;
            private int tickCount = 0;
            private boolean isEjected = false;

            @Override
            public void run() {
                if(tickCount >= vehicleLifespan) {
                    this.cancel();
                    return;
                }

                Vector delta = new Vector(deltaX / vehicleLifespan, deltaY / vehicleLifespan, deltaZ / vehicleLifespan);
                vehiclePos = vehiclePos.offset(delta.getX(), delta.getY(), delta.getZ());
                float yaw = (float) Math.toDegrees(Math.atan2(delta.getZ(), delta.getX())) + 90;
                float normalizedTick = tickCount / (float) vehicleLifespan;

                if(!isEjected) {
                    if(normalizedTick >= dropPoint) {
                        isEjected = true;
                        onEject.complete(true);
                        fallLoop(onLand);
                    }
                    airdropEntity.forceSetRotation(yaw * -1, 0);
                }

                vehicleMountEntity.setPos(vehiclePos.x(), vehiclePos.y(), vehiclePos.z());

                vehicleEntity.forceSetRotation(yaw, 0);

                tickCount++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void fallLoop(@NotNull CompletableFuture<Boolean> onLand) {
        final double deltaX = pointD.x() - pointC.x();
        final double deltaY = pointD.y() - pointC.y();
        final double deltaZ = pointD.z() - pointC.z();

        airdropMountEntity.stopRiding();
        airdropEntity.getCraft().setTransformation(new Transformation(
                new Vector3f(-2.5f, 1, -2.5f),
                new AxisAngle4f(),
                new Vector3f(5, 5, 5),
                new AxisAngle4f()
        ));
        for(int i = 0; i < 32; i++) {
            ParachuteEntity parachuteEntity = new ParachuteEntity(craftServer, level, pointC.offset(
                    RandomUtil.offsetFrom(0, 12),
                    24 + RandomUtil.offsetFrom(0, 4),
                    RandomUtil.offsetFrom(0, 12)
            ), airDropLifespan);
            parachuteEntity.create();
            parachuteEntity.setLeashedTo(airdropEntity, true);
            parachuteEntity.forceSetRotation(RandomUtil.between(-180, 180), 0);
            parachuteEntities.add(parachuteEntity);
        }
        new BukkitRunnable() {
            private Position airdropPos = pointC;
            private int tickCount = 0;

            @Override
            public void run() {
                if(tickCount >= airDropLifespan) {
                    onLand.complete(true);
                    this.cancel();
                    return;
                }

                Vector delta = new Vector(deltaX / airDropLifespan, deltaY / airDropLifespan, deltaZ / airDropLifespan);
                airdropPos = airdropPos.offset(delta.getX(), delta.getY(), delta.getZ());
                parachuteEntities.forEach(parachute -> {
                    parachute.setPos(
                            parachute.getX(),
                            parachute.getY() + delta.getY(),
                            parachute.getZ());
                });

                airdropMountEntity.setPos(airdropPos.x(), airdropPos.y(), airdropPos.z());
                tickCount++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private @NotNull BlockPosition createPointA(@NotNull BlockPosition origin) {
        Preconditions.checkNotNull(origin, "Origin cannot be null");

        int radius = POSITION_RANGE;

        double angle = Math.toRadians(RandomUtil.between(0, 360));

        int x = (int) (radius * Math.cos(angle));
        int z = (int) (radius * Math.sin(angle));
        BlockPosition pos = origin.offset(x, 0, z);

        return Position.block(pos.blockX(), SPAWN_HEIGHT, pos.blockZ());
    }

    private @NotNull BlockPosition createPointB(@NotNull BlockPosition pointA) {
        Preconditions.checkNotNull(pointA, "Point A cannot be null");

        int radius = POSITION_RANGE;

        double angle = Math.toRadians(RandomUtil.between(0, 360));

        int x = (int) (radius * Math.cos(angle));
        int z = (int) (radius * Math.sin(angle));
        BlockPosition pos = pointA.offset(x, 0, z);

        return Position.block(pos.blockX(), SPAWN_HEIGHT, pos.blockZ());
    }

    private @NotNull BlockPosition createPointC(@NotNull BlockPosition pointA, @NotNull BlockPosition pointB) {
        Preconditions.checkNotNull(pointA, "Point A cannot be null");
        Preconditions.checkNotNull(pointB, "Point B cannot be null");

        Vector vector = CurveUtil.linearBezierVector(pointA.toVector(), pointB.toVector(), dropPoint);

        return Position.block(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private @NotNull BlockPosition createPointD(@NotNull BlockPosition pointC) {
        Preconditions.checkNotNull(pointC, "Point C cannot be null");

        return world.getHighestBlockAt(pointC.blockX(), pointC.blockZ()).getLocation().toBlock();

    }

    public BlockPosition pointA() {
        return this.pointA;
    }

    public BlockPosition pointB() {
        return this.pointB;
    }

    public BlockPosition pointC() {
        return this.pointC;
    }

    public BlockPosition pointD() {
        return this.pointD;
    }
}