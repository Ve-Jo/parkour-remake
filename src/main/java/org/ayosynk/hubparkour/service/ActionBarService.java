package org.ayosynk.hubparkour.service;

import org.ayosynk.hubparkour.config.PluginConfig;
import org.ayosynk.hubparkour.model.ParkourSession;
import org.ayosynk.hubparkour.util.TextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

public class ActionBarService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ParkourService parkourService;
    private final TextFormatter formatter = new TextFormatter();
    private BukkitTask task;

    public ActionBarService(JavaPlugin plugin, PluginConfig config, ParkourService parkourService) {
        this.plugin = plugin;
        this.config = config;
        this.parkourService = parkourService;
    }

    public void start() {
        if (!config.isActionBarEnabled()) {
            return;
        }
        stop();
        long interval = config.getActionBarIntervalTicks();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, ParkourSession> entry : parkourService.getSessions().entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    continue;
                }
                ParkourSession session = entry.getValue();
                String timerValue = parkourService.formatTimer(session.getElapsedSeconds(), config.getFormatting().timerFormat(), session);
                String recordValue = parkourService.getPersonalBest(player.getUniqueId(), session.getParkourName())
                        .map(record -> parkourService.formatTimerSimple(record, config.getFormatting().timerFormat()))
                        .orElse("-");
                String actionBar = config.getFormatting().actionBar();
                player.sendActionBar(formatter.format(actionBar, player, Map.of(
                        "parkour_name", session.getParkourName(),
                        "player_name", player.getName(),
                        "timer", timerValue,
                        "record", recordValue,
                        "checkpoint", String.valueOf(session.getLastCheckpoint()),
                        "checkpoint_total", String.valueOf(session.getMaxCheckpoints())
                )));
            }
        }, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
