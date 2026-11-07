package com.mnr.greencampus.model;

import java.time.LocalDate;

public class Teak extends Tree {
    public Teak(String id, LocalDate date, Location loc) {
        super(id, date, loc);
    }

    @Override
    public String speciesName() {
        return "Teak";
    }
}

