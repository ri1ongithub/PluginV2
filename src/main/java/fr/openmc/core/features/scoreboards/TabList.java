package fr.openmc.core.features.scoreboards;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.papermc.paper.configuration.serializer.ComponentSerializer;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TabList {

    @Getter private static TabList instance;
    private ProtocolManager protocolManager = null;

    public TabList() {
        instance = this;
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null)
            this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void updateHeaderFooter(Player player, String header, String footer) {
        try {
            if (protocolManager == null) return;
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
            packet.getChatComponents().write(0, WrappedChatComponent.fromJson(header))
                    .write(1, WrappedChatComponent.fromText(footer));
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTabList(Player player) {
        int visibleOnlinePlayers = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.canSee(player)) {
                visibleOnlinePlayers++;
            }
        }



        Component header = Component.text("\n\n\n\n\n\n\n")
                .append(Component.text(PlaceholderAPI.setPlaceholders(player, "%img_openmc%")))
                .append(Component.text("\n\n  "))
                .append(Component.translatable("omc.tablist.header.online_players", Component.text(visibleOnlinePlayers), Component.text(PlaceholderAPI.setPlaceholders(player, "%server_max_players%"))));

        String footer = "\n§dplay.openmc.fr\n";



        updateHeaderFooter(player, GsonComponentSerializer.gson().serialize(header), footer);
    }

}
