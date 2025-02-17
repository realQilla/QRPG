package net.qilla.qRPG.events;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.qilla.qRPG.events.airdrop.SupplyCarrier;
import net.qilla.qRPG.events.general.PointHolder;
import net.qilla.qlibrary.util.tools.NumberUtil;
import net.qilla.qlibrary.util.tools.RandomUtil;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.craftbukkit.boss.CraftBossBar;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

public class AirdropEvent extends RPGEvent {

    private PointHolder aHighlight;
    private PointHolder bHighlight;
    private PointHolder cHighlight;

    public AirdropEvent(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world) {
        super(plugin, blockPos, world);
    }

    public void init() {
        SupplyCarrier event = new SupplyCarrier(super.plugin(), super.world(), super.blockPos());
        BlockPosition spawnPos = event.getStartPos();
        BlockPosition despawnPas = event.getEndPos();
        BlockPosition dropPos = event.getDropPos();

        this.aHighlight = new PointHolder(super.plugin(), spawnPos, super.world(), super.world().getPlayers(), Color.LIME);
        this.bHighlight = new PointHolder(super.plugin(), despawnPas, super.world(), super.world().getPlayers(), Color.RED);
        this.cHighlight = new PointHolder(super.plugin(), dropPos, super.world(), super.world().getPlayers(), Color.ORANGE);

        CompletableFuture<Boolean> ejectFuture = new CompletableFuture<>();
        CompletableFuture<Boolean> landFuture = new CompletableFuture<>();

        event.init(ejectFuture, landFuture);
        super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<yellow><red><bold>WARNING</red> There will be an <aqua><bold>AIRDROP</aqua> released somewhere near " +
                super.blockPos().blockX() + ", " + super.blockPos().blockZ()));

        aHighlight.create();
        bHighlight.create();
        cHighlight.create();

        ejectFuture.thenAccept(result -> {
            super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The airdrop has been released and will be landing  at " + dropPos.blockX() + ", " + dropPos.blockZ()));
        });

        landFuture.thenAccept(result -> {
            if(result) {
                super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The airdrop has successfully landed!"));
                bHighlight.remove();
                cHighlight.remove();
                aHighlight.remove();
            }
        });

        World world = super.world();
        CraftBossBar bossBar = new CraftBossBar("Incoming Air Drop ", BarColor.values()[RandomUtil.between(0, BarColor.values().length - 1)], BarStyle.SEGMENTED_10);

        world.getPlayers().forEach(player -> {
            CraftPlayer craftPlayer = (CraftPlayer) player;

            craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createAddPacket(bossBar.getHandle()));
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                if(ejectFuture.isDone()) {
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

                    bossBar.getHandle().setName(PaperAdventure.asVanilla(MiniMessage.miniMessage().deserialize("<gold>" + icon + " <red>Incoming Air Drop</red> " + icon)));
                    bossBar.setProgress(NumberUtil.clamp(0, 1, 1 - event.getDropProgress()));

                    craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createUpdateNamePacket(bossBar.getHandle()));
                    craftPlayer.getHandle().connection.sendPacket(ClientboundBossEventPacket.createUpdateProgressPacket(bossBar.getHandle()));
                });
            }
        }.runTaskTimer(super.plugin(), 0, 5);
    }
}