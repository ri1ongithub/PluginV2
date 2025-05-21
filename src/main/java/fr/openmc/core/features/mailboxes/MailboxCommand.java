package fr.openmc.core.features.mailboxes;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.mailboxes.letter.LetterHead;
import fr.openmc.core.features.mailboxes.menu.HomeMailbox;
import fr.openmc.core.features.mailboxes.menu.PendingMailbox;
import fr.openmc.core.features.mailboxes.menu.PlayerMailbox;
import fr.openmc.core.features.mailboxes.menu.letter.Letter;
import fr.openmc.core.features.mailboxes.menu.letter.SendingLetter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.sql.SQLException;

import static fr.openmc.core.features.mailboxes.utils.MailboxUtils.sendFailureMessage;
import static fr.openmc.core.features.mailboxes.utils.MailboxUtils.sendWarningMessage;

@Command({"mailbox", "mb", "letter", "mail", "lettre", "boite", "courrier"})
@CommandPermission("omc.commands.mailbox")
public class MailboxCommand {
    
    private OMCPlugin plugin;
    
    public MailboxCommand(OMCPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Subcommand("home")
    @Description("Ouvrir la page d'accueil de la boite aux lettres")
    public void homeMailbox(Player player) {
        HomeMailbox homeMailbox = new HomeMailbox(player, this.plugin);
        homeMailbox.openInventory();
    }

    @Subcommand("send")
    @Description("Envoyer une lettre à un joueur")
    @AutoComplete("@players")
    public void sendMailbox(Player player, @Named("player") String receiver) throws SQLException {
        OfflinePlayer receiverPlayer = Bukkit.getPlayerExact(receiver);
        if (receiverPlayer == null) receiverPlayer = Bukkit.getOfflinePlayerIfCached(receiver);
        if (receiverPlayer == null || !(receiverPlayer.hasPlayedBefore() || receiverPlayer.isOnline())) {
            Component message = Component.translatable("omc.mailbox.player_not_found", Component.text(receiver));
            sendFailureMessage(player, message);
        } else if (receiverPlayer.getPlayer() == player) {
            sendWarningMessage(player, Component.translatable("omc.mailbox.cannot_send_self"));
        } else if (MailboxManager.canSend(player, receiverPlayer)) {
            SendingLetter sendingLetter = new SendingLetter(player, receiverPlayer, plugin);
            sendingLetter.openInventory();
        } else {
            sendFailureMessage(player, Component.translatable("omc.mailbox.cannot_send_target"));
        }
    }

    @Subcommand("pending")
    @Description("Ouvrir les lettres en attente de réception")
    public void pendingMailbox(Player player) {
        PendingMailbox pendingMailbox = new PendingMailbox(player);
        pendingMailbox.openInventory();
    }

    @SecretCommand
    @Subcommand("open")
    @Description("Ouvrir une lettre")
    public void openMailbox(Player player, @Named("id") @Range(min = 1, max = Integer.MAX_VALUE) int id) {
        LetterHead letterHead = Letter.getById(player, id);
        if (letterHead == null) return;
        Letter mailbox = new Letter(player, letterHead);
        mailbox.openInventory();
    }

    @Subcommand("refuse")
    @SecretCommand
    @Description("Refuser une lettre")
    public void refuseMailbox(Player player, @Named("id") @Range(min = 1, max = Integer.MAX_VALUE) int id) {
        Letter.refuseLetter(player, id);
    }

    @Subcommand("cancel")
    @SecretCommand
    @Description("Annuler une lettre")
    public void cancelMailbox(Player player, @Named("id") @Range(min = 1, max = Integer.MAX_VALUE) int id) {
        PendingMailbox.cancelLetter(player, id);
    }

    @DefaultFor("~")
    public void mailbox(Player player) {
        PlayerMailbox playerMailbox = new PlayerMailbox(player);
        playerMailbox.openInventory();
    }
}
