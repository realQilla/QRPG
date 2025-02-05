package net.qilla.qRPG.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.qilla.qlibrary.util.tools.CurveUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public class TestCommand {

    private static final String COMMAND = "test";


    private final Plugin plugin;
    private final Commands commands;


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
        Location location = player.getLocation();
        CraftPlayer craftPlayer = (CraftPlayer) player;

        final Vec3 p0 = new Vec3(
                0, 0, 0
        );
        final Vec3 p1 = new Vec3(
                0, 1, 0
        );
        final Vec3 p2 = new Vec3(
                RandomUtil.offsetFrom(0, 4), 2, RandomUtil.offsetFrom(0, 4)
        );

        new BukkitRunnable() {
            final List<Position> blocks = new ArrayList<>();

            Position curPosition = location;
            int tick = 0;


            @Override
            public void run() {
                if(tick > 40) {

                    this.cancel();
                    return;
                }

                double normalizedTick = tick / (double) 20;

                Vec3 delta = CurveUtil.calculateBezier(p0, p1, p2, normalizedTick);

                curPosition = curPosition.offset(delta.x(), delta.y(), delta.z());

                craftPlayer.getHandle().connection.sendPacket(new ClientboundBlockUpdatePacket(
                        new BlockPos(curPosition.blockX(), curPosition.blockY(), curPosition.blockZ()),
                        Blocks.STONE.defaultBlockState())
                );
                blocks.add(curPosition);

                tick++;
            }
        }.runTaskTimer(plugin, 0, 1);
        return Command.SINGLE_SUCCESS;
    }
}