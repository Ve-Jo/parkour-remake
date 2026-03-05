package org.ayosynk.hubparkour.model;

import java.util.UUID;

public record ParkourTime(
        UUID playerUuid,
        double timeSeconds
) {}
