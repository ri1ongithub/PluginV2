package fr.openmc.core.features.corporation.shops;

import fr.openmc.api.menulib.Menu;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.core.features.corporation.MethodState;
import fr.openmc.core.features.corporation.manager.CompanyManager;
import fr.openmc.core.features.corporation.manager.ShopBlocksManager;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.ItemUtils;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@Getter
public class Shop {

    private final ShopOwner owner;
    private final EconomyManager economyManager = EconomyManager.getInstance();
    private final ShopBlocksManager blocksManager = ShopBlocksManager.getInstance();
    private final List<ShopItem> items = new ArrayList<>();
    private final List<ShopItem> sales = new ArrayList<>();
    private final Map<Long, Supply> suppliers = new HashMap<>();
    private final int index;
    private final UUID uuid;

    private double turnover = 0;

    public Shop(ShopOwner owner, int index) {
        this.owner = owner;
        this.index = index;
        this.uuid  = UUID.randomUUID();
    }

    public Shop(ShopOwner owner, int index, UUID uuid) {
        this.owner = owner;
        this.index = index;
        this.uuid = uuid;
    }

    /**
     * requirement : item need the uuid of the player who restock the shop

     * quand un item est vendu un partie du profit reviens a celui qui a approvisionner
     * @param shop the shop we want to check the stock
     */
    public static void checkStock(Shop shop) {
        ShopBlocksManager blocksManager = ShopBlocksManager.getInstance();
        Multiblock multiblock = blocksManager.getMultiblock(shop.getUuid());

        if (multiblock == null) {
            return;
        }

        Block stockBlock = multiblock.getStockBlock().getBlock();
        if (stockBlock.getType() != Material.BARREL) {
            blocksManager.removeShop(shop);
            return;
        }

        if (stockBlock.getState() instanceof Barrel barrel) {

            Inventory inventory = barrel.getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }

                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta == null) {
                    continue;
                }

                PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
                if (dataContainer.has(CompanyManager.SUPPLIER_KEY, PersistentDataType.STRING)) {

                    String supplierUUID = dataContainer.get(CompanyManager.SUPPLIER_KEY, PersistentDataType.STRING);
                    if (supplierUUID == null) {
                        continue;
                    }

                    List<UUID> possibleSuppliers = new ArrayList<>();
                    if (shop.getOwner().isCompany()) {
                        possibleSuppliers.addAll(shop.getOwner().getCompany().getAllMembers());
                    }

                    if (shop.getOwner().isPlayer()) {
                        possibleSuppliers.add(shop.getOwner().getPlayer());
                    }

                    if (!possibleSuppliers.contains(UUID.fromString(supplierUUID))) {
                        continue;
                    }

                    boolean supplied = shop.supply(item, UUID.fromString(supplierUUID));
                    if (supplied) inventory.remove(item);
                }
            }
        }
    }


    public String getName() {
        //TODO CacheOfflinePlayer
        return owner.isCompany() ? ("Shop #" + index) : Bukkit.getOfflinePlayer(owner.getPlayer()).getName() + "'s Shop";
    }

    public UUID getSupremeOwner() {
        return owner.isCompany() ? owner.getCompany().getOwner().getPlayer() : owner.getPlayer();
    }

    /**
     * know if the uuid is the shop owner
     *
     * @param uuid the uuid we check
     */
    public boolean isOwner(UUID uuid) {
        if (owner.isCompany()) {
            return owner.getCompany().isOwner(uuid);
        }
        return owner.getPlayer().equals(uuid);
    }

    /**
     * add an item to the shop
     *
     * @param itemStack the item
     * @param price the price
     * @param amount the amount of it
     */
    public boolean addItem(ItemStack itemStack, double price, int amount) {
        ShopItem item = new ShopItem(itemStack, price);
        for (ShopItem shopItem : items) {
            if (shopItem.getItem().isSimilar(itemStack)) {
                return true;
            }
        }
        if (amount>1){
            item.setAmount(amount);
        }
        items.add(item);
        return false;
    }

    /**
     * get an item from the shop
     *
     * @param index index of the item
     */
    public ShopItem getItem(int index) {
        return items.get(index);
    }

    /**
     * remove an item from the shop
     *
     * @param item the item to remove
     */
    public void removeItem(ShopItem item) {
        items.remove(item);
    }

    /**
     * update the amount of all the item in the shop according to the items in the barrel
     */
    public boolean supply(ItemStack item, UUID supplier) {
        for (ShopItem shopItem : items) {
            if (shopItem.getItem().getType().equals(item.getType())) {
                shopItem.setAmount(shopItem.getAmount() + item.getAmount());
                suppliers.put(System.currentTimeMillis(), new Supply(supplier, shopItem.getItemID(), item.getAmount()));
                return true;
            }
        }
        return false;
    }

    /**
     * get the shop Icon
     *
     * @param item the item to buy
     * @param amount the amount of it
     * @param buyer the player who buy
     * @return a MethodState
     */
    public MethodState buy(ShopItem item, int amount, Player buyer) {
        if (!ItemUtils.hasAvailableSlot(buyer)) {
            return MethodState.SPECIAL;
        }
        if (amount > item.getAmount()) {
            return MethodState.WARNING;
        }
        if (isOwner(buyer.getUniqueId())) {
            return MethodState.FAILURE;
        }
        if (!economyManager.withdrawBalance(buyer.getUniqueId(), item.getPrice(amount))) return MethodState.ERROR;
        double basePrice = item.getPrice(amount);
        item.setAmount(item.getAmount() - amount);
        turnover += item.getPrice(amount);
        if (owner.isCompany()) {
            int amountToBuy = amount;
            double price = item.getPrice(amount);
            double companyCut = price * owner.getCompany().getCut();
            double suppliersCut = price - companyCut;
            boolean supplied = false;
            List<Supply> supplies = new ArrayList<>();
            for (Map.Entry<Long, Supply> entry : suppliers.entrySet()) {
                if (entry.getValue().getItemId().equals(item.getItemID())) {
                    supplies.add(entry.getValue());
                }
            }
            if (!supplies.isEmpty()) {
                supplied = true;
                for (Supply supply : supplies) {
                    if (amountToBuy == 0) break;
                    if (amountToBuy >= supply.getAmount()) {
                        amountToBuy -= supply.getAmount();
                        removeLatestSupply();
                        double supplierCut = suppliersCut * ((double) supply.getAmount() / amount);
                        EconomyManager.addBalance(supply.getSupplier(), supplierCut);
                        Player supplier = Bukkit.getPlayer(supply.getSupplier());
                        if (supplier!=null){
                            MessagesManager.sendMessage(supplier, Component.text(buyer.getName() + " a acheté " + amount + " " + item.getItem().getType() + " pour " + basePrice + EconomyManager.getEconomyIcon() + ", vous avez reçu : " + supplierCut + EconomyManager.getEconomyIcon()), Prefix.SHOP, MessageType.SUCCESS, false);
                        }
                    }
                    else {
                        supply.setAmount(supply.getAmount() - amountToBuy);
                        double supplierCut = suppliersCut * ((double) amountToBuy / amount);
                        EconomyManager.addBalance(supply.getSupplier(), supplierCut);
                        Player supplier = Bukkit.getPlayer(supply.getSupplier());
                        if (supplier!=null){
                            MessagesManager.sendMessage(supplier, Component.text(buyer.getName() + " a acheté " + amount + " " + item.getItem().getType() + " pour " + basePrice + EconomyManager.getEconomyIcon() + ", vous avez reçu : " + supplierCut + EconomyManager.getEconomyIcon()), Prefix.SHOP, MessageType.SUCCESS, false);
                        }
                        break;
                    }
                }
            }
            if (!supplied) {
                return MethodState.ESCAPE;
            }
            owner.getCompany().deposit(companyCut, buyer, "Vente", getName());
        }
        else {
            EconomyManager.addBalance(owner.getPlayer(), item.getPrice(amount));
            Player player = Bukkit.getPlayer(owner.getPlayer());
            if (player!=null){
                MessagesManager.sendMessage(player, Component.text(buyer.getName() + " a acheté " + amount + " " + item.getItem().getType() + " pour " + item.getPrice(amount) + EconomyManager.getEconomyIcon() + ", l'argent vous a été transféré !"), Prefix.SHOP, MessageType.SUCCESS, false);
            }
        }
        ItemStack toGive = item.getItem().clone();
        toGive.setAmount(amount);
        List<ItemStack> stacks = ItemUtils.splitAmountIntoStack(toGive);
        for (ItemStack stack : stacks) {
            buyer.getInventory().addItem(stack);
        }
        sales.add(item.copy().setAmount(amount));
        return MethodState.SUCCESS;
    }

    private void removeLatestSupply() {
        long latest = 0;
        Supply supply = null;
        for (Map.Entry<Long, Supply> entry : suppliers.entrySet()) {
            if (entry.getKey() > latest) {
                latest = entry.getKey();
                supply = entry.getValue();
            }
        }
        if (supply != null) {
            suppliers.remove(latest);
        }
    }

    /**
     * get the shop Icon
     *
     * @param menu the menu
     * @param fromShopMenu know if it from shopMenu
     */
    public ItemBuilder getIcon(Menu menu, boolean fromShopMenu) {
        return new ItemBuilder(menu, fromShopMenu ? Material.GOLD_INGOT : Material.BARREL, itemMeta -> {
            itemMeta.displayName(Component.text(fromShopMenu ? "Informations" : getName())
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Chiffre d'affaire : ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(EconomyManager.getFormattedNumber(turnover))));
            lore.add(Component.text("Ventes : ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(sales.size()).color(NamedTextColor.WHITE)));
            if (!fromShopMenu)
                lore.add(Component.text("Cliquez pour accéder au shop").color(NamedTextColor.GRAY));
            itemMeta.lore(lore);
        });
    }

    public int getAllItemsAmount() {
        int amount = 0;
        for (ShopItem item : items) {
            amount += item.getAmount();
        }
        return amount;
    }

    /**
     * get the shop with what player looking
     *
     * @param player the player we check
     * @param shopBlocksManager the permission
     * @param onlyCash if we only check the cach register
     */
    public static UUID getShopPlayerLookingAt(Player player, ShopBlocksManager shopBlocksManager, boolean onlyCash) {
        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null) return null;

        if (targetBlock.getType() != Material.BARREL && targetBlock.getType() != Material.OAK_SIGN && targetBlock.getType() != Material.BARRIER) return null;
        if (onlyCash) {
            if (targetBlock.getType() != Material.OAK_SIGN && targetBlock.getType() != Material.BARRIER) return null;
        }
        Shop shop = shopBlocksManager.getShop(targetBlock.getLocation());
        if (shop == null) return null;
        return shop.getUuid();
    }

    @Getter
    public static class Multiblock {

        private final Location stockBlock;
        private final Location cashBlock;

        public Multiblock(Location stockBlock, Location cashBlock) {
            this.stockBlock = stockBlock;
            this.cashBlock = cashBlock;
        }
    }
}