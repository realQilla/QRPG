package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MeteorPathUtil {

    private static final int XZ_SPAWN_RANGE = 300;
    private static final int Y_SPAWN_RANGE = 400;

    public MeteorPathUtil() {
    }

    public static @NotNull BlockPosition getOriginPos(@NotNull BlockPosition blockPos) {
        Preconditions.checkNotNull(blockPos, "Block position cannot be null");

        Position pos = blockPos.offset(RandomUtil.offsetFrom(0, XZ_SPAWN_RANGE), 0, RandomUtil.offsetFrom(0, XZ_SPAWN_RANGE));
        return Position.block(pos.blockX(), Y_SPAWN_RANGE, pos.blockZ());
    }

    public static @NotNull BlockPosition getCrashPos(@NotNull BlockPosition blockPos, @NotNull World world) {
        Preconditions.checkNotNull(blockPos, "Block position cannot be null");
        Preconditions.checkNotNull(world, "World cannot be null");

        BlockPosition crashPos = getOriginPos(blockPos);

        return Position.block(
                crashPos.blockX(),
                world.getHighestBlockYAt(crashPos.blockX(), crashPos.blockZ()),
                crashPos.blockZ());
    }
}