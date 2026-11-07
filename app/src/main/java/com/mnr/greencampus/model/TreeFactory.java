package com.mnr.greencampus.model;

import java.time.LocalDate;

public class TreeFactory {
    public static Tree create(String species, String id, LocalDate date, Location location) {
        if (species == null) species = "Neem";
        switch (species.trim().toLowerCase()) {
            case "mango":
                return new Mango(id, date, location);
            case "banyan":
                return new Banyan(id, date, location);
            case "teak":
                return new Teak(id, date, location);
            case "neem":
            default:
                return new Neem(id, date, location);
        }
    }
}

