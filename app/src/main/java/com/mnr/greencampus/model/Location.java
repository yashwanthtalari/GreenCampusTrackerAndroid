package com.mnr.greencampus.model;

public class Location {
    private final String area;

    public Location(String area) {
        if (area == null || area.trim().isEmpty()) {
            throw new IllegalArgumentException("Location required");
        }
        this.area = area.trim();
    }

    public String getArea() {
        return area;
    }

    @Override
    public String toString() {
        return area;
    }
}

