package net.qilla.qRPG.events.airdrop;

import io.papermc.paper.math.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.qilla.qRPG.events.general.CustomEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEnderDragon;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;

public class VehicleEntity extends EnderDragon implements CustomEntity<CraftEnderDragon> {

    private final CraftEnderDragon craft;

    private final int lifespan;

    public VehicleEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position pos, int lifespan) {
        super(EntityType.ENDER_DRAGON, level);
        this.craft = new CraftEnderDragon(craftServer, this);

        super.setPos(pos.x(), pos.y(), pos.z());

        this.lifespan = lifespan;
    }

    @Override
    public CraftEnderDragon getCraft() {
        return craft;
    }

    @Override
    public void create() {
        super.level().addFreshEntity(this);
    }

    @Override
    public void aiStep() {

    }

    @Override
    public void processFlappingMovement() {

    }

    @Override
    public void onFlap() {

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
