package net.qilla.qRPG.events;

import com.google.common.base.Preconditions;
import io.papermc.paper.math.BlockPosition;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class RPGEvent {

    private final Plugin plugin;
    private final BlockPosition blockPos;
    private final World world;

    public RPGEvent(@NotNull Plugin plugin,@NotNull BlockPosition blockPos, @NotNull World world) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(blockPos, "Position cannot be null");
        Preconditions.checkNotNull(world, "World cannot be null");

        this.plugin = plugin;
        this.blockPos = blockPos;
        this.world = world;
    }

    public abstract void init();

    public @NotNull Plugin plugin() {
        return plugin;
    }

    public @NotNull BlockPosition blockPos() {
        return blockPos;
    }

    public @NotNull World world() {
        return world;
    }
}