package fr.openmc.core.features.mailboxes.menu;


import fr.openmc.core.features.mailboxes.MailboxManager;
import fr.openmc.core.features.mailboxes.letter.SenderLetter;
import fr.openmc.core.features.mailboxes.utils.PaginatedMailbox;
import fr.openmc.core.utils.CacheOfflinePlayer;
import fr.openmc.core.utils.database.DatabaseManager;
import fr.openmc.core.utils.serializer.BukkitSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import static fr.openmc.core.features.mailboxes.utils.MailboxUtils.*;

public class PendingMailbox extends PaginatedMailbox<SenderLetter> {
    static {
        invErrorMessage = "Erreur lors de la récupération de vos lettres.";
    }

    public PendingMailbox(Player player) {
        super(player);
        if (fetchMailbox()) initInventory();
    }

    public static void cancelLetter(Player player, int id) {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT receiver_id, items, items_count FROM mailbox_items WHERE id = ? AND sender_id = ?;")) {
            statement.setInt(1, id);
            statement.setString(2, player.getUniqueId().toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    int itemsCount = result.getInt("items_count");
                    ItemStack[] items = BukkitSerializer.deserializeItemStacks(result.getBytes("items"));
                    Player receiver = CacheOfflinePlayer.getOfflinePlayer(UUID.fromString(result.getString("receiver_id"))).getPlayer();
                    if (deleteLetter(id)) {
                        if (receiver != null) MailboxManager.cancelLetter(receiver, id);
                        MailboxManager.givePlayerItems(player, items);
                        Component message = Component.translatable("omc.mailbox.cancel.success", Component.text(itemsCount), Component.text(getItemCount(itemsCount)));
                        sendSuccessMessage(player, message);
                    }
                } else {
                    Component message = Component.translatable("omc.mailbox.cancel.not_found", Component.text(id));
                    sendFailureMessage(player, message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendFailureMessage(player, Component.translatable("omc.mailbox.error"));
        }
    }

    public static boolean deleteLetter(int id) throws SQLException {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("DELETE FROM mailbox_items WHERE id = ?;")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean fetchMailbox() {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT id, receiver_id, sent_at, items_count, refused FROM mailbox_items WHERE sender_id = ? ORDER BY sent_at DESC;")) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID senderUUID = UUID.fromString(result.getString("receiver_id"));
                    int id = result.getInt("id");
                    int itemsCount = result.getInt("items_count");
                    LocalDateTime sentAt = result.getTimestamp("sent_at").toLocalDateTime();
                    boolean refused = result.getBoolean("refused");
                    pageItems.add(new SenderLetter(CacheOfflinePlayer.getOfflinePlayer(senderUUID), id, itemsCount, sentAt, refused));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clickLetter(int slot) {
        SenderLetter senderLetter = getByIndex(slot);
        if (senderLetter == null) return;
        int id = senderLetter.getId();
        Component message = Component.text("Cliquez-ici", NamedTextColor.YELLOW)
                                     .clickEvent(getRunCommand("cancel " + id))
                                     .hoverEvent(getHoverEvent("Annuler la lettre #" + id))
                                     .append(Component.text(" si vous êtes sur de vouloir annuler la lettre.", NamedTextColor.GOLD));
        sendWarningMessage(player, message);
        player.closeInventory();
    }
}
