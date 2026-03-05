package org.ayosynk.hubparkour.service;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.ayosynk.hubparkour.config.PluginConfig;
import org.ayosynk.hubparkour.model.Checkpoint;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.util.TextFormatter;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PlateHologramService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ParkourRepository repository;
    private final ParkourCache cache;
    private final TextFormatter formatter = new TextFormatter();
    private final Set<String> activeHolograms = new HashSet<>();

    public PlateHologramService(JavaPlugin plugin, PluginConfig config, ParkourRepository repository, ParkourCache cache) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.cache = cache;
    }

    public void start() {
        refreshAll();
    }

    public void stop() {
        if (!isEnabled()) {
            activeHolograms.clear();
            return;
        }
        HologramManager manager = getManager();
        if (manager == null) {
            activeHolograms.clear();
            return;
        }
        for (String name : activeHolograms) {
            manager.getHologram(name).ifPresent(manager::removeHologram);
        }
        activeHolograms.clear();
    }

    public void refreshAll() {
        stop();
        if (!isEnabled()) {
            return;
        }
        for (Parkour parkour : cache.getAllParkours().values()) {
            createPlateHolograms(parkour);
        }
    }

    private void createPlateHolograms(Parkour parkour) {
        if (parkour == null) {
            return;
        }
        PluginConfig.FormattingConfig formatting = config.getFormatting();
        int totalCheckpoints = repository.getCheckpoints(parkour.id()).size();
        if (parkour.startLocation() != null) {
            Location location = parkour.startLocation().clone().add(0.5, 1.1, 0.5);
            String name = buildName(parkour.id(), "start");
            String text = formatter.formatLegacy(formatting.startPlate(), null, Map.of(
                    "parkour_name", parkour.name()
            ));
            createTextHologram(name, location, text);
        }
        if (parkour.endLocation() != null) {
            Location location = parkour.endLocation().clone().add(0.5, 1.1, 0.5);
            String name = buildName(parkour.id(), "end");
            String text = formatter.formatLegacy(formatting.endPlate(), null, Map.of(
                    "parkour_name", parkour.name()
            ));
            createTextHologram(name, location, text);
        }
        List<Checkpoint> checkpoints = repository.getCheckpoints(parkour.id());
        for (Checkpoint checkpoint : checkpoints) {
            if (checkpoint.location() == null) {
                continue;
            }
            Location location = checkpoint.location().clone().add(0.5, 1.1, 0.5);
            String name = buildName(parkour.id(), "cp-" + checkpoint.index());
            String text = formatter.formatLegacy(formatting.checkpointPlate(), null, Map.of(
                    "parkour_name", parkour.name(),
                    "checkpoint", String.valueOf(checkpoint.index()),
                    "checkpoint_total", String.valueOf(totalCheckpoints)
            ));
            createTextHologram(name, location, text);
        }
    }

    private void createTextHologram(String name, Location location, String text) {
        HologramManager manager = getManager();
        if (manager == null) {
            return;
        }
        manager.getHologram(name).ifPresent(manager::removeHologram);
        TextHologramData data = new TextHologramData(name, location);
        data.setBillboard(Display.Billboard.CENTER);
        data.setTextAlignment(TextDisplay.TextAlignment.CENTER);
        data.setText(List.of(text));
        data.setPersistent(false);
        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);
        activeHolograms.add(name);
    }

    private boolean isEnabled() {
        return config.getHologramConfig().isEnabled() && FancyHologramsPlugin.get() != null;
    }

    private HologramManager getManager() {
        FancyHologramsPlugin plugin = FancyHologramsPlugin.get();
        return plugin != null ? plugin.getHologramManager() : null;
    }

    private String buildName(int parkourId, String suffix) {
        return plugin.getName().toLowerCase(Locale.ROOT) + "-plate-" + parkourId + "-" + suffix;
    }
}
