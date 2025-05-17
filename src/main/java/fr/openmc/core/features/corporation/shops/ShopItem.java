package fr.openmc.core.features.corporation.shops;

import fr.openmc.core.utils.ItemUtils;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

@Getter
public class ShopItem {

    private final UUID itemID = UUID.randomUUID();
    private final ItemStack item;
    private final double pricePerItem;
    private double price;
    private int amount;

    public ShopItem(ItemStack item, double pricePerItem) {
        this.item = item.clone();
        this.pricePerItem = pricePerItem;
        this.item.setAmount(1);
        this.price = pricePerItem * amount;
        this.amount = 0;
    }

    /**
     * get the name of an item
     *
     * @param amount the new amount of the item
     * @return default the ShopItem
     */
    public ShopItem setAmount(int amount) {
        this.amount = amount;
        this.price = pricePerItem * amount;
        return this;
    }

    /**
     * copy an ShopItem
     *
     * @return a copy of the ShopItem
     */
    public ShopItem copy() {
        return new ShopItem(item.clone(), pricePerItem);
    }

    /**
     * get the price of a certain amount of an item
     *
     * @param amount amount of item
     * @return a price
     */
    public double getPrice(int amount) {
        return pricePerItem * amount;
    }

    /**
     * get the name of an item
     *
     * @param itemStack the item
     * @return default name if the item has no custom name
     */
    public static String getItemName(ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                return itemMeta.getDisplayName();
            }
        }
        // If no custom name, return default name
        return String.valueOf(ItemUtils.getDefaultItemName(itemStack));
    }
}
