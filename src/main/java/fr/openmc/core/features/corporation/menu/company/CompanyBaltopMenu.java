package fr.openmc.core.features.corporation.menu.company;

import fr.openmc.api.menulib.Menu;
import fr.openmc.api.menulib.utils.InventorySize;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.api.menulib.utils.ItemUtils;
import fr.openmc.core.features.city.CPermission;
import fr.openmc.core.features.corporation.company.Company;
import fr.openmc.core.features.corporation.data.MerchantData;
import fr.openmc.core.features.corporation.manager.CompanyManager;
import fr.openmc.core.utils.api.ItemAdderApi;
import fr.openmc.core.utils.api.PapiApi;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CompanyBaltopMenu extends Menu {

    public CompanyBaltopMenu(Player owner) {
        super(owner);
    }

    @Override
    public @NotNull String getName() {
        if (PapiApi.hasPAPI() && ItemAdderApi.hasItemAdder()) {
            return PlaceholderAPI.setPlaceholders(getOwner(), "§r§f%img_offset_-11%%img_company_baltop_menu%");
        } else {
            return "Baltop des entreprises";
        }
    }

    @Override
    public @NotNull InventorySize getInventorySize() {
        return InventorySize.LARGEST;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {

    }

    @Override
    public @NotNull Map<Integer, ItemStack> getContent() {
        List<Company> companies = CompanyManager.companies;
        companies.sort((company1, company2) -> Double.compare(company2.getTurnover(), company1.getTurnover()));
        Map<Integer, ItemStack> content = new HashMap<>();
        content.put(46, new ItemBuilder(this, Material.BARREL, itemMeta -> {
            itemMeta.displayName(Component.text("Baltop des entreprises")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
            itemMeta.lore(List.of(
                    Component.text("■ Voici les 3 entreprises les plus riches du serveur").color(NamedTextColor.GRAY),
                    Component.text("■ Les entreprises sont classées en fonction de leur chiffre d'affaires").color(NamedTextColor.GRAY)
            ));
        }));
        content.put(50, new ItemBuilder(this, Material.BARRIER, itemMeta -> 
            itemMeta.displayName(Component.text("Fermer").color(NamedTextColor.RED)))
            .setCloseButton());
            
        if (companies.isEmpty()) return content;
        content.put(10, new ItemBuilder(this, Material.GOLD_INGOT, itemMeta -> {
            itemMeta.displayName(Component.text("1. " + companies.getFirst().getName()).color(NamedTextColor.GOLD));
            itemMeta.lore(List.of(
                    Component.text("Chiffre d'affaire : ").color(NamedTextColor.GRAY).append(Component.text(companies.getFirst().getTurnover() + "€").color(NamedTextColor.GREEN)),
                    Component.text("Marchants : ").color(NamedTextColor.GRAY).append(Component.text(companies.getFirst().getMerchants().size()).color(NamedTextColor.GREEN))
            ));
        }));
        UUID ownerUUIDFirst;
        if (companies.getFirst().getOwner().isCity()) ownerUUIDFirst = companies.getFirst().getOwner().getCity().getPlayerWith(CPermission.OWNER);
        else ownerUUIDFirst = companies.getFirst().getOwner().getPlayer();
        content.put(12, new ItemBuilder(this, companies.getFirst().getHead(), itemMeta -> {
            String displayName = companies.getFirst().getOwner().isCity() ?
                    companies.getFirst().getOwner().getCity().getName() :
                    Bukkit.getOfflinePlayer(ownerUUIDFirst).getName();
            itemMeta.displayName(Component.text(displayName).color(NamedTextColor.GOLD));
            itemMeta.lore(List.of(Component.text("Propriétaire").color(NamedTextColor.DARK_RED)));
        }));
        for (int i = 13; i <= 16; i++) {
            ItemStack merchantHead;
            UUID merchantUUID;
            if (companies.getFirst().getMerchantsUUID().size() <= i - 13) {
                merchantUUID = null;
                merchantHead = new ItemStack(Material.AIR);
            } else {
                merchantUUID = companies.getFirst().getMerchantsUUID().get(i - 13);
                if (merchantUUID == null) merchantHead = new ItemStack(Material.AIR);
                else merchantHead = ItemUtils.getPlayerSkull(merchantUUID);
            }
            content.put(i, new ItemBuilder(this, merchantHead, itemMeta -> {
                if (merchantUUID == null) return;
                itemMeta.displayName(Component.text(Bukkit.getOfflinePlayer(merchantUUID).getName()).color(NamedTextColor.DARK_GRAY));
                MerchantData merchantData = companies.getFirst().getMerchants().get(merchantUUID);
                itemMeta.lore(List.of(
                        Component.text("■ A déposé ").color(NamedTextColor.GRAY)
                            .append(Component.text(merchantData.getAllDepositedItemsAmount() + " items").color(NamedTextColor.GREEN)),
                        Component.text("■ A gagné ").color(NamedTextColor.GRAY)
                            .append(Component.text(merchantData.getMoneyWon() + "€").color(NamedTextColor.GREEN))
                ));
            }));
        }
        if (companies.size() == 1) return content;
        content.put(19, new ItemBuilder(this, Material.GOLD_INGOT, itemMeta -> {
            itemMeta.displayName(Component.text("2. ").color(NamedTextColor.GOLD)
                .append(Component.text(companies.get(1).getName()).color(NamedTextColor.YELLOW)));
            itemMeta.lore(List.of(
                    Component.text("■ Chiffre d'affaire : ").color(NamedTextColor.GRAY)
                        .append(Component.text(companies.get(1).getTurnover() + "€").color(NamedTextColor.GREEN)),
                    Component.text("■ Marchants : ").color(NamedTextColor.GRAY)
                        .append(Component.text(companies.get(1).getMerchants().size()).color(NamedTextColor.GREEN))
            ));
        }));
        UUID ownerUUIDSecond;
        if (companies.get(1).getOwner().isCity()) ownerUUIDSecond = companies.get(1).getOwner().getCity().getPlayerWith(CPermission.OWNER);
        else ownerUUIDSecond = companies.get(1).getOwner().getPlayer();
        content.put(21, new ItemBuilder(this, ItemUtils.getPlayerSkull(ownerUUIDSecond), itemMeta -> {
            String displayName = companies.get(1).getOwner().isCity() ? 
                companies.get(1).getName() : 
                Bukkit.getOfflinePlayer(ownerUUIDSecond).getName();
            itemMeta.displayName(Component.text(displayName).color(NamedTextColor.GOLD));
            itemMeta.lore(List.of(
                Component.text("■ Propriétaire").color(NamedTextColor.DARK_RED)
            ));
        }));
        for (int i = 22; i <= 25; i++) {
            ItemStack merchantHead;
            UUID merchantUUID;
            if (companies.get(1).getMerchantsUUID().size() <= i - 22) {
                merchantUUID = null;
                merchantHead = new ItemStack(Material.AIR);
            }
            else {
                merchantUUID = companies.get(1).getMerchantsUUID().get(i - 22);
                if (merchantUUID == null) merchantHead = new ItemStack(Material.AIR);
                else merchantHead = ItemUtils.getPlayerSkull(merchantUUID);
            }
            content.put(i, new ItemBuilder(this, merchantHead, itemMeta -> {
                if (merchantUUID == null) return;
                itemMeta.displayName(Component.text(Bukkit.getOfflinePlayer(merchantUUID).getName()).color(NamedTextColor.DARK_GRAY));
                MerchantData merchantData = companies.get(1).getMerchants().get(merchantUUID);
                itemMeta.lore(List.of(
                        Component.text("■ A déposé ").color(NamedTextColor.GRAY)
                            .append(Component.text(merchantData.getAllDepositedItemsAmount() + " items").color(NamedTextColor.GREEN)),
                        Component.text("■ A gagné ").color(NamedTextColor.GRAY)
                            .append(Component.text(merchantData.getMoneyWon() + "€").color(NamedTextColor.GREEN))
                ));
            }));
        }
        if (companies.size() == 2) return content;
        content.put(28, new ItemBuilder(this, Material.GOLD_INGOT, itemMeta -> {
            itemMeta.displayName(Component.text("3. ").color(NamedTextColor.GOLD)
                .append(Component.text(companies.get(2).getName()).color(NamedTextColor.YELLOW)));
            itemMeta.lore(List.of(
                    Component.text("■ Chiffre d'affaire : ").color(NamedTextColor.GRAY)
                        .append(Component.text(companies.get(2).getTurnover() + "€").color(NamedTextColor.GREEN)),
                    Component.text("■ Marchants : ").color(NamedTextColor.GRAY)
                        .append(Component.text(companies.get(2).getMerchants().size()).color(NamedTextColor.GREEN))
            ));
        }));
        UUID ownerUUIDThird;
        if (companies.get(2).getOwner().isCity()) ownerUUIDThird = companies.get(2).getOwner().getCity().getPlayerWith(CPermission.OWNER);
        else ownerUUIDThird = companies.get(2).getOwner().getPlayer();
        content.put(30, new ItemBuilder(this, ItemUtils.getPlayerSkull(ownerUUIDThird), itemMeta -> {
            String displayName = companies.get(2).getOwner().isCity() ? 
                companies.get(2).getName() : 
                Bukkit.getOfflinePlayer(ownerUUIDThird).getName();
            itemMeta.displayName(Component.text(displayName).color(NamedTextColor.GOLD));
            itemMeta.lore(List.of(
                Component.text("■ Propriétaire").color(NamedTextColor.DARK_RED)
            ));
        }));
        for (int i = 31; i <= 34; i++) {
            ItemStack merchantHead;
            UUID merchantUUID;
            if (companies.get(2).getMerchantsUUID().size() <= i - 31) {
                merchantUUID = null;
                merchantHead = new ItemStack(Material.AIR);
            }
            else {
                merchantUUID = companies.get(2).getMerchantsUUID().get(i - 31);
                if (merchantUUID == null) merchantHead = new ItemStack(Material.AIR);
                else merchantHead = ItemUtils.getPlayerSkull(merchantUUID);
            }
            content.put(i, new ItemBuilder(this, merchantHead, itemMeta -> {
                if (merchantUUID == null) return;
                itemMeta.displayName(Component.text(Bukkit.getOfflinePlayer(merchantUUID).getName()).color(NamedTextColor.DARK_GRAY));
                MerchantData merchantData = companies.get(2).getMerchants().get(merchantUUID);
                itemMeta.lore(List.of(
                        Component.text("■ A déposé ").color(NamedTextColor.GRAY)
                            .append(Component.text(merchantData.getAllDepositedItemsAmount() + " items").color(NamedTextColor.GREEN)),
                        Component.text("■ A gagné ").color(NamedTextColor.GRAY)
                            .append(Component.text(merchantData.getMoneyWon() + "€").color(NamedTextColor.GREEN))
                ));
            }));
        }
        return content;
    }
}
