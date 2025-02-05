package net.qilla.qRPG.events;

import io.papermc.paper.math.Position;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.qilla.qRPG.events.meteor.Meteor;
import net.qilla.qRPG.events.meteor.MeteorTrail;
import net.qilla.qlibrary.util.tools.PlayerUtil;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class MeteorEvent {

    private final Plugin plugin;
    private final Location location;
    private final CraftBlockDisplay posHighlight;

    public MeteorEvent(@NotNull Plugin plugin, @NotNull Location location) {
        this.plugin = plugin;
        this.location = location;
        this.posHighlight = this.getPosHighlight(location);
    }

    public void init() {
        Meteor meteor = new Meteor(plugin, new MeteorTrail(plugin), location);
        Position crashPos = meteor.getCrashPos();

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        meteor.initAirborne(future);

        plugin.getServer().sendMessage(MiniMessage.miniMessage().deserialize("<gold><red><bold>WARNING</red> There will be a meteorite landing at: " + crashPos.blockX() + ", " + crashPos.blockY() + ", " + crashPos.blockZ()));
        this.createPosHighlightPacket(posHighlight, location.getWorld(), crashPos);

        future.thenAccept(result -> {
            if(result) {
                plugin.getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The meteorite has landed at " + crashPos.blockX() + ", " + crashPos.blockY() + ", " + crashPos.blockZ()));
                this.removePosHighlightPacket(posHighlight);
            } else {
                plugin.getServer().sendMessage(MiniMessage.miniMessage().deserialize("<red>The meteorite has phased through the world and fallen into the void! :)"));
            }
        });
    }

    private void createPosHighlightPacket(@NotNull CraftBlockDisplay display, @NotNull World world, @NotNull Position position) {
        Collection<Player> playersInvolved = display.getChunk().getPlayersSeeingChunk();

        PlayerUtil.sendPacket(playersInvolved, new ClientboundAddEntityPacket(display.getHandle(), 0,
                        new BlockPos(position.blockX(), position.blockY(), position.blockZ())));

        PlayerUtil.sendPacket(playersInvolved, new ClientboundSetEntityDataPacket(display.getEntityId(),
                        display.getHandle().getEntityData().packAll()));
    }

    private void removePosHighlightPacket(@NotNull CraftBlockDisplay display) {
        Collection<Player> playersInvolved = display.getChunk().getPlayersSeeingChunk();

        PlayerUtil.sendPacket(playersInvolved, new ClientboundRemoveEntitiesPacket(display.getEntityId()));
    }

    private CraftBlockDisplay getPosHighlight(Position position) {
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();

        CraftBlockDisplay display = new CraftBlockDisplay((CraftServer) plugin.getServer(),
                EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND));
        display.setGlowing(true);
        display.setGlowColorOverride(Color.WHITE);
        display.setBlock(Material.LIGHT_GRAY_CONCRETE.createBlockData());
        display.teleport(new Location(location.getWorld(), position.x(), position.y(), position.z()));
        display.setTransformation(new Transformation(
                new Vector3f(0.05f, 0.05f, 0.05f),
                new Quaternionf(),
                new Vector3f(0.90f, 0.90f, 0.90f),
                new Quaternionf()
        ));
        return display;
    }
}