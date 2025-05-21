package fr.openmc.core.features.mailboxes.letter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static fr.openmc.core.features.mailboxes.utils.MailboxUtils.*;
import static fr.openmc.core.utils.DateUtils.formatRelativeDate;

public class SenderLetter extends ItemStack {
    private final int id;

    public SenderLetter(OfflinePlayer player, int id, int itemsCount, LocalDateTime sentAt, boolean refused) {
        super(Material.PLAYER_HEAD, 1);
        this.id = id;
        SkullMeta skullMeta = (SkullMeta) this.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.displayName(getStatus(refused));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(colorText(Component.translatable("omc.mailbox.letter.click_to_cancel").toString(), NamedTextColor.YELLOW, true));
        lore.add(getPlayerName(player));
        lore.add(colorText(Component.translatable("omc.mailbox.letter.info", 
            Component.text(formatRelativeDate(sentAt)),
            Component.text(itemsCount),
            Component.text(getItemCount(itemsCount))).toString(), NamedTextColor.DARK_GRAY, true));
        skullMeta.lore(lore);
        this.setItemMeta(skullMeta);
    }

    public static Component getStatus(boolean refused) {
        NamedTextColor color = refused ? NamedTextColor.DARK_RED : NamedTextColor.DARK_AQUA;
        Component status = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.translatable(refused ? "omc.mailbox.letter.status.refused_symbol" : "omc.mailbox.letter.status.pending_symbol").color(color))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
            .append(Component.translatable(refused ? "omc.mailbox.letter.status.refused" : "omc.mailbox.letter.status.pending").color(color));
        return nonItalic(status);
    }

    public int getId() {
        return id;
    }
}
