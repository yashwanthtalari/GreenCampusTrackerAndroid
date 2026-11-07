package com.mnr.greencampus.model;

import java.time.LocalDate;

public abstract class Tree {
    private final String id;
    private final LocalDate plantedOn;
    private final Location location;
    private boolean alive = true;

    protected Tree(String id, LocalDate plantedOn, Location location) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Sapling ID required");
        }
        if (plantedOn == null) {
            throw new IllegalArgumentException("Planting date required");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location required");
        }
        this.id = id.trim();
        this.plantedOn = plantedOn;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public LocalDate getPlantedOn() {
        return plantedOn;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isAlive() {
        return alive;
    }

    public void markDead() {
        this.alive = false;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public abstract String speciesName();
}

