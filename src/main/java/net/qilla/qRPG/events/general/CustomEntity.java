package net.qilla.qRPG.events.general;

import org.bukkit.craftbukkit.entity.CraftEntity;

public interface CustomEntity<T extends CraftEntity> {

    T getCraft();

    void create();
}
