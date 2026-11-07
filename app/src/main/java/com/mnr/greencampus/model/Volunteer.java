package com.mnr.greencampus.model;

public class Volunteer {
    private final String id;
    private final String name;
    private final String phone;
    private int assignedCount = 0;

    public Volunteer(String id, String name, String phone) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Volunteer ID and name required");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.phone = phone != null ? phone.trim() : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public int getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}

