package net.qilla.qRPG.events.airdrop;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class AirdropPathUtil {

    private static final int XZ_SPAWN_RANGE = 300;

    public AirdropPathUtil() {
    }

    public static @NotNull BlockPosition getLandPos(@NotNull Location loc) {
        World world = loc.getWorld();
        Position pos = loc.offset(RandomUtil.offsetFrom(0, XZ_SPAWN_RANGE), 0, RandomUtil.offsetFrom(0, XZ_SPAWN_RANGE));
        BlockPosition landPos = Position.block(pos.blockX(), loc.getWorld().getMaxHeight(), pos.blockZ());

        while(!world.getBlockAt(landPos.blockX(), landPos.blockY(), landPos.blockZ()).isSolid()) {
            landPos = landPos.offset(0, -1, 0);
            if(landPos.blockY() <= world.getMinHeight()) break;
        }
        return landPos;
    }
}