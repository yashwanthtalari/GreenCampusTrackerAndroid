package com.mnr.greencampus.service;

import com.mnr.greencampus.model.Sapling;

public class ReminderTask implements Runnable {
    public interface DueReminderCallback {
        void onWateringDue(Sapling sapling);
    }

    private final SaplingService service;
    private DueReminderCallback callback;

    public ReminderTask(SaplingService service) {
        this.service = service;
    }

    public ReminderTask(SaplingService service, DueReminderCallback callback) {
        this.service = service;
        this.callback = callback;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (Sapling s : service.all()) {
                if (s.getTree().isAlive() && s.getSchedule().isDue()) {
                    String caretaker = s.getVolunteer() == null ? "Unassigned" : s.getVolunteer().getName();
                    System.out.println("WATER DUE: " + s.getTree().getId() + " | " +
                            s.getTree().speciesName() + " | " + s.getTree().getLocation().getArea() +
                            " | Caretaker: " + caretaker);
                    if (callback != null) {
                        callback.onWateringDue(s);
                    }
                }
            }
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

