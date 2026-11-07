package com.mnr.greencampus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mnr.greencampus.exception.SaplingDeadException;
import com.mnr.greencampus.model.*;
import com.mnr.greencampus.service.SaplingService;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "green_campus.db";
    private static final int DB_VERSION = 2;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS saplings (" +
                "id TEXT PRIMARY KEY, " +
                "species TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "planted TEXT NOT NULL, " +
                "interval_days INTEGER NOT NULL, " +
                "next_watering TEXT NOT NULL, " +
                "volunteer_id TEXT, " +
                "alive INTEGER NOT NULL DEFAULT 1, " +
                "health_status TEXT NOT NULL DEFAULT 'HEALTHY', " +
                "death_reason TEXT, " +
                "current_height REAL DEFAULT 0.0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS volunteers (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "phone TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS growth (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sapling_id TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "height REAL NOT NULL, " +
                "health TEXT NOT NULL, " +
                "note TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS growth");
        db.execSQL("DROP TABLE IF EXISTS volunteers");
        db.execSQL("DROP TABLE IF EXISTS saplings");
        onCreate(db);
    }

    public static String today() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String addDays(String dateStr, int days) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.plusDays(days).format(DATE_FORMATTER);
        } catch (Exception e) {
            return today();
        }
    }

    // ==================== Sapling CRUD ====================

    public boolean addSapling(String id, String species, String location, String planted, int interval) {
        try {
            LocalDate plantDate = LocalDate.parse(planted, DATE_FORMATTER);
            Location loc = new Location(location);
            Tree tree = TreeFactory.create(species, id, plantDate, loc);
            WateringSchedule schedule = new WateringSchedule(interval, plantDate);
            Sapling sapling = new Sapling(tree, schedule);
            return insertSapling(sapling);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insertSapling(Sapling s) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", s.getTree().getId());
        v.put("species", s.getTree().speciesName());
        v.put("location", s.getTree().getLocation().getArea());
        v.put("planted", s.getTree().getPlantedOn().format(DATE_FORMATTER));
        v.put("interval_days", s.getSchedule().getIntervalDays());
        v.put("next_watering", s.getSchedule().getNextWatering().format(DATE_FORMATTER));
        v.put("volunteer_id", s.getVolunteer() != null ? s.getVolunteer().getId() : null);
        v.put("alive", s.getTree().isAlive() ? 1 : 0);
        v.put("health_status", s.getHealthStatus());
        v.put("death_reason", s.getDeathReason());
        v.put("current_height", s.getCurrentHeight());
        return db.insert("saplings", null, v) != -1;
    }

    public boolean deleteSapling(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("growth", "sapling_id=?", new String[]{id});
        return db.delete("saplings", "id=?", new String[]{id}) > 0;
    }

    public Sapling getSapling(String id) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT s.id, s.species, s.location, s.planted, s.interval_days, s.next_watering, " +
                "s.alive, s.health_status, s.death_reason, s.current_height, " +
                "v.id, v.name, v.phone " +
                "FROM saplings s LEFT JOIN volunteers v ON s.volunteer_id = v.id WHERE s.id = ?";
        Cursor c = db.rawQuery(query, new String[]{id});
        Sapling s = null;
        if (c.moveToFirst()) {
            s = cursorToSapling(c);
            s.setRecords(getGrowthRecords(id));
        }
        c.close();
        return s;
    }

    public List<Sapling> getAllSaplings() {
        List<Sapling> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT s.id, s.species, s.location, s.planted, s.interval_days, s.next_watering, " +
                "s.alive, s.health_status, s.death_reason, s.current_height, " +
                "v.id, v.name, v.phone " +
                "FROM saplings s LEFT JOIN volunteers v ON s.volunteer_id = v.id ORDER BY s.id ASC";
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            list.add(cursorToSapling(c));
        }
        c.close();
        return list;
    }

    public List<Sapling> getDueSaplings() {
        List<Sapling> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT s.id, s.species, s.location, s.planted, s.interval_days, s.next_watering, " +
                "s.alive, s.health_status, s.death_reason, s.current_height, " +
                "v.id, v.name, v.phone " +
                "FROM saplings s LEFT JOIN volunteers v ON s.volunteer_id = v.id " +
                "WHERE s.alive=1 AND s.next_watering<=? ORDER BY s.next_watering ASC";
        Cursor c = db.rawQuery(sql, new String[]{today()});
        while (c.moveToNext()) {
            list.add(cursorToSapling(c));
        }
        c.close();
        return list;
    }

    public void populateService(SaplingService service) {
        service.clear();
        for (Sapling s : getAllSaplings()) {
            try {
                service.register(s);
            } catch (Exception ignored) {}
        }
    }

    private Sapling cursorToSapling(Cursor c) {
        String id = c.getString(0);
        String species = c.getString(1);
        String locationArea = c.getString(2);
        String plantedStr = c.getString(3);
        int interval = c.getInt(4);
        String nextWateringStr = c.getString(5);
        boolean alive = c.getInt(6) == 1;
        String healthStatus = c.getString(7);
        String deathReason = c.getString(8);
        double currentHeight = c.getDouble(9);

        String vId = c.getString(10);
        String vName = c.getString(11);
        String vPhone = c.getString(12);

        LocalDate plantDate = LocalDate.parse(plantedStr, DATE_FORMATTER);
        LocalDate nextWatering = LocalDate.parse(nextWateringStr, DATE_FORMATTER);

        Location location = new Location(locationArea);
        Tree tree = TreeFactory.create(species, id, plantDate, location);
        tree.setAlive(alive);

        WateringSchedule schedule = new WateringSchedule(interval, nextWatering, plantDate);
        Sapling sapling = new Sapling(tree, schedule);
        sapling.setHealthStatus(healthStatus != null ? healthStatus : (alive ? "HEALTHY" : "DEAD"));
        sapling.setDeathReason(deathReason);
        sapling.setCurrentHeight(currentHeight);

        if (vId != null) {
            Volunteer v = new Volunteer(vId, vName, vPhone);
            sapling.assignVolunteer(v);
        }

        return sapling;
    }

    // ==================== Volunteer CRUD ====================

    public boolean addVolunteer(String id, String name, String phone) {
        try {
            Volunteer v = new Volunteer(id, name, phone);
            ContentValues cv = new ContentValues();
            cv.put("id", v.getId());
            cv.put("name", v.getName());
            cv.put("phone", v.getPhone());
            return getWritableDatabase().insert("volunteers", null, cv) != -1;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteVolunteer(String id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.putNull("volunteer_id");
        db.update("saplings", cv, "volunteer_id=?", new String[]{id});
        return db.delete("volunteers", "id=?", new String[]{id}) > 0;
    }

    public boolean assignVolunteer(String saplingId, String volunteerId) {
        SQLiteDatabase db = getWritableDatabase();
        if (volunteerId != null && !volunteerId.isEmpty()) {
            Cursor c = db.rawQuery("SELECT id FROM volunteers WHERE id=?", new String[]{volunteerId});
            boolean exists = c.moveToFirst();
            c.close();
            if (!exists) return false;
        }

        ContentValues v = new ContentValues();
        if (volunteerId == null || volunteerId.isEmpty()) {
            v.putNull("volunteer_id");
        } else {
            v.put("volunteer_id", volunteerId);
        }
        return db.update("saplings", v, "id=? AND alive=1", new String[]{saplingId}) > 0;
    }

    public List<Volunteer> getAllVolunteers() {
        List<Volunteer> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT v.id, v.name, v.phone, COUNT(s.id) as assigned_count " +
                "FROM volunteers v LEFT JOIN saplings s ON v.id = s.volunteer_id AND s.alive = 1 " +
                "GROUP BY v.id, v.name, v.phone ORDER BY v.name ASC";
        Cursor c = db.rawQuery(query, null);
        while (c.moveToNext()) {
            Volunteer v = new Volunteer(c.getString(0), c.getString(1), c.getString(2));
            v.setAssignedCount(c.getInt(3));
            list.add(v);
        }
        c.close();
        return list;
    }

    public Volunteer getVolunteer(String id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, phone FROM volunteers WHERE id=?", new String[]{id});
        Volunteer v = null;
        if (c.moveToFirst()) {
            v = new Volunteer(c.getString(0), c.getString(1), c.getString(2));
        }
        c.close();
        return v;
    }

    // ==================== Watering Operations ====================

    public boolean recordWatering(String id) throws SaplingDeadException {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT interval_days, alive FROM saplings WHERE id=?", new String[]{id});
        if (!c.moveToFirst()) {
            c.close();
            return false;
        }
        int interval = c.getInt(0);
        boolean alive = c.getInt(1) == 1;
        c.close();

        if (!alive) {
            throw new SaplingDeadException("Sapling " + id + " is marked DEAD. Watering is blocked.");
        }

        String today = today();
        String nextDate = addDays(today, interval);

        ContentValues v = new ContentValues();
        v.put("next_watering", nextDate);

        return db.update("saplings", v, "id=?", new String[]{id}) > 0;
    }

    // ==================== Growth / Observation Operations ====================

    public String addGrowth(String id, double height, String health, String note, String deathReason)
            throws SaplingDeadException {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT alive FROM saplings WHERE id=?", new String[]{id});
        if (!c.moveToFirst()) {
            c.close();
            return "Sapling ID not found.";
        }
        boolean alive = c.getInt(0) == 1;
        c.close();

        if (!alive) {
            throw new SaplingDeadException("Sapling " + id + " is already marked DEAD. Updates are blocked.");
        }

        ContentValues g = new ContentValues();
        g.put("sapling_id", id);
        g.put("date", today());
        g.put("height", height);
        g.put("health", health);
        g.put("note", note);

        if (db.insert("growth", null, g) == -1) {
            return "Could not save observation.";
        }

        ContentValues s = new ContentValues();
        s.put("current_height", height);
        s.put("health_status", health);

        if ("DEAD".equalsIgnoreCase(health)) {
            s.put("alive", 0);
            s.put("death_reason", deathReason);
        }

        db.update("saplings", s, "id=?", new String[]{id});
        return "Growth / health observation saved.";
    }

    public List<GrowthRecord> getGrowthRecords(String saplingId) {
        List<GrowthRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT _id, sapling_id, date, height, health, note FROM growth WHERE sapling_id=? ORDER BY date DESC, _id DESC",
                new String[]{saplingId});
        while (c.moveToNext()) {
            LocalDate date;
            try {
                date = LocalDate.parse(c.getString(2), DATE_FORMATTER);
            } catch (Exception e) {
                date = LocalDate.now();
            }
            list.add(new GrowthRecord(
                    c.getLong(0),
                    c.getString(1),
                    date,
                    c.getDouble(3),
                    c.getString(4),
                    c.getString(5)
            ));
        }
        c.close();
        return list;
    }

    // ==================== Survival Statistics & Report ====================

    public SurvivalStats getSurvivalStats() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN alive=1 THEN 1 ELSE 0 END) FROM saplings", null);
        c.moveToFirst();
        int total = c.getInt(0);
        int living = c.isNull(1) ? 0 : c.getInt(1);
        c.close();

        SurvivalStats stats = new SurvivalStats(total, living);

        Cursor sp = db.rawQuery("SELECT species, COUNT(*), SUM(CASE WHEN alive=1 THEN 1 ELSE 0 END) FROM saplings GROUP BY species ORDER BY species", null);
        while (sp.moveToNext()) {
            stats.addSpeciesStat(new SurvivalStats.SpeciesStat(
                    sp.getString(0),
                    sp.getInt(1),
                    sp.isNull(2) ? 0 : sp.getInt(2)
            ));
        }
        sp.close();

        return stats;
    }

    public String report() {
        SurvivalStats stats = getSurvivalStats();
        DecimalFormat df = new DecimalFormat("0.00");
        StringBuilder out = new StringBuilder();
        out.append("========== SURVIVAL REPORT ==========\n\n");
        out.append("Total planted : ").append(stats.getTotalPlanted()).append("\n");
        out.append("Surviving     : ").append(stats.getSurviving()).append("\n");
        out.append("Dead          : ").append(stats.getDead()).append("\n");
        out.append("Survival rate : ").append(df.format(stats.getSurvivalRate())).append("%\n\n");
        out.append("SPECIES-WISE BREAKDOWN\n");
        out.append(String.format(Locale.US, "%-10s %-8s %-10s %s\n", "Species", "Planted", "Surviving", "Survival %"));
        out.append("------------------------------------------\n");

        for (SurvivalStats.SpeciesStat stat : stats.getSpeciesStats()) {
            out.append(String.format(Locale.US, "%-10s %-8d %-10d %s%%\n",
                    stat.getSpecies(),
                    stat.getPlanted(),
                    stat.getSurviving(),
                    df.format(stat.getSurvivalRate())));
        }

        return out.toString();
    }

    public String listGrowth(String id) {
        List<GrowthRecord> records = getGrowthRecords(id);
        if (records.isEmpty()) return "No observations recorded.";
        StringBuilder b = new StringBuilder();
        for (GrowthRecord r : records) {
            b.append(r.getDate().format(DATE_FORMATTER))
                    .append(" | ")
                    .append(r.getHeightCm())
                    .append(" cm | ")
                    .append(r.getHealth())
                    .append("\n")
                    .append(r.getNote() != null && !r.getNote().isEmpty() ? "Note: " + r.getNote() : "")
                    .append("\n\n");
        }
        return b.toString();
    }

    public boolean isEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM saplings", null);
        boolean empty = true;
        if (c.moveToFirst()) {
            empty = c.getInt(0) == 0;
        }
        c.close();
        return empty;
    }

    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("growth", null, null);
        db.delete("volunteers", null, null);
        db.delete("saplings", null, null);
    }

    public void seedDemoData() {
        clearAllData();

        addVolunteer("V01", "Priya Sharma", "+91 98765 43210");
        addVolunteer("V02", "Rahul Verma", "+91 98450 12345");
        addVolunteer("V03", "Ananya Reddy", "+91 97012 34567");
        addVolunteer("V04", "Karthik Rao", "+91 91234 56780");

        addSaplingDirect("GC101", "Neem", "Main Block - East Garden", addDays(today(), -14), 2, today(), "V01", 1, "HEALTHY", null, 38.5);
        addSaplingDirect("GC102", "Mango", "Library Courtyard", addDays(today(), -20), 3, addDays(today(), 1), "V02", 1, "HEALTHY", null, 45.0);
        addSaplingDirect("GC103", "Banyan", "Campus Main Gate", addDays(today(), -45), 5, addDays(today(), 3), "V01", 1, "HEALTHY", null, 72.0);
        addSaplingDirect("GC104", "Teak", "Sports Ground North", addDays(today(), -30), 4, today(), "V03", 1, "NEEDS_CARE", null, 52.3);
        addSaplingDirect("GC105", "Neem", "Science Complex West", addDays(today(), -10), 2, addDays(today(), -1), "V02", 1, "HEALTHY", null, 28.0);
        addSaplingDirect("GC106", "Mango", "Student Center Lawn", addDays(today(), -60), 3, addDays(today(), -10), "V04", 0, "DEAD", "Soil waterlogging after heavy rains", 31.0);
        addSaplingDirect("GC107", "Banyan", "Botanical Garden Path", addDays(today(), -40), 6, addDays(today(), 4), "V03", 1, "HEALTHY", null, 64.5);
        addSaplingDirect("GC108", "Teak", "Engineering Block East", addDays(today(), -25), 4, addDays(today(), 1), "V01", 1, "HEALTHY", null, 34.2);

        try {
            addGrowthDirect("GC101", addDays(today(), -14), 24.5, "HEALTHY", "Initial plantation measurement");
            addGrowthDirect("GC101", addDays(today(), -7), 31.0, "HEALTHY", "New leaf buds developing");
            addGrowthDirect("GC101", today(), 38.5, "HEALTHY", "Strong stem growth");

            addGrowthDirect("GC104", addDays(today(), -15), 48.0, "HEALTHY", "Normal development");
            addGrowthDirect("GC104", today(), 52.3, "NEEDS_CARE", "Slight yellowing of lower foliage; check compost");

            addGrowthDirect("GC106", addDays(today(), -40), 28.0, "HEALTHY", "Initial observation");
            addGrowthDirect("GC106", addDays(today(), -20), 31.0, "DEAD", "Root rot due to water stagnation");
        } catch (Exception ignored) {}
    }

    private void addSaplingDirect(String id, String species, String location, String planted,
                                  int interval, String nextWatering, String vId, int alive,
                                  String healthStatus, String deathReason, double height) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("species", species);
        v.put("location", location);
        v.put("planted", planted);
        v.put("interval_days", interval);
        v.put("next_watering", nextWatering);
        v.put("volunteer_id", vId);
        v.put("alive", alive);
        v.put("health_status", healthStatus);
        v.put("death_reason", deathReason);
        v.put("current_height", height);
        db.insertWithOnConflict("saplings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void addGrowthDirect(String id, String date, double height, String health, String note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues g = new ContentValues();
        g.put("sapling_id", id);
        g.put("date", date);
        g.put("height", height);
        g.put("health", health);
        g.put("note", note);
        db.insert("growth", null, g);
    }
}
