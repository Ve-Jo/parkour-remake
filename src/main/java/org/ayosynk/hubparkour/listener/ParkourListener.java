package org.ayosynk.hubparkour.listener;

import org.ayosynk.hubparkour.model.Checkpoint;
import org.ayosynk.hubparkour.service.ParkourService;
import org.ayosynk.hubparkour.util.LocationUtil;
import org.ayosynk.hubparkour.util.PressurePlateUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class ParkourListener implements Listener {
    private final ParkourService parkourService;
    private final NamespacedKey controlKey;

    public ParkourListener(JavaPlugin plugin, ParkourService parkourService) {
        this.parkourService = parkourService;
        this.controlKey = new NamespacedKey(plugin, "control_action");
    }

    @EventHandler
    public void onPlateStep(PlayerInteractEvent event) {
        if (handleControlItem(event)) {
            return;
        }
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !PressurePlateUtil.isPressurePlate(block.getType())) {
            return;
        }
        Location location = block.getLocation();
        String key = LocationUtil.toKey(location);
        Optional<Integer> startParkourId = parkourService.getCache().getParkourIdByStart(key);
        Optional<Integer> endParkourId = parkourService.getCache().getParkourIdByEnd(key);
        Player player = event.getPlayer();

        if (startParkourId.isPresent()) {
            parkourService.getCache().getParkourById(startParkourId.get())
                    .ifPresent(parkour -> parkourService.startParkour(player, parkour));
            return;
        }
        if (endParkourId.isPresent()) {
            parkourService.getCache().getParkourById(endParkourId.get())
                    .ifPresent(parkour -> parkourService.finishParkour(player, parkour));
            return;
        }
        Optional<Checkpoint> checkpoint = parkourService.getCache().getCheckpointByLocation(key);
        checkpoint.ifPresent(cp -> parkourService.handleCheckpoint(player, cp));
    }

    private boolean handleControlItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        Player player = event.getPlayer();
        if (parkourService.getSession(player.getUniqueId()).isEmpty()) {
            return false;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String action = container.get(controlKey, PersistentDataType.STRING);
        if (action == null || action.isBlank()) {
            return false;
        }
        event.setCancelled(true);
        switch (action) {
            case "restart" -> parkourService.restartParkour(player);
            case "last" -> parkourService.teleportToLastCheckpoint(player);
            case "leave" -> parkourService.leaveParkour(player, true);
            default -> {
            }
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        parkourService.leaveParkour(event.getPlayer(), false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        parkourService.leaveParkour(event.getPlayer(), false);
    }
}
