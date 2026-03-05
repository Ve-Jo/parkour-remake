package org.ayosynk.hubparkour.service;

import org.ayosynk.hubparkour.model.Checkpoint;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.repository.ParkourRepository;
import org.ayosynk.hubparkour.util.LocationUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParkourCache {
    private final ParkourRepository repository;
    private final Map<Integer, Parkour> parkoursById = new HashMap<>();
    private final Map<String, Parkour> parkoursByName = new HashMap<>();
    private final Map<String, Integer> startByLocation = new HashMap<>();
    private final Map<String, Integer> endByLocation = new HashMap<>();
    private final Map<String, Checkpoint> checkpointsByLocation = new HashMap<>();

    public ParkourCache(ParkourRepository repository) {
        this.repository = repository;
    }

    public void reload() {
        parkoursById.clear();
        parkoursByName.clear();
        startByLocation.clear();
        endByLocation.clear();
        checkpointsByLocation.clear();
        for (Parkour parkour : repository.listParkours()) {
            parkoursById.put(parkour.id(), parkour);
            parkoursByName.put(parkour.name().toLowerCase(), parkour);
            if (parkour.startLocation() != null) {
                startByLocation.put(LocationUtil.toKey(parkour.startLocation()), parkour.id());
            }
            if (parkour.endLocation() != null) {
                endByLocation.put(LocationUtil.toKey(parkour.endLocation()), parkour.id());
            }
            List<Checkpoint> checkpoints = repository.getCheckpoints(parkour.id());
            for (Checkpoint checkpoint : checkpoints) {
                if (checkpoint.location() != null) {
                    checkpointsByLocation.put(LocationUtil.toKey(checkpoint.location()), checkpoint);
                }
            }
        }
    }

    public Optional<Parkour> getParkourById(int id) {
        return Optional.ofNullable(parkoursById.get(id));
    }

    public Optional<Parkour> getParkourByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parkoursByName.get(name.toLowerCase()));
    }

    public Optional<Integer> getParkourIdByStart(String locationKey) {
        return Optional.ofNullable(startByLocation.get(locationKey));
    }

    public Optional<Integer> getParkourIdByEnd(String locationKey) {
        return Optional.ofNullable(endByLocation.get(locationKey));
    }

    public Optional<Checkpoint> getCheckpointByLocation(String locationKey) {
        return Optional.ofNullable(checkpointsByLocation.get(locationKey));
    }

    public List<Checkpoint> getCheckpoints(int parkourId) {
        return repository.getCheckpoints(parkourId);
    }

    public Map<Integer, Parkour> getAllParkours() {
        return Collections.unmodifiableMap(parkoursById);
    }
}
