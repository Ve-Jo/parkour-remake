package org.ayosynk.hubparkour.model;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class ParkourSession {
    private final String parkourName;
    private final long startMillis;
    private int maxCheckpoints;
    private int lastCheckpoint;
    private boolean allowFlight;
    private boolean wasFlying;
    private final Map<Integer, ItemStack> inventory = new HashMap<>();

    public ParkourSession(String parkourName) {
        this.parkourName = parkourName;
        this.startMillis = System.currentTimeMillis();
    }

    public String getParkourName() {
        return parkourName;
    }

    public double getElapsedSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000.0;
    }

    public int getMaxCheckpoints() {
        return maxCheckpoints;
    }

    public void setMaxCheckpoints(int maxCheckpoints) {
        this.maxCheckpoints = Math.max(0, maxCheckpoints);
    }

    public int getLastCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastCheckpoint(int lastCheckpoint) {
        this.lastCheckpoint = Math.max(0, lastCheckpoint);
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public void setAllowFlight(boolean allowFlight) {
        this.allowFlight = allowFlight;
    }

    public boolean wasFlying() {
        return wasFlying;
    }

    public void setWasFlying(boolean wasFlying) {
        this.wasFlying = wasFlying;
    }

    public Map<Integer, ItemStack> getInventory() {
        return inventory;
    }
}
