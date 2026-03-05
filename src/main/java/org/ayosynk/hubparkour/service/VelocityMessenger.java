package org.ayosynk.hubparkour.service;

import org.ayosynk.hubparkour.config.PluginConfig;
import org.ayosynk.hubparkour.model.GlobalParkourTime;
import org.ayosynk.hubparkour.model.ParkourTime;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.util.TextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class VelocityMessenger {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final TextFormatter formatter = new TextFormatter();

    public VelocityMessenger(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void register() {
        if (config.getVelocityConfig().isEnabled()) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, config.getVelocityConfig().channel());
        }
    }

    public void unregister() {
        if (config.getVelocityConfig().isEnabled()) {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, config.getVelocityConfig().channel());
        }
    }

    public void sendRecord(Player player, String parkourName, String timer, ParkourRepository repository, int broadcastTop) {
        if (!config.getVelocityConfig().isEnabled()) {
            return;
        }
        List<ParkourTime> bestTimes = repository.getBestTimes(getParkourId(parkourName, repository), broadcastTop);
        int position = -1;
        for (int i = 0; i < bestTimes.size(); i++) {
            if (bestTimes.get(i).playerUuid().equals(player.getUniqueId())) {
                position = i + 1;
                break;
            }
        }
        if (position == -1 || position > broadcastTop) {
            return;
        }
        String message = formatter.formatLegacy(config.getVelocityConfig().broadcastFormat(), player, java.util.Map.of(
                "player_name", player.getName(),
                "parkour_name", parkourName,
                "timer", timer,
                "position", String.valueOf(position)
        ));
        sendMessage(player, message);
    }

    public void sendGlobalRecord(Player player, String parkourName, String timer, ParkourRepository repository) {
        if (!config.getVelocityConfig().isEnabled()) {
            return;
        }
        int broadcastTop = config.getVelocityConfig().globalBroadcastTop();
        if (broadcastTop <= 0) {
            return;
        }
        String format = config.getVelocityConfig().globalBroadcastFormat();
        if (format == null || format.isBlank()) {
            return;
        }
        int parkourId = getParkourId(parkourName, repository);
        if (parkourId <= 0) {
            return;
        }
        List<GlobalParkourTime> bestTimes = repository.getGlobalBestTimes(broadcastTop);
        int position = -1;
        for (int i = 0; i < bestTimes.size(); i++) {
            GlobalParkourTime time = bestTimes.get(i);
            if (time.parkourId() == parkourId && time.playerUuid().equals(player.getUniqueId())) {
                position = i + 1;
                break;
            }
        }
        if (position == -1 || position > broadcastTop) {
            return;
        }
        String message = formatter.formatLegacy(format, player, java.util.Map.of(
                "player_name", player.getName(),
                "parkour_name", parkourName,
                "timer", timer,
                "position", String.valueOf(position)
        ));
        sendMessage(player, message);
    }

    private int getParkourId(String parkourName, ParkourRepository repository) {
        return repository.findParkourByName(parkourName).map(p -> p.id()).orElse(-1);
    }

    private void sendMessage(Player player, String message) {
        Player sender = player != null ? player : Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (sender == null) {
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF(message);
        } catch (IOException ignored) {
            return;
        }
        sender.sendPluginMessage(plugin, config.getVelocityConfig().channel(), stream.toByteArray());
    }
}
