package org.ayosynk.hubparkour.util;

import org.bukkit.Material;

public final class PressurePlateUtil {
    private PressurePlateUtil() {
    }

    public static boolean isPressurePlate(Material material) {
        return material != null && material.name().endsWith("_PRESSURE_PLATE");
    }
}
