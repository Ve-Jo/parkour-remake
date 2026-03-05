package org.ayosynk.hubparkour.service;

import org.ayosynk.hubparkour.config.PluginConfig;
import org.ayosynk.hubparkour.model.Checkpoint;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.model.ParkourSession;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.util.LocationUtil;
import org.ayosynk.hubparkour.util.TextFormatter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ParkourService {
    private final PluginConfig config;
    private final ParkourRepository repository;
    private final ParkourCache cache;
    private final VelocityMessenger velocityMessenger;
    private final NamespacedKey controlKey;
    private final Map<UUID, ParkourSession> sessions = new HashMap<>();
    private final TextFormatter formatter = new TextFormatter();

    public ParkourService(JavaPlugin plugin, PluginConfig config, ParkourRepository repository, ParkourCache cache, VelocityMessenger velocityMessenger) {
        this.config = config;
        this.repository = repository;
        this.cache = cache;
        this.velocityMessenger = velocityMessenger;
        this.controlKey = new NamespacedKey(plugin, "control_action");
    }

    public Map<UUID, ParkourSession> getSessions() {
        return sessions;
    }

    public ParkourCache getCache() {
        return cache;
    }

    public Optional<ParkourSession> getSession(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public void startParkour(Player player, Parkour parkour) {
        startParkour(player, parkour, true);
    }

    public void startParkour(Player player, Parkour parkour, boolean sendMessage) {
        if (player == null || parkour == null) {
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            leaveParkour(player, false);
        }
        ParkourSession session = new ParkourSession(parkour.name());
        session.setMaxCheckpoints(repository.getCheckpoints(parkour.id()).size());
        storeInventory(player, session);
        applyControlItems(player);
        boolean allowFlight = config.isAllowFly();
        session.setAllowFlight(allowFlight);
        session.setWasFlying(player.getAllowFlight());
        if (!allowFlight) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        sessions.put(player.getUniqueId(), session);
        if (sendMessage) {
            String timer = formatTimer(session.getElapsedSeconds(), config.getFormatting().timerFormat(), session);
            player.sendMessage(formatter.format(config.getFormatting().startMessage(), player, Map.of(
                    "parkour_name", parkour.name(),
                    "player_name", player.getName(),
                    "timer", timer
            )));
        }
    }

    public void restartParkour(Player player) {
        if (player == null) {
            return;
        }
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Optional<Parkour> parkour = cache.getParkourByName(session.getParkourName());
        if (parkour.isEmpty()) {
            return;
        }
        leaveParkour(player, false);
        startParkour(player, parkour.get(), false);
        if (parkour.get().startLocation() != null) {
            player.teleport(LocationUtil.toCenter(parkour.get().startLocation()));
        }
        player.sendMessage(formatter.format(config.getFormatting().resetMessage(), player, Map.of(
                "parkour_name", parkour.get().name(),
                "player_name", player.getName()
        )));
    }

    public void handleCheckpoint(Player player, Checkpoint checkpoint) {
        if (player == null || checkpoint == null) {
            return;
        }
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (checkpoint.index() <= session.getLastCheckpoint()) {
            return;
        }
        if (checkpoint.index() != session.getLastCheckpoint() + 1) {
            player.sendMessage(formatter.format(config.getFormatting().checkpointSkipMessage(), player, Map.of()));
            return;
        }
        session.setLastCheckpoint(checkpoint.index());
        String timer = formatTimer(session.getElapsedSeconds(), config.getFormatting().timerFormat(), session);
        player.sendMessage(formatter.format(config.getFormatting().checkpointMessage(), player, Map.of(
                "parkour_name", session.getParkourName(),
                "player_name", player.getName(),
                "checkpoint", String.valueOf(checkpoint.index()),
                "timer", timer
        )));
    }

    public void finishParkour(Player player, Parkour parkour) {
        if (player == null || parkour == null) {
            return;
        }
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.getParkourName().equalsIgnoreCase(parkour.name())) {
            return;
        }
        if (session.getLastCheckpoint() < session.getMaxCheckpoints()) {
            player.sendMessage(formatter.format(config.getFormatting().checkpointSkipMessage(), player, Map.of()));
            return;
        }
        double timeSeconds = session.getElapsedSeconds();
        String timer = formatTimer(timeSeconds, config.getFormatting().timerFormat(), session);
        Optional<Double> bestBefore = repository.getPersonalBest(player.getUniqueId(), parkour.id());
        repository.saveTime(player.getUniqueId(), parkour.id(), timeSeconds);
        leaveParkour(player, false);
        player.sendMessage(formatter.format(config.getFormatting().endMessage(), player, Map.of(
                "parkour_name", parkour.name(),
                "player_name", player.getName(),
                "timer", timer
        )));

        boolean isNewBest = bestBefore.isEmpty() || timeSeconds < bestBefore.get();
        if (isNewBest) {
            velocityMessenger.sendRecord(player, parkour.name(), timer, repository, config.getLeaderboardConfig().getBroadcastTop());
            velocityMessenger.sendGlobalRecord(player, parkour.name(), timer, repository);
        }
    }

    public void leaveParkour(Player player, boolean sendMessage) {
        if (player == null) {
            return;
        }
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        restoreInventory(player, session);
        if (!session.isAllowFlight()) {
            player.setAllowFlight(session.wasFlying());
            player.setFlying(session.wasFlying());
        }
        if (sendMessage) {
            String timer = formatTimer(session.getElapsedSeconds(), config.getFormatting().timerFormat(), session);
            player.sendMessage(formatter.format(config.getFormatting().cancelMessage(), player, Map.of(
                    "parkour_name", session.getParkourName(),
                    "player_name", player.getName(),
                    "timer", timer
            )));
        }
    }

    public void teleportToLastCheckpoint(Player player) {
        if (player == null) {
            return;
        }
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        cache.getAllParkours().values().stream()
                .filter(parkour -> parkour.name().equalsIgnoreCase(session.getParkourName()))
                .findFirst()
                .ifPresent(parkour -> {
                    int index = session.getLastCheckpoint();
                    Optional<Location> location = index == 0
                            ? Optional.ofNullable(parkour.startLocation())
                            : repository.getCheckpointLocation(parkour.id(), index);
                    location.ifPresent(target -> {
                        float yaw = target.getYaw();
                        float pitch = target.getPitch();
                        if (yaw == 0f && pitch == 0f) {
                            yaw = player.getLocation().getYaw();
                            pitch = player.getLocation().getPitch();
                        }
                        Location centered = new Location(target.getWorld(), target.getBlockX() + 0.5, target.getBlockY(), target.getBlockZ() + 0.5,
                                yaw, pitch);
                        player.teleport(centered);
                        player.sendMessage(formatter.format(config.getFormatting().tpMessage(), player, Map.of()));
                    });
                });
    }

    public String formatTimer(double seconds, String format, ParkourSession session) {
        int totalMs = (int) (seconds * 1000.0);
        int minutes = totalMs / 1000 / 60;
        int secs = totalMs / 1000 % 60;
        int millis = totalMs % 1000 / 10;
        return format.replace("%m%", String.format("%02d", minutes))
                .replace("%s%", String.format("%02d", secs))
                .replace("%ms%", String.format("%02d", millis))
                .replace("%checkpoint%", String.valueOf(session.getLastCheckpoint()))
                .replace("%checkpoint_total%", String.valueOf(session.getMaxCheckpoints()));
    }

    public String formatTimerSimple(double seconds, String format) {
        int totalMs = (int) (seconds * 1000.0);
        int minutes = totalMs / 1000 / 60;
        int secs = totalMs / 1000 % 60;
        int millis = totalMs % 1000 / 10;
        return format.replace("%m%", String.format("%02d", minutes))
                .replace("%s%", String.format("%02d", secs))
                .replace("%ms%", String.format("%02d", millis));
    }

    public Optional<Double> getRecordTime(String parkourName) {
        Optional<Parkour> parkour = cache.getParkourByName(parkourName);
        if (parkour.isEmpty()) {
            return Optional.empty();
        }
        return repository.getBestTime(parkour.get().id());
    }

    public Optional<Double> getPersonalBest(UUID playerUuid, String parkourName) {
        Optional<Parkour> parkour = cache.getParkourByName(parkourName);
        if (parkour.isEmpty()) {
            return Optional.empty();
        }
        return repository.getPersonalBest(playerUuid, parkour.get().id());
    }

    public void storeInventory(Player player, ParkourSession session) {
        session.getInventory().clear();
        for (int i = 0; i < 36; i++) {
            session.getInventory().put(i, player.getInventory().getItem(i));
        }
        session.getInventory().put(40, player.getInventory().getItemInOffHand());
        session.getInventory().put(41, player.getInventory().getHelmet());
        session.getInventory().put(42, player.getInventory().getChestplate());
        session.getInventory().put(43, player.getInventory().getLeggings());
        session.getInventory().put(44, player.getInventory().getBoots());
        player.getInventory().clear();
    }

    private void applyControlItems(Player player) {
        player.getInventory().setItem(3, createControlItem(Material.SLIME_BALL, "§aStart Over", "restart", "§7Restart current run"));
        player.getInventory().setItem(4, createControlItem(Material.ENDER_PEARL, "§eLast Checkpoint", "last", "§7Teleport to last checkpoint"));
        player.getInventory().setItem(5, createControlItem(Material.BARRIER, "§cLeave", "leave", "§7Cancel current run"));
    }

    private ItemStack createControlItem(Material material, String name, String action, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(controlKey, PersistentDataType.STRING, action);
            if (loreLine != null && !loreLine.isBlank()) {
                meta.setLore(java.util.List.of(loreLine));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void restoreInventory(Player player, ParkourSession session) {
        if (session.getInventory().isEmpty()) {
            return;
        }
        player.getInventory().clear();
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, session.getInventory().get(i));
        }
        player.getInventory().setItemInOffHand(session.getInventory().get(40));
        player.getInventory().setHelmet(session.getInventory().get(41));
        player.getInventory().setChestplate(session.getInventory().get(42));
        player.getInventory().setLeggings(session.getInventory().get(43));
        player.getInventory().setBoots(session.getInventory().get(44));
        session.getInventory().clear();
    }
}
