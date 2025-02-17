package net.qilla.qRPG;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.qilla.qRPG.commands.AirdropCommand;
import net.qilla.qRPG.commands.MeteorCommand;
import net.qilla.qRPG.commands.TestCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EventListener;

public final class QRPG extends JavaPlugin {

    LifecycleEventManager<Plugin> lifecycleEventManager;

    @Override
    public void onEnable() {
        this.lifecycleEventManager = super.getLifecycleManager();

        this.registerCommands();
        this.registerEvents();
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    private void registerEvents() {

    }

    private void registerCommands() {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new MeteorCommand(this, event.registrar()).register();
            new AirdropCommand(this, event.registrar()).register();
            TestCommand testCommand = new TestCommand(this, event.registrar());
            testCommand.register();
            super.getServer().getPluginManager().registerEvents(testCommand, this);
        });
    }

    public static QRPG getInstance() {
        return JavaPlugin.getPlugin(QRPG.class);
    }
}
