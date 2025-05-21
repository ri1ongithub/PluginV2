package fr.openmc.core.features.city;

import fr.openmc.core.features.city.mascots.Mascot;
import fr.openmc.core.features.city.mascots.MascotUtils;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.CacheOfflinePlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CityMessages {
    private static void sendLine(Audience audience, Component title, Component info) {
        audience.sendMessage(Component.translatable("omc.city.info.line",
                title,
                info.color(NamedTextColor.LIGHT_PURPLE)));
    }

    public static void sendInfo(CommandSender sender, City city) {
        String mascotLife = "dead";
        String cityName = city.getName();
        String mayorName = CacheOfflinePlayer.getOfflinePlayer(city.getPlayerWith(CPermission.OWNER)).getName();

        int citizens = city.getMembers().size();
        int area = city.getChunks().size();
        int power = CityManager.getCityPowerPoints(city.getUUID());

        String type = CityManager.getCityType(city.getUUID());
        Mascot mascot = MascotUtils.getMascotOfCity(city.getUUID());
        if (mascot!=null){
            LivingEntity mob = MascotUtils.loadMascot(mascot);
            if (MascotUtils.getMascotState(city.getUUID())){
                mascotLife = String.valueOf(mob.getHealth());
            }
        }

        sender.sendMessage(Component.translatable("omc.city.info.header", 
                Component.text(cityName).color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true)));

        sendLine(sender, Component.translatable("omc.city.info.mayor"), Component.text(mayorName));
        sendLine(sender, Component.translatable("omc.city.info.citizens"), Component.text(String.valueOf(citizens)));
        sendLine(sender, Component.translatable("omc.city.info.area"), Component.text((String.valueOf(area))));
        if (type!=null && type.equals("war")){
            sendLine(sender, Component.translatable("omc.city.info.power"), Component.text(String.valueOf(power)));
        }
        sendLine(sender, Component.translatable("omc.city.info.mascot_life"), Component.text((mascotLife)));
        sendLine(sender, Component.translatable("omc.city.info.type"), Component.text((type)));

        String money = EconomyManager.getFormattedSimplifiedNumber(city.getBalance()) + " " + EconomyManager.getEconomyIcon();
        if (sender instanceof Player player) {
            if (!(city.hasPermission(player.getUniqueId(), CPermission.MONEY_BALANCE))) return;
            sendLine(sender, Component.translatable("omc.city.info.bank"), Component.text(money));
        } else {
            sendLine(sender, Component.translatable("omc.city.info.bank"), Component.text(money));
        }
        if (CityManager.freeClaim.containsKey(city.getUUID())){
            sendLine(sender, Component.translatable("omc.city.info.free_claims"), 
                    Component.text(String.valueOf(CityManager.freeClaim.get(city.getUUID()))));
        }
    }
}
