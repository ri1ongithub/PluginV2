package fr.openmc.core.features.mailboxes.menu;


import fr.openmc.core.features.mailboxes.letter.LetterHead;
import fr.openmc.core.features.mailboxes.utils.PaginatedMailbox;
import fr.openmc.core.utils.CacheOfflinePlayer;
import fr.openmc.core.utils.database.DatabaseManager;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerMailbox extends PaginatedMailbox<LetterHead> {

    static {
        invErrorMessage = Component.translatable("omc.mailbox.error.fetch").toString();
    }

    public PlayerMailbox(Player player) {
        super(player);
        if (fetchMailbox()) initInventory();
    }

    public void addLetter(LetterHead letterHead) {
        pageItems.add(letterHead);
        int size = pageItems.size();
        if (size - 1 / maxIndex == page) updateInventory(false, size - 1 % maxIndex);
    }

    public void removeLetter(int id) {
        for (int i = 0; i < pageItems.size(); i++) {
            if (pageItems.get(i).getId() == id) {
                pageItems.remove(i);
                int currentPage = i / maxIndex;

                if (currentPage == page) {
                    updateInventory(false, i % maxIndex);
                } else if (currentPage < page) {
                    updateInventory(true);
                }
                break;
            }
        }
    }

    public boolean fetchMailbox() {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT id, sender_id, sent_at, items_count FROM mailbox_items WHERE receiver_id = ? AND refused = false ORDER BY sent_at DESC;")) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int id = result.getInt("id");
                    UUID senderUUID = UUID.fromString(result.getString("sender_id"));
                    int itemsCount = result.getInt("items_count");
                    LocalDateTime sentAt = result.getTimestamp("sent_at").toLocalDateTime();
                    pageItems.add(new LetterHead(CacheOfflinePlayer.getOfflinePlayer(senderUUID), id, itemsCount, sentAt));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
