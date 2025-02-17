package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.qilla.qRPG.events.general.DebrisHolder;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MeteorDebris {

    private static final int LIFESPAN = 80;

    private final Plugin plugin;
    private final ServerLevel level;
    private final CraftServer craftServer;

    public MeteorDebris(@NotNull Plugin plugin, @NotNull ServerLevel level, @NotNull CraftServer craftServer) {

        this.plugin = plugin;
        this.level = level;
        this.craftServer = craftServer;
    }

    public void burst(@NotNull List<CraftBlockData> blockDataList, Collection<Player> playersInvolved, @NotNull Position endPos) {
        Preconditions.checkNotNull(blockDataList, "Block states cannot be null");
        Preconditions.checkNotNull(playersInvolved, "Collection cannot be null");

        Collections.shuffle(blockDataList);
        List<DebrisHolder> debrisList = new ArrayList<>();

        for(CraftBlockData blockData : blockDataList) {
            DebrisHolder debrisHolder = new DebrisHolder(craftServer, level, playersInvolved, endPos, blockData,
                    RandomUtil.between((int) (LIFESPAN * 0.65f), LIFESPAN));
            debrisList.add(debrisHolder);
        }
        displaceDebris(debrisList);
    }

    private void displaceDebris(List<DebrisHolder> debrisList) {
        new BukkitRunnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if(tickCount >= LIFESPAN) {
                    this.cancel();
                    return;
                }

                debrisList.forEach(DebrisHolder::tick);
                tickCount++;
            }

        }.runTaskTimerAsynchronously(plugin, 0, 1);
    }
}