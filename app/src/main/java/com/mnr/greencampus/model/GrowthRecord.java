package com.mnr.greencampus.model;

import java.time.LocalDate;

public class GrowthRecord {
    private long id;
    private String saplingId;
    private final LocalDate date;
    private final double heightCm;
    private final String health;
    private final String note;

    public GrowthRecord(double heightCm, String health, String note) {
        if (heightCm <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        this.date = LocalDate.now();
        this.heightCm = heightCm;
        this.health = health != null ? health : "HEALTHY";
        this.note = note != null ? note : "";
    }

    public GrowthRecord(long id, String saplingId, LocalDate date, double heightCm, String health, String note) {
        this.id = id;
        this.saplingId = saplingId;
        this.date = date;
        this.heightCm = heightCm;
        this.health = health;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public String getSaplingId() {
        return saplingId;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public String getHealth() {
        return health;
    }

    public String getNote() {
        return note;
    }
}

