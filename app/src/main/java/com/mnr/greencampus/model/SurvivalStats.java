package com.mnr.greencampus.model;

import java.util.ArrayList;
import java.util.List;

public class SurvivalStats {
    public static class SpeciesStat {
        private final String species;
        private final int planted;
        private final int surviving;
        private final int dead;
        private final double survivalRate;

        public SpeciesStat(String species, int planted, int surviving) {
            this.species = species;
            this.planted = planted;
            this.surviving = surviving;
            this.dead = planted - surviving;
            this.survivalRate = planted == 0 ? 0.0 : (surviving * 100.0 / planted);
        }

        public String getSpecies() { return species; }
        public int getPlanted() { return planted; }
        public int getSurviving() { return surviving; }
        public int getDead() { return dead; }
        public double getSurvivalRate() { return survivalRate; }
    }

    private final int totalPlanted;
    private final int surviving;
    private final int dead;
    private final double survivalRate;
    private final List<SpeciesStat> speciesStats = new ArrayList<>();

    public SurvivalStats(int totalPlanted, int surviving) {
        this.totalPlanted = totalPlanted;
        this.surviving = surviving;
        this.dead = totalPlanted - surviving;
        this.survivalRate = totalPlanted == 0 ? 0.0 : (surviving * 100.0 / totalPlanted);
    }

    public void addSpeciesStat(SpeciesStat stat) {
        speciesStats.add(stat);
    }

    public int getTotalPlanted() { return totalPlanted; }
    public int getSurviving() { return surviving; }
    public int getDead() { return dead; }
    public double getSurvivalRate() { return survivalRate; }
    public List<SpeciesStat> getSpeciesStats() { return speciesStats; }
}

