package fr.openmc.core.features.city;

import com.sk89q.worldedit.math.BlockVector2;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.city.events.*;
import fr.openmc.core.features.city.mayor.CityLaw;
import fr.openmc.core.features.city.mayor.ElectionType;
import fr.openmc.core.features.city.mayor.Mayor;
import fr.openmc.core.features.city.mayor.managers.MayorManager;
import fr.openmc.core.features.city.mayor.managers.NPCManager;
import fr.openmc.core.features.city.mayor.managers.PerkManager;
import fr.openmc.core.features.city.mayor.perks.Perks;
import fr.openmc.core.features.city.menu.ChestMenu;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.CacheOfflinePlayer;
import fr.openmc.core.utils.InputUtils;
import fr.openmc.core.utils.database.DatabaseManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static fr.openmc.core.features.city.mayor.managers.MayorManager.*;

public class City {
    private final String cityUUID;
    private HashMap<UUID, Set<CPermission>> permsCache = new HashMap<>();
    private Set<UUID> members = new HashSet<>();
    private Double balance = Double.valueOf(0); // set default value cause if its null, error in updateBalance
    private String name;
    private Integer chestPages;
    private Set<BlockVector2> chunks = new HashSet<>(); // Liste des chunks claims par la ville
    private HashMap<Integer, ItemStack[]> chestContent = new HashMap<>();
    private MayorManager mayorManager;

    @Getter @Setter private UUID chestWatcher;
    @Getter @Setter private ChestMenu chestMenu;

    public City(String uuid) {
        this.cityUUID = uuid;

        CityManager.registerCity(this);

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT city_uuid, player, permission FROM city_permissions WHERE city_uuid = ?");
            statement.setString(1, cityUUID);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                try {
                    UUID player = UUID.fromString(rs.getString("player"));
                    CPermission permission = CPermission.valueOf(rs.getString("permission"));

                    Set<CPermission> playerPerms = permsCache.getOrDefault(player, new HashSet<>());
                    playerPerms.add(permission);
                    permsCache.put(player, playerPerms);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid permission: " + rs.getString("permission"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT balance FROM city WHERE uuid = ?");
            statement.setString(1, cityUUID);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                try {
                    balance = rs.getDouble("balance");
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid permission: " + rs.getString("permission"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.mayorManager = MayorManager.getInstance();
    }

    public ItemStack[] getChestContent(int page) {
        if (chestContent.containsKey(page)) {
            return chestContent.get(page);
        }

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT content FROM city_chests WHERE city_uuid = ? AND page = ? LIMIT 1");
            statement.setString(1, cityUUID);
            statement.setInt(2, page);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                byte[] content = rs.getBytes("content");
                if (content == null) {
                    return new ItemStack[54];
                }
                chestContent.put(page, ItemStack.deserializeItemsFromBytes(content));
                return chestContent.get(page);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // On ne peut pas retourner une liste vide, car s'il ferme, ça va reset son inv
            throw new RuntimeException("Error while loading chest content");
        }
        return new ItemStack[54]; // ayayay
    }

    public void saveChestContent(int page, ItemStack[] content) {
        chestContent.put(page, content);

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("UPDATE city_chests SET content=? WHERE city_uuid=? AND page=?");
                statement.setBytes(1, ItemStack.serializeItemsAsBytes(content));
                statement.setString(2, cityUUID);
                statement.setInt(3, page);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public String getUUID() {
        return cityUUID;
    }

    public void addChunk(Chunk chunk) {
        getChunks(); // Load chunks

        if (chunks.contains(BlockVector2.at(chunk.getX(), chunk.getZ()))) return;
        chunks.add(BlockVector2.at(chunk.getX(), chunk.getZ()));

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_regions (city_uuid, x, z) VALUES (?, ?, ?)");
                statement.setString(1, cityUUID);
                statement.setInt(2, chunk.getX());
                statement.setInt(3, chunk.getZ());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
            Bukkit.getPluginManager().callEvent(new ChunkClaimedEvent(this, chunk));
        });
    }

    public boolean removeChunk(int chunkX, int chunkZ) {
        getChunks(); // Load chunks

        if (!chunks.contains(BlockVector2.at(chunkX, chunkZ))) return false;
        chunks.remove(BlockVector2.at(chunkX, chunkZ));

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_regions (city_uuid, x, z) VALUES (?, ?, ?)");
                statement.setString(1, cityUUID);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return true;
    }

    public @NotNull Set<BlockVector2> getChunks() {
        if (!chunks.isEmpty()) return chunks;

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT x, z FROM city_regions WHERE city_uuid = ?");
            statement.setString(1, cityUUID);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                chunks.add(BlockVector2.at(resultSet.getInt("x"), resultSet.getInt("z")));
            }

            return chunks;
        } catch (SQLException err) {
            err.printStackTrace();
            return Set.of();
        }
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        getChunks(); // Load chunks
        return chunks.contains(BlockVector2.at(chunkX, chunkZ));
    }

    public @NotNull Integer getChestPages() {
        if (chestPages != null) return chestPages;

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT COUNT(page) FROM city_chests WHERE city_uuid = ?");
            statement.setString(1, cityUUID);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                chestPages = resultSet.getInt(1);
                return chestPages;
            }
        } catch (SQLException err) {
            err.printStackTrace();
        }
        return 0;
    }

    public @NotNull String getName() {
        if (name != null) return name;
        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT name FROM city WHERE uuid = ?");
            statement.setString(1, cityUUID);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                name = resultSet.getString("name");
                return name;
            }
        } catch (SQLException err) {
            err.printStackTrace();
        }
        return "inconnu";
    }

    public ElectionType getElectionType() {
        Mayor mayor = mayorManager.cityMayor.get(this);
        if (mayor == null) return null;

        return mayor.getElectionType();
    }

    public Mayor getMayor() {
        MayorManager mayorManager = MayorManager.getInstance();

        return mayorManager.cityMayor.get(CityManager.getCity(cityUUID));
    }

    public boolean hasMayor() {
        Mayor mayor = mayorManager.cityMayor.get(this);
        if (mayor == null) return false;

        return mayor.getUUID() != null;
    }

    public CityLaw getLaw() {
        MayorManager mayorManager = MayorManager.getInstance();

        return mayorManager.cityLaws.get(CityManager.getCity(cityUUID));
    }

    /**
     * Gets the list of members (UUIDs) of a specific city.
     *
     * @return A list of UUIDs representing the members of the city.
     */
    public Set<UUID> getMembers() {
        if (!members.isEmpty()) return members;

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT player FROM city_members WHERE city_uuid = ?");
            statement.setString(1, cityUUID);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                members.add(UUID.fromString(resultSet.getString(1)));
                CityManager.cachePlayer(UUID.fromString(resultSet.getString(1)), this);
            }

            return members;
        } catch (SQLException err) {
            err.printStackTrace();
            return Set.of();
        }
    }

    public boolean isMember(Player player) {
        return this.getMembers().contains(player.getUniqueId());
    }

    /**
     * Sets the balance for a given City and updates it in the database asynchronously.
     *
     * @param value The new balance value to be set.
     */
    public void setBalance(Double value) {
        Double old = getBalance();
        balance = value;
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("UPDATE city SET balance=? WHERE uuid=?;");
                statement.setDouble(1, value);
                statement.setString(2, cityUUID);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                balance = old;
            }
        });
    }

    public int addChestPages() {
        chestPages += 1;
        chestContent.put(chestPages, new ItemStack[54]);
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_chests (city_uuid, page) VALUES (?, ?)");
                statement.setString(1, cityUUID);
                statement.setInt(2, chestPages);
                statement.executeUpdate();
                chestContent.remove(chestPages);
            } catch (SQLException e) {
                e.printStackTrace();
                chestPages -= 1;
            }
        });
        return chestPages;
    }

    /**
     * Renames a city.
     *
     * @param newName The new name for the city.
     */
    public void renameCity(String newName) {
        Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
                    Bukkit.getPluginManager().callEvent(new CityRenameEvent(this.name, this));
                });
        name = newName;

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("UPDATE city SET name=? WHERE uuid=?;");
                statement.setString(1, newName);
                statement.setString(2, cityUUID);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Retrieves the balance for a given UUID. If the balance is not cached, it retrieves it from the database.
     *
     * @return The balance of the city, or 0 if no balance is found or an error occurs.
     */
    @NotNull
    public Double getBalance() {
        if (balance != null) return balance;

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT balance FROM city WHERE uuid = ?");
            statement.setString(1, cityUUID);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                balance = resultSet.getDouble("balance");
                return balance;
            }
        } catch (SQLException err) {
            err.printStackTrace();
        }

        return 0d;
    }

    /**
     * Changes the owner of a city.
     *
     * @param player The UUID of the new owner.
     */
    public void changeOwner(UUID player) {
        removePermission(getPlayerWith(CPermission.OWNER), CPermission.OWNER);
        addPermission(player, CPermission.OWNER);

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("UPDATE city SET owner=? WHERE uuid=?;");
                statement.setString(1, player.toString());
                statement.setString(2, cityUUID);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Updates the balance for a given City by adding a difference amount and updating it in the database asynchronously.
     *
     * @param diff The amount to be added to the existing balance.
     */
    public void updateBalance(Double diff) {
        Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
                    Bukkit.getPluginManager().callEvent(new CityMoneyUpdateEvent(this, balance, balance + diff));
                });
        setBalance(balance+diff);
    }

    /**
     * Adds money to the city bank and removes it from {@link Player}
     * @param player The player depositing into the bank
     * @param input The input string to get the money value
     */
    public void depositCityBank(Player player, String input) {
        if (InputUtils.isInputMoney(input)) {
            double moneyDeposit = InputUtils.convertToMoneyValue(input);

            if (EconomyManager.getInstance().withdrawBalance(player.getUniqueId(), moneyDeposit)) {
                updateBalance(moneyDeposit);
                MessagesManager.sendMessage(player, Component.text("Tu as transféré §d" + EconomyManager.getInstance().getFormattedSimplifiedNumber(moneyDeposit) + "§r" + EconomyManager.getEconomyIcon() + " à ta ville"), Prefix.CITY, MessageType.ERROR, false);
            } else {
                MessagesManager.sendMessage(player, MessagesManager.Message.MONEYPLAYERMISSING.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            }
        } else {
            MessagesManager.sendMessage(player, Component.text("Veuillez mettre une entrée correcte"), Prefix.CITY, MessageType.ERROR, true);
        }
    }

    /**
     * Removes money from the city bank and add it to {@link Player}
     * @param player The player withdrawing from the bank
     * @param input The input string to get the money value
     */
    public void withdrawCityBank(Player player, String input) {
        if (InputUtils.isInputMoney(input)) {
            double moneyDeposit = InputUtils.convertToMoneyValue(input);

            if (getBalance() < moneyDeposit) {
                MessagesManager.sendMessage(player, Component.text("Ta ville n'a pas assez d'argent en banque"), Prefix.CITY, MessageType.ERROR, false);
            } else {
                updateBalance(moneyDeposit * -1);
                EconomyManager.getInstance().addBalance(player.getUniqueId(), moneyDeposit);
                MessagesManager.sendMessage(player, Component.text("§d" + EconomyManager.getInstance().getFormattedSimplifiedNumber(moneyDeposit) + "§r" + EconomyManager.getEconomyIcon() + " ont été transférés à votre compte"), Prefix.CITY, MessageType.SUCCESS, false);
            }
        } else {
            MessagesManager.sendMessage(player, Component.text("Veuillez mettre une entrée correcte"), Prefix.CITY, MessageType.ERROR, true);
        }
    }

    /**
     * Updates the power of a City by adding or removing points.
     *
     * @param point The amount to be added or remove to the existing power.
     */
    public void updatePowerPoints(int point){
        try {
            int result = CityManager.getCityPowerPoints(cityUUID) + point;
            if (result<0)result=0;
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("UPDATE city_power SET power_point=? WHERE city_uuid=?;");
            statement.setInt(1, result);
            statement.setString(2, cityUUID);
            statement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the name of a city by its UUID.
     *
     * @return The name of the city, or null if the city does not exist.
     */
    @Nullable
    public String getCityName() {
        if (name != null) return name;

        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT name FROM city WHERE uuid = ? LIMIT 1");
            statement.setString(1, cityUUID);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                name = resultSet.getString("name");
                return name;
            }
        } catch (SQLException err) {
            err.printStackTrace();
        }
        return "inconnu";
    }

    private boolean loadPermission(UUID player) {
        if (!permsCache.containsKey(player)) {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT city_uuid, player, permission FROM city_permissions WHERE city_uuid = ? AND player = ?");
                statement.setString(1, cityUUID);
                statement.setString(2, player.toString());
                ResultSet rs = statement.executeQuery();

                Set<CPermission> plrPerms = permsCache.getOrDefault(player, new HashSet<>());

                while (rs.next()) {
                    try {
                        plrPerms.add(CPermission.valueOf(rs.getString("permission")));
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid permission: " + rs.getString("permission"));
                    }
                }

                permsCache.put(player, plrPerms);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public Set<CPermission> getPermissions(UUID player) {
        loadPermission(player);
        return permsCache.get(player);
    }

    public boolean hasPermission(UUID uuid, CPermission permission) {
        loadPermission(uuid);
        Set<CPermission> playerPerms = permsCache.get(uuid);

        if (playerPerms.contains(CPermission.OWNER)) return true;

        return playerPerms.contains(permission);
    }

    public UUID getPlayerWith(CPermission permission) {
        for (UUID player: permsCache.keySet()) {
            if (permsCache.get(player).contains(permission)) {
                return player;
            }
        }
        return null;
    }

    public boolean removePermission(UUID uuid, CPermission permission) {
        loadPermission(uuid);
        Set<CPermission> playerPerms = permsCache.get(uuid);

        if (playerPerms == null) {
            return true;
        }

        if (playerPerms.contains(permission)) {
            playerPerms.remove(permission);
            permsCache.put(uuid, playerPerms);

            Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
                try {
                    PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("DELETE FROM city_permissions WHERE city_uuid = ? AND player = ? AND permission = ?");
                    statement.setString(1, cityUUID);
                    statement.setString(2, uuid.toString());
                    statement.setString(3, permission.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
                Bukkit.getPluginManager().callEvent(new CityPermissionChangeEvent(this, CacheOfflinePlayer.getOfflinePlayer(uuid), permission, false));
            });
            return true;
        }
        return false;
    }

    /**
     * Delete every information about a player
     * @param uuid Player to forgot
     */
    public void forgetPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("DELETE FROM city_permissions WHERE city_uuid = ? AND player = ?");
                statement.setString(1, cityUUID);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                permsCache.remove(uuid);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void addPermission(UUID uuid, CPermission permission) {
        Set<CPermission> playerPerms = permsCache.getOrDefault(uuid, new HashSet<>());

        if (!playerPerms.contains(permission)) {
            playerPerms.add(permission);
            permsCache.put(uuid, playerPerms);

            Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
                try {
                    PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_permissions (city_uuid, player, permission) VALUES (?, ?, ?)");
                    statement.setString(1, cityUUID);
                    statement.setString(2, uuid.toString());
                    statement.setString(3, permission.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
                Bukkit.getPluginManager().callEvent(new CityPermissionChangeEvent(this, CacheOfflinePlayer.getOfflinePlayer(uuid), permission, true));
            });
        }
    }

    /**
     * Allows a player to leave a city and updates the database and region permissions.
     *
     * @param player The UUID of the player leaving the city.
     * @return True if the player successfully left the city, false otherwise.
     */
    public boolean removePlayer(UUID player) {
        forgetPlayer(player);
        CityManager.uncachePlayer(player);
        members.remove(player);
        Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
            Bukkit.getPluginManager().callEvent(new MemberLeaveEvent(CacheOfflinePlayer.getOfflinePlayer(player), this));
        });
        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("DELETE FROM city_members WHERE player=?");
            statement.setString(1, player.toString());
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Adds a player as a member of a specific city.
     *
     * @param player The UUID of the player to add.
     */
    public void addPlayer(UUID player) {
        members.add(player);
        Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
                    Bukkit.getPluginManager().callEvent(new MemberJoinEvent(CacheOfflinePlayer.getOfflinePlayer(player), this));
                });
        CityManager.cachePlayer(player, this);
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_members VALUE (?, ?)");
                statement.setString(1, cityUUID);
                statement.setString(2, player.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Deletes a city, removing it from records and updating members and regions accordingly.
     */
    public void delete() {
        CityManager.forgetCity(cityUUID);

        NPCManager.removeNPCS(cityUUID);

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                String[] queries = {
                        "DELETE FROM city_members WHERE city_uuid=?",
                        "DELETE FROM city WHERE uuid=?",
                        "DELETE FROM city_permissions WHERE city_uuid=?",
                        "DELETE FROM city_regions WHERE city_uuid=?",
                        "DELETE FROM city_chests WHERE city_uuid=?",
                        "DELETE FROM city_power WHERE city_uuid=?",
                        "DELETE FROM " + TABLE_MAYOR + " WHERE city_uuid = ?",
                        "DELETE FROM " + TABLE_ELECTION + " WHERE city_uuid = ?",
                        "DELETE FROM " + TABLE_VOTE + " WHERE city_uuid = ?"
                };

                for (String sql : queries) {
                    PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql);
                    statement.setString(1, cityUUID);
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
            Bukkit.getPluginManager().callEvent(new CityDeleteEvent(this));
        });
    }

    public void upgradeChest() {
        chestPages += 1;
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_chests (city_uuid, page) VALUES (?, ?)");
                statement.setString(1, cityUUID);
                statement.setInt(2, chestPages);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                chestPages -= 1;
            }
        });
    }

    // Interests calculated as proportion not percentage (eg: 0.01 = 1%)
    public double calculateCityInterest() {
        double interest = .01; // base interest is 1%

        if (MayorManager.getInstance().phaseMayor == 2) {
            if (PerkManager.hasPerk(getMayor(), Perks.BUISNESS_MAN.getId())) {
                interest = .03; // interest is 3% when perk Buisness Man actived
            }
        }

        return interest;
    }

    public void applyCityInterest() {
        double interest = calculateCityInterest();
        double amount = getBalance() * interest;
        updateBalance(amount);
    }
}
