package com.mnr.greencampus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.mnr.greencampus.exception.SaplingDeadException;
import com.mnr.greencampus.model.*;
import com.mnr.greencampus.service.ReminderTask;
import com.mnr.greencampus.service.SaplingService;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {

    private DatabaseHelper db;
    private SaplingService saplingService;
    private Thread reminderThread;

    private LinearLayout contentLayout;
    private TextView statsBanner;

    private final int COLOR_PRIMARY = Color.rgb(27, 94, 32);     // #1B5E20
    private final int COLOR_ACCENT = Color.rgb(46, 125, 50);     // #2E7D32
    private final int COLOR_BG = Color.rgb(244, 248, 245);       // Soft green-grey
    private final int COLOR_CARD = Color.WHITE;
    private final int COLOR_TEXT_DARK = Color.rgb(30, 40, 32);
    private final int COLOR_TEXT_MUTED = Color.rgb(100, 115, 105);
    private final int COLOR_ALERT = Color.rgb(211, 47, 47);      // Red
    private final int COLOR_WARNING = Color.rgb(230, 81, 0);     // Orange

    private final String[] SPECIES_OPTIONS = {"Neem", "Mango", "Banyan", "Teak"};
    private final String[] HEALTH_OPTIONS = {"HEALTHY", "NEEDS_CARE", "DEAD"};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);
        saplingService = new SaplingService();
        db.populateService(saplingService);

        // Pre-seed demo data if empty so the app is immediately usable
        if (db.isEmpty()) {
            db.seedDemoData();
            db.populateService(saplingService);
        }

        createNotificationChannel();
        requestNotificationPermission();

        // Start background reminder thread (as specified in Chapter 7.3 & Chapter 8.1 of report)
        startReminderDaemon();

        // Build Java-based UI
        buildUserInterface();

        // Check watering tasks due today
        checkWateringDue(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            db.populateService(saplingService);
            updateStatsBanner();
            checkWateringDue(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reminderThread != null && reminderThread.isAlive()) {
            reminderThread.interrupt();
        }
    }

    // =========================================================================
    // BACKGROUND REMINDER THREAD (Chapter 7.3 & Chapter 8 of Report)
    // =========================================================================

    private void startReminderDaemon() {
        reminderThread = new Thread(new ReminderTask(saplingService, sapling -> {
            // Post notification if reminder triggers
            runOnUiThread(() -> {
                sendWateringNotification(1, sapling.getTree().getId() + " (" + sapling.getTree().speciesName() + ")");
            });
        }), "watering-reminder-daemon");
        reminderThread.setDaemon(true);
        reminderThread.start();
    }

    // =========================================================================
    // PURE JAVA PROGRAMMATIC UI
    // =========================================================================

    private void buildUserInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.setFillViewport(true);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(16), dp(16), dp(24));
        scrollView.addView(contentLayout);
        setContentView(scrollView);

        // 1. Header Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(roundedCard(COLOR_PRIMARY, 16));

        TextView univTag = new TextView(this);
        univTag.setText("MNR UNIVERSITY • SOET");
        univTag.setTextColor(Color.rgb(165, 214, 167));
        univTag.setTextSize(11);
        univTag.setTypeface(null, Typeface.BOLD);
        univTag.setLetterSpacing(0.08f);
        header.addView(univTag);

        TextView title = new TextView(this);
        title.setText("🌱 Green Campus Tracker");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, dp(4), 0, 0);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tree Plantation, Caretaker & Survival System");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.rgb(232, 245, 233));
        subtitle.setPadding(0, dp(2), 0, 0);
        header.addView(subtitle);

        contentLayout.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // 2. Metrics & Stats Dashboard Card
        statsBanner = new TextView(this);
        statsBanner.setTextColor(COLOR_TEXT_DARK);
        statsBanner.setTextSize(13);
        statsBanner.setLineSpacing(dp(4), 1.0f);
        statsBanner.setPadding(dp(16), dp(14), dp(16), dp(14));
        statsBanner.setBackground(roundedCard(COLOR_CARD, 12));

        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.topMargin = dp(12);
        statsParams.bottomMargin = dp(14);
        contentLayout.addView(statsBanner, statsParams);
        updateStatsBanner();

        // 3. Section Title
        TextView menuHeader = new TextView(this);
        menuHeader.setText("CAMPUS OPERATIONS (REPORT MODULES)");
        menuHeader.setTextSize(12);
        menuHeader.setTypeface(null, Typeface.BOLD);
        menuHeader.setTextColor(COLOR_TEXT_MUTED);
        menuHeader.setLetterSpacing(0.06f);
        menuHeader.setPadding(dp(4), dp(4), 0, dp(8));
        contentLayout.addView(menuHeader);

        // 4. Menu Options (Matching Chapter 9 of the Report)
        addMenuCard("1. 🌿  Register Sapling",
                "Add Neem, Mango, Banyan or Teak with location & interval (1–30 days)",
                this::registerSaplingDialog);

        addMenuCard("2. 👤  Register Volunteer",
                "Add a student or faculty caretaker to the volunteer list",
                this::registerVolunteerDialog);

        addMenuCard("3. ↗  Assign Caretaker",
                "Assign a registered volunteer to care for a campus sapling",
                this::assignVolunteerDialog);

        addMenuCard("4. 💧  Record Watering",
                "Mark sapling as watered and calculate next watering date",
                this::recordWateringDialog);

        addMenuCard("5. 📈  Add Growth / Health Record",
                "Log height (cm), health status, and reason if DEAD (blocks updates if dead)",
                this::addGrowthRecordDialog);

        addMenuCard("6. 🔎  Search Sapling & View History",
                "Search by ID to inspect metadata and complete growth history timeline",
                this::searchSaplingDialog);

        addMenuCard("7. 📋  View All Saplings",
                "Browse all campus saplings with health badges and caretaker details",
                this::showAllSaplings);

        addMenuCard("8. 📊  Survival Report",
                "Overall and species-wise survival percentage report (Chapter 9 & 10)",
                this::showSurvivalReport);

        addMenuCard("9. ⏰  Watering Due Today",
                "View saplings that need watering today with 1-tap watering action",
                this::showDueSaplings);

        // 5. Utility Actions
        LinearLayout utilRow = new LinearLayout(this);
        utilRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams utilParams = new LinearLayout.LayoutParams(-1, -2);
        utilParams.topMargin = dp(8);
        utilRow.setLayoutParams(utilParams);

        Button btnDemo = createButton("🌱 Load Demo Data", COLOR_PRIMARY);
        btnDemo.setOnClickListener(v -> {
            db.seedDemoData();
            db.populateService(saplingService);
            updateStatsBanner();
            toast("MNR campus demo data loaded.");
            checkWateringDue(false);
        });
        utilRow.addView(btnDemo, new LinearLayout.LayoutParams(0, -2, 1.0f));

        View spacer = new View(this);
        utilRow.addView(spacer, new LinearLayout.LayoutParams(dp(8), 1));

        Button btnReset = createButton("🗑 Reset Data", COLOR_ALERT);
        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reset All Data")
                    .setMessage("Clear all campus records and start fresh?")
                    .setPositiveButton("Clear", (d, w) -> {
                        db.clearAllData();
                        db.populateService(saplingService);
                        updateStatsBanner();
                        toast("Database cleared.");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        utilRow.addView(btnReset, new LinearLayout.LayoutParams(0, -2, 1.0f));
        contentLayout.addView(utilRow);

        // Footer
        TextView footer = new TextView(this);
        footer.setText("MNR University • Green Campus Tree Plantation Tracker\nJava Object Oriented Programming (SOET CSE)");
        footer.setTextColor(COLOR_TEXT_MUTED);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(20), 0, 0);
        contentLayout.addView(footer);
    }

    private void updateStatsBanner() {
        if (statsBanner == null || db == null) return;
        SurvivalStats stats = db.getSurvivalStats();
        List<Sapling> due = db.getDueSaplings();
        List<Volunteer> vols = db.getAllVolunteers();
        DecimalFormat df = new DecimalFormat("0.0");

        String dueAlert = due.isEmpty() ? "✅ All watered" : "⚠️ " + due.size() + " Due for watering!";

        statsBanner.setText(
                "📊 Campus Overview:\n" +
                "• Total Planted: " + stats.getTotalPlanted() + "  |  Surviving: " + stats.getSurviving() + "  |  Dead: " + stats.getDead() + "\n" +
                "• Survival Rate: " + df.format(stats.getSurvivalRate()) + "%\n" +
                "• Active Caretakers: " + vols.size() + "  |  Schedule: " + dueAlert
        );
    }

    private void addMenuCard(String title, String desc, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundedCard(COLOR_CARD, 12));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        card.setLayoutParams(p);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(COLOR_PRIMARY);
        card.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(12);
        d.setTextColor(COLOR_TEXT_MUTED);
        d.setPadding(0, dp(4), 0, 0);
        card.addView(d);

        card.setOnClickListener(v -> action.run());
        contentLayout.addView(card);
    }

    // =========================================================================
    // 1. REGISTER SAPLING (Chapter 8.1, Appendix A)
    // =========================================================================

    private void registerSaplingDialog() {
        LinearLayout box = createFormContainer();

        EditText inputId = createField("Sapling ID (e.g. GC101)");
        EditText inputLoc = createField("Campus Location (e.g. East Garden)");
        EditText inputInterval = createField("Watering interval in days (1–30)");
        inputInterval.setText("2");

        // Planting Date with DatePicker
        final String[] plantDate = {DatabaseHelper.today()};
        TextView dateDisplay = new TextView(this);
        dateDisplay.setText("Planting Date: " + plantDate[0] + " (Tap to change)");
        dateDisplay.setTextSize(14);
        dateDisplay.setTextColor(COLOR_PRIMARY);
        dateDisplay.setPadding(dp(8), dp(10), dp(8), dp(10));
        dateDisplay.setOnClickListener(v -> {
            LocalDate now = LocalDate.now();
            new DatePickerDialog(this, (dp, y, m, d) -> {
                plantDate[0] = LocalDate.of(y, m + 1, d).format(DATE_FMT);
                dateDisplay.setText("Planting Date: " + plantDate[0] + " (Tap to change)");
            }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth()).show();
        });

        // Species Spinner
        TextView spLabel = createLabel("Select Species:");
        Spinner spSpecies = new Spinner(this);
        spSpecies.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SPECIES_OPTIONS));

        // Caretaker Spinner
        TextView volLabel = createLabel("Assign Caretaker (Optional):");
        Spinner spVol = new Spinner(this);
        List<Volunteer> vols = db.getAllVolunteers();
        List<String> volNames = new ArrayList<>();
        volNames.add("None (Unassigned)");
        for (Volunteer v : vols) volNames.add(v.getName() + " (" + v.getId() + ")");
        spVol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, volNames));

        box.addView(inputId);
        box.addView(spLabel);
        box.addView(spSpecies);
        box.addView(inputLoc);
        box.addView(dateDisplay);
        box.addView(inputInterval);
        box.addView(volLabel);
        box.addView(spVol);

        new AlertDialog.Builder(this)
                .setTitle("Register Sapling")
                .setView(box)
                .setPositiveButton("Register", (dialog, which) -> {
                    String id = val(inputId).toUpperCase(Locale.US);
                    String species = spSpecies.getSelectedItem().toString();
                    String loc = val(inputLoc);
                    String intStr = val(inputInterval);

                    if (id.isEmpty() || loc.isEmpty() || intStr.isEmpty()) {
                        toast("Please fill all required fields.");
                        return;
                    }

                    int interval;
                    try {
                        interval = Integer.parseInt(intStr);
                        if (interval < 1 || interval > 30) {
                            toast("Watering interval must be between 1 and 30 days.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        toast("Enter a valid number for watering interval.");
                        return;
                    }

                    if (db.getSapling(id) != null) {
                        toast("Sapling ID " + id + " already exists. Duplicate ID blocked.");
                        return;
                    }

                    boolean ok = db.addSapling(id, species, loc, plantDate[0], interval);
                    if (ok) {
                        int volPos = spVol.getSelectedItemPosition();
                        if (volPos > 0) {
                            db.assignVolunteer(id, vols.get(volPos - 1).getId());
                        }
                        db.populateService(saplingService);
                        updateStatsBanner();
                        toast("Sapling " + id + " registered successfully.");
                    } else {
                        toast("Could not register sapling.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // 2. REGISTER VOLUNTEER (Chapter 8.1, Appendix A)
    // =========================================================================

    private void registerVolunteerDialog() {
        LinearLayout box = createFormContainer();
        EditText inputId = createField("Volunteer ID (e.g. V01)");
        EditText inputName = createField("Full Name");
        EditText inputPhone = createField("Phone Number (Optional)");

        box.addView(inputId);
        box.addView(inputName);
        box.addView(inputPhone);

        new AlertDialog.Builder(this)
                .setTitle("Register Volunteer Caretaker")
                .setView(box)
                .setPositiveButton("Save", (dialog, which) -> {
                    String id = val(inputId).toUpperCase(Locale.US);
                    String name = val(inputName);
                    String phone = val(inputPhone);

                    if (id.isEmpty() || name.isEmpty()) {
                        toast("Volunteer ID and Name are required.");
                        return;
                    }

                    if (db.getVolunteer(id) != null) {
                        toast("Volunteer ID " + id + " already exists.");
                        return;
                    }

                    boolean ok = db.addVolunteer(id, name, phone);
                    if (ok) {
                        updateStatsBanner();
                        toast("Volunteer " + name + " registered.");
                    } else {
                        toast("Could not register volunteer.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // 3. ASSIGN VOLUNTEER (Chapter 8.1, Appendix A)
    // =========================================================================

    private void assignVolunteerDialog() {
        List<Sapling> saplings = db.getAllSaplings();
        List<Sapling> living = new ArrayList<>();
        List<String> saplingLabels = new ArrayList<>();
        for (Sapling s : saplings) {
            if (s.getTree().isAlive()) {
                living.add(s);
                saplingLabels.add(s.getTree().getId() + " - " + s.getTree().speciesName() + " (" + s.getTree().getLocation().getArea() + ")");
            }
        }

        if (living.isEmpty()) {
            toast("No living saplings available to assign.");
            return;
        }

        List<Volunteer> vols = db.getAllVolunteers();
        List<String> volLabels = new ArrayList<>();
        volLabels.add("None (Unassigned)");
        for (Volunteer v : vols) volLabels.add(v.getName() + " (" + v.getId() + ")");

        LinearLayout box = createFormContainer();
        box.addView(createLabel("Select Sapling:"));
        Spinner spSapling = new Spinner(this);
        spSapling.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, saplingLabels));
        box.addView(spSapling);

        box.addView(createLabel("Select Caretaker:"));
        Spinner spVol = new Spinner(this);
        spVol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, volLabels));
        box.addView(spVol);

        new AlertDialog.Builder(this)
                .setTitle("Assign Volunteer Caretaker")
                .setView(box)
                .setPositiveButton("Assign", (dialog, which) -> {
                    Sapling target = living.get(spSapling.getSelectedItemPosition());
                    int volPos = spVol.getSelectedItemPosition();
                    String volId = volPos == 0 ? null : vols.get(volPos - 1).getId();

                    boolean ok = db.assignVolunteer(target.getTree().getId(), volId);
                    if (ok) {
                        db.populateService(saplingService);
                        updateStatsBanner();
                        toast("Caretaker assigned successfully.");
                    } else {
                        toast("Could not assign caretaker.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // 4. RECORD WATERING (Chapter 8.1, Appendix A)
    // =========================================================================

    private void recordWateringDialog() {
        List<Sapling> saplings = db.getAllSaplings();
        List<Sapling> living = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (Sapling s : saplings) {
            if (s.getTree().isAlive()) {
                living.add(s);
                String flag = s.getSchedule().isDue() ? " [⚠️ DUE TODAY]" : "";
                labels.add(s.getTree().getId() + " - " + s.getTree().speciesName() + flag);
            }
        }

        if (living.isEmpty()) {
            toast("No living saplings to water.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Record Watering")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    waterSapling(living.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void waterSapling(Sapling s) {
        try {
            boolean ok = db.recordWatering(s.getTree().getId());
            if (ok) {
                db.populateService(saplingService);
                updateStatsBanner();
                Sapling updated = db.getSapling(s.getTree().getId());
                String next = updated.getSchedule().getNextWatering().format(DATE_FMT);
                toast("💧 Watered " + s.getTree().getId() + "! Next due date: " + next);
            } else {
                toast("Could not record watering.");
            }
        } catch (SaplingDeadException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Operation Blocked")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    // =========================================================================
    // 5. ADD GROWTH / HEALTH RECORD (Chapter 8.1, Appendix A)
    // =========================================================================

    private void addGrowthRecordDialog() {
        List<Sapling> saplings = db.getAllSaplings();
        List<Sapling> living = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (Sapling s : saplings) {
            if (s.getTree().isAlive()) {
                living.add(s);
                labels.add(s.getTree().getId() + " (" + s.getTree().speciesName() + " - " + s.getTree().getLocation().getArea() + ")");
            }
        }

        if (living.isEmpty()) {
            toast("No living saplings available to record observations.");
            return;
        }

        LinearLayout box = createFormContainer();
        box.addView(createLabel("Select Sapling:"));
        Spinner spSapling = new Spinner(this);
        spSapling.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        box.addView(spSapling);

        EditText inputHeight = createField("Height in cm (e.g. 24.5)");
        box.addView(inputHeight);

        box.addView(createLabel("Health Condition:"));
        Spinner spHealth = new Spinner(this);
        spHealth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, HEALTH_OPTIONS));
        box.addView(spHealth);

        EditText inputNote = createField("Observation Note (e.g. Healthy leaf growth)");
        box.addView(inputNote);

        EditText inputReason = createField("Reason (Required ONLY if status is DEAD)");
        box.addView(inputReason);

        new AlertDialog.Builder(this)
                .setTitle("Add Growth / Health Record")
                .setView(box)
                .setPositiveButton("Save", (dialog, which) -> {
                    Sapling target = living.get(spSapling.getSelectedItemPosition());
                    String hStr = val(inputHeight);
                    String health = spHealth.getSelectedItem().toString();
                    String note = val(inputNote);
                    String reason = val(inputReason);

                    if (hStr.isEmpty()) {
                        toast("Height is required.");
                        return;
                    }

                    double height;
                    try {
                        height = Double.parseDouble(hStr);
                        if (height <= 0) {
                            toast("Height must be positive.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        toast("Enter a valid height number.");
                        return;
                    }

                    if ("DEAD".equalsIgnoreCase(health) && reason.isEmpty()) {
                        toast("Reason is mandatory when marking a sapling DEAD.");
                        return;
                    }

                    try {
                        String msg = db.addGrowth(target.getTree().getId(), height, health, note, reason);
                        db.populateService(saplingService);
                        updateStatsBanner();
                        toast(msg);
                    } catch (SaplingDeadException ex) {
                        toast(ex.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // 6. SEARCH SAPLING (Chapter 8.1, Appendix A)
    // =========================================================================

    private void searchSaplingDialog() {
        LinearLayout box = createFormContainer();
        EditText inputId = createField("Enter Sapling ID (e.g. GC101)");
        box.addView(inputId);

        new AlertDialog.Builder(this)
                .setTitle("Search Sapling")
                .setView(box)
                .setPositiveButton("Search", (dialog, which) -> {
                    String id = val(inputId).toUpperCase(Locale.US);
                    if (id.isEmpty()) {
                        toast("Enter an ID to search.");
                        return;
                    }

                    Sapling s = db.getSapling(id);
                    if (s == null) {
                        toast("No sapling found with ID: " + id);
                        return;
                    }

                    displaySaplingDetails(s);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void displaySaplingDetails(Sapling s) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(s.getTree().getId()).append("\n");
        sb.append("Species: ").append(s.getTree().speciesName()).append("\n");
        sb.append("Location: ").append(s.getTree().getLocation().getArea()).append("\n");
        sb.append("Planted: ").append(s.getTree().getPlantedOn().format(DATE_FMT)).append("\n");
        sb.append("Watering Interval: Every ").append(s.getSchedule().getIntervalDays()).append(" days\n");
        sb.append("Next Watering: ").append(s.getSchedule().getNextWatering().format(DATE_FMT)).append("\n");
        sb.append("Status: ").append(s.getTree().isAlive() ? s.getHealthStatus() : "DEAD").append("\n");
        sb.append("Caretaker: ").append(s.getVolunteer() != null ? s.getVolunteer().getName() : "Unassigned").append("\n");

        if (!s.getTree().isAlive() && s.getDeathReason() != null) {
            sb.append("Death Reason: ").append(s.getDeathReason()).append("\n");
        }

        sb.append("\n========== GROWTH HISTORY ==========\n");
        sb.append(db.listGrowth(s.getTree().getId()));

        showScrollDialog("Sapling: " + s.getTree().getId(), sb.toString());
    }

    // =========================================================================
    // 7. ALL SAPLINGS (Chapter 9)
    // =========================================================================

    private void showAllSaplings() {
        List<Sapling> list = db.getAllSaplings();
        if (list.isEmpty()) {
            showScrollDialog("All Saplings", "No saplings registered yet.\nUse 'Register Sapling' or 'Load Demo Data'.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Sapling s = list.get(i);
            sb.append("[").append(i + 1).append("] ID: ").append(s.getTree().getId()).append("\n");
            sb.append("• Species: ").append(s.getTree().speciesName()).append("\n");
            sb.append("• Location: ").append(s.getTree().getLocation().getArea()).append("\n");
            sb.append("• Planted: ").append(s.getTree().getPlantedOn().format(DATE_FMT)).append("\n");
            sb.append("• Status: ").append(s.getTree().isAlive() ? s.getHealthStatus() : "DEAD").append("\n");
            sb.append("• Next Watering: ").append(s.getSchedule().getNextWatering().format(DATE_FMT))
              .append(s.getSchedule().isDue() && s.getTree().isAlive() ? " (⚠️ DUE TODAY)" : "").append("\n");
            sb.append("• Caretaker: ").append(s.getVolunteer() != null ? s.getVolunteer().getName() : "Unassigned").append("\n");
            if (!s.getTree().isAlive() && s.getDeathReason() != null) {
                sb.append("• Cause of Death: ").append(s.getDeathReason()).append("\n");
            }
            sb.append("------------------------------------------\n\n");
        }

        showScrollDialog("Registered Saplings (" + list.size() + ")", sb.toString());
    }

    // =========================================================================
    // 8. SURVIVAL REPORT (Chapter 9 & 10)
    // =========================================================================

    private void showSurvivalReport() {
        String reportText = db.report();

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Campus Survival Report");

        TextView t = new TextView(this);
        t.setText(reportText);
        t.setTextSize(14);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextColor(COLOR_TEXT_DARK);
        t.setPadding(dp(18), dp(14), dp(18), dp(14));

        ScrollView s = new ScrollView(this);
        s.addView(t);
        b.setView(s);

        b.setPositiveButton("Close", null);
        b.setNeutralButton("Share Report", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Green Campus Tree Survival Report");
            intent.putExtra(Intent.EXTRA_TEXT, reportText);
            startActivity(Intent.createChooser(intent, "Share Survival Report via"));
        });
        b.show();
    }

    // =========================================================================
    // 9. WATERING DUE TODAY (Chapter 7.3, Appendix A)
    // =========================================================================

    private void showDueSaplings() {
        List<Sapling> due = db.getDueSaplings();
        if (due.isEmpty()) {
            showScrollDialog("Watering Schedule", "✅ No watering tasks are due today!\nAll saplings are up to date.");
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView header = new TextView(this);
        header.setText(due.size() + " sapling(s) require watering today:\n");
        header.setTextSize(14);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(COLOR_WARNING);
        container.addView(header);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Watering Due Today")
                .setView(new ScrollView(this) {{ addView(container); }})
                .setPositiveButton("Close", null)
                .create();

        for (Sapling s : due) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(10), dp(10), dp(10), dp(10));
            item.setBackground(roundedCard(Color.rgb(255, 243, 224), 8));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.bottomMargin = dp(8);
            item.setLayoutParams(p);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView line1 = new TextView(this);
            line1.setText("💧 " + s.getTree().getId() + " — " + s.getTree().speciesName());
            line1.setTypeface(null, Typeface.BOLD);
            line1.setTextColor(COLOR_TEXT_DARK);
            textCol.addView(line1);

            TextView line2 = new TextView(this);
            line2.setText(s.getTree().getLocation().getArea() + " • Caretaker: " +
                    (s.getVolunteer() != null ? s.getVolunteer().getName() : "Unassigned"));
            line2.setTextSize(12);
            line2.setTextColor(COLOR_TEXT_MUTED);
            textCol.addView(line2);

            item.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

            Button btnWater = createButton("Water", COLOR_PRIMARY);
            btnWater.setOnClickListener(v -> {
                waterSapling(s);
                dialog.dismiss();
            });
            item.addView(btnWater, new LinearLayout.LayoutParams(-2, dp(38)));

            container.addView(item);
        }

        dialog.show();
    }

    // =========================================================================
    // NOTIFICATIONS & REMINDERS
    // =========================================================================

    private void checkWateringDue(boolean userInitiated) {
        if (db == null) return;
        List<Sapling> due = db.getDueSaplings();
        if (!due.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Sapling s : due) {
                sb.append(s.getTree().getId()).append(" (").append(s.getTree().speciesName()).append("), ");
            }
            sendWateringNotification(due.size(), sb.toString());
            if (userInitiated) {
                toast(due.size() + " saplings currently need watering.");
            }
        } else if (userInitiated) {
            toast("All saplings are watered and up to date!");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    "watering_due",
                    "Watering Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminders for saplings requiring scheduled watering");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
        }
    }

    private void sendWateringNotification(int count, String details) {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ?
                new Notification.Builder(this, "watering_due") : new Notification.Builder(this);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(count + " Sapling(s) Need Watering Today")
                .setContentText(details.length() > 65 ? details.substring(0, 62) + "..." : details)
                .setAutoCancel(true);

        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mgr != null) {
            mgr.notify(101, builder.build());
        }
    }

    // =========================================================================
    // UI HELPER METHODS
    // =========================================================================

    private void showScrollDialog(String title, String message) {
        TextView t = new TextView(this);
        t.setText(message);
        t.setTextSize(14);
        t.setTextColor(COLOR_TEXT_DARK);
        t.setLineSpacing(dp(3), 1.0f);
        t.setPadding(dp(20), dp(14), dp(20), dp(14));

        ScrollView s = new ScrollView(this);
        s.addView(t);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(s)
                .setPositiveButton("Close", null)
                .show();
    }

    private LinearLayout createFormContainer() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(10));
        return box;
    }

    private EditText createField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.bottomMargin = dp(8);
        e.setLayoutParams(p);
        return e;
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(COLOR_PRIMARY);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(6), 0, dp(2));
        return tv;
    }

    private Button createButton(String text, int bgColor) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(roundedCard(bgColor, 8));
        return b;
    }

    private GradientDrawable roundedCard(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private String val(EditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
