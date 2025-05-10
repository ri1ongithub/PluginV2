package fr.openmc.core.utils.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import lombok.Getter;

import java.util.Map;

public class MessagesManager {

    /*
    For use the beautiful message, create a prefix.
     */

    /**
     * Sends a formatted message to the player with or without sound.
     *
     * @param sender  The player to send the message to (can be a console)
     * @param message The content of the message
     * @param prefix  The prefix for the message
     * @param type    The type of message (information, error, success, warning)
     * @param sound   Indicates whether a sound should be played (true) or not (false)
     */

    public static void sendMessage(CommandSender sender, Component message, Prefix prefix, MessageType type, float soundVolume, boolean sound) {
        MiniMessage.miniMessage().deserialize("e");
        Component messageComponent =
                Component.text(type == MessageType.NONE ? "" : "§7(" + type.getPrefix() + "§7) ")
                        .append(MiniMessage.miniMessage().deserialize(prefix.getPrefix()))
                        .append(Component.text(" §7» ")
                        .append(message)
                );

        if(sender instanceof Player player && sound) {
            player.playSound(player.getLocation(), type.getSound(), soundVolume, 1.0F);
        }

        sender.sendMessage(messageComponent);
    }

    public static void sendMessage(CommandSender sender, Component message, Prefix prefix, MessageType type, boolean sound) {
        sendMessage(sender, message, prefix, type, 1.0F, sound);
    }

    /**
     *
     * Sends a formatted message to the player with an accompanying sound.
     *
     * @param sender  The player to send the message to (can be a console)
     * @param message The content of the message
     * @param prefix  The prefix for the message
     */
    public static void sendMessage(CommandSender sender, Component message, Prefix prefix) {
        sendMessage(sender, message, prefix, MessageType.NONE, false);
    }

    /**
     *
     * Broadcasts a formatted message to the entire server
     *
     * @param message The content of the message
     * @param prefix  The prefix for the message
     * @param type    The type of message (information, error, success, warning)
     */
    public static void broadcastMessage(Component message, Prefix prefix, MessageType type) {
        MiniMessage.miniMessage().deserialize("e");
        Component messageComponent =
                Component.text(type == MessageType.NONE ? "" : "§7(" + type.getPrefix() + "§7) ")
                        .append(MiniMessage.miniMessage().deserialize(prefix.getPrefix()))
                        .append(Component.text(" §7» ")
                        .append(message)
                );

        Bukkit.broadcast(messageComponent);
    }
        
    public static String textToSmall(String text) {
        Map<Character, Character> charMap = Map.ofEntries(
                Map.entry('A', 'ᴀ'), Map.entry('B', 'ʙ'), Map.entry('C', 'ᴄ'), Map.entry('D', 'ᴅ'),
                Map.entry('E', 'ᴇ'), Map.entry('F', 'ꜰ'), Map.entry('G', 'ɢ'), Map.entry('H', 'ʜ'),
                Map.entry('I', 'ɪ'), Map.entry('J', 'ᴊ'), Map.entry('K', 'ᴋ'), Map.entry('L', 'ʟ'),
                Map.entry('M', 'ᴍ'), Map.entry('N', 'ɴ'), Map.entry('O', 'ᴏ'), Map.entry('P', 'ᴘ'),
                Map.entry('Q', 'ǫ'), Map.entry('R', 'ʀ'), Map.entry('S', 'ѕ'), Map.entry('T', 'ᴛ'),
                Map.entry('U', 'ᴜ'), Map.entry('V', 'ᴠ'), Map.entry('W', 'ᴡ'), Map.entry('X', 'ʏ'),
                Map.entry('Y', 'ʏ'), Map.entry('Z', 'ᴢ'), Map.entry('1', '₁'), Map.entry('2', '₂'),
                Map.entry('3', '₃'), Map.entry('4', '₄'), Map.entry('5', '₅'), Map.entry('6', '₆'),
                Map.entry('7', '₇'), Map.entry('8', '₈'), Map.entry('9', '₉'), Map.entry('0', '₀')
        );

        StringBuilder result = new StringBuilder();
        for(char c : text.toCharArray()) {
            result.append(charMap.getOrDefault(c, c));
        }

        return result.toString();
    }

    @Getter
    public enum Message {
        NOPERMISSION(Component.translatable("omc.messages.no_permission")),
        NOPERMISSION2(Component.translatable("omc.messages.no_permission2")),
        MISSINGARGUMENT(Component.translatable("omc.messages.missing_argument")),
        MONEYPLAYERMISSING(Component.translatable("omc.messages.money_player_missing")),

        // City messages
        PLAYERNOCITY(Component.translatable("omc.city.player_no_city")),
        PLAYERINCITY(Component.translatable("omc.city.player_in_city")),
        CITYNOFREECLAIM(Component.translatable("omc.city.no_free_claims")),

        PLAYERNOCLAIM(Component.translatable("omc.city.no_claim_permission")),
        PLAYERNOOWNER(Component.translatable("omc.city.not_mayor")),
        PLAYERNORENAME(Component.translatable("omc.city.cannot_rename")),
        PLAYERNOMONEYGIVE(Component.translatable("omc.city.no_money_give")),
        PLAYERNOMONEYTAKE(Component.translatable("omc.city.no_money_take")),

        CITYNOTFOUND(Component.translatable("omc.city.not_found")),


        ;

        private final Component message;

        Message(Component message) {
            this.message = message;
        }

    }

}
