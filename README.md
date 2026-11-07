# 🌱 Green Campus: Tree Plantation and Survival Tracker

**MNR UNIVERSITY**  
School of Engineering & Technology (SOET)  
Department of Computer Science and Engineering  
B.Tech — Artificial Intelligence and Machine Learning (Academic Year 2026–2027)  
Course: Object Oriented Programming (Java)

---

## 📱 Project Description

This project is a native Android application built **exclusively using pure Java** and native Android SDK APIs, strictly implementing the architecture, data models, exception handling, and operational workflows specified in the **MNR University Mini Project Report** (`Green_Campus_Tree_Survival_Tracker_Report.pdf`).

All user interfaces, dialogs, validation rules, notifications, and background threads are built in Java code with zero external third-party UI dependencies.

---

## 🏛️ Project Architecture & OOP Principles

Directly matching **Chapter 5, Chapter 8, and Appendix A** of the report:

1. **`model/Tree.java` (Abstract Class)**:
   - Encapsulates common sapling properties (`id`, `plantedOn`, `location`, `alive`).
   - Abstract method `speciesName()` for polymorphism.
   - `markDead()` method.
2. **`model/Neem.java`, `Mango.java`, `Banyan.java`, `Teak.java` (Inheritance & Polymorphism)**:
   - Subtypes extending `Tree` and overriding `speciesName()`.
3. **`model/Location.java`**: Campus area value object with validation.
4. **`model/Volunteer.java`**: Caretaker details (`id`, `name`, `phone`).
5. **`model/WateringSchedule.java`**: Calculates 1–30 day watering intervals, due checks (`isDue()`), and watering updates (`markWatered()`).
6. **`model/GrowthRecord.java`**: Height measurement, observation date, and health status (`HEALTHY`, `NEEDS_CARE`, `DEAD`).
7. **`model/Sapling.java`**: Aggregates `Tree`, `Location`, `WateringSchedule`, `Volunteer`, and multiple `GrowthRecord`s.
8. **`exception/SaplingDeadException.java`**: Custom checked exception thrown when updates or watering are attempted on a dead tree.
9. **`service/SaplingService.java`**: `HashMap`-based in-memory service for registration, search, and survival calculation.
10. **`service/ReminderTask.java`**: Daemon `Runnable` thread checking watering due dates in the background.
11. **`DatabaseHelper.java`**: SQLite persistence storing all saplings, volunteers, and growth records across app restarts.
12. **`MainActivity.java`**: Native Java UI implementing all menu operations from Chapter 9.

---

## 📋 Features Implemented (Chapter 9 Menu Operations)

1. **🌿 Register Sapling**:
   - Register Neem, Mango, Banyan, or Teak with unique ID, campus location, planting date (with calendar picker), and 1–30 day watering interval.
2. **👤 Register Volunteer**:
   - Add campus volunteers with unique ID, name, and contact details.
3. **↗ Assign Caretaker**:
   - Assign registered volunteers to saplings using living-tree and volunteer dropdowns.
4. **💧 Record Watering**:
   - Mark a sapling as watered; recalculates the next watering date. Throws `SaplingDeadException` if the tree is dead.
5. **📈 Add Growth / Health Record**:
   - Log height in cm, condition (`HEALTHY`, `NEEDS_CARE`, `DEAD`), and observation notes.
   - Requires a mandatory reason if marked `DEAD`, blocking any future growth or watering updates.
6. **🔎 Search Sapling & View History**:
   - Look up saplings by ID to inspect metadata and view the chronological growth timeline.
7. **📋 View All Saplings**:
   - Browse all registered saplings with health statuses and caretaker assignments.
8. **📊 Survival Report**:
   - Displays the formatted overall and species-wise survival report directly matching Chapter 9 & 10.
   - Includes a **Share Report** action.
9. **⏰ Watering Due Today**:
   - Filter and display saplings requiring watering today with quick 1-tap "Water" buttons.
10. **🌱 Load Campus Demo Data / Reset**:
    - Instant demo dataset for quick faculty evaluation and grading.

---

## 🧪 Unit Testing (Test Cases T1 – T10)

Unit tests in `app/src/test/java/com/mnr/greencampus/GreenCampusTrackerTest.java` test all 10 specifications from Chapter 10 of the report:

| Test ID | Test Description | Result |
|---|---|---|
| **T1** | Register valid sapling (`GC101`, `Neem`) | **Pass** |
| **T2** | Validate empty ID and location | **Pass** |
| **T3** | Validate interval (1–30 days) | **Pass** |
| **T4** | Search & retrieve sapling details | **Pass** |
| **T5** | Assign volunteer caretaker | **Pass** |
| **T6** | Add growth record (height, health) | **Pass** |
| **T7** | Update dead sapling throws `SaplingDeadException` | **Pass** |
| **T8** | Overall & species-wise survival calculation | **Pass** |
| **T9** | Date arithmetic for watering cycle | **Pass** |
| **T10** | Watering schedule due checking (`isDue()`) | **Pass** |

Run tests anytime with:
```bash
./gradlew test
```

---

## 🚀 Running in Android Studio

1. Open Android Studio and choose **Open** → select `GreenCampusTrackerAndroid`.
2. Allow Gradle sync to complete (configured to use JDK 17).
3. Select an emulator or connected device and click **Run 'app'** (`Shift + F10`).
