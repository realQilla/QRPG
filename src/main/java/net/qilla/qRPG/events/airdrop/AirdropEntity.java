package net.qilla.qRPG.events.airdrop;

import io.papermc.paper.math.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.qilla.qRPG.events.general.CustomEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class AirdropEntity extends Display.BlockDisplay implements CustomEntity<CraftBlockDisplay> {

    private final CraftBlockDisplay craft;

    private final int lifespan;

    public AirdropEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position pos, int lifespan) {
        super(EntityType.BLOCK_DISPLAY, level);

        this.craft = new CraftBlockDisplay(craftServer, this);

        super.setShadowRadius(100);
        super.setBlockState(Blocks.CHEST.defaultBlockState());
        super.setBrightnessOverride(new Brightness(15, 15));
        super.setPos(pos.x(), pos.y(), pos.z());
        craft.setTransformation(new Transformation(
                new Vector3f(-2.5f, 0, -2.5f),
                new AxisAngle4f(),
                new Vector3f(5, 5, 5),
                new AxisAngle4f()
        ));
        this.lifespan = lifespan;
    }

    @Override
    public CraftBlockDisplay getCraft() {
        return craft;
    }

    @Override
    public void create() {
        super.level().addFreshEntity(this);
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
