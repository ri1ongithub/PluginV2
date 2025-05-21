package fr.openmc.core.features.corporation.menu.company;

import fr.openmc.api.menulib.PaginatedMenu;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.api.menulib.utils.ItemUtils;
import fr.openmc.api.menulib.utils.StaticSlots;
import fr.openmc.core.features.corporation.company.Company;
import fr.openmc.core.features.corporation.data.MerchantData;
import fr.openmc.core.utils.api.ItemAdderApi;
import fr.openmc.core.utils.api.PapiApi;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CompanyMenu extends PaginatedMenu {

    private final Company company;
    private final boolean isBackButton;

    public CompanyMenu(Player owner, Company company, boolean isBackButton) {
        super(owner);
        this.company = company;
        this.isBackButton = isBackButton;
    }

    @Override
    public @Nullable Material getBorderMaterial() {
        return Material.BLUE_STAINED_GLASS_PANE;
    }

    @Override
    public @NotNull List<Integer> getStaticSlots() {
        return StaticSlots.combine(StaticSlots.STANDARD, List.of(12, 13, 14));
    }

    @Override
    public @NotNull List<ItemStack> getItems() {
        Set<UUID> merchants = company.getMerchants().keySet();
        List<ItemStack> items = new ArrayList<>();
        for (UUID merchant : merchants) {
            items.add(new ItemBuilder(this, ItemUtils.getPlayerSkull(merchant), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.company.menu.merchant.name", Bukkit.getOfflinePlayer(merchant).getName()));
                MerchantData merchantData = company.getMerchants().get(merchant);
                itemMeta.setLore(List.of(
                        Component.translatable("omc.company.menu.merchant.items_deposited", Component.text(merchantData.getAllDepositedItemsAmount())),
                        Component.translatable("omc.company.menu.merchant.money_earned", Component.text(merchantData.getMoneyWon()))
                ));
            }));
        }
        return items;
    }

    @Override
    public Map<Integer, ItemStack> getButtons() {
        Map<Integer, ItemStack> buttons = new HashMap<>();

        buttons.put(49, new ItemBuilder(this, CustomItemRegistry.getByName("menu:close_button").getBest(), 
            itemMeta -> itemMeta.displayName(Component.text("Fermer").color(NamedTextColor.DARK_RED)))
                .setCloseButton());

        buttons.put(48, new ItemBuilder(this, CustomItemRegistry.getByName("menu:previous_page").getBest(), 
            itemMeta -> itemMeta.displayName(Component.translatable("omc.menus.buttons.previous")))
                .setPreviousPageButton());

        buttons.put(50, new ItemBuilder(this, CustomItemRegistry.getByName("menu:next_page").getBest(), 
            itemMeta -> itemMeta.displayName(Component.translatable("omc.menus.buttons.next")))
                .setNextPageButton());

        ItemStack ownerItem;
        if (company.getOwner().isPlayer()) {
            ownerItem = new ItemBuilder(this, company.getHead(), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.company.menu.owner.player.name", 
                    Component.text(Bukkit.getOfflinePlayer(company.getOwner().getPlayer()).getName())));
                itemMeta.lore(List.of(
                    Component.translatable("omc.company.menu.owner.player.type"),
                    Component.translatable("omc.company.menu.owner.merchants", Component.text(company.getMerchants().size()))
                ));
            });
        } else {
            ownerItem = new ItemBuilder(this, company.getHead(), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.company.menu.owner.city.name", 
                    Component.text(company.getOwner().getCity().getName())));
                itemMeta.lore(List.of(
                    Component.translatable("omc.company.menu.owner.city.type"),
                    Component.translatable("omc.company.menu.owner.merchants", Component.text(company.getMerchants().size()))
                ));
            });
        }

        buttons.put(4, ownerItem);

        ItemBuilder bankButton = new ItemBuilder(this, Material.GOLD_INGOT, itemMeta -> {
            itemMeta.displayName(Component.translatable("omc.company.menu.bank.title"));
            itemMeta.lore(List.of(
                Component.translatable("omc.company.menu.bank.balance", Component.text(company.getBalance())),
                Component.translatable("omc.company.menu.bank.turnover", Component.text(company.getTurnover())),
                Component.translatable("omc.company.menu.bank.click_transactions")
            ));
        });

        ItemBuilder shopsButton = new ItemBuilder(this, Material.BARREL, itemMeta -> {
            itemMeta.displayName(Component.translatable("omc.company.menu.shops.title"));
            itemMeta.lore(List.of(
                Component.translatable("omc.company.menu.shops.count", Component.text(company.getShops().size())),
                Component.translatable("omc.company.menu.shops.click_view")
            ));
        });

        if (company.isIn(getOwner().getUniqueId())) {
            buttons.put(26, bankButton.setNextMenu(new CompanyBankTransactionsMenu(getOwner(), company)));
            buttons.put(35, shopsButton.setNextMenu(new ShopManageMenu(getOwner(), company)));
        } else {
            buttons.put(26, bankButton);
            buttons.put(35, shopsButton);
        }

        return buttons;
    }

    @Override
    public @NotNull String getName() {
        if (PapiApi.hasPAPI() && ItemAdderApi.hasItemAdder()) {
            return PlaceholderAPI.setPlaceholders(getOwner(), "§r§f%img_offset_-11%%img_company_baltop_menu%");
        } else {
            return company.getName();
        }
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {

    }
}
