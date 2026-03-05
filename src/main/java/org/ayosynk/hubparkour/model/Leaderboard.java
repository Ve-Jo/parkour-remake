package org.ayosynk.hubparkour.model;

import org.bukkit.Location;

public record Leaderboard(
        int id,
        int parkourId,
        Location location
) {}
