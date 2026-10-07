package me.saif.betterenderchests.enderchest;

import com.google.common.collect.Sets;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.saif.betterenderchests.OberonEnder;
import me.saif.betterenderchests.data.DataManager;
import me.saif.betterenderchests.lang.MessageKey;
import me.saif.betterenderchests.utils.Callback;
import me.saif.betterenderchests.utils.CaselessString;
import me.saif.betterenderchests.utils.FoliaScheduler;
import me.saif.betterenderchests.utils.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EnderChestManager extends Manager<OberonEnder> implements Listener {

    private final DataManager dataManager;

    private final Map<UUID, EnderChest> uuidEnderChestMap = new ConcurrentHashMap<>();
    private final Map<CaselessString, UUID> nameUUIDMap = new HashMap<>();

    private final Map<UUID, Callback<EnderChest>> uuidCallbackMap = new HashMap<>();
    private final Map<CaselessString, Callback<EnderChest>> nameCallbackMap = new HashMap<>();
    private final Set<EnderChestSnapshot> snapshotsToSave = Sets.newConcurrentHashSet();

    private final Map<UUID, Block> openFromBlocks = new HashMap<>();
    private volatile int defaultRows;
    private volatile boolean convert;

    public static final String PERMISSION_USE = "enderchest.use";

    private volatile SoundSetting blockOpenSound;
    private volatile SoundSetting blockCloseSound;
    private volatile SoundSetting commandOpenSound;
    private volatile SoundSetting commandCloseSound;
    //players looking at an enderchest they opened by command, so closing it plays the command close sound
    private final Set<UUID> openFromCommand = new HashSet<>();

    private volatile Set<Material> blacklist = new HashSet<>();

    public EnderChestManager(OberonEnder plugin) {
        super(plugin);
        this.dataManager = getPlugin().getDataManager();


        reloadSettings();

        //load data for already online players eg. if plugin is reloaded.
        FoliaScheduler.runGlobal(plugin, () -> {


            Set<UUID> toGet = Bukkit.getOnlinePlayers().stream().map((Function<Player, UUID>) Entity::getUniqueId).collect(Collectors.toSet());
            Map<UUID, EnderChestSnapshot> data = this.dataManager.loadEnderChestsByUUID(toGet);

            for (Player player : Bukkit.getOnlinePlayers()) {
                this.dataManager.saveNameAndUUID(player.getName(), player.getUniqueId());

                EnderChestSnapshot snapshot = data.get(player.getUniqueId());
                if (snapshot == null) {
                    this.getPlugin().getLogger().info("Creating new enderchest for " + player.getName());
                    this.uuidEnderChestMap.put(player.getUniqueId(),
                            createNew(player));
                } else {
                    this.getPlugin().getLogger().info("Loaded enderchest for " + player.getName());
                    this.uuidEnderChestMap.put(player.getUniqueId(),
                            new EnderChest(player.getUniqueId(), player.getName(), snapshot));
                }

                //if player has vanilla chest open, close it and open their VEC
                //if in disabled world, no need to do this

                if (this.getPlugin().getDisabledWorlds().contains(player.getWorld().getName()))
                    continue;

                final Player p = player;
                FoliaScheduler.runEntity(this.getPlugin(), p, () -> {
                    if (p.getOpenInventory().getTopInventory().equals(p.getEnderChest())) {
                        p.closeInventory();
                        int rows = this.getNumRows(p);
                        if (rows == 0) {
                            this.getPlugin().getMessenger().sendMessage(p, MessageKey.NO_ENDERCHEST_SELF);
                        }
                        this.openEnderChest(this.getEnderChest(p), p, this.getNumRows(p));
                    }
                }, null);
            }
        });

        FoliaScheduler.runGlobalTimer(this.getPlugin(), () -> {
            Map<UUID, EnderChestSnapshot> toSave = new HashMap<>();
            Set<UUID> toRemove = new HashSet<>();
            for (EnderChest enderChest : this.uuidEnderChestMap.values()) {
                toSave.put(enderChest.getUUID(), enderChest.snapshot());
                if (Bukkit.getPlayer(enderChest.getUUID()) == null && !enderChest.hasViewers()) {
                    toRemove.add(enderChest.getUUID());
                }
            }

            for (UUID uuid : toRemove) {
                EnderChest enderChest = this.uuidEnderChestMap.remove(uuid);

                this.getPlugin().getLogger().info("Saving data for " + enderChest.getName());
            }

            FoliaScheduler.runAsync(this.getPlugin(), () -> this.dataManager.saveEnderChestMultiple(toSave));

            for (CaselessString minecraftName : this.nameCallbackMap.keySet()) {
                Player player = Bukkit.getPlayerExact(minecraftName.getOriginal());
                if (player == null)
                    this.nameCallbackMap.remove(minecraftName);
            }
        }, 6000L, 6000L);
    }

    private EnderChest loadData(String name, UUID uuid) {
        EnderChestSnapshot enderChestSnapshot = this.dataManager.loadEnderChest(uuid);

        if (enderChestSnapshot == null) {
            return null;
        }

        return new EnderChest(uuid, name, enderChestSnapshot);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();

        this.nameUUIDMap.put(new CaselessString(name), uuid);

        if (this.uuidEnderChestMap.containsKey(uuid)) {
            return;
        }

        FoliaScheduler.runAsync(this.getPlugin(), () -> {
            dataManager.saveNameAndUUID(name, uuid);
            EnderChest enderChest = loadData(name, uuid);

            if (enderChest == null) {
                this.getPlugin().getLogger().info("Creating new enderchest for " + name);
            } else {
                this.getPlugin().getLogger().info("Loaded enderchest for " + name);
            }

            FoliaScheduler.runGlobal(getPlugin(), () -> uuidEnderChestMap.put(uuid, enderChest == null ? createNew(event.getPlayer()) : enderChest));
        });
    }

    @EventHandler(ignoreCancelled = true)
    private void onInventoryOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();

        //if world disabled, use vanilla
        if (this.getPlugin().getDisabledWorlds().contains(player.getWorld().getName()))
            return;


        if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);

            if (!player.hasPermission(PERMISSION_USE)) {
                this.getPlugin().getMessenger().sendMessage(player, MessageKey.NO_PERMISSION_USE);
                return;
            }

            int rows = this.getNumRows(player);
            if (rows == 0) {
                this.getPlugin().getMessenger().sendMessage(player, MessageKey.NO_ENDERCHEST_SELF);
            } else {
                EnderChest enderChest = this.getEnderChest(player);

                if (enderChest == null) {
                    this.getPlugin().getLogger().severe("Enderchest for online player " + player.getName() + " could not be found.");
                    return;
                }

                if (enderChest.isLoadFailed()) {
                    this.getPlugin().getMessenger().sendMessage(player, MessageKey.ENDERCHEST_LOAD_FAILED);
                    return;
                }

                //opened from the block, so it is no longer a command viewer
                this.openFromCommand.remove(player.getUniqueId());

                if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_19_R1)) {
                    org.bukkit.block.EnderChest chestBlock = ((org.bukkit.block.EnderChest) event.getClickedBlock().getState());
                    chestBlock.open();
                    chestBlock.update();
                }

                this.blockOpenSound.play(event.getPlayer(), event.getClickedBlock().getLocation());
                this.openEnderChest(enderChest, player, rows);
                this.openFromBlocks.put(player.getUniqueId(), event.getClickedBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onInventoryClose(InventoryCloseEvent event) {
        if (this.openFromBlocks.containsKey(event.getPlayer().getUniqueId())) {
            event.getViewers().remove(event.getPlayer());

            Block block = this.openFromBlocks.remove(event.getPlayer().getUniqueId());


            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_19_R1) && block.getState() instanceof org.bukkit.block.EnderChest && event.getViewers().isEmpty()) {
                org.bukkit.block.EnderChest chestBlock = ((org.bukkit.block.EnderChest) block.getState());
                chestBlock.close();
                chestBlock.update();
            }
            this.blockCloseSound.play((Player) event.getPlayer(), block.getLocation());
        } else if (this.openFromCommand.remove(event.getPlayer().getUniqueId())
                && event.getInventory().getHolder() instanceof EnderChest) {
            Player player = (Player) event.getPlayer();
            this.commandCloseSound.play(player, player.getLocation());
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        this.openFromBlocks.remove(event.getPlayer().getUniqueId());
        this.openFromCommand.remove(event.getPlayer().getUniqueId());

        EnderChest enderChest = getEnderChest(event.getPlayer());

        //remove the player as a viewer
        enderChest.getInventory().getViewers().remove(event.getPlayer());
        event.getPlayer().closeInventory();


        //keep the enderchest loaded so people can't dupe. Will reassess this bug later
        /*if (!enderChest.hasViewers()) {
            this.getPlugin().getLogger().info("Saving data for " + enderChest.getName());
            EnderChestSnapshot snapshot = enderChest.snapshot();
            this.uuidEnderChestMap.remove(snapshot.getUuid());
            this.nameUUIDMap.remove(new CaselessString(snapshot.getName()));

            //adds snapshot
            this.snapshotsToSave.add(snapshot);

            Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
                this.dataManager.saveEnderChest(snapshot.getUuid(), snapshot);
                //removes snapshot from savelist
                this.snapshotsToSave.remove(snapshot);
            });
        }*/

        //someone is watching the enderchest so keep it loaded
        //we will deal with this later when clearing cache periodically.
    }

    /**
     * Reads the sounds, default rows, blacklist and converter switch from the plugin config. Runs at startup and
     * on /oberonender reload; it never touches stored ender chests.
     */
    public void reloadSettings() {
        //the default sound depends on the version, the config can override it
        Sound defaultOpen;
        Sound defaultClose;
        if (MinecraftVersion.getVersion() == MinecraftVersion.MC1_8_R3) {
            defaultOpen = Sound.valueOf("CHEST_OPEN");
            defaultClose = Sound.valueOf("CHEST_CLOSE");
        } else if (!MinecraftVersion.isNewerThan(MinecraftVersion.MC1_12_R1)) {
            defaultOpen = Sound.valueOf("BLOCK_ENDERCHEST_OPEN");
            defaultClose = Sound.valueOf("BLOCK_ENDERCHEST_CLOSE");
        } else {
            defaultOpen = Sound.valueOf("BLOCK_ENDER_CHEST_OPEN");
            defaultClose = Sound.valueOf("BLOCK_ENDER_CHEST_CLOSE");
        }

        ConfigurationSection config = this.getPlugin().getConfig();
        Logger logger = this.getPlugin().getLogger();
        this.blockOpenSound = SoundSetting.load(config.getConfigurationSection("sounds.block.open"), defaultOpen, "sounds.block.open", logger);
        this.blockCloseSound = SoundSetting.load(config.getConfigurationSection("sounds.block.close"), defaultClose, "sounds.block.close", logger);
        this.commandOpenSound = SoundSetting.load(config.getConfigurationSection("sounds.command.open"), defaultOpen, "sounds.command.open", logger);
        this.commandCloseSound = SoundSetting.load(config.getConfigurationSection("sounds.command.close"), defaultClose, "sounds.command.close", logger);

        //getting config values
        this.convert = this.getPlugin().getConfig().getBoolean("convert-current-ender-chest", true);
        this.defaultRows = this.getPlugin().getConfig().getInt("default-rows", 3);
        if (this.defaultRows > 6)
            this.defaultRows = 6;
        else if (this.defaultRows < 0)
            this.defaultRows = 0;

        Set<Material> newBlacklist = new HashSet<>();
        for (String s : this.getPlugin().getConfig().getStringList("blacklisted-items")) {
            Material material = Material.matchMaterial(s);
            if (material == null) {
                this.getPlugin().getLogger().warning("Blacklisted item by the name of \"" + s + "\" found. This is not a valid minecraft material. Ignoring...");
                continue;
            }

            newBlacklist.add(material);
        }
        this.blacklist = newBlacklist;
    }

    public EnderChest getEnderChest(Player player) {
        EnderChest enderChest = this.uuidEnderChestMap.get(player.getUniqueId());

        if (enderChest == null) {
            this.getPlugin().getLogger().warning(player.getName() + "'s enderchest could not be found! Loading it synchronously");
            EnderChestSnapshot snapshot = this.dataManager.loadEnderChest(player.getUniqueId());

            if (snapshot == null) {
                enderChest = createNew(player);
            } else {
                enderChest = new EnderChest(snapshot.getUuid(), snapshot.getName(), snapshot);
            }

            this.uuidEnderChestMap.put(player.getUniqueId(), enderChest);
            this.nameUUIDMap.put(new CaselessString(player.getName()), player.getUniqueId());
        }

        return enderChest;
    }

    //opens ender chest and reopens updated enderchest for players
    public void openEnderChest(EnderChest chest, Player player, int rows) {
        chest.setRows(rows);
        this.openEnderChest(chest, player);
    }

    public void openEnderChest(EnderChest chest, Player player) {
        this.open(chest, player);
    }

    //opens the chest for a command (or the console) and plays the command sounds
    public void openEnderChestFromCommand(EnderChest chest, Player player, int rows) {
        chest.setRows(rows);
        this.openEnderChestFromCommand(chest, player);
    }

    public void openEnderChestFromCommand(EnderChest chest, Player player) {
        if (!this.open(chest, player))
            return;

        this.openFromBlocks.remove(player.getUniqueId());
        this.openFromCommand.add(player.getUniqueId());
        this.commandOpenSound.play(player, player.getLocation());
    }

    private boolean open(EnderChest chest, Player player) {
        //the stored data could not be read, opening an empty chest would let the owner overwrite it
        if (chest.isLoadFailed()) {
            this.getPlugin().getMessenger().sendMessage(player, MessageKey.ENDERCHEST_LOAD_FAILED);
            return false;
        }
        chest.openInventory(player);
        return true;
    }

    public void openEnderChest(Player player, int rows) {
        EnderChest chest = this.getEnderChest(player);
        chest.setRows(rows);
        this.openEnderChest(chest, player);
    }

    //returns whether there are items and if the inventory was opened.
    public boolean openRetriever(EnderChest chest, Player player, int rows) {
        chest.setRows(rows);
        if (chest.isLoadFailed()) {
            this.getPlugin().getMessenger().sendMessage(player, MessageKey.ENDERCHEST_LOAD_FAILED);
            return false;
        }

        Inventory inventory = chest.getRetriever().getInventory();

        if (!inventory.isEmpty()) {
            player.openInventory(inventory);
            return true;
        }

        return false;
    }

    public void clearEnderChest(EnderChest enderChest) {
        enderChest.clearContents();
    }

    public int getNumRows(Player player) {
        for (int i = 6; i > 0; i--) {
            if (player.hasPermission("enderchest.size." + i)) {
                return i;
            }
        }
        return defaultRows;
    }

    public void updateRows(Player player) {
        getEnderChest(player).setRows(getNumRows(player));
    }

    public Callback<EnderChest> getEnderChest(String name) {
        CaselessString mcName = new CaselessString(name);
        if (this.nameUUIDMap.containsKey(mcName) && this.uuidEnderChestMap.get(this.nameUUIDMap.get(mcName)) != null)
            return Callback.withResult(this.uuidEnderChestMap.get(this.nameUUIDMap.get(mcName)));

        if (this.nameCallbackMap.containsKey(mcName))
            return nameCallbackMap.get(mcName);

        Callback<EnderChest> callback = new Callback<>();
        this.nameCallbackMap.put(mcName, callback);

        FoliaScheduler.runAsync(this.getPlugin(), () -> {
            EnderChestSnapshot snapshot = this.dataManager.loadEnderChest(name);

            EnderChest enderChest = snapshot == null ? null : new EnderChest(snapshot.getUuid(), snapshot.getName(), snapshot);
            FoliaScheduler.runGlobal(this.getPlugin(), () -> {
                this.nameCallbackMap.remove(mcName);
                if (enderChest != null) {
                    this.uuidEnderChestMap.put(snapshot.getUuid(), enderChest);
                    this.nameUUIDMap.put(mcName, snapshot.getUuid());
                }
                callback.setResult(enderChest);
            });
        });

        return callback;
    }

    public Callback<EnderChest> getEnderChest(UUID uuid) {
        if (this.uuidEnderChestMap.get(uuid) != null)
            return Callback.withResult(this.uuidEnderChestMap.get(uuid));

        if (this.uuidCallbackMap.containsKey(uuid))
            return uuidCallbackMap.get(uuid);

        Callback<EnderChest> callback = new Callback<>();
        this.uuidCallbackMap.put(uuid, callback);

        FoliaScheduler.runAsync(this.getPlugin(), () -> {
            EnderChestSnapshot snapshot = this.dataManager.loadEnderChest(uuid);

            EnderChest enderChest = snapshot == null ? null : new EnderChest(snapshot.getUuid(), snapshot.getName(), snapshot);
            FoliaScheduler.runGlobal(this.getPlugin(), () -> {
                this.uuidCallbackMap.remove(uuid);
                this.uuidEnderChestMap.put(uuid, enderChest);
                callback.setResult(enderChest);
            });
        });

        return callback;
    }

    public void deleteEnderChest(UUID uuid) {
        EnderChest enderChest = this.uuidEnderChestMap.get(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (enderChest != null) {
            enderChest.clearContents();

            if (player != null) {
                if (this.convert)
                    enderChest.setContents(player.getEnderChest().getContents());
                return;
            }

            //unload enderchest without saving
            this.uuidEnderChestMap.remove(enderChest.getUUID());
        }
        this.dataManager.deleteEnderChest(uuid);
    }

    public void deleteEnderChest(String name) {
        CaselessString caselessString = new CaselessString(name);
        EnderChest enderChest = this.uuidEnderChestMap.get(this.nameUUIDMap.get(caselessString));
        Player player = Bukkit.getPlayerExact(name);
        if (enderChest != null) {
            enderChest.clearContents();

            if (player != null) {
                if (this.convert)
                    enderChest.setContents(player.getEnderChest().getContents());
                return;
            }

            //unload enderchest without saving
            this.uuidEnderChestMap.remove(enderChest.getUUID());
        }
        this.dataManager.deleteEnderChest(name);
    }

    //saves all data
    public void finishUp() {
        for (EnderChest value : this.uuidEnderChestMap.values()) {
            List<HumanEntity> list = new ArrayList<>(value.getInventory().getViewers());
            for (HumanEntity humanEntity : list) {
                humanEntity.closeInventory();
            }
        }
        Map<UUID, EnderChestSnapshot> enderChestSnapshotMap = new HashMap<>();
        for (EnderChest value : this.uuidEnderChestMap.values()) {
            enderChestSnapshotMap.put(value.getUUID(), value.snapshot());

            this.getPlugin().getLogger().info("Saving data for " + value.getName());
        }
        this.dataManager.saveEnderChestMultiple(enderChestSnapshotMap);

        //saves the snapshots that have not been saved
        for (EnderChestSnapshot enderChestSnapshot : this.snapshotsToSave) {
            this.dataManager.saveEnderChest(enderChestSnapshot.getUuid(), enderChestSnapshot);
        }

        this.snapshotsToSave.clear();

        this.uuidCallbackMap.clear();
        this.nameCallbackMap.clear();
        this.uuidEnderChestMap.clear();
        this.nameUUIDMap.clear();

        enderChestSnapshotMap.clear();
        nameUUIDMap.clear();
    }

    private EnderChest createNew(Player player) {
        return new EnderChest(player.getUniqueId(), player.getName(), this.convert ? player.getEnderChest().getContents() : new ItemStack[]{});
    }

    public Set<Material> getBlacklist() {
        return blacklist;
    }
}
