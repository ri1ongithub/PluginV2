package fr.openmc.core.features.corporation.menu.company;


import fr.openmc.api.menulib.PaginatedMenu;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.api.menulib.utils.StaticSlots;
import fr.openmc.core.features.corporation.company.Company;
import fr.openmc.core.features.corporation.manager.CompanyManager;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.api.ItemAdderApi;
import fr.openmc.core.utils.api.PapiApi;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompanySearchMenu extends PaginatedMenu {

    private final CompanyManager companyManager = CompanyManager.getInstance();

    public CompanySearchMenu(Player owner) {
        super(owner);
    }

    @Override
    public @Nullable Material getBorderMaterial() {
        return Material.BLUE_STAINED_GLASS_PANE;
    }

    @Override
    public @NotNull List<Integer> getStaticSlots() {
        if (companyManager.isInCompany(getOwner().getUniqueId())) {
            return StaticSlots.combine(StaticSlots.STANDARD, List.of(12, 13, 14));
        }
        return StaticSlots.STANDARD;
    }

    @Override
    public @NotNull List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Company company : companyManager.getCompanies()) {
            if (companyManager.isInCompany(getOwner().getUniqueId())) {
                items.add(new ItemBuilder(this, company.getHead(), itemMeta -> {
                    itemMeta.displayName(Component.text(company.getName()).color(NamedTextColor.YELLOW));
                    itemMeta.lore(List.of(
                        Component.text("Chiffre d'affaires : ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(company.getTurnover() + "€").color(NamedTextColor.GREEN)),
                        Component.text("Marchants : ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(company.getMerchants().size()).color(NamedTextColor.WHITE)),
                        Component.text("Cliquez pour voir les informations de l'entreprise").color(NamedTextColor.GRAY)
                    ));
                }).setNextMenu(new CompanyMenu(getOwner(), company, true)));
            } else {
                items.add(new ItemBuilder(this, company.getHead(), itemMeta -> {
                    itemMeta.displayName(Component.text(company.getName()).color(NamedTextColor.YELLOW));
                    itemMeta.lore(List.of(
                        Component.text("Chiffre d'affaires : ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(company.getTurnover() + "€").color(NamedTextColor.GREEN)),
                        Component.text("Marchants : ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(company.getMerchants().size()).color(NamedTextColor.WHITE)),
                        Component.text("Candidatures : ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(companyManager.getPendingApplications(company).size()).color(NamedTextColor.WHITE)),
                        Component.text("Cliquez pour postuler").color(NamedTextColor.GRAY)
                    ));
                }).setOnClick((inventoryClickEvent) -> {
                    companyManager.applyToCompany(getOwner().getUniqueId(), company);
                    MessagesManager.sendMessage(getOwner(),
                        Component.translatable("omc.company.success.applied", Component.text(company.getName())), 
                        Prefix.COMPANY, MessageType.SUCCESS, false);
                    company.broadCastOwner(Component.translatable("omc.company.success.someone_applied", 
                        Component.text(getOwner().getName())).toString());
                }));
            }
        }
        return items;
    }

    @Override
    public Map<Integer, ItemStack> getButtons() {
        Map<Integer, ItemStack> map = new HashMap<>();
        if (companyManager.isInCompany(getOwner().getUniqueId())) {
            map.put(4, new ItemBuilder(this, companyManager.getCompany(getOwner().getUniqueId()).getHead(), itemMeta -> {
                itemMeta.displayName(Component.text(companyManager.getCompany(getOwner().getUniqueId()).getName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
                itemMeta.lore(List.of(
                    Component.text("- Entreprise -").color(NamedTextColor.GRAY),
                    Component.text("Chiffre d'affaires : ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(
                            companyManager.getCompany(getOwner().getUniqueId()).getTurnover() + 
                            EconomyManager.getEconomyIcon()).color(NamedTextColor.GREEN)),
                    Component.text("Marchants : ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(companyManager.getCompany(getOwner().getUniqueId()).getMerchants().size())
                        .color(NamedTextColor.WHITE)),
                    Component.text("Cliquez pour voir les informations de l'entreprise").color(NamedTextColor.GRAY)
                ));
            }).setNextMenu(new CompanyMenu(getOwner(), companyManager.getCompany(getOwner().getUniqueId()), true)));
        }

        map.put(49, new ItemBuilder(this, CustomItemRegistry.getByName("menu:close_button").getBest(), 
            itemMeta -> itemMeta.displayName(Component.text("Fermer").color(NamedTextColor.DARK_RED)))
            .setCloseButton());
        
        return map;
    }

    @Override
    public @NotNull String getName() {
        if (PapiApi.hasPAPI() && ItemAdderApi.hasItemAdder()) {
            return PlaceholderAPI.setPlaceholders(getOwner(), "§r§f%img_offset_-11%%img_paginate_company_menu%");
        } else {
            return companyManager.isInCompany(getOwner().getUniqueId()) ? "Rechercher une entreprise" : "Pôle travail";
        }
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {

    }
}
