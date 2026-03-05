package org.ayosynk.hubparkour.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationUtil {
    private LocationUtil() {
    }

    public static String toKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public static Location fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.split(":");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Location toCenter(Location location) {
        if (location == null) {
            return null;
        }
        return new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY(), location.getBlockZ() + 0.5);
    }

    public static String toStorage(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ()
                + ":" + location.getYaw() + ":" + location.getPitch();
    }

    public static Location fromStorage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(":");
        if (parts.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
