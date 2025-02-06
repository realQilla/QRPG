package net.qilla.qRPG.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.qilla.qRPG.events.AirdropEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AirdropCommand {

    private static final String COMMAND = "airdrop";
    private static final String AMOUNT = "amount";

    private final Plugin plugin;
    private final Commands commands;


    public AirdropCommand(Plugin plugin, Commands commands) {
        this.plugin = plugin;
        this.commands = commands;
    }


    public void register() {
        commands.register(Commands.literal(COMMAND)
                .requires(source -> source.getSender() instanceof Player player && player.isOp())
                .executes(this::airdrop)
                .then(Commands.argument(AMOUNT, IntegerArgumentType.integer())
                        .executes(this::airdropAmount))
                .build());
    }

    private int airdrop(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();

        AirdropEvent event = new AirdropEvent(plugin, player.getLocation().toBlock(), player.getWorld());
        event.init();

        return Command.SINGLE_SUCCESS;
    }

    private int airdropAmount(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        Integer amount = context.getArgument(AMOUNT, Integer.class);

        for(int i = 0; i < amount; i++) {
            AirdropEvent event = new AirdropEvent(plugin, player.getLocation().toBlock(), player.getWorld());
            event.init();
        }

        return Command.SINGLE_SUCCESS;
    }
}