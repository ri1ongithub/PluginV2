package fr.openmc.core.utils.customitems.buttons;

import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.core.utils.customitems.CustomItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TenButton extends CustomItem {

    public TenButton() { super("omc_company:10_btn"); }

    @Override
    public ItemStack getVanilla() {
        return new ItemStack(Material.PAPER);
    }

    @Override
    public ItemStack getItemsAdder() {
        CustomStack stack = CustomStack.getInstance(this.getName());
        if (stack != null) {
            return stack.getItemStack();
        } else {
            return null;
        }
    }
}
