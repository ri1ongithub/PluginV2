package fr.openmc.core.features.city.mascots;

import fr.openmc.api.cooldown.DynamicCooldownManager;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.utils.ItemUtils;
import fr.openmc.core.utils.database.DatabaseManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MascotsManager {

    public static long IMMUNITY_COOLDOWN = 10080L * 60 * 1000; // 10080 minutes en ms

    public static NamespacedKey chestKey;
    public static NamespacedKey mascotsKey;
    public static List<Mascot> mascots = new ArrayList<>();

    public MascotsManager(OMCPlugin plugin) {
        //changement du spigot.yml pour permettre aux mascottes d'avoir 3000 cœurs
        File spigotYML = new File("spigot.yml");
        YamlConfiguration spigotYMLConfig = YamlConfiguration.loadConfiguration(spigotYML);
        spigotYMLConfig.set("settings.attribute.maxHealth.max", 6000.0);
        try {
            spigotYMLConfig.save(new File("spigot.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        chestKey = new NamespacedKey(plugin, "mascots_chest");
        mascotsKey = new NamespacedKey(plugin, "mascotsKey");

        mascots = getAllMascots();

        for (Mascot mascot : mascots){
            Entity mob = MascotUtils.loadMascot(mascot);
            if (mascot.isImmunity()){
                if (mob != null) mob.setGlowing(true);
            } else if (mob != null) mob.setGlowing(false);
        }
    }

    public static void init_db(Connection conn) throws SQLException {
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS mascots (city_uuid VARCHAR(8) NOT NULL PRIMARY KEY, level INT NOT NULL, mascot_uuid VARCHAR(36) NOT NULL, immunity BOOLEAN NOT NULL, alive BOOLEAN NOT NULL, x MEDIUMINT NOT NULL, z MEDIUMINT NOT NULL);").executeUpdate();
    }

    public static List<Mascot> getAllMascots() {
        List<Mascot> mascots = new ArrayList<>();

        String query = "SELECT city_uuid, mascot_uuid, level, immunity, alive, x, z FROM mascots";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                World world = Bukkit.getWorld("world");

                String cityUuid = rs.getString("city_uuid");
                String mascotUuid = rs.getString("mascot_uuid");
                int level = rs.getInt("level");
                boolean immunity = rs.getBoolean("immunity");
                boolean alive = rs.getBoolean("alive");
                Chunk chunk = world.getChunkAt(rs.getInt("x"), rs.getInt("z"));
                mascots.add(new Mascot(cityUuid, UUID.fromString(mascotUuid), level, immunity, alive, chunk)); // Ajouter à la liste
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mascots;
    }

    public static void saveFreeClaims(HashMap<String, Integer> freeClaims){
        String query;

        if (OMCPlugin.isUnitTestVersion()) {
            query = "MERGE INTO free_claim KEY(city_uuid) VALUES (?, ?)";
        } else {
            query = "INSERT INTO free_claim (city_uuid, claim) VALUES (?, ?) ON DUPLICATE KEY UPDATE claim = ?";
        }
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
            for (Map.Entry<String, Integer> entry : freeClaims.entrySet()) {
                if (entry.getValue() > 0) {
                    statement.setString(1, entry.getKey());
                    statement.setInt(2, entry.getValue());
                    statement.setInt(3, entry.getValue());
                    statement.addBatch();
                } else {
                        try (PreparedStatement deleteStatement = DatabaseManager.getConnection().prepareStatement(
                                "DELETE FROM free_claim WHERE city_uuid = ?")) {
                            deleteStatement.setString(1, entry.getKey());
                            deleteStatement.executeUpdate();
                        }
                    }

                }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMascots(List<Mascot> mascots) {
        String query;

        if (OMCPlugin.isUnitTestVersion()) {
            query = "MERGE INTO mascots " +
                    "KEY(city_uuid) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            query = "INSERT INTO mascots (city_uuid, mascot_uuid, level, immunity, alive, x, z) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ? )" +
                    "ON DUPLICATE KEY UPDATE mascot_uuid = ?, level = ?, immunity = ?, alive = ?, x = ?, z = ?";
        }

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
            for (Mascot mascot : mascots) {

                statement.setString(1, mascot.getCityUuid());
                statement.setString(2, mascot.getMascotUuid().toString());
                statement.setInt(3, mascot.getLevel());
                statement.setBoolean(4, mascot.isImmunity());
                statement.setBoolean(5, mascot.isAlive());
                statement.setInt(6, mascot.getChunk().getX());
                statement.setInt(7, mascot.getChunk().getZ());


                statement.setString(8, mascot.getMascotUuid().toString());
                statement.setInt(9, mascot.getLevel());
                statement.setBoolean(10, mascot.isImmunity());
                statement.setBoolean(11, mascot.isAlive());
                statement.setInt(12, mascot.getChunk().getX());
                statement.setInt(13, mascot.getChunk().getZ());

                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createMascot(String city_uuid, World player_world, Location mascot_spawn) {
        LivingEntity mob = (LivingEntity) player_world.spawnEntity(mascot_spawn,EntityType.ZOMBIE);

        Chunk chunk = mascot_spawn.getChunk();
        setMascotsData(mob,null, 300, 300);
        mob.setGlowing(true);

        PersistentDataContainer data = mob.getPersistentDataContainer();
        // L'uuid de la ville lui est approprié pour l'identifier
        data.set(mascotsKey, PersistentDataType.STRING, city_uuid);

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO mascots VALUE (?, 1, ?, true, true, ?, ?)");
                statement.setString(1, city_uuid);
                statement.setString(2, String.valueOf(mob.getUniqueId()));
                statement.setInt(3, chunk.getX());
                statement.setInt(4, chunk.getZ());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        MascotUtils.addMascotForCity(city_uuid, mob.getUniqueId(), chunk);
        // Immunité persistante de 7 jours pour la mascotte
        DynamicCooldownManager.use(city_uuid, "mascot:immunity", IMMUNITY_COOLDOWN);
    }

    public static void removeMascotsFromCity(String city_uuid) {
        Mascot mascot = MascotUtils.getMascotOfCity(city_uuid);

        if (mascot!=null){
            LivingEntity mascots = MascotUtils.loadMascot(mascot);
            if (mascots!=null){
                mascots.remove();
            }
            Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
                try {
                    PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("DELETE FROM mascots WHERE city_uuid = ?");
                    statement.setString(1, city_uuid);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }

        MascotUtils.removeMascotOfCity(city_uuid);
    }

    public static void giveMascotsEffect(UUID playerUUID) {
        if (Bukkit.getPlayer(playerUUID) instanceof Player player) {
            City city = CityManager.getPlayerCity(playerUUID);
            if (city!=null) {
                if (MascotUtils.mascotsContains(city.getUUID())) {
                    if (!MascotUtils.getMascotState(city.getUUID())) {
                        if (MascotUtils.mascotsContains(city.getUUID())) {
                            int level = MascotUtils.getMascotLevel(city.getUUID());
                            if (!MascotUtils.getMascotState(city.getUUID())) {
                                for (PotionEffect potionEffect : MascotsLevels.valueOf("level" + level).getMalus()) {
                                    player.addPotionEffect(potionEffect);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void reviveMascots(String city_uuid) {
        if (MascotUtils.mascotsContains(city_uuid)){

            MascotUtils.changeMascotState(city_uuid, true);
            MascotUtils.changeMascotImmunity(city_uuid, false);
            int level = MascotUtils.getMascotLevel(city_uuid);
            Mascot mascot = MascotUtils.getMascotOfCity(city_uuid);

            if (mascot!=null){

                LivingEntity entity = MascotUtils.loadMascot(mascot);

                if (entity!=null){

                    entity.setHealth(Math.floor(0.10 * entity.getMaxHealth()));
                    entity.setCustomName("§l" + MascotUtils.getCityFromMascot(mascot.getMascotUuid()).getName() + " §c" + entity.getHealth() + "/" + entity.getMaxHealth() + "❤");
                    entity.setGlowing(false);
                    MascotsListener.mascotsRegeneration(mascot.getMascotUuid());
                    City city = CityManager.getCity(city_uuid);

                    if (city==null){return;}

                    for (UUID townMember : city.getMembers()){
                        if (Bukkit.getEntity(townMember) instanceof Player player){
                            for (PotionEffect potionEffect : MascotsLevels.valueOf("level"+level).getMalus()){
                                player.removePotionEffect(potionEffect.getType());
                            }
                        }
                    }
                }
            }
        }
    }

    public static void upgradeMascots(String city_uuid) {
        Mascot mascot = MascotUtils.getMascotOfCity(city_uuid);
        if (mascot==null){
            return;
        }
        LivingEntity mob = MascotUtils.loadMascot(mascot);
        if (mob==null){
            return;
        }
        if (MascotUtils.isMascot(mob)){

            MascotsLevels mascotsLevels = MascotsLevels.valueOf("level" + MascotUtils.getMascotLevel(city_uuid));
            double lastHealth = mascotsLevels.getHealth();
            if (mascotsLevels != MascotsLevels.level10){

                MascotUtils.setMascotLevel(city_uuid, MascotUtils.getMascotLevel(city_uuid)+1);
                mascotsLevels = MascotsLevels.valueOf("level" + MascotUtils.getMascotLevel(city_uuid));

                try {
                    int maxHealth = mascotsLevels.getHealth();
                    mob.setMaxHealth(maxHealth);
                    if (mob.getHealth() == lastHealth){
                        mob.setHealth(maxHealth);
                    }
                    double currentHealth = mob.getHealth();
                    mob.setCustomName("§l" + MascotUtils.getCityFromMascot(mascot.getMascotUuid()).getName() + " §c" + currentHealth + "/" + maxHealth + "❤");
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public static void changeMascotsSkin(Entity mascots, EntityType skin, Player player, Material matAywenite, int aywenite) {
        World world = Bukkit.getWorld("world");
        Location mascotsLoc = mascots.getLocation();
        Mascot mascot = MascotUtils.getMascotByEntity(mascots);
        if (mascot==null){
            return;
        }
        LivingEntity mob = MascotUtils.loadMascot(mascot);
        boolean glowing = mascots.isGlowing();
        long cooldown = 0;
        boolean hasCooldown = false;

        // to avoid the suffocation of the mascot when it changes skin to a spider for exemple
        if (mascotsLoc.clone().add(0, 1, 0).getBlock().getType().isSolid() && mob.getHeight() <= 1.0) {
            MessagesManager.sendMessage(player, Component.text("Libérez de l'espace au dessus de la macotte pour changer son skin"), Prefix.CITY, MessageType.INFO, false);
            return;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location checkLoc = mascotsLoc.clone().add(x, 0, z);
                Material blockType = checkLoc.getBlock().getType();

                if (blockType != Material.AIR) {
                    MessagesManager.sendMessage(player, Component.text("Libérez de l'espace tout autour de la macotte pour changer son skin"), Prefix.CITY, MessageType.INFO, false);
                    return;
                }
            }
        }

        double baseHealth = mob.getHealth();
        double maxHealth = mob.getMaxHealth();
        String name = mob.getCustomName();
        String mascotsCustomUUID = mob.getPersistentDataContainer().get(mascotsKey, PersistentDataType.STRING);

        if (!DynamicCooldownManager.isReady(mascots.getUniqueId().toString(), "mascots:move")) {
            cooldown = DynamicCooldownManager.getRemaining(mascots.getUniqueId().toString(), "mascots:move");
            hasCooldown = true;
            DynamicCooldownManager.clear(mob.getUniqueId().toString(), "mascots:move");
        }

        mob.remove();

        if (world != null) {
            LivingEntity newMascots = (LivingEntity) world.spawnEntity(mascotsLoc, skin);
            newMascots.setGlowing(glowing);

            if (hasCooldown){
                DynamicCooldownManager.use(newMascots.getUniqueId().toString(), "mascots:move" , cooldown);
            }

            setMascotsData(newMascots, name, maxHealth, baseHealth);
            PersistentDataContainer newData = newMascots.getPersistentDataContainer();

            if (mascotsCustomUUID != null) {
                newData.set(mascotsKey, PersistentDataType.STRING, mascotsCustomUUID);
                MascotUtils.setMascotUUID(mascotsCustomUUID, newMascots.getUniqueId());
            }
        }
        ItemUtils.removeItemsFromInventory(player, matAywenite, aywenite);
    }


    private static void setMascotsData(LivingEntity mob, String customName, double maxHealth, double baseHealth) {
        mob.setAI(false);
        mob.setMaxHealth(maxHealth);
        mob.setHealth(baseHealth);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);

        mob.setCustomName(Objects.requireNonNullElseGet(customName, () -> "§lMascotte §c" + mob.getHealth() + "/300❤"));
        mob.setCustomNameVisible(true);

        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, true));

        mob.setCanPickupItems(false);

        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) {
            equipment.clear();

            equipment.setHelmetDropChance(0f);
            equipment.setChestplateDropChance(0f);
            equipment.setLeggingsDropChance(0f);
            equipment.setBootsDropChance(0f);
            equipment.setItemInMainHandDropChance(0f);
            equipment.setItemInOffHandDropChance(0f);
        }
    }

}
