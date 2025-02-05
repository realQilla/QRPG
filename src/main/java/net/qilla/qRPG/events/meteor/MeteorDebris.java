package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.Position;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MeteorDebris {

    private static final int LIFESPAN = 80;

    private final Meteor meteor;

    public MeteorDebris(@NotNull Meteor meteor) {
        Preconditions.checkNotNull(meteor, "Meteor cannot be null");

        this.meteor = meteor;
    }

    public void burst(@NotNull List<CraftBlockData> blockDataList, Collection<Player> playersInvolved) {
        Preconditions.checkNotNull(blockDataList, "Block states cannot be null");

        for(CraftBlockData blockData : blockDataList) {
            List<DebrisHolder> debrisList = new ArrayList<>();

            Position curPosition = meteor.getCrashPos().offset(
                    RandomUtil.offsetFrom(0, 6),
                    RandomUtil.offsetFrom(-3, 3),
                    RandomUtil.offsetFrom(0, 6));
            DebrisHolder debrisHolder = new DebrisHolder(playersInvolved, meteor.getLevel(), meteor.getCraftServer(), curPosition, blockData);
            debrisHolder.setLifespan(RandomUtil.between((int) (LIFESPAN * 0.65f), (LIFESPAN)));
            debrisList.add(debrisHolder);

            displaceDebris(debrisList);
        }
    }

    private void displaceDebris(List<DebrisHolder> debrisList) {
        new BukkitRunnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if(tickCount > LIFESPAN) {
                    debrisList.forEach(debrisHolder -> {
                        if(debrisHolder.isValid()) debrisHolder.remove();
                    });
                    this.cancel();
                    return;
                }

                debrisList.forEach(DebrisHolder::tick);
                tickCount++;
            }

        }.runTaskTimerAsynchronously(meteor.getPlugin(), 0, 1);
    }
}