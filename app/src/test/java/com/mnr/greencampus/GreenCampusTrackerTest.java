package com.mnr.greencampus;

import com.mnr.greencampus.exception.SaplingDeadException;
import com.mnr.greencampus.model.*;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class GreenCampusTrackerTest {

    // T1: Register valid sapling
    @Test
    public void testT1_RegisterValidSapling() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        Location loc = new Location("Main Block - East Garden");
        Tree tree = TreeFactory.create("Neem", "GC101", date, loc);
        WateringSchedule schedule = new WateringSchedule(2, date);
        Sapling sapling = new Sapling(tree, schedule);

        assertNotNull(sapling);
        assertEquals("GC101", sapling.getTree().getId());
        assertEquals("Neem", sapling.getTree().speciesName());
        assertTrue(sapling.getTree().isAlive());
    }

    // T2: Duplicate ID / ID validation
    @Test
    public void testT2_SaplingIdValidation() {
        try {
            new Location("");
            fail("Expected IllegalArgumentException for blank location");
        } catch (IllegalArgumentException e) {
            assertEquals("Location required", e.getMessage());
        }

        try {
            TreeFactory.create("Neem", "", LocalDate.now(), new Location("East Garden"));
            fail("Expected IllegalArgumentException for empty tree id");
        } catch (IllegalArgumentException e) {
            assertEquals("Sapling ID required", e.getMessage());
        }
    }

    // T3: Invalid interval (0 days or > 30)
    @Test
    public void testT3_InvalidWateringInterval() {
        LocalDate now = LocalDate.now();
        try {
            new WateringSchedule(0, now);
            fail("Expected exception for 0 days interval");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("1 and 30"));
        }

        try {
            new WateringSchedule(35, now);
            fail("Expected exception for >30 days interval");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("1 and 30"));
        }
    }

    // T4: Search / retrieve sapling details
    @Test
    public void testT4_SaplingDetails() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        Tree tree = TreeFactory.create("Mango", "GC102", date, new Location("Library Courtyard"));
        WateringSchedule schedule = new WateringSchedule(3, date);
        Sapling sapling = new Sapling(tree, schedule);

        assertEquals("GC102", sapling.getTree().getId());
        assertEquals("Mango", sapling.getTree().speciesName());
        assertEquals("Library Courtyard", sapling.getTree().getLocation().getArea());
    }

    // T5: Assign volunteer
    @Test
    public void testT5_AssignVolunteer() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        Tree tree = TreeFactory.create("Banyan", "GC103", date, new Location("Campus Gate"));
        Sapling sapling = new Sapling(tree, new WateringSchedule(5, date));

        Volunteer vol = new Volunteer("V02", "Rahul Verma", "9845012345");
        sapling.assignVolunteer(vol);

        assertNotNull(sapling.getVolunteer());
        assertEquals("V02", sapling.getVolunteer().getId());
        assertEquals("Rahul Verma", sapling.getVolunteer().getName());
    }

    // T6: Add growth record
    @Test
    public void testT6_AddGrowthRecord() throws SaplingDeadException {
        LocalDate date = LocalDate.of(2026, 8, 15);
        Tree tree = TreeFactory.create("Teak", "GC104", date, new Location("Sports Ground"));
        Sapling sapling = new Sapling(tree, new WateringSchedule(4, date));

        GrowthRecord record = new GrowthRecord(24.5, "HEALTHY", "Initial measurement");
        sapling.addRecord(record);

        assertEquals(1, sapling.getRecords().size());
        assertEquals(24.5, sapling.getCurrentHeight(), 0.001);
        assertEquals("HEALTHY", sapling.getHealthStatus());
    }

    // T7: Update dead sapling throws SaplingDeadException
    @Test
    public void testT7_UpdateDeadSaplingThrowsException() throws SaplingDeadException {
        LocalDate date = LocalDate.of(2026, 8, 15);
        Tree tree = TreeFactory.create("Neem", "GC105", date, new Location("Garden"));
        Sapling sapling = new Sapling(tree, new WateringSchedule(2, date));

        // Mark as dead
        GrowthRecord deadRecord = new GrowthRecord(30.0, "DEAD", "Severe waterlogging");
        sapling.addRecord(deadRecord);
        assertFalse(sapling.getTree().isAlive());

        // Subsequent update must be blocked by SaplingDeadException
        try {
            GrowthRecord newRecord = new GrowthRecord(35.0, "HEALTHY", "Revival attempt");
            sapling.addRecord(newRecord);
            fail("Expected SaplingDeadException when updating a dead sapling");
        } catch (SaplingDeadException e) {
            assertTrue(e.getMessage().contains("already marked DEAD"));
        }
    }

    // T8: Survival report calculation
    @Test
    public void testT8_SurvivalReportCalculation() {
        SurvivalStats stats = new SurvivalStats(10, 9);
        assertEquals(10, stats.getTotalPlanted());
        assertEquals(9, stats.getSurviving());
        assertEquals(1, stats.getDead());
        assertEquals(90.0, stats.getSurvivalRate(), 0.01);

        SurvivalStats.SpeciesStat neem = new SurvivalStats.SpeciesStat("Neem", 4, 4);
        assertEquals(100.0, neem.getSurvivalRate(), 0.01);

        SurvivalStats.SpeciesStat mango = new SurvivalStats.SpeciesStat("Mango", 3, 2);
        assertEquals(66.67, mango.getSurvivalRate(), 0.01);
    }

    // T9: Date calculations
    @Test
    public void testT9_DateCalculations() {
        String base = "2026-08-15";
        String added = DatabaseHelper.addDays(base, 5);
        assertEquals("2026-08-20", added);
    }

    // T10: Watering schedule due check
    @Test
    public void testT10_WateringScheduleDue() {
        LocalDate past = LocalDate.now().minusDays(5);
        WateringSchedule schedule = new WateringSchedule(2, past);
        assertTrue(schedule.isDue());

        schedule.markWatered();
        assertFalse(schedule.isDue());
        assertEquals(LocalDate.now().plusDays(2), schedule.getNextWatering());
    }
}

