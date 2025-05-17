package fr.openmc.core.features.city.mayor.managers;

import fr.openmc.api.cooldown.DynamicCooldownManager;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.city.CPermission;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.city.mayor.*;
import fr.openmc.core.features.city.mayor.listeners.JoinListener;
import fr.openmc.core.features.city.mayor.listeners.PhaseListener;
import fr.openmc.core.features.city.mayor.listeners.UrneListener;
import fr.openmc.core.features.city.mayor.perks.Perks;
import fr.openmc.core.features.city.mayor.perks.basic.*;
import fr.openmc.core.features.city.mayor.perks.event.*;
import fr.openmc.core.utils.CacheOfflinePlayer;
import fr.openmc.core.utils.api.FancyNpcApi;
import fr.openmc.core.utils.api.ItemAdderApi;
import fr.openmc.core.utils.database.DatabaseManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.*;

public class MayorManager {
    @Getter
    static MayorManager instance;

    private final OMCPlugin plugin;

    public int MEMBER_REQ_ELECTION = 3;

    public static final String TABLE_MAYOR = "city_mayor";
    public static final String TABLE_ELECTION = "city_election";
    public static final String TABLE_VOTE = "city_vote";
    public static final String TABLE_LAW = "city_law";
    public static final String TABLE_CONSTANTS = "mayor_constants";

    private final List<NamedTextColor> LIST_MAYOR_COLOR = List.of(
            NamedTextColor.RED,
            NamedTextColor.GOLD,
            NamedTextColor.YELLOW,
            NamedTextColor.GREEN,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.BLUE,
            NamedTextColor.AQUA,
            NamedTextColor.DARK_BLUE,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.WHITE,
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
    );

    public static final DayOfWeek PHASE_1_DAY = DayOfWeek.TUESDAY;
    public static final DayOfWeek PHASE_2_DAY = DayOfWeek.THURSDAY;

    public int phaseMayor;
    public HashMap<City, Mayor> cityMayor = new HashMap<>();
    public static HashMap<City, CityLaw> cityLaws = new HashMap<>();
    public Map<City, List<MayorCandidate>> cityElections = new HashMap<>(){};
    public Map<City, List<MayorVote>> playerVote = new HashMap<>();


    private static final Random RANDOM = new Random();


    public MayorManager(OMCPlugin plugin) {
        instance = this;

        this.plugin = plugin;

        // LISTENERS
        new PhaseListener(plugin);
        OMCPlugin.registerEvents(
                new JoinListener(),
                new RagePerk(),
                new MinerPerk(),
                new MascotFriendlyPerk(),
                new DemonFruitPerk(),
                new CityHunterPerk(),
                new AyweniterPerk(),
                new GPSTrackerPerk(),
                new SymbiosisPerk(),
                new ImpotCollection(),
                new AgriculturalEssorPerk(),
                new MineralRushPerk(),
                new MilitaryDissuasion(),
                new IdyllicRain()
        );
        if (ItemAdderApi.hasItemAdder()) {
            OMCPlugin.registerEvents(
                    new UrneListener()
            );
        }
        if (FancyNpcApi.hasFancyNpc()) {
            OMCPlugin.registerEvents(
                    new NPCManager()
            );
        }

        loadMayorConstant();
        loadCityMayors();
        loadMayorCandidates();
        loadPlayersVote();
        loadCityLaws();



//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                Bukkit.getLogger().info("===== MayorManager Debug =====");
//
//                Bukkit.getLogger().info("City Mayors:");
//                System.out.println(cityMayor);
//                for (Map.Entry<City, Mayor> entry : cityMayor.entrySet()) {
//                    Bukkit.getLogger().info(entry.getKey() + " -> " + entry.getValue().getName() + " " + entry.getValue().getUUID() + " " + entry.getValue().getIdPerk1()+ " " + entry.getValue().getIdPerk2()+ " " + entry.getValue().getIdPerk3());
//                }
//
//                Bukkit.getLogger().info("City Law:");
//                System.out.println(cityLaws);
//                for (Map.Entry<City, CityLaw> entry : cityLaws.entrySet()) {
//                    Bukkit.getLogger().info(entry.getKey() + " -> war^" + entry.getValue().getWarp() + " PVP " + entry.getValue().isPvp());
//                }
//
//                Bukkit.getLogger().info("City Elections:");
//                for (Map.Entry<City, List<MayorCandidate>> entry : cityElections.entrySet()) {
//                    Bukkit.getLogger().info(entry.getKey() + " -> " + entry.getValue());
//                }
//
//                Bukkit.getLogger().info("Player Votes:");
//                for (Map.Entry<City, List<MayorVote>> entry : playerVote.entrySet()) {
//                    Bukkit.getLogger().info(entry.getKey() + " -> " + entry.getValue());
//                }
//
//                Bukkit.getLogger().info("================================");
//            }
//        }.runTaskTimer(plugin, 0, 600L); // 600 ticks = 30 secondes
    }

    public static void init_db(Connection conn) throws SQLException {
        // create city_mayor : contient l'actuel maire et les réformes actuelles
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_MAYOR + " (city_uuid VARCHAR(8) UNIQUE, mayorUUID VARCHAR(36), mayorName VARCHAR(36), mayorColor VARCHAR(36), idPerk1 int, idPerk2 int, idPerk3 int, electionType VARCHAR(36))").executeUpdate();
        // create city_election : contient les membres d'une ville ayant participé pour etre maire
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_ELECTION + " (city_uuid VARCHAR(8) NOT NULL, candidateUUID VARCHAR(36) UNIQUE NOT NULL, candidateName VARCHAR(36) NOT NULL, candidateColor VARCHAR(36) NOT NULL, idChoicePerk2 int, idChoicePerk3 int, vote int)").executeUpdate();
        // create city_voted : contient les membres d'une ville ayant deja voté
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_VOTE + " (city_uuid VARCHAR(8) NOT NULL, voterUUID VARCHAR(36) UNIQUE NOT NULL, candidateUUID VARCHAR(36) NOT NULL)").executeUpdate();
        // create city_law : contient les parametres d'une ville
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_LAW + " (city_uuid VARCHAR(8) UNIQUE, pvp BOOLEAN NOT NULL DEFAULT FALSE, warp_x DOUBLE, warp_y DOUBLE, warp_z DOUBLE, warp_world VARCHAR(255))").executeUpdate();
        // create constants : contient une information universelle pour tout le monde
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_CONSTANTS + " (mayorPhase int)").executeUpdate();
        PreparedStatement state = conn.prepareStatement("SELECT COUNT(*) FROM " + TABLE_CONSTANTS);
        ResultSet rs = state.executeQuery();
        if (rs.next() && rs.getInt(1) == 0) {
            PreparedStatement states = conn.prepareStatement("INSERT INTO " + TABLE_CONSTANTS + " (mayorPhase) VALUES (1)");
            states.executeUpdate();
        }
    }

    // Load and Save Data Methods
    public void loadMayorConstant() {
        try (PreparedStatement states = DatabaseManager.getConnection().prepareStatement("SELECT * FROM " + TABLE_CONSTANTS + " WHERE 1")) {
            ResultSet result = states.executeQuery();
            while (result.next()) {
                phaseMayor = result.getInt("mayorPhase");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMayorConstant() {
        try (PreparedStatement states = DatabaseManager.getConnection().prepareStatement("UPDATE " + TABLE_CONSTANTS + " SET mayorPhase = ?")) {
            plugin.getLogger().info("Sauvegarde des constantes pour les Maires...");
            states.setInt(1, phaseMayor);

            states.executeUpdate();
            plugin.getLogger().info("Sauvegarde des constantes pour les Maires réussie.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Echec de la sauvegarde des constantes pour les Maires.");
            throw new RuntimeException(e);
        }
    }

    public void loadCityMayors() {
        try (PreparedStatement states = DatabaseManager.getConnection().prepareStatement("SELECT * FROM " + TABLE_MAYOR + " WHERE 1")) {
            ResultSet result = states.executeQuery();
            while (result.next()) {
                String city_uuid = result.getString("city_uuid");
                City city = CityManager.getCity(city_uuid);
                String mayorUUIDString = result.getString("mayorUUID");
                UUID mayor_uuid = (mayorUUIDString != null && !mayorUUIDString.isEmpty()) ? UUID.fromString(mayorUUIDString) : null;
                String mayor_name = result.getString("mayorName");
                mayor_name = (mayor_name != null) ? mayor_name : "Inconnu";
                NamedTextColor mayor_color = NamedTextColor.NAMES.valueOr(result.getString("mayorColor"), NamedTextColor.WHITE);
                int idPerk1 = result.getInt("idPerk1");
                int idPerk2 = result.getInt("idPerk2");
                int idPerk3 = result.getInt("idPerk3");
                String electionTypeStr = result.getString("electionType");
                ElectionType electionType = ElectionType.valueOf(electionTypeStr);

                cityMayor.put(city, new Mayor(city, mayor_name, mayor_uuid, mayor_color, idPerk1, idPerk2, idPerk3, electionType));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveCityMayors() {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(
                "INSERT INTO " + TABLE_MAYOR + " (city_uuid, mayorUUID, mayorName, mayorColor, idPerk1, idPerk2, idPerk3, electionType) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "city_uuid = VALUES(city_uuid), mayorUUID = VALUES(mayorUUID), mayorName = VALUES(mayorName), mayorColor = VALUES(mayorColor), idPerk1 = VALUES(idPerk1), idPerk2 = VALUES(idPerk2), idPerk3 = VALUES(idPerk3), electionType = VALUES(electionType)"
        )) {
            plugin.getLogger().info("Sauvegarde des données des joueurs maire...");
            cityMayor.forEach((city, mayor) -> {
                try {
                    statement.setString(1, city.getUUID());
                    statement.setString(2, mayor.getUUID() != null ? mayor.getUUID().toString() : null);
                    statement.setString(3, mayor.getName() != null ? mayor.getName() : null);
                    statement.setString(4, mayor.getMayorColor() != null ? mayor.getMayorColor().toString() : null);
                    statement.setInt(5, mayor.getIdPerk1());
                    statement.setInt(6, mayor.getIdPerk2());
                    statement.setInt(7, mayor.getIdPerk3());
                    statement.setString(8, mayor.getElectionType().toString());

                    statement.addBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            statement.executeBatch();

            plugin.getLogger().info("Sauvegarde des données des joueurs maire réussie.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Echec de la sauvegarde des données des joueurs maire.");
            e.printStackTrace();
        }
    }

    public void loadMayorCandidates() {
        try (PreparedStatement states = DatabaseManager.getConnection().prepareStatement("SELECT * FROM " + TABLE_ELECTION)) {
            ResultSet result = states.executeQuery();
            while (result.next()) {
                String city_uuid = result.getString("city_uuid");
                City city = CityManager.getCity(city_uuid);
                UUID candidate_uuid = UUID.fromString(result.getString("candidateUUID"));
                String candidate_name = result.getString("candidateName");
                NamedTextColor candidate_color = NamedTextColor.NAMES.valueOr(result.getString("candidateColor"), NamedTextColor.WHITE);
                int idChoicePerk2 = result.getInt("idChoicePerk2");
                int idChoicePerk3 = result.getInt("idChoicePerk3");
                int vote = result.getInt("vote");

                MayorCandidate mayorCandidate = new MayorCandidate(city, candidate_name, candidate_uuid, candidate_color, idChoicePerk2, idChoicePerk3, vote);

                cityElections.computeIfAbsent(city, k -> new ArrayList<>()).add(mayorCandidate);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMayorCandidates() {
        String sql = "INSERT INTO " + TABLE_ELECTION + " (city_uuid, candidateUUID, candidateName, candidateColor, idChoicePerk2, idChoicePerk3, vote) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "candidateName = VALUES(candidateName), candidateColor = VALUES(candidateColor), " +
                "idChoicePerk2 = VALUES(idChoicePerk2), idChoicePerk3 = VALUES(idChoicePerk3), vote = VALUES(vote)";

        try (Connection connection = DatabaseManager.getConnection();

             PreparedStatement statement = connection.prepareStatement(sql)) {
            plugin.getLogger().info("Sauvegarde des données des joueurs qui se sont présentés...");

            for (Map.Entry<City, List<MayorCandidate>> entry : cityElections.entrySet()) {
                City city = entry.getKey();
                List<MayorCandidate> candidates = entry.getValue();

                for (MayorCandidate candidate : candidates) {
                    statement.setString(1, city.getUUID());
                    statement.setString(2, candidate.getUUID().toString());
                    statement.setString(3, candidate.getName());
                    statement.setString(4, candidate.getCandidateColor().toString());
                    statement.setInt(5, candidate.getIdChoicePerk2());
                    statement.setInt(6, candidate.getIdChoicePerk3());
                    statement.setInt(7, candidate.getVote());

                    statement.addBatch();
                }
            }

            statement.executeBatch();
            plugin.getLogger().info("Sauvegarde des données des joueurs qui se sont présentés réussie.");

        } catch (SQLException e) {
            plugin.getLogger().severe("Échec de la sauvegarde des données des joueurs qui se sont présentés.");
            e.printStackTrace();
        }
    }

    public void loadPlayersVote() {
        try (PreparedStatement states = DatabaseManager.getConnection().prepareStatement("SELECT * FROM " + TABLE_VOTE)) {
            ResultSet result = states.executeQuery();
            while (result.next()) {
                String city_uuid = result.getString("city_uuid");
                UUID voter_uuid = UUID.fromString(result.getString("voterUUID"));
                UUID candidate_uuid = UUID.fromString(result.getString("candidateUUID"));

                City city = CityManager.getCity(city_uuid);
                if (city == null) {
                    continue;
                }

                List<MayorCandidate> candidates = cityElections.get(city);
                if (candidates == null) {
                    continue;
                }

                MayorCandidate candidateFound = null;
                for (MayorCandidate candidate : candidates) {
                    if (candidate.getUUID().equals(candidate_uuid)) {
                        candidateFound = candidate;
                        break;
                    }
                }

                if (candidateFound != null) {
                    MayorVote vote = new MayorVote(voter_uuid, candidateFound);
                    playerVote.computeIfAbsent(city, k -> new ArrayList<>()).add(vote);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePlayersVote() {
        String sql = "INSERT INTO " + TABLE_VOTE + " (city_uuid, voterUUID, candidateUUID) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "city_uuid = VALUES(city_uuid), voterUUID = VALUES(voterUUID), candidateUUID = VALUES(candidateUUID)";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            plugin.getLogger().info("Sauvegarde des données des Joueurs qui ont voté pour un maire...");

            playerVote.forEach((city, mayorVotes) -> {
                for (MayorVote vote : mayorVotes) {
                    try {
                        statement.setString(1, city.getUUID());
                        statement.setString(2, vote.getVoterUUID().toString());
                        statement.setString(3, vote.getCandidate().getUUID().toString());

                        statement.addBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });

            statement.executeBatch();
            plugin.getLogger().info("Sauvegarde des données des Joueurs qui ont voté pour un maire réussie.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Échec de la sauvegarde des données des Joueurs qui ont voté pour un maire.");
            e.printStackTrace();
        }
    }

    public void loadCityLaws() {
        String sql = "SELECT city_uuid, pvp, warp_x, warp_y, warp_z, warp_world FROM " + TABLE_LAW;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String cityUUID = resultSet.getString("city_uuid");
                boolean pvp = resultSet.getBoolean("pvp");

                double x = resultSet.getDouble("warp_x");
                double y = resultSet.getDouble("warp_y");
                double z = resultSet.getDouble("warp_z");
                String worldName = resultSet.getString("warp_world");

                Location warp = null;
                if (worldName != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        warp = new Location(world, x, y, z);
                    }
                }

                City city = CityManager.getCity(cityUUID);
                if (city != null) {
                    CityLaw law = new CityLaw(pvp, warp);
                    cityLaws.put(city, law);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveCityLaws() {
        String sql = "INSERT INTO " + TABLE_LAW + " (city_uuid, pvp, warp_x, warp_y, warp_z, warp_world) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "pvp = VALUES(pvp), warp_x = VALUES(warp_x), warp_y = VALUES(warp_y), " +
                "warp_z = VALUES(warp_z), warp_world = VALUES(warp_world)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            plugin.getLogger().info("Sauvegarde des lois des villes...");

            for (Map.Entry<City, CityLaw> entry : cityLaws.entrySet()) {
                City city = entry.getKey();
                CityLaw law = entry.getValue();

                statement.setString(1, city.getUUID());
                statement.setBoolean(2, law.isPvp());
                statement.setDouble(3, law.getWarp() != null ? law.getWarp().getX() : 0);
                statement.setDouble(4, law.getWarp() != null ? law.getWarp().getY() : 0);
                statement.setDouble(5, law.getWarp() != null ? law.getWarp().getZ() : 0);
                statement.setString(6, law.getWarp() != null ? law.getWarp().getWorld().getName() : "");

                statement.addBatch();
            }

            statement.executeBatch();
            plugin.getLogger().info("Sauvegarde des lois des villes réussie.");

        } catch (SQLException e) {
            plugin.getLogger().severe("Échec de la sauvegarde des lois des villes.");
            e.printStackTrace();
        }
    }

    // setup elections
    public void initPhase1() throws SQLException {
        // ---OUVERTURE DES ELECTIONS---
        phaseMayor = 1;

        DynamicCooldownManager.clear("city:agricultural_essor");
        DynamicCooldownManager.clear("city:mineral_rush");

        // On vide toutes les tables
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            String deleteSql1 = "DELETE FROM " + TABLE_MAYOR;
            String deleteSql2 = "DELETE FROM " + TABLE_VOTE;
            String deleteSql3 = "DELETE FROM " + TABLE_ELECTION;
            try (Connection connection = DatabaseManager.getConnection()) {
                PreparedStatement deleteStmt1 = connection.prepareStatement(deleteSql1);
                PreparedStatement deleteStmt2 = connection.prepareStatement(deleteSql2);
                PreparedStatement deleteStmt3 = connection.prepareStatement(deleteSql3);

                deleteStmt1.executeUpdate();
                deleteStmt2.executeUpdate();
                deleteStmt3.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Échec du vidage des tables pour les Maires");
                e.printStackTrace();
            }
        });
        HashMap<City, Mayor> copyCityMayor = cityMayor;
        cityMayor = new HashMap<>();
        cityElections = new HashMap<>(){};
        playerVote = new HashMap<>();
        for (City city : CityManager.getCities()) {
            // PERKS INIT
            for (UUID uuid : city.getMembers()) {
                OfflinePlayer offlinePlayer = CacheOfflinePlayer.getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline()) {
                    Player player = offlinePlayer.getPlayer();
                    // Fou de Rage
                    if (PerkManager.hasPerk(copyCityMayor.get(city), Perks.FOU_DE_RAGE.getId())) {
                        player.removePotionEffect(PotionEffectType.STRENGTH);
                        player.removePotionEffect(PotionEffectType.RESISTANCE);
                    }

                    // Mineur Dévoué
                    if (PerkManager.hasPerk(copyCityMayor.get(city), Perks.MINER.getId())) {
                        MinerPerk.updatePlayerEffects(player);
                    }


                    // Mascotte de Compagnie
                    if (PerkManager.hasPerk(copyCityMayor.get(city), Perks.MASCOTS_FRIENDLY.getId())) {
                        MascotFriendlyPerk.updatePlayerEffects(player);
                    }

                    // Fruit du Démon
                    if (PerkManager.hasPerk(copyCityMayor.get(city), Perks.FRUIT_DEMON.getId())) {
                        DemonFruitPerk.removeReachBonus(player);
                    }
                }
            }

            if (city.getMembers().size()>=MEMBER_REQ_ELECTION) {
                createMayor(null,null, city, null, null, null, null, ElectionType.ELECTION);
            }
            createMayor(null, null, city, null, null, null, null, ElectionType.OWNER_CHOOSE);

        }

        NPCManager.updateAllNPCS();

        Bukkit.broadcast(Component.text("""
                        §8§m                                                     §r
                        §7
                        §3§lMAIRE!§r §7Les Elections sont ouvertes !§7
                        §8§oPrésentez vous, votez pour des maires, ...
                        §8§oRegardez si vous avez assez de membres!
                        §7
                        §8§m                                                     §r"""
        ));
    }

    public void initPhase2() {
        phaseMayor = 2;

        // TRAITEMENT DE CHAQUE VILLE - Complexité de O(n log(n))
        for (City city : CityManager.getCities()) {
            runSetupMayor(city);

            for (UUID uuid : city.getMembers()) {
                OfflinePlayer offlinePlayer = CacheOfflinePlayer.getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline()) {
                    Player player = offlinePlayer.getPlayer();
                    // Mineur Dévoué
                    if (PerkManager.hasPerk(city.getMayor(), Perks.MINER.getId())) {
                        MinerPerk.updatePlayerEffects(player);
                    }

                    // Mascotte de Compagnie
                    if (PerkManager.hasPerk(city.getMayor(), Perks.MASCOTS_FRIENDLY.getId())) {
                        MascotFriendlyPerk.updatePlayerEffects(player);
                    }

                    // Fruit du Démon
                    if (PerkManager.hasPerk(city.getMayor(), Perks.FRUIT_DEMON.getId())) {
                        DemonFruitPerk.applyReachBonus(player);
                    }
                }
            }
        }

        NPCManager.updateAllNPCS();

        Bukkit.broadcast(Component.text("""
                        §8§m                                                     §r
                        §7
                        §3§lMAIRE!§r §7Vos Réformes sont actives !§7
                        §8§oFaites vos stratégies, farmez, et pleins d'autres choses !
                        §7
                        §8§m                                                     §r"""
        ));
    }

    /**
     * Create a new mayor for the city with the given perks and color.
     *
     * @param city The city to update mayor
     */
    public void runSetupMayor(City city) {
        UUID ownerUUID = city.getPlayerWith(CPermission.OWNER);
        String ownerName = CacheOfflinePlayer.getOfflinePlayer(ownerUUID).getName();
        Mayor mayor = city.getMayor();

        if (city.getElectionType() == ElectionType.OWNER_CHOOSE) {
            // si maire a pas choisis les perks
            if ((mayor.getIdPerk1() == 0) && (mayor.getIdPerk2() == 0) && (mayor.getIdPerk3() == 0)) {
                NamedTextColor color = getRandomMayorColor();
                List<Perks> perks = PerkManager.getRandomPerksAll();
                createMayor(ownerName, ownerUUID, city, perks.getFirst(), perks.get(1), perks.get(2), color, ElectionType.OWNER_CHOOSE);
            }
        } else {
            // si owner a pas choisi perk event
            if (mayor.getIdPerk1() == 0) {
                mayor.setIdPerk1(PerkManager.getRandomPerkEvent().getId());
            }

            if (cityElections.containsKey(city)) { // si y'a des maires qui se sont présenter
                List<MayorCandidate> candidates = cityElections.get(city);

                // Code fait avec ChatGPT pour avoir une complexité de O(n log(n)) au lieu de 0(n²)
                PriorityQueue<MayorCandidate> candidateQueue = new PriorityQueue<>(
                        Comparator.comparingInt(MayorCandidate::getVote).reversed()
                );
                candidateQueue.addAll(candidates);

                MayorCandidate mayorWinner = candidateQueue.peek();
                Perks perk1 = PerkManager.getPerkById(mayor.getIdPerk1());
                Perks perk2 = PerkManager.getPerkById(mayorWinner.getIdChoicePerk2());
                Perks perk3 = PerkManager.getPerkById(mayorWinner.getIdChoicePerk3());

                createMayor(mayorWinner.getName(), mayorWinner.getUUID(), city, perk1, perk2, perk3, mayorWinner.getCandidateColor(), ElectionType.ELECTION);

            } else {
                // personne s'est présenté, owner = maire
                NamedTextColor color = getRandomMayorColor();
                List<Perks> perks = PerkManager.getRandomPerksBasic();
                createMayor(ownerName, ownerUUID, city, PerkManager.getPerkById(mayor.getIdPerk1()), perks.getFirst(), perks.get(1), color, ElectionType.ELECTION);

            }
        }


        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                String[] queries = {
                        "DELETE FROM " + TABLE_ELECTION + " WHERE city_uuid = ?",
                        "DELETE FROM " + TABLE_VOTE + " WHERE city_uuid = ?"
                };

                for (String sql : queries) {
                    PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql);
                    statement.setString(1, city.getUUID());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // on supprime donc les elections de la ville ou le maire a été élu
        cityElections.remove(city);
        // on supprime donc les votes de la ville ou le maire a été élu
        playerVote.remove(city);
    }

    /**
     * Create a new candidate for the city with the given perks and color.
     *
     * @param city The city to add candidate
     * @param candidate The candidate to add
     */
    public void createCandidate(City city, MayorCandidate candidate) {
        List<MayorCandidate> candidates = cityElections.computeIfAbsent(city, key -> new ArrayList<>());

        candidates.add(candidate);
    }

    /**
     * Get the candidate for the player in the city.
     *
     * @param player The player to get candidate
     */
    public MayorCandidate getCandidate(Player player) {
        UUID playerUUID = player.getUniqueId();

        for (List<MayorCandidate> candidates : cityElections.values()) {
            for (MayorCandidate candidate : candidates) {
                if (candidate.getUUID().equals(playerUUID)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * Has the player candidated for the city.
     *
     * @param player The player to check
     */
    public boolean hasCandidated(Player player) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        if (cityElections.get(playerCity) == null) return false;

        return cityElections.get(playerCity)
                .stream()
                .anyMatch(candidate -> candidate.getUUID().equals(player.getUniqueId()));
    }

    /**
     * Remove the player from the vote list.
     *
     * @param player The player to remove vote
     */
    public void removeVotePlayer(Player player) {
        playerVote.forEach((city, votes) ->
                votes.removeIf(vote -> vote.getVoterUUID().equals(player.getUniqueId()))
        );
    }

    /**
     * Vote a candidate for the player in the city.
     *
     * @param playerCity The city where player are
     * @param player The player who vote
     * @param candidate The candidate to vote
     */
    public void voteCandidate(City playerCity, Player player, MayorCandidate candidate) {
        candidate.setVote(candidate.getVote() + 1);
        List<MayorVote> votes = playerVote.computeIfAbsent(playerCity, key -> new ArrayList<>());

        votes.add(new MayorVote(player.getUniqueId(), candidate));
    }

    /**
     * Check if the player has voted for the city.
     *
     * @param player The player to check
     */
    public boolean hasVoted(Player player) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        if (playerVote.get(playerCity) == null) return false;

        return playerVote.get(playerCity)
                .stream()
                .anyMatch(mayorVote -> mayorVote.getVoterUUID().equals(player.getUniqueId()));
    }

    /**
     * Get the player vote for the city.
     *
     * @param player The player to get vote
     */
    public MayorCandidate getPlayerVote(Player player) {
        for (List<MayorVote> votes : playerVote.values()) {
            for (MayorVote vote : votes) {
                if (vote.getVoterUUID().equals(player.getUniqueId())) {
                    return vote.getCandidate();
                }
            }
        }

        return null;
    }

    /**
     * Check if the owner has a choice perk.
     *
     * @param player The player to check
     */
    public boolean hasChoicePerkOwner(Player player) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        Mayor mayor = cityMayor.get(playerCity);
        if (mayor == null) return false;

        return mayor.getIdPerk1() != 0;
    }

    /**
     * Set the perk for the owner.
     *
     * @param city The city to set perk
     * @param perk1 The perk to set
     */
    public void put1Perk(City city, Perks perk1) {
        Mayor mayor = cityMayor.get(city);
        if (mayor != null) {
            mayor.setIdPerk1(perk1.getId());
        } else { //au cas ou meme si théoriquement c impossible
            cityMayor.put(city, new Mayor(city, null, null, null, perk1.getId(), 0, 0, city.getElectionType()));
        }
    }

    /**
     * Create a new mayor for the city with the given perks and color.
     *
     * @param playerName The name of the mayor elected
     * @param playerUUID The UUID of the mayor elected
     * @param city The city to create mayor
     * @param perk1 The first perk of the mayor
     * @param perk2 The second perk of the mayor
     * @param perk3 The third perk of the mayor
     * @param color The color of the mayor
     * @param type The type of the election
     */
    public void createMayor(String playerName, UUID playerUUID, City city, Perks perk1, Perks perk2, Perks perk3, NamedTextColor color, ElectionType type) {
        Mayor mayor = cityMayor.get(city);
        int idPerk1 = perk1 != null ? perk1.getId() : 0;
        int idPerk2 = perk2 != null ? perk2.getId() : 0;
        int idPerk3 = perk3 != null ? perk3.getId() : 0;
        if (mayor != null) {
            mayor.setName(playerName);
            mayor.setUUID(playerUUID);
            mayor.setMayorColor(color);
            mayor.setIdPerk1(idPerk1);
            mayor.setIdPerk2(idPerk2);
            mayor.setIdPerk3(idPerk3);
            mayor.setElectionType(city.getElectionType());
        } else { // au cas ou meme si c théoriquement impossible (on défini tous les maires a la phase 1 et on le crée quand on crée la ville)
            cityMayor.put(city, new Mayor(city, playerName, playerUUID, color, idPerk1, idPerk2, idPerk3, type));
        }
    }

    /**
     * Get random mayor color from the list.
     */
    public NamedTextColor getRandomMayorColor() {
        return LIST_MAYOR_COLOR.get(RANDOM.nextInt(LIST_MAYOR_COLOR.size()));
    }

    /**
     * Create a new city law for the city with the given pvp and warp.
     *
     * @param city The city to create law
     * @param pvp The pvp of the city
     * @param locationWarp The warp location of the city
     */
    public static void createCityLaws(City city, boolean pvp, Location locationWarp) {
        CityLaw laws = city.getLaw();
        if (laws != null) {
            laws.setPvp(pvp);
            laws.setWarp(locationWarp);

        } else { // au cas ou meme si c théoriquement impossible (on défini tous les maires a la phase 1 et on le crée quand on crée la ville)
            cityLaws.put(city, new CityLaw(pvp, locationWarp));
        }
    }
}
