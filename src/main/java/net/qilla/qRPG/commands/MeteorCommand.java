package net.qilla.qRPG.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.qilla.qRPG.events.MeteorEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MeteorCommand {

    private static final String COMMAND = "meteor";

    private static final String AMOUNT = "amount";

    private final Plugin plugin;
    private final Commands commands;


    public MeteorCommand(Plugin plugin, Commands commands) {
        this.plugin = plugin;
        this.commands = commands;
    }


    public void register() {
        commands.register(Commands.literal(COMMAND)
                .requires(source -> source.getSender() instanceof Player player && player.isOp())
                .executes(this::meteor)
                .then(Commands.argument(AMOUNT, IntegerArgumentType.integer())
                        .executes(this::meteorAmount))
                .build());
    }

    private int meteor(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();

        MeteorEvent event = new MeteorEvent(plugin, player.getLocation());
        event.init();

        return Command.SINGLE_SUCCESS;
    }

    private int meteorAmount(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        Integer amount = context.getArgument(AMOUNT, Integer.class);

        for(int i = 0; i < amount; i++) {
            MeteorEvent event = new MeteorEvent(plugin, player.getLocation());
            event.init();
        }

        return Command.SINGLE_SUCCESS;
    }
}