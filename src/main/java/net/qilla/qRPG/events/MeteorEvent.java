package net.qilla.qRPG.events;

import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.qilla.qRPG.events.general.PointHolder;
import net.qilla.qRPG.events.meteor.Meteor;
import net.qilla.qRPG.events.meteor.MeteorTrail;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

public class MeteorEvent extends RPGEvent {

    private PointHolder posHightlight;

    public MeteorEvent(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world) {
        super(plugin, blockPos, world);

    }

    public void init() {
        Meteor meteor = new Meteor(super.plugin(), new MeteorTrail(super.plugin()), super.blockPos(), super.world());
        BlockPosition crashPos = meteor.getCrashPos();

        this.posHightlight = new PointHolder(super.plugin(), crashPos, super.world(), super.world().getPlayers(), Color.ORANGE);

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        meteor.initAirborne(future);

        super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<yellow><red><bold>WARNING</red> There will be a <gold><bold>METEORITE</gold> landing somewhere near " +
                super.blockPos().blockX() + ", " + super.blockPos().blockY() + ", " + super.blockPos().blockZ()));
        posHightlight.create();

        future.thenAccept(result -> {
            if(result) {
                super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The meteorite has landed at " + crashPos.blockX() + ", " + crashPos.blockY() + ", " + crashPos.blockZ()));
                posHightlight.remove();
            } else {
                super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<red>The meteorite has phased through the world and fallen into the void! :)"));
            }
        });
    }
}