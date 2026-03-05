package org.ayosynk.hubparkour.model;

import org.bukkit.Location;

public record Parkour(
        int id,
        String name,
        Location startLocation,
        Location endLocation
) {
    public boolean hasStart() {
        return startLocation != null;
    }

    public boolean hasEnd() {
        return endLocation != null;
    }
}
