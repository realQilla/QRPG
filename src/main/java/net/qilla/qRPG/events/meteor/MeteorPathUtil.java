package net.qilla.qRPG.events.meteor;

import io.papermc.paper.math.Position;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MeteorPathUtil {

    private static final int XZ_SPAWN_RANGE = 200;
    private static final int Y_SPAWN_RANGE = 500;

    public MeteorPathUtil() {
    }

    public static @NotNull Position getOriginPos(@NotNull Location loc) {
        Position pos = loc.offset(RandomUtil.offsetFrom(0, XZ_SPAWN_RANGE), 0, RandomUtil.offsetFrom(0, XZ_SPAWN_RANGE));
        return Position.block(pos.blockX(), Y_SPAWN_RANGE, pos.blockZ());
    }

    public static @Nullable Position getCrashPos(@NotNull Location loc) {
        World world = loc.getWorld();
        Position crashPos = getOriginPos(loc);

        while(!world.getBlockAt(crashPos.blockX(), crashPos.blockY(), crashPos.blockZ()).isSolid()) {
            crashPos = crashPos.offset(0, -1, 0);
            if(crashPos.blockY() < world.getMinHeight()) return null;
        }
        return crashPos;
    }
}