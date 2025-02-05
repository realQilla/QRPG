package net.qilla.qRPG.events.meteor;

import io.papermc.paper.math.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;

public class MountEntity extends ArmorStand {

    private int lifespan = 0;
    private final CraftArmorStand craft;

    public MountEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position position) {
        super(EntityType.ARMOR_STAND, level);

        super.setGlowingTag(true);
        super.setNoGravity(true);
        super.setMarker(true);
        super.setInvisible(true);
        super.setSmall(true);
        super.setNoBasePlate(true);
        super.setPos(position.x(), position.y(), position.z());

        this.craft = new CraftArmorStand(craftServer, this);
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public CraftArmorStand getCraft() {
        return craft;
    }

    @Override
    public boolean save(@NotNull net.minecraft.nbt.CompoundTag tag) {
        return false;
    }

    @Override
    public void tick() {
        if(lifespan > 0) {
            if(super.tickCount > lifespan) {
                super.discard(EntityRemoveEvent.Cause.DISCARD);
            }
        }
    }
}
