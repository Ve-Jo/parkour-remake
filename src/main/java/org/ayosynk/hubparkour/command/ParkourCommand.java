package org.ayosynk.hubparkour.command;

import org.ayosynk.hubparkour.HubParkourPlugin;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.service.LeaderboardService;
import org.ayosynk.hubparkour.service.ParkourCache;
import org.ayosynk.hubparkour.service.ParkourGuiService;
import org.ayosynk.hubparkour.service.ParkourService;
import org.ayosynk.hubparkour.util.PressurePlateUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ParkourCommand implements CommandExecutor, TabCompleter {
    private final HubParkourPlugin plugin;
    private final ParkourService parkourService;
    private final ParkourRepository repository;
    private final ParkourCache cache;
    private final LeaderboardService leaderboardService;
    private final ParkourGuiService guiService;

    public ParkourCommand(HubParkourPlugin plugin, ParkourService parkourService, ParkourRepository repository, ParkourCache cache,
                          LeaderboardService leaderboardService, ParkourGuiService guiService) {
        this.plugin = plugin;
        this.parkourService = parkourService;
        this.repository = repository;
        this.cache = cache;
        this.leaderboardService = leaderboardService;
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("leave") && sender instanceof Player player) {
            parkourService.leaveParkour(player, true);
            return true;
        }
        if (sub.equals("last") && sender instanceof Player player) {
            parkourService.teleportToLastCheckpoint(player);
            return true;
        }
        if (sub.equals("menu") && sender instanceof Player player) {
            guiService.openPlayerGui(player);
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadPluginConfig();
            cache.reload();
            leaderboardService.restart();
            sender.sendMessage("§aHubParkour reloaded.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "gui" -> handleGui(player);
            case "rename" -> handleRename(player, args);
            case "delete" -> handleDeleteParkour(player, args);
            case "setstart" -> handleSetStart(player, args);
            case "setend" -> handleSetEnd(player, args);
            case "addcheckpoint" -> handleAddCheckpoint(player, args);
            case "delcheckpoint" -> handleDeleteCheckpoint(player, args);
            case "list" -> handleList(player);
            case "leaderboard" -> handleLeaderboard(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /parkour create <name>");
            return;
        }
        String name = joinArgs(args, 1);
        boolean created = repository.createParkour(name, null, null, null, null);
        if (created) {
            cache.reload();
            refreshPlateHolograms();
            player.sendMessage("§aParkour created: " + name);
        } else {
            player.sendMessage("§cFailed to create parkour. Maybe it already exists.");
        }
    }

    private void handleGui(Player player) {
        guiService.openAdminGui(player);
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /parkour rename <old name> -> <new name>");
            return;
        }
        String combined = joinArgs(args, 1);
        String[] parts = combined.split("->", 2);
        if (parts.length < 2) {
            player.sendMessage("§cUsage: /parkour rename <old name> -> <new name>");
            return;
        }
        String oldName = parts[0].trim();
        String newName = parts[1].trim();
        if (oldName.isEmpty() || newName.isEmpty()) {
            player.sendMessage("§cBoth old and new names are required.");
            return;
        }
        Optional<Parkour> parkour = repository.findParkourByName(oldName);
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        boolean updated = repository.updateName(parkour.get().id(), newName);
        if (updated) {
            cache.reload();
            refreshPlateHolograms();
            leaderboardService.restart();
            player.sendMessage("§aParkour renamed to: " + newName);
        } else {
            player.sendMessage("§cFailed to rename parkour.");
        }
    }

    private void handleDeleteParkour(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /parkour delete <name>");
            return;
        }
        String name = joinArgs(args, 1);
        Optional<Parkour> parkour = repository.findParkourByName(name);
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        boolean deleted = repository.deleteParkour(parkour.get().id());
        if (deleted) {
            cache.reload();
            refreshPlateHolograms();
            player.sendMessage("§aParkour deleted: " + name);
        } else {
            player.sendMessage("§cFailed to delete parkour.");
        }
    }

    private void handleSetStart(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /parkour setstart <name>");
            return;
        }
        String name = joinArgs(args, 1);
        Optional<Parkour> parkour = repository.findParkourByName(name);
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a pressure plate or look at one.");
            return;
        }
        Location location = plate.getLocation().clone();
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        boolean updated = repository.updateStart(parkour.get().id(), location, plate.getType());
        if (updated) {
            cache.reload();
            refreshPlateHolograms();
            player.sendMessage("§aStart updated for " + name + ".");
        } else {
            player.sendMessage("§cFailed to update start.");
        }
    }

    private void handleSetEnd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /parkour setend <name>");
            return;
        }
        String name = joinArgs(args, 1);
        Optional<Parkour> parkour = repository.findParkourByName(name);
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a pressure plate or look at one.");
            return;
        }
        Location location = plate.getLocation().clone();
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        boolean updated = repository.updateEnd(parkour.get().id(), location, plate.getType());
        if (updated) {
            cache.reload();
            refreshPlateHolograms();
            player.sendMessage("§aEnd updated for " + name + ".");
        } else {
            player.sendMessage("§cFailed to update end.");
        }
    }

    private void handleAddCheckpoint(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /parkour addcheckpoint <name>");
            return;
        }
        String name = joinArgs(args, 1);
        Optional<Parkour> parkour = repository.findParkourByName(name);
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        Block plate = resolvePlateBlock(player);
        if (plate == null) {
            player.sendMessage("§cStand on a pressure plate or look at one.");
            return;
        }
        Location location = plate.getLocation().clone();
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());
        int nextIndex = repository.getMaxCheckpointIndex(parkour.get().id()) + 1;
        boolean created = repository.createCheckpoint(parkour.get().id(), nextIndex, location, plate.getType());
        if (created) {
            cache.reload();
            refreshPlateHolograms();
            player.sendMessage("§aCheckpoint " + nextIndex + " added for " + name + ".");
        } else {
            player.sendMessage("§cFailed to add checkpoint.");
        }
    }

    private void handleDeleteCheckpoint(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /parkour delcheckpoint <name> <index>");
            return;
        }
        int index;
        try {
            index = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("§cIndex must be a number.");
            return;
        }
        String name = joinArgs(args, 1, args.length - 1);
        Optional<Parkour> parkour = repository.findParkourByName(name);
        if (parkour.isEmpty()) {
            player.sendMessage("§cParkour not found.");
            return;
        }
        boolean deleted = repository.deleteCheckpoint(parkour.get().id(), index);
        if (deleted) {
            cache.reload();
            refreshPlateHolograms();
            player.sendMessage("§aCheckpoint removed.");
        } else {
            player.sendMessage("§cFailed to remove checkpoint.");
        }
    }

    private void handleList(Player player) {
        StringBuilder builder = new StringBuilder("§aParkours: ");
        cache.getAllParkours().values().forEach(parkour -> builder.append(parkour.name()).append(" "));
        player.sendMessage(builder.toString().trim());
    }

    private void handleLeaderboard(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /parkour leaderboard <place|remove> <parkour|id>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("place")) {
            String name = joinArgs(args, 2);
            Block block = resolvePlateBlock(player);
            Location location = block != null ? block.getLocation().add(0.5, 1.2, 0.5) : player.getLocation();
            leaderboardService.createLeaderboard(name, location, player);
            return;
        }
        if (action.equals("remove")) {
            try {
                int id = Integer.parseInt(args[2]);
                leaderboardService.removeLeaderboard(id, player);
            } catch (NumberFormatException ex) {
                player.sendMessage("§cLeaderboard id must be a number.");
            }
            return;
        }
        player.sendMessage("§cUsage: /parkour leaderboard <place|remove> <parkour|id>");
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

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/parkour create <name>");
        sender.sendMessage("§e/parkour gui");
        sender.sendMessage("§e/parkour menu");
        sender.sendMessage("§e/parkour rename <old name> -> <new name>");
        sender.sendMessage("§e/parkour delete <name>");
        sender.sendMessage("§e/parkour setstart <name>");
        sender.sendMessage("§e/parkour setend <name>");
        sender.sendMessage("§e/parkour addcheckpoint <name>");
        sender.sendMessage("§e/parkour delcheckpoint <name> <index>");
        sender.sendMessage("§e/parkour leaderboard place <name>");
        sender.sendMessage("§e/parkour leaderboard remove <id>");
        sender.sendMessage("§e/parkour list");
        sender.sendMessage("§e/parkour last");
        sender.sendMessage("§e/parkour leave");
        sender.sendMessage("§e/parkour reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            return List.of("create", "gui", "menu", "rename", "delete", "setstart", "setend", "addcheckpoint", "delcheckpoint", "leaderboard", "list", "leave", "last", "reload");
        }
        if (args.length == 2 && List.of("setstart", "setend", "addcheckpoint", "delcheckpoint", "delete", "leaderboard").contains(args[0])) {
            if (args[0].equalsIgnoreCase("leaderboard")) {
                return List.of("place", "remove");
            }
            cache.getAllParkours().values().forEach(parkour -> results.add(parkour.name()));
            return results;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard") && args[1].equalsIgnoreCase("place")) {
            cache.getAllParkours().values().forEach(parkour -> results.add(parkour.name()));
            return results;
        }
        return results;
    }

    private void refreshPlateHolograms() {
        if (plugin.getPlateHologramService() != null) {
            plugin.getPlateHologramService().refreshAll();
        }
    }

    private String joinArgs(String[] args, int start) {
        return joinArgs(args, start, args.length);
    }

    private String joinArgs(String[] args, int start, int end) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }
}
