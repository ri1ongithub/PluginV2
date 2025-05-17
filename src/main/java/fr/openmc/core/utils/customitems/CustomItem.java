package fr.openmc.core.utils.customitems;

import fr.openmc.core.utils.api.ItemAdderApi;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

public abstract class CustomItem {
    public abstract ItemStack getVanilla();
    public abstract ItemStack getItemsAdder();
    @Getter private final String name;

    public CustomItem(String name) {
        this.name = name;
        CustomItemRegistry.register(name, this);
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (anotherObject instanceof ItemStack anotherItem) {
            CustomItem citem = CustomItemRegistry.getByItemStack(anotherItem);

            if (citem == null) return false;
            return citem.getName().equals(this.getName());
        }

        if (anotherObject instanceof String name) {
            return this.getName().equals(name);
        }

        if (anotherObject instanceof CustomItem citem) {
            return citem.getName().equals(this.getName());
        }

        return false;
    }

    /**
     * Order:
     * 1. ItemsAdder
     * 2. Vanilla
     * @return Best ItemStack to use for the server
     */
    public ItemStack getBest() {
        ItemStack item = null;
        if (ItemAdderApi.hasItemAdder()) item = getItemsAdder();

        if (item == null) {
            item = getVanilla();
        }

        return item;
    }
}