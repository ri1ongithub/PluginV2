package fr.openmc.core.features.corporation.commands;


import fr.openmc.core.features.corporation.*;
import fr.openmc.core.features.corporation.company.Company;
import fr.openmc.core.features.corporation.manager.CompanyManager;
import fr.openmc.core.features.corporation.manager.PlayerShopManager;
import fr.openmc.core.features.corporation.manager.ShopBlocksManager;
import fr.openmc.core.features.corporation.menu.company.ShopManageMenu;
import fr.openmc.core.features.corporation.menu.shop.ShopMenu;
import fr.openmc.core.features.corporation.MethodState;
import fr.openmc.core.features.corporation.menu.shop.ShopSearchMenu;
import fr.openmc.core.features.corporation.shops.Shop;
import fr.openmc.core.features.corporation.shops.ShopItem;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Objects;
import java.util.UUID;

@Command({"shop", "shops"})
@Description("Manage shops")
@CommandPermission("omc.commands.shop")
public class ShopCommand {

    private final CompanyManager companyManager = CompanyManager.getInstance();
    private final PlayerShopManager playerShopManager = PlayerShopManager.getInstance();
    private final ShopBlocksManager shopBlocksManager = ShopBlocksManager.getInstance();

    @DefaultFor("~")
    public void onCommand(Player player) {
        boolean isInCompany = companyManager.isInCompany(player.getUniqueId());
        if (isInCompany) {
            ShopManageMenu shopManageMenu = new ShopManageMenu(player, companyManager.getCompany(player.getUniqueId()));
            shopManageMenu.open();
        }
    }

    @Subcommand("help")
    @Description("Explique comment marche un shop")
    @Cooldown(30)
    public void help(Player player) {
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.commands.list.title")
            .append(Component.text("\n\n"))
            .append(Component.translatable("omc.shop.commands.list.create"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.shop.commands.list.sell"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.shop.commands.list.unsell"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.shop.commands.list.delete"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.shop.commands.list.manage"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.shop.commands.list.search")),
            Prefix.ENTREPRISE, MessageType.INFO, false);
    }

    @Subcommand("create") 
    @Description("Create a shop")
    public void createShop(Player player) {
        boolean isInCompany = companyManager.isInCompany(player.getUniqueId());
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() != Material.BARREL) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.error.look_barrel"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        Block aboveBlock = Objects.requireNonNull(targetBlock.getLocation().getWorld())
            .getBlockAt(targetBlock.getLocation().clone().add(0, 1, 0));
        if (aboveBlock.getType() != Material.AIR) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.error.space_above"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        if (isInCompany) {
            Company company = companyManager.getCompany(player.getUniqueId());
            if (!company.hasPermission(player.getUniqueId(), CorpPermission.CREATESHOP)) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.error.no_company_permission"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (!company.createShop(player.getUniqueId(), targetBlock, aboveBlock, null)) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.error.company_no_money"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.money.company_cost"),
                Prefix.SHOP, MessageType.SUCCESS, false);
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.success.company"),
                Prefix.SHOP, MessageType.SUCCESS, false);
            return;
        }
        if (playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.error.already_has_shop"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        if (!playerShopManager.createShop(player.getUniqueId(), targetBlock, aboveBlock, null)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.error.no_personal_money"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.money.personal_cost"),
            Prefix.SHOP, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.create.success.personal"),
            Prefix.SHOP, MessageType.SUCCESS, false);
    }

    @Subcommand("sell")
    @Description("Sell an item in your shop")
    public void sellItem(Player player, @Named("price") double price) {
        boolean isInCompany = companyManager.isInCompany(player.getUniqueId());
        if (isInCompany) {
            UUID shopUUID = Shop.getShopPlayerLookingAt(player, shopBlocksManager, false);
            if (shopUUID == null) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.not_found"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            Shop shop = companyManager.getCompany(player.getUniqueId()).getShop(shopUUID);
            if (shop == null) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.not_owner"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (!CompanyManager.getInstance().getCompany(player.getUniqueId())
                .hasPermission(player.getUniqueId(), CorpPermission.SELLER)) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.no_permission"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.empty_hand"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            boolean itemThere = shop.addItem(item, price, 1);
            if (itemThere) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.item_exists"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.success"),
                Prefix.SHOP, MessageType.SUCCESS, false);
            return;
        }
        if (!playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.no_shop"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        Shop shop = playerShopManager.getPlayerShop(player.getUniqueId());
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.empty_hand"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        boolean itemThere = shop.addItem(item, price, 1);
        if (itemThere) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.item_exists"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.success"),
            Prefix.SHOP, MessageType.SUCCESS, false);
    }

    @Subcommand("unsell")
    @Description("Unsell an item in your shop")
    public void unsellItem(Player player, @Named("item number") int itemIndex) {
        boolean isInCompany = companyManager.isInCompany(player.getUniqueId());
        if (isInCompany) {
            UUID shopUUID = Shop.getShopPlayerLookingAt(player, shopBlocksManager, false);
            if (shopUUID == null) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.not_found"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            Shop shop = companyManager.getCompany(player.getUniqueId()).getShop(shopUUID);
            if (shop == null) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.not_owner"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (!CompanyManager.getInstance().getCompany(player.getUniqueId())
                .hasPermission(player.getUniqueId(), CorpPermission.SELLER)) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.error.no_remove_permission"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (itemIndex < 1 || itemIndex >= shop.getItems().size() + 1) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.error.invalid_item"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            ShopItem item = shop.getItem(itemIndex - 1);
            if (item == null) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.error.invalid_item"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            shop.removeItem(item);
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.success.removed"),
                Prefix.SHOP, MessageType.SUCCESS, false);
            if (item.getAmount() > 0) {
                ItemStack toGive = item.getItem().clone();
                toGive.setAmount(item.getAmount());
                player.getInventory().addItem(toGive);
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.success.stock_returned"),
                    Prefix.SHOP, MessageType.INFO, false);
            }
            return;
        }
        if (!playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.no_shop"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        Shop shop = playerShopManager.getPlayerShop(player.getUniqueId());
        ShopItem item = shop.getItem(itemIndex - 1);
        if (item == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.error.invalid_item"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        shop.removeItem(item);
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.success.removed"),
            Prefix.SHOP, MessageType.SUCCESS, false);
        if (item.getAmount() > 0) {
            ItemStack toGive = item.getItem().clone();
            toGive.setAmount(item.getAmount());
            player.getInventory().addItem(toGive);
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.unsell.success.stock_returned"),
                Prefix.SHOP, MessageType.SUCCESS, false);
        }
    }

    @Subcommand("delete")
    @Description("Delete a shop")
    public void deleteShop(Player player) {
        boolean isInCompany = companyManager.isInCompany(player.getUniqueId());
        UUID shopUUID = Shop.getShopPlayerLookingAt(player, shopBlocksManager, false);
        if (shopUUID == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.not_found"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        if (isInCompany) {
            Shop shop = companyManager.getCompany(player.getUniqueId()).getShop(shopUUID);
            if (shop == null) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.not_found"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (!companyManager.getCompany(player.getUniqueId())
                .hasPermission(player.getUniqueId(), CorpPermission.DELETESHOP)) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.no_permission"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            MethodState deleteState = companyManager.getCompany(player.getUniqueId())
                .deleteShop(player, shop.getUuid());
            if (deleteState == MethodState.ERROR) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.not_found"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (deleteState == MethodState.WARNING) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.not_empty_shop"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (deleteState == MethodState.SPECIAL) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.need_refund_money"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            if (deleteState == MethodState.ESCAPE) {
                MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.cashbox_missing"),
                    Prefix.SHOP, MessageType.INFO, false);
                return;
            }
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.success.name", shop.getName()),
                Prefix.SHOP, MessageType.SUCCESS, false);
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.success.company_refund"),
                Prefix.SHOP, MessageType.SUCCESS, false);
            return;
        }
        if (!playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.no_shop"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        MethodState methodState = playerShopManager.deleteShop(player.getUniqueId());
        if (methodState == MethodState.WARNING) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.not_empty_shop"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        if (methodState == MethodState.ESCAPE) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.error.cashbox_missing"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.success.personal"),
            Prefix.SHOP, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(player, Component.translatable("omc.shop.delete.success.personal_refund"),
            Prefix.SHOP, MessageType.SUCCESS, false);
    }

    @Subcommand("manage")
    @Description("Manage a shop")
    public void manageShop(Player player) {
        boolean isInCompany = companyManager.isInCompany(player.getUniqueId());
        if (isInCompany) {
            ShopManageMenu shopManageMenu = new ShopManageMenu(player, companyManager.getCompany(player.getUniqueId()));
            shopManageMenu.open();
            return;
        }
        if (!playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.shop.sell.error.no_shop"),
                Prefix.SHOP, MessageType.INFO, false);
            return;
        }
        ShopMenu shopMenu = new ShopMenu(player, playerShopManager.getPlayerShop(player.getUniqueId()), 0);
        shopMenu.open();
    }

    @Subcommand("search")
    @Description("Recherche un shop")
    public void searchShop(Player player) {
        new ShopSearchMenu(player).open();
    }
}
