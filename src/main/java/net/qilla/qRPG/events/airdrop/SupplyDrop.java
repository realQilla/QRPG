package net.qilla.qRPG.events.airdrop;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.server.level.ServerLevel;
import net.qilla.qRPG.events.general.MountEntity;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SupplyDrop {

    private final int lifespan = RandomUtil.between(1024, 2048);

    private final Plugin plugin;
    private final World world;
    private final CraftServer craftServer;
    private final ServerLevel level;
    private final BlockPosition dropPos;
    private final BlockPosition landPos;
    private MountEntity mountEntity;
    private AirdropEntity airdropEntity;
    private final List<ParachuteEntity> parachuteEntities = new ArrayList<>();

    public SupplyDrop(@NotNull Plugin plugin, @NotNull World world, @NotNull BlockPosition dropPos) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(world, "World cannot be null");
        Preconditions.checkNotNull(dropPos, "Drop position cannot be null");

        this.plugin = plugin;
        this.world = world;
        this.craftServer = (CraftServer) plugin.getServer();
        this.level = ((CraftWorld) world).getHandle();
        this.dropPos = dropPos;
        this.landPos = this.createLandPoint(dropPos);
    }

    public void init(@NotNull CompletableFuture<Boolean> onLand) {
        mountEntity = new MountEntity(craftServer, level, dropPos, lifespan);
        mountEntity.create();
        airdropEntity = new AirdropEntity(craftServer, level, dropPos, lifespan);
        airdropEntity.create();
        airdropEntity.startRiding(mountEntity, true);

        airdropEntity.getCraft().setTransformation(new Transformation(
                new Vector3f(-2.5f, 1, -2.5f),
                new AxisAngle4f(),
                new Vector3f(5, 5, 5),
                new AxisAngle4f()
        ));
        ParachuteEntity parachuteEntity = new ParachuteEntity(craftServer, level, dropPos.offset(0, 24, 0), lifespan);
        parachuteEntity.create();
        parachuteEntity.setLeashedTo(airdropEntity, true);
        parachuteEntity.forceSetRotation(RandomUtil.between(-180, 180), 0);
        parachuteEntities.add(parachuteEntity);

        final double deltaX = landPos.x() - dropPos.x();
        final double deltaY = landPos.y() - dropPos.y();
        final double deltaZ = landPos.z() - dropPos.z();

        new BukkitRunnable() {
            private Position airdropPos = dropPos;
            private int tickCount = 0;

            @Override
            public void run() {
                if(tickCount >= lifespan) {
                    onLand.complete(true);
                    this.cancel();
                    return;
                }

                Vector delta = new Vector(deltaX / lifespan, deltaY / lifespan, deltaZ / lifespan);
                airdropPos = airdropPos.offset(delta.getX(), delta.getY(), delta.getZ());

                mountEntity.setPos(airdropPos.x(), airdropPos.y(), airdropPos.z());

                parachuteEntities.forEach(parachute -> {
                    parachute.setPos(
                            parachute.getX(),
                            parachute.getY() + delta.getY(),
                            parachute.getZ());
                });

                tickCount++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private @NotNull BlockPosition createLandPoint(@NotNull BlockPosition pointC) {
        Preconditions.checkNotNull(pointC, "Point C cannot be null");

        return world.getHighestBlockAt(pointC.blockX(), pointC.blockZ()).getLocation().toBlock();

    }
}