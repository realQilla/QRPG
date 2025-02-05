package net.qilla.qRPG.events.meteor;

import com.mojang.math.Transformation;
import io.papermc.paper.math.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DebrisEntity extends Display.BlockDisplay {

    private final CraftBlockDisplay craft;

    private int lifespan = 0;
    private float yRotation = -90;
    private float xRotation = 0;
    private final float rotationRate = RandomUtil.between(0.5f, 1.5f);

    public DebrisEntity(@NotNull CraftServer craftServer, @NotNull ServerLevel level, @NotNull Position pos, @NotNull CraftBlockState blockState) {
        super(EntityType.BLOCK_DISPLAY, level);
        super.setShadowRadius(2);
        super.setBlockState(blockState.getHandle());
        super.setPos(pos.x(), pos.y(), pos.z());
        super.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.5f),
                new Quaternionf(),
                new Vector3f(1, 1, 1),
                new Quaternionf()
        ));

        this.craft = new CraftBlockDisplay(craftServer, this);
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public CraftBlockDisplay getCraft() {
        return craft;
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
