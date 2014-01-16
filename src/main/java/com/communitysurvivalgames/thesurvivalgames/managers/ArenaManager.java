/**
 * Name: ArenaManager.java Edited: 7 December 2013
 *
 * @version 1.0.0
 */
package com.communitysurvivalgames.thesurvivalgames.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.communitysurvivalgames.thesurvivalgames.TheSurvivalGames;
import com.communitysurvivalgames.thesurvivalgames.exception.ArenaNotFoundException;
import com.communitysurvivalgames.thesurvivalgames.locale.I18N;
import com.communitysurvivalgames.thesurvivalgames.objects.SGArena;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class ArenaManager {

    public final String prefix = ChatColor.DARK_AQUA + "[TheSurvivalGames]" + ChatColor.GOLD;
    public final String error = ChatColor.DARK_AQUA + "[TheSurvivalGames]" + ChatColor.RED;

    private final Map<String, SGArena> creators = new HashMap<>();
    private final Map<String, Location> locs = new HashMap<>();
    private static final ArenaManager am = new ArenaManager();
    private final Map<String, ItemStack[]> inv = new HashMap<>();
    private final Map<String, ItemStack[]> armor = new HashMap<>();
    private final List<SGArena> arenas = new ArrayList<>();
    private int arenaSize = 0;

    /**
     * Initialize the singleton with a SurvivalGames plugin field
     * 
     */
    public ArenaManager() {

    }

    /**
     * Gets an arena from an integer ID
     * 
     * @param i The ID to get the Arena from
     * @return The arena from which the ID represents. May be null.
     * @throws ArenaNotFoundException
     */
    public SGArena getArena(int i) throws ArenaNotFoundException {
        for (SGArena a : arenas) {
            if (a.getId() == i) {
                return a;
            }
        }
        throw new ArenaNotFoundException("Could not find given arena with given ID: " + i);
    }

    public SGArena getArena(Player p) throws ArenaNotFoundException {
        for (SGArena a : arenas) {
            if (a.getPlayers().contains(p.getName())) {
                return a;
            }
        }
        throw new ArenaNotFoundException("Could not find given arena with given Player: " + p.getDisplayName());
    }

    /**
     * Adds a player to the specified arena
     * 
     * @param p The player to be added
     * @param i The arena ID in which the player will be added to.
     */
    public void addPlayer(Player p, int i) {
        SGArena a;
        try {
            a = getArena(i);
        } catch (ArenaNotFoundException e) {
            Bukkit.getLogger().severe(e.getMessage());
            return;
        }

        if (isInGame(p)) {
            p.sendMessage(error + I18N.getLocaleString("NOT_JOINABLE"));
            return;
        }

        if (a.getState() != null && !a.getState().equals(SGArena.ArenaState.WAITING_FOR_PLAYERS)) {
            // set player to spectator
            return;
        }

        a.getPlayers().add(p.getName());
        inv.put(p.getName(), p.getInventory().getContents());
        armor.put(p.getName(), p.getInventory().getArmorContents());

        p.getInventory().setArmorContents(null);
        p.getInventory().clear();
        p.setExp(0);

        p.teleport(a.lobby);

        // Ding!
        for (Player player : SGApi.getPlugin().getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 1);
        }
    }

    /**
     * Removes the player from an arena
     * 
     * @param p The player to remove from an arena
     */
    public void removePlayer(Player p) {
        SGArena a = null;
        for (SGArena arena : arenas) {
            if (arena.getPlayers().contains(p.getName())) {
                a = arena;
            }
        }
        if (a == null || !a.getPlayers().contains(p.getName())) {
            p.sendMessage("Invalid operation!");
            return;
        }

        if (a.getSpectators().contains(p.getName()))
            a.getSpectators().remove(p.getName());
        a.getPlayers().remove(p.getName());

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        p.getInventory().setContents(inv.get(p.getName()));
        p.getInventory().setArmorContents(armor.get(p.getName()));

        inv.remove(p.getName());
        armor.remove(p.getName());
        p.teleport(locs.get(p.getName()));
        locs.remove(p.getName());

        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }

        p.setFireTicks(0);
    }

    /**
     * Creates a new arena
     * 
     * @param creator The creator attributed with making the arena
     */
    public void createArena(final Player creator, final String worldName) {
        final int num = arenaSize + 1;
        arenaSize++;

        creator.getInventory().addItem(new ItemStack(Material.BLAZE_ROD));

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(TheSurvivalGames.getPlugin(TheSurvivalGames.class), new Runnable() {

            @Override
            public void run() {
                SGArena a = new SGArena(num, MultiWorld.getInstance().createRandomWorld(worldName));
                arenas.add(a);

                creators.put(creator.getName(), a);

                // TODO Create new file configuration with default values here

                SGApi.getPlugin().saveConfig();
            }
        });

    }

    public void createArenaFromDownload(final Player creator, final String worldName) {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(TheSurvivalGames.getPlugin(TheSurvivalGames.class), new Runnable() {

            @Override
            public void run() {
                int num = arenaSize + 1;
                arenaSize++;

                SGArena a;
                a = new SGArena(num, MultiWorld.getInstance().copyFromInternet(creator, worldName));
                arenas.add(a);
            }
        });

    }

    public void createArenaFromImport(final Player creator, final String worldName) {

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(TheSurvivalGames.getPlugin(TheSurvivalGames.class), new Runnable() {

            @Override
            public void run() {
                int num = arenaSize + 1;
                arenaSize++;

                SGArena a = new SGArena(num, MultiWorld.getInstance().importWorldFromFolder(creator, worldName));
                arenas.add(a);
            }
        });

    }

    /**
     * Stores an existing arena in the list, for example after reloads
     * 
     * @param i The location the arena spawn will be at
     */
    private void reloadArena(int i) {
        FileConfiguration arenaConfig = YamlConfiguration.loadConfiguration(new File(Bukkit.getServer().getWorldContainer(), arenas.get(i).getArenaWorld().getName()));
        List<String> spawnLocsString = arenaConfig.getStringList("spawn-points");
        List<Location> spawnLocs = new ArrayList<>();
        for (String aSpawnLocsString : spawnLocsString) {
            spawnLocs.add(deserializeLoc(aSpawnLocsString));
        }
        Location lobby = deserializeLoc(arenaConfig.getString("lobby-spawn-point"));
        Location deathmatch = deserializeLoc(arenaConfig.getString("deathmatch-spawn-point"));
        int minPlayers = arenaConfig.getInt("min-players");
        int maxPlayers = arenaConfig.getInt("max-players");
        String arenaName = arenaConfig.getString("arena-name");
        arenas.get(i).initialize(spawnLocs, lobby, maxPlayers, minPlayers, arenaName);

    }

    /**
     * Removes an arena from memory
     * 
     * @param i The ID of the arena to be removed
     */
    public void removeArena(int i) {
        SGArena a;
        try {
            a = getArena(i);
        } catch (ArenaNotFoundException e) {
            Bukkit.getLogger().severe(e.getMessage());
            return;
        }
        arenas.remove(a);

        SGApi.getPlugin().getConfig().set("Arenas." + i, null);
        List<Integer> list = SGApi.getPlugin().getConfig().getIntegerList("Arenas.Arenas");
        list.remove(i);
        SGApi.getPlugin().getConfig().set("Arenas.Arenas", list);
        SGApi.getPlugin().saveConfig();
    }

    /**
     * Gets whether the player is playing
     * 
     * @param p The player that will be scanned
     * @return Whether the player is in a game
     */
    public boolean isInGame(Player p) {
        for (SGArena a : arenas) {
            if (a.getPlayers().contains(p.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads the game into memory after a shutdown or a relaod
     */
    public void loadGames() {
        arenaSize = 0;

        if (SGApi.getPlugin().getConfig().getIntegerList("Arenas.Arenas").isEmpty()) {
            return;
        }

        for (int i : SGApi.getPlugin().getConfig().getIntegerList("Arenas.Arenas")) {
            reloadArena(i);
        }
    }

    /**
     * Gets the HashMap that contains the creators
     * 
     * @return The HashMap of creators
     */
    public Map<String, SGArena> getCreators() {
        return creators;
    }

    /**
     * Serializes a location to a string
     * 
     * @param l The location to serialize
     * @return The serialized location
     */
    public String serializeLoc(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    /**
     * Gets a location from a string
     * 
     * @param s The string to deserialize
     * @return The location represented from the string
     */
    private Location deserializeLoc(String s) {
        String[] st = s.split(",");
        return new Location(Bukkit.getWorld(st[0]), Integer.parseInt(st[1]), Integer.parseInt(st[2]), Integer.parseInt(st[3]));
    }
}
