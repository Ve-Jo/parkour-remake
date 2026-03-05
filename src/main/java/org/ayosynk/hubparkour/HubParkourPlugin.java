package org.ayosynk.hubparkour;

import org.ayosynk.hubparkour.command.ParkourCommand;
import org.ayosynk.hubparkour.config.PluginConfig;
import org.ayosynk.hubparkour.db.DatabaseManager;
import org.ayosynk.hubparkour.listener.ParkourListener;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.service.ActionBarService;
import org.ayosynk.hubparkour.service.LeaderboardService;
import org.ayosynk.hubparkour.service.ParkourCache;
import org.ayosynk.hubparkour.service.ParkourGuiService;
import org.ayosynk.hubparkour.service.ParkourService;
import org.ayosynk.hubparkour.service.PlateHologramService;
import org.ayosynk.hubparkour.service.VelocityMessenger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HubParkourPlugin extends JavaPlugin {
    private static HubParkourPlugin instance;
    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private ParkourRepository parkourRepository;
    private ParkourCache parkourCache;
    private ParkourService parkourService;
    private LeaderboardService leaderboardService;
    private ActionBarService actionBarService;
    private VelocityMessenger velocityMessenger;
    private PlateHologramService plateHologramService;
    private ParkourGuiService parkourGuiService;

    public static HubParkourPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadPluginConfig();

        databaseManager = new DatabaseManager(pluginConfig.getDatabaseConfig(), getLogger());
        databaseManager.connect();

        parkourRepository = new ParkourRepository(databaseManager);
        parkourCache = new ParkourCache(parkourRepository);
        parkourCache.reload();

        velocityMessenger = new VelocityMessenger(this, pluginConfig);
        parkourService = new ParkourService(this, pluginConfig, parkourRepository, parkourCache, velocityMessenger);
        leaderboardService = new LeaderboardService(this, pluginConfig, parkourRepository, parkourCache);
        actionBarService = new ActionBarService(this, pluginConfig, parkourService);
        plateHologramService = new PlateHologramService(this, pluginConfig, parkourRepository, parkourCache);
        parkourGuiService = new ParkourGuiService(this, parkourRepository, parkourCache, leaderboardService, plateHologramService, parkourService);

        Bukkit.getPluginManager().registerEvents(new ParkourListener(this, parkourService), this);
        Bukkit.getPluginManager().registerEvents(parkourGuiService, this);
        ParkourCommand command = new ParkourCommand(this, parkourService, parkourRepository, parkourCache, leaderboardService, parkourGuiService);
        getCommand("parkour").setExecutor(command);
        getCommand("parkour").setTabCompleter(command);

        leaderboardService.start();
        actionBarService.start();
        velocityMessenger.register();
        plateHologramService.start();
    }

    @Override
    public void onDisable() {
        if (actionBarService != null) {
            actionBarService.stop();
        }
        if (leaderboardService != null) {
            leaderboardService.stop();
        }
        if (plateHologramService != null) {
            plateHologramService.stop();
        }
        if (velocityMessenger != null) {
            velocityMessenger.unregister();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig = new PluginConfig(getConfig());
        if (plateHologramService != null) {
            plateHologramService.refreshAll();
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public PlateHologramService getPlateHologramService() {
        return plateHologramService;
    }
}
