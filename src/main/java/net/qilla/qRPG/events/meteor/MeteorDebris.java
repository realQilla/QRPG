package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.Position;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.qilla.qlibrary.util.tools.CurveUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class MeteorDebris {

    private static final int LIFESPAN = 80;

    private final Meteor meteor;

    public MeteorDebris(@NotNull Meteor meteor) {
        Preconditions.checkNotNull(meteor, "Meteor cannot be null");

        this.meteor = meteor;
    }

    public void burst(@NotNull List<CraftBlockState> blockStates) {
        Preconditions.checkNotNull(blockStates, "Block states cannot be null");

        Bukkit.getScheduler().runTask(meteor.getPlugin(), () -> {
            for(CraftBlockState blockState : blockStates) {
                List<Pair<MountEntity, DebrisEntity>> debrisList = new ArrayList<>();

                Position curPosition = meteor.getCrashPos().offset(
                        RandomUtil.offsetFrom(0, 6),
                        RandomUtil.offsetFrom(-3, 3),
                        RandomUtil.offsetFrom(0, 6));
                MountEntity mount = new MountEntity(meteor.getCraftServer(), meteor.getLevel(), curPosition);
                mount.setLifespan(RandomUtil.between((int) (LIFESPAN * 0.65f), (int) (LIFESPAN * 0.95f)));
                DebrisEntity debris = new DebrisEntity(meteor.getCraftServer(), meteor.getLevel(), curPosition, blockState);
                debris.setLifespan(RandomUtil.between((int) (LIFESPAN * 0.65f), (int) (LIFESPAN * 0.95f)));
                debrisList.add(Pair.of(mount, debris));
                meteor.getLevel().addFreshEntity(mount);
                meteor.getLevel().addFreshEntity(debris);

                debris.startRiding(mount);
                displaceDebris(curPosition, mount, debris);
            }
        });
    }

    private void displaceDebris(@NotNull Position position, @NotNull MountEntity debrisMount, @NotNull DebrisEntity debrisDisplay) {
        new BukkitRunnable() {

            Position curPosition = position;

            final Vec3 p0 = new Vec3(
                    0, 0, 0
            );
            final Vec3 p1 = new Vec3(
                    0, RandomUtil.between(0, 2), 0
            );
            final Vec3 p2 = new Vec3(
                    RandomUtil.offsetFrom(0, 4), RandomUtil.between(0, 4), RandomUtil.offsetFrom(0, 4)
            );
            int tick = 0;

            @Override
            public void run() {
                if(tick > LIFESPAN) {
                    this.cancel();
                    return;
                }

                double normalizedTick = tick / (double) 5;

                Vec3 delta = CurveUtil.calculateBezier(p0, p1, p2, normalizedTick);
                curPosition = curPosition.offset(delta.x(), delta.y(), delta.z());

                debrisMount.setDeltaMovement(delta.x(), delta.y(), delta.z());

                debrisMount.setPos(curPosition.x(), curPosition.y(), curPosition.z());

                debrisDisplay.setPos(curPosition.x(), curPosition.y(), curPosition.z());

                tick++;
            }

        }.runTaskTimer(meteor.getPlugin(), 0, 1);
    }
}