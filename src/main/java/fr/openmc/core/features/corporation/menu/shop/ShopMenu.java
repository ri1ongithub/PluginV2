package fr.openmc.core.features.corporation.menu.shop;

import fr.openmc.api.menulib.Menu;
import fr.openmc.api.menulib.default_menu.ConfirmMenu;
import fr.openmc.api.menulib.utils.InventorySize;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.core.features.corporation.MethodState;
import fr.openmc.core.features.corporation.company.Company;
import fr.openmc.core.features.corporation.manager.CompanyManager;
import fr.openmc.core.features.corporation.manager.PlayerShopManager;
import fr.openmc.core.features.corporation.shops.Shop;
import fr.openmc.core.features.corporation.shops.ShopItem;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.ItemUtils;
import fr.openmc.core.utils.api.ItemAdderApi;
import fr.openmc.core.utils.api.PapiApi;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopMenu extends Menu {

    private final List<ShopItem> items = new ArrayList<>();
    private final CompanyManager companyManager = CompanyManager.getInstance();
    private final PlayerShopManager playerShopManager = PlayerShopManager.getInstance();
    private final Shop shop;
    private final int itemIndex;

    private int amountToBuy = 1;

    public ShopMenu(Player owner, Shop shop, int itemIndex) {
        super(owner);
        this.shop = shop;
        this.itemIndex = itemIndex;
        items.addAll(shop.getItems());
        Shop.checkStock(shop);
    }

    @Override
    public @NotNull String getName() {
        if (PapiApi.hasPAPI() && ItemAdderApi.hasItemAdder()) {
            return PlaceholderAPI.setPlaceholders(getOwner(), "§r§f%img_offset_-11%%img_shop_menu%");
        } else {
            return shop.getName();
        }
    }

    @Override
    public @NotNull InventorySize getInventorySize() {
        if (shop.getOwner().isCompany()){
            Company company = shop.getOwner().getCompany();
            if (company.getAllMembers().contains(getOwner().getUniqueId())){
                return InventorySize.LARGER;
            }
        }
        if (!shop.isOwner(getOwner().getUniqueId()))
            return InventorySize.LARGE;
        return InventorySize.LARGER;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {

    }

    @Override
    public @NotNull Map<Integer, ItemStack> getContent() {
        Company company = null;

        int previousItemSlot;
        int nextItemSlot;
        int closeMenuSlot;

        int purpleSetOne;
        int redRemoveTen;
        int redRemoveOne;
        int itemSlot;
        int greenAddOne;
        int greenAddTen;
        int purpleAddSixtyFour;

        int catalogue;

        boolean ownerItem = false;

        if (shop.getOwner().isCompany()){
            company = shop.getOwner().getCompany();
        }
        if (company == null && shop.isOwner(getOwner().getUniqueId())) {
            previousItemSlot = 39;
            nextItemSlot = 41;
            closeMenuSlot = 40;
            purpleSetOne = 19;
            redRemoveTen = 20;
            redRemoveOne = 21;
            itemSlot = 22;
            greenAddOne = 23;
            greenAddTen = 24;
            purpleAddSixtyFour = 25;
            catalogue = 44;
            ownerItem = true;
        } else if (company != null && company.getAllMembers().contains(getOwner().getUniqueId())) {
            previousItemSlot = 39;
            nextItemSlot = 41;
            closeMenuSlot = 40;
            purpleSetOne = 19;
            redRemoveTen = 20;
            redRemoveOne = 21;
            itemSlot = 22;
            greenAddOne = 23;
            greenAddTen = 24;
            purpleAddSixtyFour = 25;
            catalogue = 44;
            ownerItem = true;
        } else {
            previousItemSlot = 30;
            nextItemSlot = 32;
            closeMenuSlot = 31;
            purpleSetOne = 10;
            redRemoveTen = 11;
            redRemoveOne = 12;
            itemSlot = 13;
            greenAddOne = 14;
            greenAddTen = 15;
            purpleAddSixtyFour = 16;
            catalogue = 35;
        }

        Map<Integer, ItemStack> content = new HashMap<>();

        content.put(previousItemSlot, new ItemBuilder(this, CustomItemRegistry.getByName("menu:previous_page").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§cItem précédent");
        }).setNextMenu(new ShopMenu(getOwner(), shop, onFirstItem() ? itemIndex : itemIndex - 1)));

        content.put(nextItemSlot, new ItemBuilder(this, CustomItemRegistry.getByName("menu:next_page").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§aItem suivant");
        }).setNextMenu(new ShopMenu(getOwner(), shop, onLastItem() ? itemIndex : itemIndex + 1)));

        content.put(closeMenuSlot, new ItemBuilder(this, CustomItemRegistry.getByName("menu:close_button").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§7Fermer");
        }).setCloseButton());

        if (ownerItem)
            putOwnerItems(content);
        content.put(purpleSetOne, new ItemBuilder(this, CustomItemRegistry.getByName("omc_menus:minus_btn").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§5Définir à 1");
        }).setOnClick(inventoryClickEvent -> {
            if (getCurrentItem() == null) return;
            amountToBuy = 1;
            open();
        }));

        content.put(redRemoveTen, new ItemBuilder(this, CustomItemRegistry.getByName("omc_company:10_btn").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§cRetirer 10");
        }).setOnClick(inventoryClickEvent -> {
            if (getCurrentItem() == null) return;
            if (amountToBuy == 1) return;
            if (amountToBuy - 10 < 1) {
                amountToBuy = 1;
            } else {
                amountToBuy -= 10;
            }
            open();
        }));

        content.put(redRemoveOne, new ItemBuilder(this, CustomItemRegistry.getByName("omc_menus:1_btn").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§cRetirer 1");
        }).setOnClick(inventoryClickEvent -> {
            if (getCurrentItem() == null) return;
            if (amountToBuy == 1) return;
            amountToBuy--;
            open();
        }));

        if (getCurrentItem() != null)

            content.put(itemSlot, new ItemBuilder(this, getCurrentItem().getItem(), itemMeta -> {
                itemMeta.setDisplayName("§l§f" + ItemUtils.getItemTranslation(getCurrentItem().getItem()));
                List<String> lore = new ArrayList<>();
                lore.add("§7■ Prix: §c" + (getCurrentItem().getPricePerItem() * amountToBuy) + EconomyManager.getEconomyIcon());
                lore.add("§7■ En stock: " + EconomyManager.getInstance().getFormattedNumber(getCurrentItem().getAmount()));
                lore.add("§7■ Cliquez pour en acheter §f" + amountToBuy);
                itemMeta.setLore(lore);
            }).setNextMenu(new ConfirmMenu(getOwner(), this::buyAccept, this::refuse, List.of(Component.text("§aAcheter")), List.of(Component.text("§cAnnuler l'achat")))));

        content.put(greenAddOne, new ItemBuilder(this, CustomItemRegistry.getByName("omc_menus:1_btn").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§aAjouter 1");
        }).setOnClick(inventoryClickEvent -> {
            if (getCurrentItem() == null) return;
            amountToBuy = getCurrentItem().getAmount()<=amountToBuy ? getCurrentItem().getAmount() : amountToBuy + 1;
            open();
        }));

        content.put(greenAddTen, new ItemBuilder(this, CustomItemRegistry.getByName("omc_company:10_btn").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§aAjouter 10");
        }).setOnClick(inventoryClickEvent -> {
            if (getCurrentItem() == null) return;
            amountToBuy = getCurrentItem().getAmount()<=amountToBuy ? getCurrentItem().getAmount() : amountToBuy + 10;
            open();
        }));

        content.put(purpleAddSixtyFour, new ItemBuilder(this, CustomItemRegistry.getByName("omc_menus:64_btn").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§5Ajouter 64");
        }).setOnClick(inventoryClickEvent -> {
            if (getCurrentItem() == null) return;
            if (amountToBuy == 1) amountToBuy = 64;
            else amountToBuy = getCurrentItem().getAmount()<=amountToBuy ? getCurrentItem().getAmount() : amountToBuy + 64;
            open();
        }));

        content.put(catalogue, new ItemBuilder(this, CustomItemRegistry.getByName("omc_company:company_box").getBest(), itemMeta -> {
            itemMeta.setDisplayName("§7Catalogue");
        }).setNextMenu(new ShopCatalogueMenu(getOwner(), shop, itemIndex)));

        return content;
    }

    private void putOwnerItems(Map<Integer, ItemStack> content) {

        content.put(0, new ItemBuilder(this, CustomItemRegistry.getByName("omc_homes:omc_homes_icon_bin_red").getBest(), itemMeta -> {
            itemMeta.displayName(Component.text("Supprimer le shop")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD));
        }).setNextMenu(new ConfirmMenu(getOwner(), this::accept, this::refuse, 
            List.of(Component.text("Supprimer").color(NamedTextColor.GREEN)), 
            List.of(Component.text("Annuler la suppression").color(NamedTextColor.RED)))));

        content.put(3, new ItemBuilder(this, Material.PAPER, itemMeta -> {
            itemMeta.displayName(Component.text("Vos ventes")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Ventes: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(shop.getSales().size()).color(NamedTextColor.WHITE)));
            lore.add(Component.text("Cliquer pour voir vos ventes sur ce shop").color(NamedTextColor.GRAY));
            itemMeta.lore(lore);
        }).setNextMenu(new ShopSalesMenu(getOwner(), shop, itemIndex)));

        content.put(4, shop.getIcon(this, true));

        content.put(5, new ItemBuilder(this, Material.BARREL, itemMeta -> {
            itemMeta.displayName(Component.text("Voir les stocks")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Stocks: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(shop.getAllItemsAmount()).color(NamedTextColor.WHITE)));
            lore.add(Component.text("Cliquer pour voir les stocks de ce shop").color(NamedTextColor.GRAY));
            itemMeta.lore(lore);
        }).setNextMenu(new ShopStocksMenu(getOwner(), shop, itemIndex)));

        content.put(8, new ItemBuilder(this, Material.LIME_WOOL, itemMeta -> {
            itemMeta.displayName(Component.text("Ce shop vous appartient").color(NamedTextColor.GREEN));
            if (shop.getOwner().isCompany()) {
                if (shop.getOwner().getCompany().getOwner().isCity()) {
                    itemMeta.setLore(List.of(
                            "§7■ Car vous faites partie de l'entreprise"
                    ));
                }
            }
        }));

        content.put(36, new ItemBuilder(this, Material.WRITABLE_BOOK, itemMeta -> {
            itemMeta.setDisplayName("§7Comment utiliser les shops");
        }).setOnClick(inventoryClickEvent -> {

            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            if (meta != null) {
                meta.setTitle("Guide des Shop");
                meta.setAuthor("Nocolm");
                meta.addPage(
                        "Comment utiliser les shops !\n\n" +
                                "§l§6Stock§r :\n" +
                                "1. Utilisez la commande §d§l/shop sell §r§7<prix> §r en tenant l'item en main\n" +
                                "2. Ajoutez les items dans le barril §c§l* le raccourci avec les chiffres ne fonctionnera pas *\n"
                );
                meta.addPage(
                        "3. Ouvrez une fois le shop pour renouveler son stock\n\n" +
                                "Et voilà comment utiliser votre shops\n\n" +
                                "§e▪ Pour plus d'info : /shop help§r"
                );

                book.setItemMeta(meta);
            }
            getOwner().closeInventory();
            getOwner().openBook(book);

            content.remove(44);
        }));
    }

    /**
     * @return the current ShopItem
     */
    private ShopItem getCurrentItem() {
        if (itemIndex < 0 || itemIndex >= items.size()) {
            return null;
        }
        return items.get(itemIndex);
    }

    /**
     * @return true if the menu is on the first item
     */
    private boolean onFirstItem() {
        return itemIndex == 0;
    }

    /**
     * @return true if the menu is on the last item
     */
    private boolean onLastItem() {
        return itemIndex == items.size() - 1;
    }

    private void buyAccept() {
        MethodState buyState = shop.buy(getCurrentItem(), amountToBuy, getOwner());
        if (buyState == MethodState.ERROR) {
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.not_enough_money"), Prefix.SHOP, MessageType.INFO, false);
            getOwner().closeInventory();
            return;
        }
        if (buyState == MethodState.FAILURE) {
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.own_items"), Prefix.SHOP, MessageType.INFO, false);
            getOwner().closeInventory();
            return;
        }
        if (buyState == MethodState.WARNING) {
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.not_enough_stock"), Prefix.SHOP, MessageType.INFO, false);
            getOwner().closeInventory();
            return;
        }
        if (buyState == MethodState.SPECIAL) {
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.not_enough_space"), Prefix.SHOP, MessageType.INFO, false);
            getOwner().closeInventory();
            return;
        }
        if (buyState == MethodState.ESCAPE) {
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.purchase_failed"), Prefix.SHOP, MessageType.INFO, false);
            getOwner().closeInventory();
            return;
        }
        MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.success.purchase", Component.text(amountToBuy), Component.text(ShopItem.getItemName(getCurrentItem().getItem())), Component.text(getCurrentItem().getPricePerItem() * amountToBuy), Component.text(EconomyManager.getEconomyIcon())), Prefix.SHOP, MessageType.INFO, false);
        getOwner().closeInventory();
    }

    private void accept () {
        boolean isInCompany = companyManager.isInCompany(getOwner().getUniqueId());
        if (isInCompany) {
            MethodState deleteState = companyManager.getCompany(getOwner().getUniqueId()).deleteShop(getOwner(), shop.getUuid());
            if (deleteState == MethodState.ERROR) {
                MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.not_in_company"), Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (deleteState == MethodState.WARNING) {
                MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.not_empty"), Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (deleteState == MethodState.SPECIAL) {
                MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.refund_required"), Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (deleteState == MethodState.ESCAPE) {
                MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.cashbox_not_found"), Prefix.SHOP, MessageType.INFO, false);
            }
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.success.deleted", Component.text(shop.getName())), Prefix.SHOP, MessageType.INFO, false);
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.success.refunded_company", Component.text(75), Component.text(EconomyManager.getEconomyIcon())), Prefix.SHOP, MessageType.INFO, false);
        }
        else {
            MethodState methodState = playerShopManager.deleteShop(getOwner().getUniqueId());
            if (methodState == MethodState.WARNING) {
                MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.not_empty"), Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (methodState == MethodState.ESCAPE) {
                MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.error.cashbox_not_found"), Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.success.deleted"), Prefix.SHOP, MessageType.INFO, false);
            MessagesManager.sendMessage(getOwner(), Component.translatable("omc.shop.success.refunded_personal", Component.text(400), Component.text(EconomyManager.getEconomyIcon())), Prefix.SHOP, MessageType.INFO, false);
        }
        getOwner().closeInventory();
    }

    private void refuse() {
        getOwner().closeInventory();
    }
}
