package com.mnr.greencampus.model;

import com.mnr.greencampus.exception.SaplingDeadException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sapling {
    private final Tree tree;
    private Volunteer volunteer;
    private final WateringSchedule schedule;
    private String healthStatus = "HEALTHY";
    private String deathReason;
    private double currentHeight = 0.0;
    private final ArrayList<GrowthRecord> records = new ArrayList<>();

    public Sapling(Tree tree, WateringSchedule schedule) {
        this.tree = tree;
        this.schedule = schedule;
    }

    public Tree getTree() {
        return tree;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public WateringSchedule getSchedule() {
        return schedule;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getDeathReason() {
        return deathReason;
    }

    public void setDeathReason(String deathReason) {
        this.deathReason = deathReason;
    }

    public double getCurrentHeight() {
        return currentHeight;
    }

    public void setCurrentHeight(double currentHeight) {
        this.currentHeight = currentHeight;
    }

    public List<GrowthRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public void assignVolunteer(Volunteer v) {
        this.volunteer = v;
    }

    public void addRecord(GrowthRecord r) throws SaplingDeadException {
        if (!tree.isAlive()) {
            throw new SaplingDeadException("Sapling " + tree.getId() + " is already marked DEAD. Updates are blocked.");
        }
        records.add(0, r); // latest first
        this.healthStatus = r.getHealth();
        this.currentHeight = r.getHeightCm();
        if ("DEAD".equalsIgnoreCase(r.getHealth())) {
            tree.markDead();
        }
    }

    public void setRecords(List<GrowthRecord> list) {
        records.clear();
        if (list != null) {
            records.addAll(list);
            if (!records.isEmpty()) {
                currentHeight = records.get(0).getHeightCm();
            }
        }
    }
}

