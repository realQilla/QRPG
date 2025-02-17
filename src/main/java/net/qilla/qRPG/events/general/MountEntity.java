package net.qilla.qRPG.events.general;

import io.papermc.paper.math.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;

public class MountEntity extends ArmorStand implements CustomEntity<CraftArmorStand> {

    private final int lifespan;
    private final CraftArmorStand craft;

    public MountEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position position, int lifespan) {
        super(EntityType.ARMOR_STAND, level);

        super.setGlowingTag(true);
        super.setNoGravity(true);
        super.setMarker(true);
        super.setInvisible(true);
        super.setSmall(true);
        super.setNoBasePlate(true);
        super.setPos(position.x(), position.y(), position.z());

        this.lifespan = lifespan;
        this.craft = new CraftArmorStand(craftServer, this);
    }

    @Override
    public @NotNull CraftArmorStand getCraft() {
        return craft;
    }

    @Override
    public void create() {
        CraftWorld craftWorld = level().getWorld();
        craftWorld.addEntityToWorld(this, CreatureSpawnEvent.SpawnReason.COMMAND);
        ChunkPos chunkPos = this.chunkPosition();

        if(!craftWorld.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            craftWorld.getChunkAt(chunkPos.x, chunkPos.z);
        }
    }

    @Override
    public boolean save(@NotNull CompoundTag tag) {
        return false;
    }

    @Override
    public void tick() {
        if(lifespan >= 0) {
            if(super.tickCount > lifespan) {
                super.discard(EntityRemoveEvent.Cause.DISCARD);
            }
        }
    }

    public static @NotNull CraftArmorStand getMount(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position position) {
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