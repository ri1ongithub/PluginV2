package fr.openmc.core.features.homes.menu;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import fr.openmc.api.menulib.Menu;
import fr.openmc.api.menulib.utils.InventorySize;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.core.features.homes.Home;
import fr.openmc.core.features.homes.utils.HomeUtil;
import fr.openmc.core.features.mailboxes.utils.MailboxMenuManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeConfigMenu extends Menu {

    private final Home home;

    public HomeConfigMenu(Player owner, Home home) {
        super(owner);
        this.home = home;
    }

    @Override
    public @NotNull String getName() {
        return PlaceholderAPI.setPlaceholders(this.getOwner(), "§r§f%img_offset_-8%%img_omc_homes_menus_home_settings%");
    }

    @Override
    public @NotNull InventorySize getInventorySize() {
        return InventorySize.LARGER;
    }

    @Override
    public @NotNull Map<Integer, ItemStack> getContent() {
        Map<Integer, ItemStack> content = new HashMap<>();

        content.put(4, home.getIconItem());

        content.put(20, new ItemBuilder(this, HomeUtil.getRandomsIcons(), itemMeta -> {
            itemMeta.displayName(Component.translatable("omc.homes.menu.config.icon.change"));
            itemMeta.lore(List.of(Component.translatable("omc.homes.menu.config.icon.change.lore")));
        }).setNextMenu(new HomeChangeIconMenu(getOwner(), home)));

        content.put(24, new ItemBuilder(this, CustomStack.getInstance("omc_homes:omc_homes_icon_bin_red").getItemStack(), itemMeta -> {
            itemMeta.displayName(Component.text(new FontImageWrapper("omc_homes:bin").getString() + " ").append(Component.translatable("omc.homes.menu.config.delete")));
            itemMeta.lore(List.of(Component.translatable("omc.homes.menu.config.delete.lore")));
        }).setNextMenu(new HomeDeleteConfirmMenu(getOwner(), home)));

        content.put(36, new ItemBuilder(this, MailboxMenuManager.previousPageBtn()).setNextMenu(new HomeMenu(getOwner())));
        content.put(44, new ItemBuilder(this, MailboxMenuManager.cancelBtn()).setCloseButton());

        return content;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {}
}
