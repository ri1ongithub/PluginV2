package fr.openmc.core.utils.customitems.buttons;

import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.core.utils.customitems.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RefuseButton extends CustomItem {
    public RefuseButton() {
        super("omc_menus:refuse_btn");
    }

    private ItemStack format(ItemStack initial) {
        ItemMeta meta = initial.getItemMeta();
        meta.displayName(Component.translatable("omc.menus.buttons.decline").decoration(TextDecoration.ITALIC, false));
        initial.setItemMeta(meta);
        return initial;
    }

    @Override
    public ItemStack getVanilla() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        return format(item);
    }

    @Override
    public ItemStack getItemsAdder() {
        CustomStack stack = CustomStack.getInstance("omc_menus:refuse_btn");
        if (stack != null) {
            return format(stack.getItemStack());
        } else {
            return null;
        }
    }
}
