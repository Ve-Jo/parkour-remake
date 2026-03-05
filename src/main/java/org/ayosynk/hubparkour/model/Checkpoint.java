package org.ayosynk.hubparkour.model;

import org.bukkit.Location;
import org.bukkit.Material;

public record Checkpoint(
        int id,
        int parkourId,
        int index,
        Location location,
        Material material
) {}
