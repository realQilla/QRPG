package net.qilla.qRPG.events.airdrop;

import io.papermc.paper.math.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ChunkPos;
import net.qilla.qRPG.events.general.CustomEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftChicken;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;

public class ParachuteEntity extends Chicken implements CustomEntity<CraftChicken> {


    private final CraftChicken craft;

    private final int lifespan;
    private Position curPos;

    public ParachuteEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position pos, int lifespan) {
        super(EntityType.CHICKEN, level);
        this.craft = new CraftChicken(craftServer, this);

        super.setPos(pos.x(), pos.y(), pos.z());
        craft.getAttribute(Attribute.SCALE).setBaseValue(25);

        this.lifespan = lifespan;
        curPos = pos;
    }

    @Override
    public @NotNull CraftChicken getCraft() {
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
}
