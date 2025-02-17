package net.qilla.qRPG.events.meteor;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.qilla.qlibrary.util.tools.BlockUtil;
import net.qilla.qlibrary.util.tools.PlayerUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.*;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MeteorTrail {

    private static final int TRAIL_SIZE = 4;
    private static final List<CraftBlockState> SMOKE_MATERIALS = BlockUtil.getCraftState(List.of(
            Material.WHITE_WOOL,
            Material.GRAY_WOOL,
            Material.LIGHT_GRAY_WOOL,
            Material.WHITE_STAINED_GLASS,
            Material.GRAY_STAINED_GLASS,
            Material.LIGHT_GRAY_STAINED_GLASS
    ));

    private final Plugin plugin;
    private final List<Position> trailCollection = Collections.synchronizedList(new ArrayList<>());
    private boolean isCleaning = false;

    public MeteorTrail(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void tickSmoke(@NotNull Collection<Player> playersInvolved, @NotNull BlockPosition position, float gapChance) {
        Preconditions.checkNotNull(position, "Location cannot be null");
        Preconditions.checkNotNull(playersInvolved, "Collection cannot be null");

        for(int x = -TRAIL_SIZE; x <= TRAIL_SIZE; x++) {
            for(int y = -TRAIL_SIZE; y <= TRAIL_SIZE; y++) {
                for(int z = -TRAIL_SIZE; z <= TRAIL_SIZE; z++) {
                    if(x * x + y * y + z * z > TRAIL_SIZE * TRAIL_SIZE) continue;
                    if(Math.random() > gapChance) continue;

                    Position newPos = position.offset(x, y, z);
                    PlayerUtil.sendPacket(playersInvolved, new ClientboundBlockUpdatePacket(new BlockPos(
                            newPos.blockX(), newPos.blockY(), newPos.blockZ()),
                            SMOKE_MATERIALS.get(RandomUtil.between(0, SMOKE_MATERIALS.size() - 1)).getHandle()));
                    trailCollection.add(newPos);
                }
            }
        }
    }

    public void trailCleanup(@NotNull Collection<Player> playersInvolved, @NotNull World world) {
        isCleaning = true;
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if(!isCleaning && trailCollection.isEmpty()) {
                task.cancel();
                return;
            }

            for(int i = 0; i < 8; i++) {
                if(trailCollection.isEmpty()) break;
                Position pos = trailCollection.get(RandomUtil.random().nextInt(0, trailCollection.size()));
                Location loc = new Location(world, pos.x(), pos.y(), pos.z());

                PlayerUtil.sendPacket(playersInvolved, new ClientboundBlockUpdatePacket(
                        new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()),
                        ((CraftBlockState) world.getBlockAt(loc).getState()).getHandle()));
                trailCollection.remove(pos);
                world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, 1, RandomUtil.between(0.5f, 1.5f));
                world.spawnParticle(Particle.SMOKE, loc, 20, 0.25, 0.25, 0.25, 0);
            }
        }, 0, 1);
    }

    public void endCleanup() {
        isCleaning = false;
    }

    public boolean isCleaning() {
        return isCleaning;
    }
}