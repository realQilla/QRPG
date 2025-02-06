package net.qilla.qRPG.events.general;

import io.papermc.paper.math.Position;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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

    private final Vector p0 = new Vector(
            0, 0, 0
    );
    private final Vector p1 = new Vector(
            0, RandomUtil.between(0, 2), 0
    );
    private final Vector p2 = new Vector(
            RandomUtil.offsetFrom(0, 4), RandomUtil.between(0, 4), RandomUtil.offsetFrom(0, 4)
    );
    private int tickCount = 0;
    private final int lifespan;
    private final ServerLevel level;
    private final CraftServer craftServer;
    private final Collection<Player> playersInvolved;
    private Position position;
    private final CraftArmorStand debrisMount;
    private final CraftBlockDisplay debrisDisplay;
    private int yRotation = -90;
    private int xRotation = 0;
    private final int rotationRate = RandomUtil.between(1, 4);
    private boolean isValid;

    public DebrisHolder(@NotNull Collection<Player> playersInvolved, @NotNull ServerLevel level, @NotNull CraftServer craftServer, @NotNull Position position, @NotNull CraftBlockData blockData, int lifespan) {
        this.playersInvolved = playersInvolved;
        this.level = level;
        this.craftServer = craftServer;
        this.position = position;
        this.debrisMount = getDebrisMount();
        this.debrisDisplay = getDebrisDisplay(blockData);
        this.lifespan = lifespan;
        this.createPacket();
        isValid = true;
    }

    public void tick() {
        if(!isValid) return;
        if(tickCount > lifespan) {
            this.remove();
            return;
        }
        double normalizedTick = tickCount / (double) 3;

        Vector delta = CurveUtil.quadraticBezierVector(p0, p1, p2, normalizedTick);
        this.tickMovement(new Vector(delta.getX(), delta.getY(), delta.getZ()));

        if((yRotation += rotationRate) > 90) yRotation = -90;
        if((xRotation += rotationRate) > 90) xRotation = -90;
        this.tickRotation();
        this.tickScale();

        this.tickCount++;
    }

    public boolean isValid() {
        return isValid;
    }

    private void tickMovement(@NotNull Vector delta) {
        this.position = position.offset(delta.getX(), delta.getY(), delta.getZ());

        debrisMount.getHandle().setPos(position.x(), position.y(), position.z());
        debrisDisplay.getHandle().setPos(position.x(), position.y(), position.z());

        PlayerUtil.sendPacket(playersInvolved, new ClientboundMoveEntityPacket.Pos(debrisMount.getEntityId(),
                (short) (delta.getX() * 4096), (short) (delta.getY() * 4096), (short) (delta.getZ() * 4096),
                false
        ));
    }

    private void tickRotation() {
        PlayerUtil.sendPacket(playersInvolved, new ClientboundMoveEntityPacket.Rot(debrisDisplay.getEntityId(), (byte) yRotation, (byte) xRotation, false));
    }

    private void tickScale() {
        float normalizedScale = 1 - (((float) tickCount) / lifespan) ;

        debrisDisplay.setTransformation(new Transformation(
                new Vector3f(-(normalizedScale / 2), -(normalizedScale / 2), -(normalizedScale / 2)),
                new Quaternionf(),
                new Vector3f(normalizedScale, normalizedScale, normalizedScale),
                new Quaternionf()
        ));

        PlayerUtil.sendPacket(playersInvolved, new ClientboundSetEntityDataPacket(debrisDisplay.getEntityId(), debrisDisplay.getHandle().getEntityData().packAll()));
    }

    private void createPacket() {
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

    private @NotNull CraftBlockDisplay getDebrisDisplay(@NotNull CraftBlockData blockData) {
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

    private @NotNull CraftArmorStand getDebrisMount() {
        CraftArmorStand mount = new CraftArmorStand(craftServer, EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND));

        mount.setGravity(false);
        mount.setMarker(true);
        mount.setInvisible(true);
        mount.setSmall(true);
        mount.setBasePlate(false);
        mount.getHandle().setPos(position.x(), position.y(), position.z());

        return mount;
    }
}