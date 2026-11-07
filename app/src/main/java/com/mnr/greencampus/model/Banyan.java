package com.mnr.greencampus.model;

import java.time.LocalDate;

public class Banyan extends Tree {
    public Banyan(String id, LocalDate date, Location loc) {
        super(id, date, loc);
    }

    @Override
    public String speciesName() {
        return "Banyan";
    }
}

