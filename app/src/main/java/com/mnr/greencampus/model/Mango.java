package com.mnr.greencampus.model;

import java.time.LocalDate;

public class Mango extends Tree {
    public Mango(String id, LocalDate date, Location loc) {
        super(id, date, loc);
    }

    @Override
    public String speciesName() {
        return "Mango";
    }
}

