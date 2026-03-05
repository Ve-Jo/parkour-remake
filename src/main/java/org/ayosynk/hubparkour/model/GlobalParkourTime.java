package org.ayosynk.hubparkour.model;

import java.util.UUID;

public record GlobalParkourTime(
        int parkourId,
        UUID playerUuid,
        double timeSeconds
) {}
