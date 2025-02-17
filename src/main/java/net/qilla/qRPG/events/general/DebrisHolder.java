package net.qilla.qRPG.events.general;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.Position;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import net.qilla.qlibrary.util.tools.CurveUtil;
import net.qilla.qlibrary.util.tools.PlayerUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Collection;

public class DebrisHolder {

    private static final int SPAWN_RADIUS = 8;
    private static final int SHOOT_DISTANCE = 128;

    private int tickCount = 0;
    private float tickIncrement = 3;
    private final int lifespan;
    private final Collection<Player> playersInvolved;
    private final Position position;
    private final Vector startDelta;
    private final Vector endDelta;
    private final CraftArmorStand debrisMount;
    private final CraftBlockDisplay debrisDisplay;
    private int yawRotation = -180;
    private int pitchRotation = -90;
    private final int rotationRate = RandomUtil.between(1, 4);
    private boolean isValid;

    public DebrisHolder(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Collection<Player> playersInvolved, Position position, @NotNull CraftBlockData blockData, int lifespan) {
        Preconditions.checkNotNull(craftServer, "CraftServer cannot be null");
        Preconditions.checkNotNull(level, "Level cannot be null");
        Preconditions.checkNotNull(playersInvolved, "Collection cannot be null");
        Preconditions.checkNotNull(position, "Position cannot be null");
        Preconditions.checkNotNull(blockData, "Block Data cannot be null");

        this.playersInvolved = playersInvolved;
        this.position = position;
        this.debrisMount = MountEntity.getMount(craftServer, level, position);
        this.debrisDisplay = getDebrisDisplay(craftServer,level, position, blockData);
        this.lifespan = lifespan;
        this.startDelta = calcStartDelta();
        this.endDelta = calcEndDelta(startDelta);
        this.create();
        isValid = true;
    }

    private Vector calcStartDelta() {
        double angle = Math.toRadians(RandomUtil.between(0f, 360f));

        int deltaX = (int) (RandomUtil.between(2, SPAWN_RADIUS) * Math.cos(angle));
        int deltaY = RandomUtil.between(0, SPAWN_RADIUS);
        int deltaZ = (int) (RandomUtil.between(2, SPAWN_RADIUS) * Math.sin(angle));

        return new Vector(deltaX, deltaY, deltaZ);
    }

    private Vector calcEndDelta(Vector startDelta) {
        Preconditions.checkNotNull(startDelta, "Start position cannot be null");
        return startDelta.clone().normalize().multiply(SHOOT_DISTANCE);
    }

    public void tick() {
        if(!isValid) return;
        if(tickCount >= lifespan) {
            this.remove();
            return;
        }
        float normalizedTick = (float) tickCount / lifespan;

        Vector delta = CurveUtil.linearBezierVector(startDelta, endDelta, normalizedTick);
        this.tickMovement(delta);

        if((yawRotation += rotationRate) > 180) yawRotation = -180;
        if((pitchRotation += rotationRate) > 90) pitchRotation = -90;
        this.tickScale();

        tickCount += (int) tickIncrement;
        if(tickIncrement > 1) {
            tickIncrement = Math.max(1, (tickIncrement - 0.01f));
        }
    }

    private void create() {
        PlayerUtil.sendPacket(playersInvolved, new ClientboundAddEntityPacket(debrisMount.getEntityId(), debrisMount.getUniqueId(),
                position.x(), position.y(), position.z(),
                0, 0, EntityType.ARMOR_STAND, 0,
                new Vec3(0, 0, 0), 0));
        PlayerUtil.sendPacket(playersInvolved, new ClientboundSetEntityDataPacket(debrisMount.getEntityId(), debrisMount.getHandle().getEntityData().packAll()));

        PlayerUtil.sendPacket(playersInvolved, new ClientboundAddEntityPacket(debrisDisplay.getEntityId(), debrisDisplay.getUniqueId(),
                position.x(), position.y(), position.z(),
                0, 0, EntityType.BLOCK_DISPLAY, 0,
                new Vec3(0, 0, 0), 0));
        PlayerUtil.sendPacket(playersInvolved, new ClientboundSetEntityDataPacket(debrisDisplay.getEntityId(), debrisDisplay.getHandle().getEntityData().packAll()));

        debrisMount.setPassenger(debrisDisplay);
        PlayerUtil.sendPacket(playersInvolved, new ClientboundSetPassengersPacket(debrisMount.getHandle()));
    }

    public void remove() {
        PlayerUtil.sendPacket(playersInvolved, new ClientboundRemoveEntitiesPacket(debrisDisplay.getEntityId(), debrisMount.getEntityId()));
        isValid = false;
    }

    private void tickMovement(@NotNull Vector delta) {
        Preconditions.checkNotNull(delta, "Vector cannot be null");
        Position curPos = position.offset(delta.getX(), delta.getY(), delta.getZ());

        debrisMount.getHandle().setPos(curPos.x(), curPos.y(), curPos.z());
        debrisMount.getHandle().setRot(yawRotation, pitchRotation);

        PlayerUtil.sendPacket(playersInvolved, new ClientboundEntityPositionSyncPacket(debrisMount.getEntityId(),
                new PositionMoveRotation(new Vec3(curPos.x(), curPos.y(), curPos.z()),
                        new Vec3(delta.getX(), delta.getY(), delta.getZ()),
                        yawRotation, pitchRotation), false));
    }

    private void tickScale() {
        float normalizedScale = 1 - (((float) tickCount) / lifespan);

        debrisDisplay.setTransformation(new Transformation(
                new Vector3f(-(normalizedScale / 2), -(normalizedScale / 2), -(normalizedScale / 2)),
                new Quaternionf(),
                new Vector3f(normalizedScale, normalizedScale, normalizedScale),
                new Quaternionf()
        ));

        PlayerUtil.sendPacket(playersInvolved, new ClientboundSetEntityDataPacket(debrisDisplay.getEntityId(), debrisDisplay.getHandle().getEntityData().packAll()));
    }

    private static @NotNull CraftBlockDisplay getDebrisDisplay(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position position, @NotNull CraftBlockData blockData) {
        CraftBlockDisplay debris = new CraftBlockDisplay(craftServer, EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND));

        debris.setShadowRadius(2);
        debris.setBlock(blockData);
        debris.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.5f),
                new AxisAngle4f(),
                new Vector3f(1, 1, 1),
                new AxisAngle4f()
        ));
        debris.getHandle().setPos(position.x(), position.y(), position.z());

        return debris;
    }
}