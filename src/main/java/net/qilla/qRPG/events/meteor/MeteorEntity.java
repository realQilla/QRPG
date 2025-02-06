package net.qilla.qRPG.events.meteor;

import io.papermc.paper.math.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.qilla.qRPG.events.general.CustomEntity;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MeteorEntity extends Display.BlockDisplay implements CustomEntity<CraftBlockDisplay> {

    private final CraftBlockDisplay craft;

    private final int lifespan;
    private float yRotation = -90;
    private float xRotation = 0;
    private final float rotationRate = RandomUtil.between(0.5f, 1.5f);

    public MeteorEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position pos, @NotNull BlockState blockState, int lifespan) {
        super(EntityType.BLOCK_DISPLAY, level);

        this.craft = new CraftBlockDisplay(craftServer, this);

        super.setShadowRadius(100);
        super.setBlockState(blockState);
        super.setBrightnessOverride(new Brightness(15, 15));
        super.setPos(pos.x(), pos.y(), pos.z());
        craft.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.5f),
                new AxisAngle4f(),
                new Vector3f(1, 1, 1),
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
        if(lifespan > 0) {
            if(super.tickCount > lifespan) {
                super.discard(EntityRemoveEvent.Cause.DISCARD);
            }
        }

        if((yRotation += rotationRate) > 90) yRotation = -90;
        if((xRotation += rotationRate) > 90) xRotation = -90;
        super.forceSetRotation(yRotation, xRotation);
    }
}