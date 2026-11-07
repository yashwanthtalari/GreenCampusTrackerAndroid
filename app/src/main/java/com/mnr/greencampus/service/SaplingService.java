package com.mnr.greencampus.service;

import com.mnr.greencampus.model.Sapling;
import java.util.*;

public class SaplingService {
    private final HashMap<String, Sapling> saplings = new HashMap<>();

    public void register(Sapling s) {
        String id = s.getTree().getId();
        if (saplings.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ID: " + id);
        }
        saplings.put(id, s);
    }

    public Sapling search(String id) {
        return saplings.get(id);
    }

    public Collection<Sapling> all() {
        return saplings.values();
    }

    public void clear() {
        saplings.clear();
    }

    public double survivalRate() {
        if (saplings.isEmpty()) return 0.0;
        long alive = saplings.values().stream().filter(s -> s.getTree().isAlive()).count();
        return 100.0 * alive / saplings.size();
    }
}

