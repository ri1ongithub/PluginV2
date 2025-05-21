package fr.openmc.core.features.corporation.commands;

import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.corporation.*;
import fr.openmc.core.features.corporation.company.Company;
import fr.openmc.core.features.corporation.company.CompanyOwner;
import fr.openmc.core.features.corporation.manager.CompanyManager;
import fr.openmc.core.features.corporation.manager.PlayerShopManager;
import fr.openmc.core.features.corporation.menu.company.CompanyBaltopMenu;
import fr.openmc.core.features.corporation.menu.company.CompanyMenu;
import fr.openmc.core.features.corporation.menu.company.CompanySearchMenu;
import fr.openmc.core.features.corporation.MethodState;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.UUID;

@Command({"company", "companies",  "entreprise", "enterprises"})
@Description("Gestion des entreprises")
@CommandPermission("omc.commands.company")
public class CompanyCommand {

    private final CompanyManager manager = CompanyManager.getInstance();
    private final PlayerShopManager playerShopManager = PlayerShopManager.getInstance();

    @DefaultFor("~")
    public void onCommand(Player player) {
        if (!manager.isInCompany(player.getUniqueId())) {
            search(player);
            return;
        }
        CompanyMenu menu = new CompanyMenu(player, manager.getCompany(player.getUniqueId()), false);
        menu.open();
    }

    @Subcommand("help")
    @Description("Explique comment marche une entreprise")
    @Cooldown(30)
    public void help(Player player) {
        MessagesManager.sendMessage(player, Component.translatable("omc.company.commands.list.title")
            .append(Component.text("\n\n"))
            .append(Component.translatable("omc.company.commands.list.baltop"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.balance"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.create"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.menu"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.search"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.apply"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.deny"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.accept"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.withdraw"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.deposit"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.setcut"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.leave"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.fire"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.owner"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.liquidate"))
            .append(Component.text("\n"))
            .append(Component.translatable("omc.company.commands.list.perms")),
            Prefix.ENTREPRISE, MessageType.INFO, false);
    }

    @Subcommand("apply")
    @Description("Postuler dans une entreprise")
    public void apply(Player player, @Named("name") String name) {
        if (!manager.companyExists(name)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_exists"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.already_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.has_shop"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        Company company = manager.getCompany(name);
        manager.applyToCompany(player.getUniqueId(), company);
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.applied", Component.text(name)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
        company.broadCastOwner(Component.translatable("omc.company.success.someone_applied", Component.text(player.getName())).toString());
    }

    @Subcommand("accept")
    @Description("Accepter une candidature")
    public void accept(Player player, @Named("target") Player target) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).hasPermission(player.getUniqueId(), CorpPermission.HIRINGER)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_hire_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.hasPendingApplicationFor(target.getUniqueId(), manager.getCompany(player.getUniqueId()))) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_application"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        Company company = manager.getCompany(player.getUniqueId());
        manager.acceptApplication(target.getUniqueId(), company);
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.accepted_applicant", Component.text(target.getName())), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(target, Component.translatable("omc.company.success.application_accepted", Component.text(company.getName())), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("deny")
    @Description("Refuser une candidature")
    public void deny(Player player, @Named("target") Player target) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).hasPermission(player.getUniqueId(), CorpPermission.HIRINGER)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_hire_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.hasPendingApplicationFor(target.getUniqueId(), manager.getCompany(player.getUniqueId()))) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_application"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        Company company = manager.getCompany(player.getUniqueId());
        manager.denyApplication(target.getUniqueId());
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.denied_applicant", Component.text(target.getName())), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(target, Component.translatable("omc.company.success.application_denied", Component.text(company.getName())), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("search")
    @Description("Rechercher une entreprise")
    public void search(Player player) {
        CompanySearchMenu menu = new CompanySearchMenu(player);
        menu.open();
    }

    @Subcommand("liquidate")
    @Description("Liquider une entreprise")
    public void liquidate(Player player) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_any"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        Company company = manager.getCompany(player.getUniqueId());
        if (!company.hasPermission(player.getUniqueId(), CorpPermission.LIQUIDATESHOP)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_liquidate_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.liquidateCompany(company)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.cannot_liquidate"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.liquidated"), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("leave")
    @Description("Quitter une entreprise")
    public void leave(Player player) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        MethodState leaveResult = manager.leaveCompany(player.getUniqueId());
        if (leaveResult == MethodState.FAILURE) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.transfer_ownership"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (leaveResult == MethodState.WARNING) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.last_member"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (leaveResult == MethodState.SPECIAL) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.city_owner"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.left"), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("fire")
    @Description("Renvoyer un membre de l'entreprise")
    public void fire(Player player, @Named("target") Player target) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).hasPermission(player.getUniqueId(), CorpPermission.FIRE)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_fire_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.isMerchantOfCompany(target.getUniqueId(), manager.getCompany(player.getUniqueId()))) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_member"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        manager.getCompany(player.getUniqueId()).fireMerchant(target.getUniqueId());
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.fired", Component.text(target.getName())), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("baltop")
    @Description("Afficher le top des entreprises")
    public void baltop(Player player) {
        CompanyBaltopMenu menu = new CompanyBaltopMenu(player);
        menu.open();
    }

    @Subcommand("balance")
    @Description("Afficher le solde de l'entreprise")
    public void balance(Player player) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.company.balance.display",
            Component.text(manager.getCompany(player.getUniqueId()).getBalance()),
            Component.text(EconomyManager.getEconomyIcon())),
            Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    // définit la part de l'entreprise sur tous ses shops
    @Subcommand("setcut")
    @Description("Définir la part de l'entreprise lors d'une vente")
    public void setCut(Player player, @Named("cut") double cut) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).hasPermission(player.getUniqueId(), CorpPermission.SETCUT)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_setcut_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (cut < 0 || cut > 100) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.invalid_cut"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        manager.getCompany(player.getUniqueId()).setCut(cut / 100);
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.cut_set", Component.text(cut)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    // seul le joueur est propriétaire
    @Subcommand("create")
    @Description("Créer une entreprise")
    public void createCompany(Player player, @Named("name") String name) {
        if (!check(player, name, false)) return;
        manager.createCompany(name, new CompanyOwner(player.getUniqueId()), false, null);
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.created", Component.text(name)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    // laissez en commentaire, le système d'entreprise de ville doit être refait

    // les membres de la ville sont propriétaires
//    @Subcommand("cityCreate")
//    @Description("Créer une entreprise de ville")
//    public void createTeamCompany(Player player, @Named("name") String name) {
//        if (!check(player, name, true)) return;
//        City city = CityManager.getPlayerCity(player.getUniqueId());
//        if (city==null){
//            MessagesManager.sendMessage(player, MessagesManager.Message.PLAYERNOCITY.getMessage(), Prefix.CITY, MessageType.INFO, false);
//            return;
//        }
//        if (player.getUniqueId() != city.getPlayerWith(CPermission.OWNER)) {
//            MessagesManager.sendMessage(player, Component.text("§cVous ne pouvez pas créer d'entreprise au nom de votre ville sans en être l'owner"), Prefix.ENTREPRISE, MessageType.INFO, false);
//            return;
//        }
//        manager.createCompany(name, new CompanyOwner(CityManager.getPlayerCity(player.getUniqueId())), true, null);
//        MessagesManager.sendMessage(player, Component.text("§aL'entreprise " + name + " a été créée avec succès !"), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
//    }

    @Subcommand("menu")
    @Description("Ouvrir le menu de l'entreprise")
    public void openMenu(Player player) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        CompanyMenu menu = new CompanyMenu(player, manager.getCompany(player.getUniqueId()), false);
        menu.open();
    }

    @Subcommand("withdraw")
    @Description("Retirer de l'argent de l'entreprise")
    public void withdraw(Player player, @Named("amount") double amount) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).hasPermission(player.getUniqueId(), CorpPermission.WITHDRAW)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_withdraw_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).withdraw(amount, player, "Retrait")) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_enough_money"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.withdrawn", Component.text(amount)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("deposit")
    @Description("Déposer de l'argent dans l'entreprise")
    public void deposit(Player player, @Named("amount") double amount) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).hasPermission(player.getUniqueId(), CorpPermission.DEPOSIT)){
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_deposit_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).deposit(amount, player, "Dépôt")) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_enough_personal_money"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.deposited", Component.text(amount)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    @Subcommand("owner")
    @Description("Transférer la propriété de l'entreprise")
    public void transferOwner(Player player, @Named("target") Player target) {
        if (!manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.getCompany(player.getUniqueId()).isUniqueOwner(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.not_unique_owner"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!manager.isMerchantOfCompany(target.getUniqueId(), manager.getCompany(player.getUniqueId()))) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.target_not_member"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        manager.getCompany(player.getUniqueId()).setOwner(target.getUniqueId());
        MessagesManager.sendMessage(player, Component.translatable("omc.company.success.ownership_transferred", Component.text(target.getName())), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    // add permissions
    @Subcommand({"permission give", "perms give"})
    @Description("Donner les permissions aux joueurs")
    @AutoComplete("@company_perms")
    void giveSuperior(Player sender, @Named("company_perms") String perms, @Named("target") Player target) {
        Company company = manager.getCompany(target.getUniqueId());
        if (!manager.isInCompany(sender.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (company != manager.getCompany(sender.getUniqueId())){
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.target_not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!company.hasPermission(sender.getUniqueId(), CorpPermission.SUPERIOR)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.no_permission_management"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (company.hasPermission(target.getUniqueId(), CorpPermission.valueOf(perms))){
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.already_has_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        company.addPermission(target.getUniqueId(), CorpPermission.valueOf(perms));
        MessagesManager.sendMessage(sender, Component.translatable("omc.company.success.permission_added", Component.text(perms)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(target, Component.translatable("omc.company.success.permission_received", Component.text(perms)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }


    //remove permissions
    @Subcommand({"permission remove", "perms remove"})
    @Description("Retire les permissions aux joueurs")
    @AutoComplete("@company_perms")
    void removeSuperior(Player sender, @Named("company_perms") String perms, @Named("target") Player target) {
        Company company = manager.getCompany(target.getUniqueId());
        if (!manager.isInCompany(sender.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (company != manager.getCompany(sender.getUniqueId())){
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.target_not_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!company.hasPermission(sender.getUniqueId(), CorpPermission.SUPERIOR)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.no_remove_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        if (!company.hasPermission(target.getUniqueId(), CorpPermission.valueOf(perms))){
            MessagesManager.sendMessage(sender, Component.translatable("omc.company.error.doesnt_have_permission"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return;
        }
        company.removePermission(target.getUniqueId(), CorpPermission.valueOf(perms));
        MessagesManager.sendMessage(sender, Component.translatable("omc.company.success.permission_removed", Component.text(perms)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(target, Component.translatable("omc.company.success.permission_lost", Component.text(perms)), Prefix.ENTREPRISE, MessageType.SUCCESS, false);
    }

    private boolean check(Player player, String name, boolean teamCreate) {
        if (manager.isInCompany(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.already_in_company"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return false;
        }
        if (name.length() < 3 || name.length() > 16) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.name_length"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return false;
        }
        if (manager.companyExists(name)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.name_exists"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return false;
        }
        if (playerShopManager.hasShop(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.has_shop_create"), Prefix.ENTREPRISE, MessageType.INFO, false);
            return false;
        }
        if (teamCreate) {
            City city = CityManager.getPlayerCity(player.getUniqueId());
            if (city != null) {
                for (UUID cityMember : city.getMembers()) {
                    if (playerShopManager.hasShop(cityMember)) {
                        MessagesManager.sendMessage(player, Component.translatable("omc.company.error.city_member_has_shop"), Prefix.ENTREPRISE, MessageType.INFO, false);
                        return false;
                    }
                    if (manager.isInCompany(cityMember)) {
                        if (Bukkit.getPlayer(cityMember) == null) {
                            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.city_member_offline"), Prefix.ENTREPRISE, MessageType.INFO, false);
                        } else {
                            MessagesManager.sendMessage(player, Component.translatable("omc.company.error.city_member_in_company", Component.text(Bukkit.getPlayer(cityMember).getName())), Prefix.ENTREPRISE, MessageType.INFO, false);
                        }
                        return false;
                    }
                }
            } else {
                MessagesManager.sendMessage(player, Component.translatable("omc.company.error.no_city"), Prefix.ENTREPRISE, MessageType.INFO, false);
                return false;
            }
        }
        return true;
    }

}
