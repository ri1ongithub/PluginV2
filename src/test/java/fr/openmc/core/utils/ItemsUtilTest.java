package fr.openmc.core.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;

public class ItemsUtilTest {

    @Test
    @DisplayName("getItemTranslation with ItemStack")
    public void testGetTranslationWithStack() {
        Assertions.assertEquals(
                "block.minecraft.chest",
                ItemUtils.getItemTranslation(new ItemStack(Material.CHEST)).key()
        );
    }

    @Test
    @DisplayName("getItemTranslation with Material")
    public void testGetTranslationWithMaterial() {
        Assertions.assertEquals(
                "block.minecraft.dirt",
                ItemUtils.getItemTranslation(Material.DIRT).key()
        );
    }

}
