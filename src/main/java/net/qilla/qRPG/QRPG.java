package net.qilla.qRPG;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.qilla.qRPG.commands.MeteorCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
        });
    }
}
