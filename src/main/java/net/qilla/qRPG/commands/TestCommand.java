package net.qilla.qRPG.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TestCommand implements Listener {

    private static final String COMMAND = "test";

    private final Plugin plugin;
    private final Commands commands;

    private static Block block;
    private static CraftArmorStand entity;

    public TestCommand(Plugin plugin, Commands commands) {
        this.plugin = plugin;
        this.commands = commands;
    }

    public void register() {
        commands.register(Commands.literal(COMMAND)
                .requires(source -> source.getSender() instanceof Player player && player.isOp())
                .executes(this::meteor)
                .build());
    }

    private int meteor(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        World world = player.getWorld();
        Level level = ((CraftWorld) world).getHandle();

        if(entity != null) {
            entity.remove();
            entity = null;
        }

        entity = new CraftArmorStand((CraftServer) plugin.getServer(), EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND));
        entity.getHandle().setPos(player.getLocation().getBlockX() + 0.5, player.getLocation().getBlockY(), player.getLocation().getBlockZ() + 0.5);
        level.addFreshEntity(entity.getHandle());

        new BukkitRunnable() {
            @Override
            public void run() {
                if(!entity.isValid() || block == null) {
                    this.cancel();
                    return;
                }
                double deltaX = block.getX() - entity.getHandle().getX();
                double deltaZ = block.getZ() - entity.getHandle().getZ();
                double deltaY = block.getY() - entity.getHandle().getY();

                float yaw = (float) Math.toDegrees(Math.atan2(deltaZ + 0.5, deltaX + 0.5)) - 90;

                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, distance));

                entity.getHandle().forceSetRotation(yaw, pitch);
                player.sendMessage("Yaw: " + yaw);
                player.sendMessage("Pitch: " + pitch);
            }
        }.runTaskTimer(plugin, 0, 1);

        return Command.SINGLE_SUCCESS;
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        block = event.getBlockPlaced();
    }

    @EventHandler
    private void onBlockBreak(BlockPlaceEvent event) {
        if(block != null && block.getLocation().equals(event.getBlock().getLocation())) block = null;
    }
}