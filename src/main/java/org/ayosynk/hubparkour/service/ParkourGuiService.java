package org.ayosynk.hubparkour.service;

import org.ayosynk.hubparkour.model.Checkpoint;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.util.LocationUtil;
import org.ayosynk.hubparkour.util.PressurePlateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ParkourGuiService implements Listener {
    private static final String ADMIN_TITLE = "Parkour Admin";
    private static final String SETUP_TITLE_PREFIX = "Parkour Setup: ";
    private static final String PLAYER_TITLE = "Parkour";
    private static final String KEY_OPEN_SETUP = "hubparkour:open_setup";
    private static final String KEY_CREATE = "hubparkour:create";
    private static final String KEY_SET_START = "hubparkour:set_start";
    private static final String KEY_SET_END = "hubparkour:set_end";
    private static final String KEY_ADD_CHECKPOINT = "hubparkour:add_checkpoint";
    private static final String KEY_DELETE_CHECKPOINT = "hubparkour:delete_checkpoint";
    private static final String KEY_DELETE_PARKOUR = "hubparkour:delete_parkour";
    private static final String KEY_PLACE_LEADERBOARD = "hubparkour:place_leaderboard";
    private static final String KEY_BACK = "hubparkour:back";
    private static final String KEY_RESTART = "hubparkour:restart";
    private static final String KEY_LAST = "hubparkour:last";
    private static final String KEY_LEAVE = "hubparkour:leave";

    private final JavaPlugin plugin;
    private final ParkourRepository repository;
    private final ParkourCache cache;
    private final LeaderboardService leaderboardService;
    private final PlateHologramService plateHologramService;
    private final ParkourService parkourService;
    private final NamespacedKey guiKey;
    private final NamespacedKey parkourKey;
    private final Map<UUID, GuiContext> openContexts = new HashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();

    public ParkourGuiService(JavaPlugin plugin,
                             ParkourRepository repository,
                             ParkourCache cache,
                             LeaderboardService leaderboardService,
                             PlateHologramService plateHologramService,
                             ParkourService parkourService) {
        this.plugin = plugin;
        this.repository = repository;
        this.cache = cache;
        this.leaderboardService = leaderboardService;
        this.plateHologramService = plateHologramService;
        this.parkourService = parkourService;
        this.guiKey = new NamespacedKey(plugin, "gui_action");
        this.parkourKey = new NamespacedKey(plugin, "parkour_id");
    }

    public void openAdminGui(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, ADMIN_TITLE);
        int slot = 0;
        for (Parkour parkour : cache.getAllParkours().values()) {
            if (slot >= 26) {
                break;
            }
            ItemStack item = createParkourItem(parkour);
            inventory.setItem(slot++, item);
        }
        inventory.setItem(26, createItem(Material.ANVIL, "§eCreate Parkour", KEY_CREATE, List.of("§7Click, then type a name")));
        player.openInventory(inventory);
        openContexts.put(player.getUniqueId(), new GuiContext(GuiType.ADMIN, null));
    }

    public void openPlayerGui(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 9, PLAYER_TITLE);
        inventory.setItem(2, createItem(Material.SLIME_BALL, "§aStart Over", KEY_RESTART, List.of("§7Restart current run")));
        inventory.setItem(4, createItem(Material.ENDER_PEARL, "§eLast Checkpoint", KEY_LAST, List.of("§7Teleport to last checkpoint")));
        inventory.setItem(6, createItem(Material.BARRIER, "§cLeave", KEY_LEAVE, List.of("§7Cancel current run")));
        player.openInventory(inventory);
        openContexts.put(player.getUniqueId(), new GuiContext(GuiType.PLAYER, null));
    }

    private void openSetupGui(Player player, Parkour parkour) {
        Inventory inventory = Bukkit.createInventory(player, 27, SETUP_TITLE_PREFIX + parkour.name());
        inventory.setItem(10, createItem(Material.LIME_WOOL, "§aSet Start", KEY_SET_START, List.of("§7Look at/stand on plate")));
        inventory.setItem(11, createItem(Material.RED_WOOL, "§cSet End", KEY_SET_END, List.of("§7Look at/stand on plate")));
        inventory.setItem(12, createItem(Material.LIGHT_BLUE_WOOL, "§bAdd Checkpoint", KEY_ADD_CHECKPOINT, List.of("§7Look at/stand on plate")));
        inventory.setItem(13, createItem(Material.BARRIER, "§eDelete Checkpoint", KEY_DELETE_CHECKPOINT, List.of("§7Look at/stand on checkpoint")));
        inventory.setItem(14, createItem(Material.NETHER_STAR, "§dPlace Leaderboard", KEY_PLACE_LEADERBOARD, List.of("§7Places at your position")));
        inventory.setItem(15, createItem(Material.REDSTONE_BLOCK, "§4Delete Parkour", KEY_DELETE_PARKOUR, List.of("§7Deletes all data")));
        inventory.setItem(22, createItem(Material.ARROW, "§7Back", KEY_BACK, List.of()));
        player.openInventory(inventory);
        openContexts.put(player.getUniqueId(), new GuiContext(GuiType.SETUP, parkour.name()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GuiContext context = openContexts.get(player.getUniqueId());
        if (context == null) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.equals(ADMIN_TITLE) && !title.startsWith(SETUP_TITLE_PREFIX) && !title.equals(PLAYER_TITLE)) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        String key = getKey(item);
        if (key == null) {
            return;
        }
        if (key.equals(KEY_OPEN_SETUP) && item.getItemMeta() != null) {
            ItemMeta meta = item.getItemMeta();
            Integer parkourId = meta.getPersistentDataContainer().get(parkourKey, PersistentDataType.INTEGER);
            if (parkourId != null) {
                cache.getParkourById(parkourId).ifPresent(parkour -> openSetupGui(player, parkour));
                return;
            }
            String name = stripColor(meta.getDisplayName());
            cache.getParkourByName(name).ifPresent(parkour -> openSetupGui(player, parkour));
            return;
        }
        if (key.equals(KEY_RESTART)) {
            if (parkourService.getSession(player.getUniqueId()).isEmpty()) {
                player.sendMessage("§cYou are not in a parkour run.");
                return;
            }
            parkourService.restartParkour(player);
            player.closeInventory();
            return;
        }
        if (key.equals(KEY_LAST)) {
            if (parkourService.getSession(player.getUniqueId()).isEmpty()) {
                player.sendMessage("§cYou are not in a parkour run.");
                return;
            }
            parkourService.teleportToLastCheckpoint(player);
            player.closeInventory();
            return;
        }
        if (key.equals(KEY_LEAVE)) {
            if (parkourService.getSession(player.getUniqueId()).isEmpty()) {
                player.sendMessage("§cYou are not in a parkour run.");
                return;
            }
            parkourService.leaveParkour(player, true);
            player.closeInventory();
            return;
        }
        if (key.equals(KEY_CREATE)) {
            pendingActions.put(player.getUniqueId(), new PendingAction(PendingType.CREATE_PARKOUR));
            player.closeInventory();
            player.sendMessage("§eType the new parkour name in chat.");
            return;
        }
        if (key.equals(KEY_BACK)) {
            openAdminGui(player);
            return;
        }
        if (context.type() != GuiType.SETUP || context.parkourName() == null) {
            return;
        }
        Optional<Parkour> parkour = cache.getParkourByName(context.parkourName());
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        switch (key) {
            case KEY_SET_START -> handleSetStart(player, parkour.get());
            case KEY_SET_END -> handleSetEnd(player, parkour.get());
            case KEY_ADD_CHECKPOINT -> handleAddCheckpoint(player, parkour.get());
            case KEY_DELETE_CHECKPOINT -> handleDeleteCheckpoint(player, parkour.get());
            case KEY_DELETE_PARKOUR -> handleDeleteParkour(player, parkour.get());
            case KEY_PLACE_LEADERBOARD -> handlePlaceLeaderboard(player, parkour.get());
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openContexts.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PendingAction pending = pendingActions.remove(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String name = event.getMessage().trim();
        if (name.isEmpty()) {
            event.getPlayer().sendMessage("§cName cannot be empty.");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean created = repository.createParkour(name, null, null, null, null);
            if (created) {
                cache.reload();
                refreshHolograms();
                event.getPlayer().sendMessage("§aParkour created: " + name);
            } else {
                event.getPlayer().sendMessage("§cFailed to create parkour.");
            }
        });
    }

    private void handleSetStart(Player player, Parkour parkour) {
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a pressure plate or look at one.");
            return;
        }
        Location location = plate.getLocation().clone();
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        boolean updated = repository.updateStart(parkour.id(), location, plate.getType());
        if (updated) {
            cache.reload();
            refreshHolograms();
            player.sendMessage("§aStart updated for " + parkour.name() + ".");
        } else {
            player.sendMessage("§cFailed to update start.");
        }
    }

    private void handleSetEnd(Player player, Parkour parkour) {
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a pressure plate or look at one.");
            return;
        }
        Location location = plate.getLocation().clone();
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        boolean updated = repository.updateEnd(parkour.id(), location, plate.getType());
        if (updated) {
            cache.reload();
            refreshHolograms();
            player.sendMessage("§aEnd updated for " + parkour.name() + ".");
        } else {
            player.sendMessage("§cFailed to update end.");
        }
    }

    private void handleAddCheckpoint(Player player, Parkour parkour) {
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a pressure plate or look at one.");
            return;
        }
        Location location = plate.getLocation().clone();
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        int nextIndex = repository.getMaxCheckpointIndex(parkour.id()) + 1;
        boolean created = repository.createCheckpoint(parkour.id(), nextIndex, location, plate.getType());
        if (created) {
            cache.reload();
            refreshHolograms();
            player.sendMessage("§aCheckpoint " + nextIndex + " added for " + parkour.name() + ".");
        } else {
            player.sendMessage("§cFailed to add checkpoint.");
        }
    }

    private void handleDeleteCheckpoint(Player player, Parkour parkour) {
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a checkpoint plate or look at one.");
            return;
        }
        String key = LocationUtil.toKey(plate.getLocation());
        Optional<Checkpoint> checkpoint = cache.getCheckpointByLocation(key);
        if (checkpoint.isEmpty() || checkpoint.get().parkourId() != parkour.id()) {
            player.sendMessage("§cCheckpoint not found for this parkour.");
            return;
        }
        boolean deleted = repository.deleteCheckpoint(parkour.id(), checkpoint.get().index());
        if (deleted) {
            cache.reload();
            refreshHolograms();
            player.sendMessage("§aCheckpoint removed.");
        } else {
            player.sendMessage("§cFailed to remove checkpoint.");
        }
    }

    private void handleDeleteParkour(Player player, Parkour parkour) {
        boolean deleted = repository.deleteParkour(parkour.id());
        if (deleted) {
            cache.reload();
            refreshHolograms();
            player.sendMessage("§aParkour deleted: " + parkour.name());
            openAdminGui(player);
        } else {
            player.sendMessage("§cFailed to delete parkour.");
        }
    }

    private void handlePlaceLeaderboard(Player player, Parkour parkour) {
        Location location = player.getLocation().add(0, 1.2, 0);
        leaderboardService.createLeaderboard(parkour.name(), location, player);
    }

    private void refreshHolograms() {
        if (plateHologramService != null) {
            plateHologramService.refreshAll();
        }
    }

    private Block resolvePlateBlock(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target != null && PressurePlateUtil.isPressurePlate(target.getType())) {
            return target;
        }
        Block below = player.getLocation().subtract(0, 1, 0).getBlock();
        if (PressurePlateUtil.isPressurePlate(below.getType())) {
            return below;
        }
        return null;
    }

    private ItemStack createItem(Material material, String name, String key, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, key);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createParkourItem(Parkour parkour) {
        ItemStack item = createItem(Material.PAPER, "§a" + parkour.name(), KEY_OPEN_SETUP, List.of("§7Click to configure"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(parkourKey, PersistentDataType.INTEGER, parkour.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getKey(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String key = container.get(guiKey, PersistentDataType.STRING);
        return key == null || key.isBlank() ? null : key;
    }

    private String stripColor(String input) {
        return input == null ? "" : input.replaceAll("§.", "");
    }

    private enum GuiType {
        ADMIN,
        SETUP,
        PLAYER
    }

    private record GuiContext(GuiType type, String parkourName) {
    }

    private enum PendingType {
        CREATE_PARKOUR
    }

    private record PendingAction(PendingType type) {
    }
}
