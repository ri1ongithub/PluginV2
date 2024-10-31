package fr.openmc.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class ItemUtils {
    /**
     * Return a {@link TranslatableComponent} from a {@link ItemStack}
     * @param stack ItemStack that get translate
     * @return a {@link TranslatableComponent} that can be translated by client
     */
    public static TranslatableComponent getItemTranslation(ItemStack stack) {
        return Component.translatable(Objects.requireNonNullElse(
                stack.getType().getItemTranslationKey(),
                "block.minecraft.stone"
        ));
    }

    /**
     * Return a {@link TranslatableComponent} from a {@link Material}
     * @param material Material that get translate
     * @return a {@link TranslatableComponent} that can be translated by client
     */
    public static TranslatableComponent getItemTranslation(Material material) {
        return getItemTranslation(new ItemStack(material));
    }
}
