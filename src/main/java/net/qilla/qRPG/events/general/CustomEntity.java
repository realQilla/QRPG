package net.qilla.qRPG.events.general;

import org.bukkit.craftbukkit.entity.CraftEntity;
import org.jetbrains.annotations.NotNull;

public interface CustomEntity<T extends CraftEntity> {

    @NotNull T getCraft();

    void create();
}
