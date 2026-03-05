package org.ayosynk.hubparkour.util;

import org.bukkit.Material;

public final class MaterialUtil {
    private MaterialUtil() {
    }

    public static Material fromId(String id, Material fallback) {
        if (id == null || id.isBlank()) {
            return fallback;
        }
        String normalized = id.replace("minecraft:", "").toUpperCase();
        Material material = Material.matchMaterial(normalized);
        return material == null ? fallback : material;
    }
}
