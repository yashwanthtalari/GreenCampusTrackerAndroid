package com.mnr.greencampus.model;

import java.time.LocalDate;

public class Neem extends Tree {
    public Neem(String id, LocalDate date, Location loc) {
        super(id, date, loc);
    }

    @Override
    public String speciesName() {
        return "Neem";
    }
}

