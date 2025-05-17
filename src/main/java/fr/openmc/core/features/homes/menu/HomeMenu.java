package fr.openmc.core.features.homes.menu;

import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.api.menulib.PaginatedMenu;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.core.features.homes.Home;
import fr.openmc.core.features.homes.HomesManager;
import fr.openmc.core.features.homes.utils.HomeUtil;
import fr.openmc.core.features.mailboxes.utils.MailboxMenuManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeMenu extends PaginatedMenu {

    private final OfflinePlayer target;
    private boolean wasTarget = false;
    public HomeMenu(Player player, OfflinePlayer target) {
        super(player);
        this.target = target;
        this.wasTarget = true;
    }

    public HomeMenu(Player player) {
        super(player);
        this.target = player;
    }

    @Override
    public @NotNull String getName() {
        return PlaceholderAPI.setPlaceholders(this.getOwner(), "§r§f%img_offset_-8%%img_omc_homes_menus_home%");
    }

    @Override
    public @Nullable Material getBorderMaterial() {
        return Material.BLUE_STAINED_GLASS_PANE;
    }

    @Override
    public @NotNull List<Integer> getStaticSlots() {
        List<Integer> staticSlots = new ArrayList<>();
        staticSlots.add(45);
        staticSlots.add(48);
        staticSlots.add(49);
        staticSlots.add(50);
        staticSlots.add(53);

        return staticSlots;
    }

    @Override
    public @NotNull List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();

        for(Home home : HomesManager.getHomes(target.getUniqueId())) {
            items.add(new ItemBuilder(this, HomeUtil.getHomeIconItem(home), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.homes.menu.home.name", Component.text(home.getName())));
                itemMeta.lore(List.of(
                        Component.translatable("omc.homes.menu.home.lore.teleport"),
                        Component.translatable("omc.homes.menu.home.lore.configure")
                ));
            }).setOnClick(event -> {
                if(event.isLeftClick()) {
                    this.getInventory().close();
                    MessagesManager.sendMessage(getOwner(),
                            Component.translatable("omc.homes.menu.teleport.success", Component.text(home.getName())),
                            Prefix.HOME, MessageType.SUCCESS, true
                    );
                    getOwner().teleport(home.getLocation());
                } else if(event.isRightClick()) {
                    Player player = (Player) event.getWhoClicked();
                    new HomeConfigMenu(player, home).open();
                }
            }));
        }

        return items;
    }

    @Override
    public Map<Integer, ItemStack> getButtons() {
        Map<Integer, ItemStack> map = new HashMap<>();

        if(!wasTarget) {
            map.put(45, new ItemBuilder(this, CustomStack.getInstance("omc_homes:omc_homes_icon_information").getItemStack(), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.homes.menu.infos.title"));
                itemMeta.lore(List.of(
                        Component.translatable("omc.homes.menu.infos.lore.1"),
                        Component.empty(),
                        Component.translatable("omc.homes.menu.infos.lore.2"),
                        Component.translatable("omc.homes.menu.infos.lore.3")
                ));
            }));

            map.put(53, new ItemBuilder(this, CustomStack.getInstance("omc_homes:omc_homes_icon_upgrade").getItemStack(), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.homes.menu.upgrade.title"));
                itemMeta.lore(List.of(Component.translatable("omc.homes.menu.upgrade.lore")));
            }).setOnClick(event -> new HomeUpgradeMenu(getOwner()).open()));
        }

        map.put(48, new ItemBuilder(this, MailboxMenuManager.previousPageBtn()).setPreviousPageButton());
        map.put(49, new ItemBuilder(this, MailboxMenuManager.cancelBtn()).setCloseButton());
        map.put(50, new ItemBuilder(this, MailboxMenuManager.nextPageBtn()).setNextPageButton());

        return map;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {}
}
