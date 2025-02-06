package net.qilla.qRPG.events;

import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.qilla.qRPG.events.airdrop.Airdrop;
import net.qilla.qRPG.events.general.PointHolder;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AirdropEvent extends RPGEvent{

    private PointHolder aHighlight;
    private PointHolder bHighlight;
    private PointHolder cHighlight;
    private PointHolder dHighlight;

    public AirdropEvent(@NotNull Plugin plugin, @NotNull BlockPosition blockPos, @NotNull World world) {
        super(plugin, blockPos, world);

    }

    public void init() {
        Airdrop airDrop = new Airdrop(super.plugin(), super.blockPos(), super.world());
        BlockPosition pointA = airDrop.pointA();
        BlockPosition pointB = airDrop.pointB();
        BlockPosition pointC = airDrop.pointC();
        BlockPosition pointD = airDrop.pointD();

        this.aHighlight = new PointHolder(super.plugin(), pointA, super.world(), super.world().getPlayers(), Color.LIME);
        this.bHighlight = new PointHolder(super.plugin(), pointB, super.world(), super.world().getPlayers(), Color.RED);
        this.cHighlight = new PointHolder(super.plugin(), pointC, super.world(), super.world().getPlayers(), Color.ORANGE);
        this.dHighlight = new PointHolder(super.plugin(), pointD, super.world(), super.world().getPlayers(), Color.GREEN);

        CompletableFuture<Boolean> ejectFuture = new CompletableFuture<>();
        CompletableFuture<Boolean> landFuture = new CompletableFuture<>();

        airDrop.initAirborne(ejectFuture, landFuture);
        super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<yellow><red><bold>WARNING</red> There will be an <aqua><bold>AIRDROP</aqua> released somewhere near " +
                super.blockPos().blockX() + ", " + super.blockPos().blockY() + ", " + super.blockPos().blockZ()));

        aHighlight.create();
        bHighlight.create();
        cHighlight.create();
        dHighlight.create();

        ejectFuture.thenAccept(result -> {
            super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The airdrop has been released and will be landing  at " + pointD.blockX() + ", " + pointD.blockY() + ", " + pointD.blockZ()));
            aHighlight.remove();
        });

        landFuture.thenAccept(result -> {
            if(result) {
                super.plugin().getServer().sendMessage(MiniMessage.miniMessage().deserialize("<green>The airdrop has successfully landed!"));
                bHighlight.remove();
                cHighlight.remove();
                dHighlight.remove();
            }
        });

        landFuture.thenAccept(result -> {

        });
    }
}
