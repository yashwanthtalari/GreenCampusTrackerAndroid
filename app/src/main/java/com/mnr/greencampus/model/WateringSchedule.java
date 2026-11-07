package com.mnr.greencampus.model;

import java.time.LocalDate;

public class WateringSchedule {
    private final int intervalDays;
    private LocalDate nextWatering;
    private LocalDate lastWatered;

    public WateringSchedule(int intervalDays, LocalDate plantedOn) {
        if (intervalDays < 1 || intervalDays > 30) {
            throw new IllegalArgumentException("Watering interval must be between 1 and 30 days");
        }
        this.intervalDays = intervalDays;
        this.nextWatering = plantedOn.plusDays(intervalDays);
        this.lastWatered = plantedOn;
    }

    public WateringSchedule(int intervalDays, LocalDate nextWatering, LocalDate lastWatered) {
        if (intervalDays < 1 || intervalDays > 30) {
            throw new IllegalArgumentException("Watering interval must be between 1 and 30 days");
        }
        this.intervalDays = intervalDays;
        this.nextWatering = nextWatering;
        this.lastWatered = lastWatered;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public LocalDate getNextWatering() {
        return nextWatering;
    }

    public LocalDate getLastWatered() {
        return lastWatered;
    }

    public boolean isDue() {
        return !LocalDate.now().isBefore(nextWatering);
    }

    public void markWatered() {
        this.lastWatered = LocalDate.now();
        this.nextWatering = LocalDate.now().plusDays(intervalDays);
    }
}

