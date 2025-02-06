package net.qilla.qRPG.events.general;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.qilla.qlibrary.util.tools.PlayerUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Collection;

public class PointHolder {

    private final ServerLevel level;
    private final CraftServer craftServer;
    private final BlockPosition blockPos;
    private final CraftBlockDisplay pointHighlight;
    private final Collection<Player> audience;

    public PointHolder(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world, @NotNull Collection<Player> audience, @NotNull Color color) {
        this.level = ((CraftWorld) world).getHandle();
        this.craftServer = (CraftServer) plugin.getServer();
        this.blockPos = blockPos;
        this.pointHighlight = getPointDisplay(color);
        this.audience = audience;
    }

    public PointHolder(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world, @NotNull Collection<Player> audience) {
        this(plugin, blockPos, world, audience, Color.WHITE);
    }

    public void create() {
        PlayerUtil.sendPacket(audience, new ClientboundAddEntityPacket(pointHighlight.getHandle(), 0,
                new BlockPos(blockPos.blockX(), blockPos.blockY(), blockPos.blockZ())));

        PlayerUtil.sendPacket(audience, new ClientboundSetEntityDataPacket(pointHighlight.getEntityId(),
                pointHighlight.getHandle().getEntityData().packAll()));
    }

    public void remove() {
        PlayerUtil.sendPacket(audience, new ClientboundRemoveEntitiesPacket(pointHighlight.getEntityId()));
    }

    public @NotNull CraftBlockDisplay getPointDisplay(@NotNull Color color) {
        Preconditions.checkNotNull(color, "Color cannot be null");

        CraftBlockDisplay display = new CraftBlockDisplay(craftServer,
                EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND));
        display.setGlowing(true);
        display.setGlowColorOverride(color);
        display.setBlock(Material.LIGHT_GRAY_CONCRETE.createBlockData());
        display.setTransformation(new Transformation(
                new Vector3f(0.05f, 0.05f, 0.05f),
                new Quaternionf(),
                new Vector3f(0.90f, 0.90f, 0.90f),
                new Quaternionf()
        ));
        display.getHandle().setPos(blockPos.x(), blockPos.y(), blockPos.z());
        return display;
    }
}