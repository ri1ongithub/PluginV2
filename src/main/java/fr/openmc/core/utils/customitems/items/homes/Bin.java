package fr.openmc.core.utils.customitems.items.homes;

import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.core.utils.customitems.CustomItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Bin extends CustomItem {
    public Bin() {
        super("omc_homes:omc_homes_icon_bin");
    }

    @Override
    public ItemStack getVanilla() {
        return new ItemStack(Material.CHEST);
    }

    @Override
    public ItemStack getItemsAdder() {
        CustomStack stack = CustomStack.getInstance("omc_homes:omc_homes_icon_bin");
        if (stack != null) {
            return stack.getItemStack();
        } else {
            return null;
        }
    }
}
