package org.ayosynk.hubparkour.service;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.ayosynk.hubparkour.config.PluginConfig;
import org.ayosynk.hubparkour.model.Leaderboard;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.model.ParkourTime;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.util.TextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LeaderboardService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ParkourRepository repository;
    private final ParkourCache cache;
    private final TextFormatter formatter = new TextFormatter();
    private BukkitTask updateTask;

    public LeaderboardService(JavaPlugin plugin, PluginConfig config, ParkourRepository repository, ParkourCache cache) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.cache = cache;
    }

    public void start() {
        if (!isEnabled()) {
            return;
        }
        refreshAllLeaderboards();
        long interval = config.getLeaderboardConfig().getUpdateIntervalTicks();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllLeaderboards, interval, interval);
    }

    public void restart() {
        stop();
        start();
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void createLeaderboard(String parkourName, Location location, Player player) {
        Optional<Parkour> parkour = repository.findParkourByName(parkourName);
        if (parkour.isEmpty()) {
            if (player != null) {
                player.sendMessage("§cParkour not found.");
            }
            return;
        }
        Optional<Leaderboard> created = repository.createLeaderboard(parkour.get().id(), location);
        if (created.isEmpty()) {
            if (player != null) {
                player.sendMessage("§cFailed to create leaderboard.");
            }
            return;
        }
        spawnLeaderboard(created.get(), parkour.get());
        if (player != null) {
            player.sendMessage("§aLeaderboard created (#" + created.get().id() + ").");
        }
    }

    public void removeLeaderboard(int leaderboardId, Player player) {
        HologramManager manager = getManager();
        if (manager != null) {
            repository.getLeaderboardLineNames(leaderboardId).forEach(name -> manager.getHologram(name).ifPresent(manager::removeHologram));
        }
        repository.deleteLeaderboard(leaderboardId);
        if (player != null) {
            player.sendMessage("§aLeaderboard removed.");
        }
    }

    private void refreshAllLeaderboards() {
        for (Leaderboard leaderboard : repository.listLeaderboards()) {
            Optional<Parkour> parkour = cache.getParkourById(leaderboard.parkourId());
            parkour.ifPresent(value -> {
                if (!isHologramPresent(leaderboard)) {
                    spawnLeaderboard(leaderboard, value);
                    return;
                }
                updateLeaderboard(leaderboard, value);
            });
        }
    }

    private boolean isHologramPresent(Leaderboard leaderboard) {
        HologramManager manager = getManager();
        if (manager == null) {
            return false;
        }
        String name = buildName(leaderboard.id());
        return manager.getHologram(name).isPresent();
    }

    private void spawnLeaderboard(Leaderboard leaderboard, Parkour parkour) {
        if (!isEnabled()) {
            return;
        }
        HologramManager manager = getManager();
        if (manager == null) {
            return;
        }
        PluginConfig.LeaderboardConfig.LeaderboardFormatting formatting = config.getLeaderboardConfig().formatting();
        Location base = leaderboard.location().clone();
        if (manager != null) {
            repository.getLeaderboardLineNames(leaderboard.id()).forEach(name -> manager.getHologram(name).ifPresent(manager::removeHologram));
        }
        repository.deleteLeaderboardLines(leaderboard.id());
        String name = buildName(leaderboard.id());
        TextHologramData data = new TextHologramData(name, base.clone().add(0, 0.6, 0));
        data.setBillboard(Display.Billboard.VERTICAL);
        data.setTextAlignment(TextDisplay.TextAlignment.CENTER);
        data.setText(buildLines(formatting, parkour));
        data.setPersistent(false);
        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);
        repository.storeLeaderboardLine(leaderboard.id(), 0, name, base.clone().add(0, 0.6, 0));
        updateLeaderboard(leaderboard, parkour);
    }

    private void updateLeaderboard(Leaderboard leaderboard, Parkour parkour) {
        HologramManager manager = getManager();
        if (manager == null) {
            return;
        }
        PluginConfig.LeaderboardConfig.LeaderboardFormatting formatting = config.getLeaderboardConfig().formatting();
        String name = buildName(leaderboard.id());
        Hologram hologram = manager.getHologram(name).orElse(null);
        if (hologram == null) {
            return;
        }
        if (!(hologram.getData() instanceof TextHologramData textData)) {
            return;
        }
        textData.setText(buildLines(formatting, parkour));
        hologram.queueUpdate();
    }

    private boolean isEnabled() {
        return config.getLeaderboardConfig().isEnabled() && config.getHologramConfig().isEnabled() && FancyHologramsPlugin.get() != null;
    }

    private HologramManager getManager() {
        if (!isEnabled()) {
            return null;
        }
        return FancyHologramsPlugin.get().getHologramManager();
    }

    private String buildName(int leaderboardId) {
        return "hubparkour_lb_" + leaderboardId;
    }

    private String resolveLineFormatting(PluginConfig.LeaderboardConfig.LeaderboardFormatting formatting, int position) {
        List<String> custom = formatting.lines();
        if (position - 1 < custom.size()) {
            String line = custom.get(position - 1);
            if (line != null && !line.isBlank()) {
                return line;
            }
        }
        return formatting.defaultLineStyle();
    }

    private List<String> buildLines(PluginConfig.LeaderboardConfig.LeaderboardFormatting formatting, Parkour parkour) {
        List<String> lines = new ArrayList<>();
        lines.add(formatter.formatLegacy(formatting.title(), null, Map.of("parkour_name", parkour.name())));
        List<ParkourTime> bestTimes = repository.getBestTimes(parkour.id(), formatting.maximumDisplayed());
        for (int position = 1; position <= formatting.maximumDisplayed(); position++) {
            String rawLine = resolveLineFormatting(formatting, position);
            String lineText = formatter.formatLegacy(formatting.emptyLineStyle(), null, Map.of());
            if (position - 1 < bestTimes.size()) {
                ParkourTime time = bestTimes.get(position - 1);
                String playerName = Optional.ofNullable(Bukkit.getOfflinePlayer(time.playerUuid()).getName()).orElse("Player");
                lineText = formatter.formatLegacy(rawLine, null, Map.of(
                        "position", String.valueOf(position),
                        "player_name", playerName,
                        "timer", formatTimer(time.timeSeconds())
                ));
            }
            lines.add(lineText);
        }
        return lines;
    }

    private String formatTimer(double seconds) {
        int totalMs = (int) (seconds * 1000.0);
        int minutes = totalMs / 1000 / 60;
        int secs = totalMs / 1000 % 60;
        int millis = totalMs % 1000 / 10;
        String format = config.getFormatting().timerFormat();
        return format.replace("%m%", String.format("%02d", minutes))
                .replace("%s%", String.format("%02d", secs))
                .replace("%ms%", String.format("%02d", millis));
    }
}
