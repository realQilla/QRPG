package net.qilla.qRPG.events;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.qilla.qRPG.events.general.PointHolder;
import net.qilla.qRPG.events.meteor.Meteor;
import net.qilla.qRPG.events.meteor.MeteorTrail;
import net.qilla.qlibrary.util.tools.NumberUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.craftbukkit.boss.CraftBossBar;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

public class MeteorEvent extends RPGEvent {

    private PointHolder posHightlight;

    public MeteorEvent(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world) {
        super(plugin, blockPos, world);

    }

    public void init() {
        Meteor event = new Meteor(super.plugin(), new MeteorTrail(super.plugin()), super.blockPos(), super.world());
        BlockPosition crashPos = event.getEndPos();

        this.posHightlight = new PointHolder(super.plugin(), crashPos, super.world(), super.world().getPlayers(), Color.ORANGE);

        CompletableFuture<Boolean> crashFuture = new CompletableFuture<>();

        event.initAirborne(crashFuture);

        super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<yellow><red><bold>WARNING</red> There will be a <gold><bold>METEORITE</gold> landing somewhere near " +
                super.blockPos().blockX() + ", " + super.blockPos().blockY() + ", " + super.blockPos().blockZ()));
        posHightlight.create();

        crashFuture.thenAccept(result -> {
            if(result) {
                super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The meteorite has landed at " + crashPos.blockX() + ", " + crashPos.blockY() + ", " + crashPos.blockZ()));
                posHightlight.remove();
            } else {
                super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<red>The meteorite has phased through the world and fallen into the void! :)"));
            }
        });

        World world = super.world();
        CraftBossBar bossBar = new CraftBossBar("Incoming Meteorite ", BarColor.values()[RandomUtil.between(0, BarColor.values().length - 1)], BarStyle.SEGMENTED_10);

        world.getPlayers().forEach(player -> {
            CraftPlayer craftPlayer = (CraftPlayer) player;

            craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createAddPacket(bossBar.getHandle()));
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                if(crashFuture.isDone()) {
                    world.getPlayers().forEach(player -> {
                        CraftPlayer craftPlayer = (CraftPlayer) player;

                        craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createRemovePacket(bossBar.getHandle().getId()));
                    });

                    this.cancel();
                    return;
                }

                Vector eventVector = event.getCurPos().toVector();

                world.getPlayers().forEach(player -> {
                    CraftPlayer craftPlayer = (CraftPlayer) player;

                    Vector playerVector = player.getLocation().toVector();
                    float playerYaw = player.getEyeLocation().getYaw();

                    Vector dirToEvent = eventVector.subtract(playerVector).normalize();

                    float eventYaw = (float) (Math.toDegrees(Math.atan2(dirToEvent.getZ(), dirToEvent.getX())) - 90);

                    float relativeYaw = eventYaw - playerYaw;

                    if(relativeYaw <= -180) relativeYaw += 360;
                    if(relativeYaw > 180) relativeYaw -= 360;

                    String icon = NumberUtil.getDirection(relativeYaw).symbol();

                    bossBar.getHandle().setName(PaperAdventure.asVanilla(MiniMessage.miniMessage().deserialize("<gold>" + icon + " <red>Incoming Meteorite</red> " + icon)));
                    bossBar.setProgress(NumberUtil.clamp(0, 1, 1 - event.getProgress()));

                    craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createUpdateNamePacket(bossBar.getHandle()));
                    craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createUpdateProgressPacket(bossBar.getHandle()));
                });
            }
        }.runTaskTimer(super.plugin(), 0, 5);
    }
}