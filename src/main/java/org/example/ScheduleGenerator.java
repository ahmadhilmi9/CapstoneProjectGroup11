package org.example;

import java.util.*;

/**
 * ULTRA OPTIMIZED Schedule Generator v2.0
 * Menggunakan Hybrid Metaheuristic Algorithm dengan PJOK Priority:
 * 1. PJOK-First Constraint Satisfaction - PJOK WAJIB 2-1 pattern dengan 2 jam max di jam 5
 * 2. Simulated Annealing - untuk escape local optima
 * 3. Tabu Search - mencegah cycling
 * 4. Constraint-Based Constructive Heuristic - memastikan feasibility
 * 5. Adaptive Repair Mechanism - perbaikan constraint violations
 *
 * Target: 100% Completion + 0 Violations + PJOK Pattern 100% Valid
 */
public class ScheduleGenerator {
    private final List<Assignment> assignments;
    private Random random;
    private final Set<String> mgmpTeachers;
    private TabuList tabuList;

    // Constraint constants
    private static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
    private static final int[] PERIODS_PER_DAY = {10, 10, 10, 9, 8};
    private static final int MAX_HOURS_PER_SUBJECT_PER_DAY = 3;
    private static final int MGMP_MAX_PERIOD_RABU = 4;

    // PJOK CONSTRAINTS - SANGAT KETAT
    // 2 jam berurutan HARUS dimulai maksimal jam ke-4 (berakhir jam ke-5)
    private static final int PJOK_DOUBLE_MAX_START = 4;  // Jam mulai maksimal untuk 2 jam berurutan
    private static final int PJOK_DOUBLE_MAX_END = 5;    // Jam berakhir maksimal untuk 2 jam berurutan
    private static final int PJOK_SINGLE_MAX = 10;       // 1 jam bebas boleh sampai jam 10

    // Simulated Annealing parameters - HIGHLY OPTIMIZED
    private static final double INITIAL_TEMPERATURE = 15000.0;
    private static final double COOLING_RATE = 0.9997;
    private static final double MIN_TEMPERATURE = 0.00001;

    // Tabu Search parameters
    private static final int TABU_TENURE = 50;
    private static final int MAX_ITERATIONS = 12000;
    private static final long MAX_TIME_MS = 600000; // 10 minutes per run

    // MULTI-START parameters
    private static final int NUM_RUNS = 7; // Increased from 5 to 7 for better exploration
    private static final long[] SEEDS = {42L, 123456L, 789012L, 345678L, 901234L, 111111L, 999999L};

    private static final Set<String> MGMP_SUBJECTS = new HashSet<>(Arrays.asList(
            "SKI", "B.ARAB", "AQIDAH AKHLAK", "QURDITS", "FIQIH", "AQIDAH A.",
            "B. ARAB", "AL-QUR'AN HADITS", "AL QUR'AN HADITS", "BAHASA ARAB", "FIKIH"
    ));

    // Pola distribusi mata pelajaran
    private static final Map<String, int[]> SUBJECT_DISTRIBUTION_PATTERNS = new HashMap<>();
    static {
        SUBJECT_DISTRIBUTION_PATTERNS.put("MATEMATIKA_5", new int[]{3, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("IPA_5", new int[]{3, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("BAHASA INDONESIA_6", new int[]{2, 2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("B. INDONESIA_6", new int[]{2, 2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("BAHASA INGGRIS_4", new int[]{2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("B. INGGRIS_4", new int[]{2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("IPS_4", new int[]{2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("PJOK_3", new int[]{2, 1}); // WAJIB 2-1
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_3_SPLIT", new int[]{2, 1});
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_3_SINGLE", new int[]{3});
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_2", new int[]{2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_1", new int[]{1});
    }

    public ScheduleGenerator(List<Assignment> assignments) {
        this.assignments = new ArrayList<>(assignments);
        this.random = new Random();
        this.mgmpTeachers = identifyMGMPTeachers();
        this.tabuList = new TabuList(TABU_TENURE);
    }

    private Set<String> identifyMGMPTeachers() {
        Set<String> teachers = new HashSet<>();
        // MGMP ditentukan berdasarkan GURU yang mengajar mapel MGMP (berdasarkan ID guru)
        for (Assignment assignment : assignments) {
            if (isMGMPSubject(assignment.getSubject())) {
                teachers.add(assignment.getTeacher());
            }
        }
        return teachers;
    }

    private boolean isMGMPSubject(String subject) {
        String upperSubject = subject.toUpperCase().trim();
        for (String mgmpSubject : MGMP_SUBJECTS) {
            if (upperSubject.contains(mgmpSubject.toUpperCase())) return true;
        }
        return false;
    }

    private boolean isPJOKSubject(String subject) {
        return subject.toUpperCase().trim().contains("PJOK");
    }

    public Schedule generate() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║    ULTRA OPTIMIZED SCHEDULER v2.0 - PJOK PRIORITY             ║");
        System.out.println("║    PJOK: 2 jam berurutan (max jam 5) + 1 jam bebas            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        long totalStartTime = System.currentTimeMillis();

        Schedule bestOverallSchedule = null;
        double bestOverallScore = Double.NEGATIVE_INFINITY;
        double bestOverallCompletion = 0;
        int bestOverallViolations = Integer.MAX_VALUE;
        int bestPJOKPatternViolations = Integer.MAX_VALUE;

        for (int run = 0; run < NUM_RUNS; run++) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.printf("║  RUN #%d/%d (Seed: %d)                                       ║%n", run + 1, NUM_RUNS, SEEDS[run]);
            System.out.println("╚══════════════════════════════════════════════════════════════╝");

            this.random = new Random(SEEDS[run]);
            this.tabuList = new TabuList(TABU_TENURE);

            Schedule currentRunSchedule = generateSingleRun();

            double currentScore = evaluateFitness(currentRunSchedule);
            double currentCompletion = getCompletionPercentage(currentRunSchedule);
            int currentViolations = countAllViolations(currentRunSchedule);
            int currentPJOKPatternViol = countPJOKPatternViolations(currentRunSchedule);

            System.out.printf("\n✓ Run #%d: %.1f%% complete, %d violations, PJOK pattern violations: %d%n",
                (run + 1), currentCompletion, currentViolations, currentPJOKPatternViol);

            // Prioritas: PJOK pattern > completion > violations > score
            boolean isBetter = false;
            if (bestOverallSchedule == null) {
                isBetter = true;
            } else if (currentPJOKPatternViol < bestPJOKPatternViolations) {
                isBetter = true;
            } else if (currentPJOKPatternViol == bestPJOKPatternViolations) {
                if (currentCompletion > bestOverallCompletion + 0.1) {
                    isBetter = true;
                } else if (Math.abs(currentCompletion - bestOverallCompletion) <= 0.1) {
                    if (currentViolations < bestOverallViolations) {
                        isBetter = true;
                    } else if (currentViolations == bestOverallViolations && currentScore > bestOverallScore) {
                        isBetter = true;
                    }
                }
            }

            if (isBetter) {
                bestOverallSchedule = currentRunSchedule;
                bestOverallScore = currentScore;
                bestOverallCompletion = currentCompletion;
                bestOverallViolations = currentViolations;
                bestPJOKPatternViolations = currentPJOKPatternViol;
                System.out.println("   ⭐ NEW BEST SOLUTION!");
            }

            // Perfect solution found
            if (currentCompletion >= 99.9 && currentViolations == 0 && currentPJOKPatternViol == 0) {
                System.out.println("   🎯 PERFECT SOLUTION WITH VALID PJOK PATTERN!");
                break;
            }
        }

        long totalEndTime = System.currentTimeMillis();
        double totalSeconds = (totalEndTime - totalStartTime) / 1000.0;

        printDetailedReport(bestOverallSchedule, totalSeconds);
        printPJOKPatternReport(bestOverallSchedule);

        return bestOverallSchedule;
    }

    private Schedule generateSingleRun() {
        long startTime = System.currentTimeMillis();

        // Phase 1: Construct with PJOK-First Strategy
        System.out.println("\n[PHASE 1] PJOK-First Construction...");
        Schedule currentSchedule = constructWithPJOKFirst();
        System.out.printf("   Initial: %.1f%% complete, %d violations, PJOK pattern: %d%n",
            getCompletionPercentage(currentSchedule), countAllViolations(currentSchedule),
            countPJOKPatternViolations(currentSchedule));

        Schedule bestSchedule = currentSchedule.clone();
        double bestScore = evaluateFitness(bestSchedule);

        // Phase 2: Hybrid SA + Tabu
        System.out.println("\n[PHASE 2] Hybrid SA + Tabu Search...");
        double temperature = INITIAL_TEMPERATURE;
        int iteration = 0;
        int noImprovementCount = 0;
        int maxNoImprovement = 300;

        while (temperature > MIN_TEMPERATURE && iteration < MAX_ITERATIONS && noImprovementCount < maxNoImprovement) {
            if (System.currentTimeMillis() - startTime > MAX_TIME_MS) break;

            Schedule neighbor = generateSmartNeighbor(currentSchedule);
            double neighborScore = evaluateFitness(neighbor);
            double currentScore = evaluateFitness(currentSchedule);

            double delta = neighborScore - currentScore;
            boolean accept = delta > 0 || (!tabuList.isTabu(neighbor) && random.nextDouble() < Math.exp(delta / temperature));

            if (accept) {
                currentSchedule = neighbor;
                tabuList.add(neighbor);

                if (neighborScore > bestScore) {
                    bestSchedule = neighbor.clone();
                    bestScore = neighborScore;
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
            }

            temperature *= COOLING_RATE;
            iteration++;

            if (getCompletionPercentage(bestSchedule) >= 99.9 && countAllViolations(bestSchedule) == 0
                && countPJOKPatternViolations(bestSchedule) == 0) {
                break;
            }
        }

        System.out.printf("   After SA: %.1f%% complete, iterations: %d%n",
            getCompletionPercentage(bestSchedule), iteration);

        // Phase 3: Intensive PJOK Repair
        System.out.println("\n[PHASE 3] Intensive PJOK Pattern Repair...");
        repairAllPJOKPatterns(bestSchedule);

        // Phase 4: Complete remaining assignments
        System.out.println("\n[PHASE 4] Complete Remaining Assignments...");
        completeRemainingAssignments(bestSchedule);

        // Phase 5: Final constraint repair
        System.out.println("\n[PHASE 5] Final Constraint Repair...");
        finalConstraintRepair(bestSchedule);

        return bestSchedule;
    }

    /**
     * PJOK-First Construction: Tempatkan PJOK terlebih dahulu dengan constraint ketat
     */
    private Schedule constructWithPJOKFirst() {
        Set<String> classes = new HashSet<>();
        for (Assignment a : assignments) {
            classes.add(a.getClassName());
        }
        Schedule schedule = new Schedule(classes);

        // Separate PJOK and non-PJOK assignments
        List<Assignment> pjokAssignments = new ArrayList<>();
        List<Assignment> otherAssignments = new ArrayList<>();

        for (Assignment a : assignments) {
            if (isPJOKSubject(a.getSubject())) {
                pjokAssignments.add(a);
            } else {
                otherAssignments.add(a);
            }
        }

        // Sort PJOK by class name for consistent ordering
        pjokAssignments.sort(Comparator.comparing(Assignment::getClassName));

        System.out.println("   → Placing " + pjokAssignments.size() + " PJOK assignments first...");

        // Place ALL PJOK first with strict 2-1 pattern
        for (Assignment pjok : pjokAssignments) {
            boolean placed = placePJOKStrict21Pattern(schedule, pjok);
            if (!placed) {
                System.out.printf("      ⚠ PJOK [%s] initial placement failed, will retry%n", pjok.getClassName());
            }
        }

        // Sort other assignments by priority
        otherAssignments.sort((a, b) -> {
            boolean aMGMP = mgmpTeachers.contains(a.getTeacher());
            boolean bMGMP = mgmpTeachers.contains(b.getTeacher());
            if (aMGMP && !bMGMP) return -1;
            if (!aMGMP && bMGMP) return 1;
            return Integer.compare(b.getTotalHours(), a.getTotalHours());
        });

        System.out.println("   → Placing " + otherAssignments.size() + " other assignments...");

        // Place other assignments
        for (Assignment assignment : otherAssignments) {
            placeAssignmentWithPattern(schedule, assignment);
        }

        // Aggressive completion
        for (int round = 0; round < 500; round++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) break;

            boolean progress = false;
            for (Assignment a : incomplete) {
                if (isPJOKSubject(a.getSubject())) continue; // Skip PJOK here
                if (placeOneHourSmart(schedule, a)) progress = true;
            }
            if (!progress) break;
        }

        return schedule;
    }

    /**
     * STRICT PJOK 2-1 Pattern Placement
     * 2 jam berurutan: HARUS dimulai jam 1-4 (berakhir max jam 5)
     * 1 jam: bebas di mana saja
     */
    private boolean placePJOKStrict21Pattern(Schedule schedule, Assignment pjok) {
        String className = pjok.getClassName();
        String teacher = pjok.getTeacher();
        int totalHours = pjok.getTotalHours();

        if (totalHours != 3) {
            // Jika bukan 3 jam, place normally
            return placeAssignmentWithPattern(schedule, pjok);
        }

        System.out.printf("      Placing PJOK [%s] with STRICT 2-1 pattern%n", className);

        // STEP 1: Place 2 jam berurutan (HARUS mulai jam 1-4, berakhir max jam 5)
        boolean doublePlaced = false;
        String doubleDay = null;
        int doubleEndPeriod = -1;

        // Try each day in order: Senin, Selasa, Kamis, Jumat (skip Rabu for variety)
        String[] preferredDays = {"Senin", "Selasa", "Kamis", "Jumat", "Rabu"};

        for (String day : preferredDays) {
            if (doublePlaced) break;

            int maxPeriods = getPeriodsForDay(day);
            // 2 jam berurutan harus mulai maksimal jam ke-4 (berakhir jam ke-5)
            int maxStartPeriod = Math.min(PJOK_DOUBLE_MAX_START, maxPeriods - 1);

            for (int startPeriod = 1; startPeriod <= maxStartPeriod; startPeriod++) {
                int endPeriod = startPeriod + 1;

                // Pastikan berakhir maksimal jam ke-5
                if (endPeriod > PJOK_DOUBLE_MAX_END) continue;

                TimeSlot slot1 = schedule.getSlot(day, startPeriod, className);
                TimeSlot slot2 = schedule.getSlot(day, endPeriod, className);

                if (slot1 != null && slot2 != null && slot1.isEmpty() && slot2.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, startPeriod, className) &&
                    schedule.isTeacherAvailable(teacher, day, endPeriod, className)) {

                    slot1.assign(pjok, 1);
                    slot2.assign(pjok, 2);
                    doublePlaced = true;
                    doubleDay = day;
                    doubleEndPeriod = endPeriod;
                    System.out.printf("         ✓ 2 jam berurutan: %s jam %d-%d%n", day, startPeriod, endPeriod);
                    break;
                }
            }
        }

        if (!doublePlaced) {
            System.out.println("         → Trying with slot displacement for 2 consecutive hours...");
            // Fallback: Coba pindahkan assignment lain untuk buat tempat
            for (String day : preferredDays) {
                if (doublePlaced) break;

                int maxPeriods = getPeriodsForDay(day);
                int maxStartPeriod = Math.min(PJOK_DOUBLE_MAX_START, maxPeriods - 1);

                for (int startPeriod = 1; startPeriod <= maxStartPeriod; startPeriod++) {
                    int endPeriod = startPeriod + 1;
                    if (endPeriod > PJOK_DOUBLE_MAX_END) continue;

                    TimeSlot slot1 = schedule.getSlot(day, startPeriod, className);
                    TimeSlot slot2 = schedule.getSlot(day, endPeriod, className);

                    if (slot1 == null || slot2 == null) continue;

                    // Check if we can make space by relocating
                    Assignment occupant1 = slot1.isEmpty() ? null : slot1.getAssignment();
                    Assignment occupant2 = slot2.isEmpty() ? null : slot2.getAssignment();

                    // Skip if PJOK is already there
                    if ((occupant1 != null && isPJOKSubject(occupant1.getSubject())) ||
                        (occupant2 != null && isPJOKSubject(occupant2.getSubject()))) {
                        continue;
                    }

                    // Check teacher availability
                    if (!schedule.isTeacherAvailable(teacher, day, startPeriod, className) ||
                        !schedule.isTeacherAvailable(teacher, day, endPeriod, className)) {
                        continue;
                    }

                    // Try to relocate occupants
                    boolean canPlace = true;
                    List<Assignment> toRelocate = new ArrayList<>();

                    if (occupant1 != null) {
                        slot1.clear();
                        toRelocate.add(occupant1);
                    }
                    if (occupant2 != null) {
                        slot2.clear();
                        toRelocate.add(occupant2);
                    }

                    // Try to relocate all displaced assignments
                    for (Assignment displaced : toRelocate) {
                        if (!placeOneHourAnywhere(schedule, displaced)) {
                            canPlace = false;
                            break;
                        }
                    }

                    if (canPlace) {
                        slot1.assign(pjok, 1);
                        slot2.assign(pjok, 2);
                        doublePlaced = true;
                        doubleDay = day;
                        doubleEndPeriod = endPeriod;
                        System.out.printf("         ✓ 2 jam berurutan (displaced): %s jam %d-%d%n", day, startPeriod, endPeriod);
                        break;
                    } else {
                        // Restore if failed
                        for (Assignment displaced : toRelocate) {
                            placeOneHourAnywhere(schedule, displaced);
                        }
                    }
                }
            }
        }

        if (!doublePlaced) {
            System.out.println("         ❌ Gagal menempatkan 2 jam berurutan!");
            return false;
        }

        // STEP 2: Place 1 jam di hari BERBEDA
        boolean singlePlaced = false;

        // Preferensi hari berbeda dari hari double
        for (String day : DAYS) {
            if (singlePlaced) break;
            if (day.equals(doubleDay)) continue; // HARUS hari berbeda

            int maxPeriods = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriods; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);

                if (slot != null && slot.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className)) {

                    slot.assign(pjok, 3);
                    singlePlaced = true;
                    System.out.printf("         ✓ 1 jam bebas: %s jam %d%n", day, period);
                    break;
                }
            }
        }

        if (!singlePlaced) {
            System.out.println("         → Trying 1 hour placement on same day (non-consecutive)...");
            // Fallback: boleh di hari yang sama tapi tidak berurutan dengan yang 2 jam
            for (String day : DAYS) {
                if (singlePlaced) break;
                int maxPeriods = getPeriodsForDay(day);

                for (int period = 1; period <= maxPeriods; period++) {
                    TimeSlot slot = schedule.getSlot(day, period, className);

                    if (slot != null && slot.isEmpty() &&
                        schedule.isTeacherAvailable(teacher, day, period, className)) {

                        // Cek tidak berurutan dengan 2 jam yang sudah ada
                        boolean adjacent = false;
                        if (day.equals(doubleDay)) {
                            // Cek apakah adjacent dengan double hours
                            if (period == doubleEndPeriod + 1 || period == doubleEndPeriod - 2) {
                                adjacent = true;
                            }
                        }

                        if (!adjacent) {
                            slot.assign(pjok, 3);
                            singlePlaced = true;
                            System.out.printf("         ✓ 1 jam (same day, non-consecutive): %s jam %d%n", day, period);
                            break;
                        }
                    }
                }
            }
        }

        if (!singlePlaced) {
            System.out.println("         → Forcing 1 hour placement by displacing...");
            // Super Fallback: Displace existing assignment untuk buat tempat
            for (String day : DAYS) {
                if (singlePlaced) break;
                if (day.equals(doubleDay)) continue;

                int maxPeriods = getPeriodsForDay(day);

                for (int period = 1; period <= maxPeriods; period++) {
                    if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot == null) continue;

                    Assignment occupant = slot.isEmpty() ? null : slot.getAssignment();
                    if (occupant != null && isPJOKSubject(occupant.getSubject())) continue;

                    if (occupant != null) {
                        slot.clear();
                        if (!placeOneHourAnywhere(schedule, occupant)) {
                            slot.assign(occupant, 1); // restore
                            continue;
                        }
                    }

                    slot.assign(pjok, 3);
                    singlePlaced = true;
                    System.out.printf("         ✓ 1 jam (forced): %s jam %d%n", day, period);
                    break;
                }
            }
        }

        boolean success = doublePlaced && singlePlaced;
        if (success) {
            System.out.printf("      ✅ PJOK [%s] completed: 2+1 = 3 jam%n", className);
        } else {
            System.out.printf("      ⚠ PJOK [%s] incomplete (double=%s, single=%s)%n",
                className, doublePlaced, singlePlaced);
        }

        return success;
    }

    private int getPeriodsForDay(String day) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) return PERIODS_PER_DAY[i];
        }
        return 10;
    }

    /**
     * Count PJOK pattern violations
     * Violation if: tidak ada 2 jam berurutan ATAU 2 jam berurutan berakhir setelah jam 5
     */
    private int countPJOKPatternViolations(Schedule schedule) {
        int violations = 0;

        for (String className : schedule.getAllClasses()) {
            Map<String, List<TimeSlot>> pjokByTeacher = new HashMap<>();

            for (String day : DAYS) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())) {
                        String teacher = slot.getAssignment().getTeacher();
                        pjokByTeacher.computeIfAbsent(teacher, k -> new ArrayList<>()).add(slot);
                    }
                }
            }

            for (List<TimeSlot> slots : pjokByTeacher.values()) {
                if (slots.isEmpty()) continue;

                // Find if there's a valid consecutive pair (2 jam berurutan ending at max period 5)
                boolean hasValidPair = false;

                for (int i = 0; i < slots.size(); i++) {
                    for (int j = i + 1; j < slots.size(); j++) {
                        TimeSlot s1 = slots.get(i);
                        TimeSlot s2 = slots.get(j);

                        if (s1.getDay().equals(s2.getDay()) &&
                            Math.abs(s1.getPeriod() - s2.getPeriod()) == 1) {

                            int endP = Math.max(s1.getPeriod(), s2.getPeriod());
                            if (endP <= PJOK_DOUBLE_MAX_END) {
                                hasValidPair = true;
                                break;
                            }
                        }
                    }
                    if (hasValidPair) break;
                }

                if (!hasValidPair) {
                    violations++;
                }
            }
        }

        return violations;
    }

    /**
     * Repair all PJOK patterns to ensure 2-1 with correct timing
     */
    private void repairAllPJOKPatterns(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            Map<String, List<TimeSlot>> pjokByTeacher = new HashMap<>();

            for (String day : DAYS) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())) {
                        String teacher = slot.getAssignment().getTeacher();
                        pjokByTeacher.computeIfAbsent(teacher, k -> new ArrayList<>()).add(slot);
                    }
                }
            }

            for (Map.Entry<String, List<TimeSlot>> entry : pjokByTeacher.entrySet()) {
                List<TimeSlot> slots = entry.getValue();

                if (slots.size() != 3) continue;

                // Check if pattern is valid
                boolean hasValidPair = false;
                for (int i = 0; i < slots.size() && !hasValidPair; i++) {
                    for (int j = i + 1; j < slots.size(); j++) {
                        TimeSlot s1 = slots.get(i);
                        TimeSlot s2 = slots.get(j);

                        if (s1.getDay().equals(s2.getDay()) &&
                            Math.abs(s1.getPeriod() - s2.getPeriod()) == 1 &&
                            Math.max(s1.getPeriod(), s2.getPeriod()) <= PJOK_DOUBLE_MAX_END) {
                            hasValidPair = true;
                            break;
                        }
                    }
                }

                if (!hasValidPair) {
                    // Clear and re-place
                    Assignment pjok = slots.get(0).getAssignment();
                    System.out.printf("   Repairing PJOK [%s]...%n", className);

                    for (TimeSlot slot : slots) {
                        slot.clear();
                    }

                    placePJOKStrict21Pattern(schedule, pjok);
                }
            }
        }
    }

    private boolean placeAssignmentWithPattern(Schedule schedule, Assignment assignment) {
        int[] pattern = getDistributionPattern(assignment);
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isMGMP = mgmpTeachers.contains(teacher);

        int totalPlaced = 0;
        for (int blockSize : pattern) {
            boolean placed = false;

            for (String day : DAYS) {
                if (placed) break;

                int maxPeriod = getPeriodsForDay(day);
                if (isMGMP && day.equals("Rabu")) {
                    maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
                }

                int existingHours = countSubjectHoursOnDay(schedule, assignment, day);
                if (existingHours > 0) continue;

                for (int startPeriod = 1; startPeriod <= maxPeriod - blockSize + 1; startPeriod++) {
                    boolean canPlace = true;

                    for (int offset = 0; offset < blockSize; offset++) {
                        int period = startPeriod + offset;
                        TimeSlot slot = schedule.getSlot(day, period, className);

                        if (slot == null || !slot.isEmpty() ||
                            !schedule.isTeacherAvailable(teacher, day, period, className)) {
                            canPlace = false;
                            break;
                        }
                    }

                    if (canPlace) {
                        for (int offset = 0; offset < blockSize; offset++) {
                            int period = startPeriod + offset;
                            TimeSlot slot = schedule.getSlot(day, period, className);
                            slot.assign(assignment, totalPlaced + offset + 1);
                        }
                        totalPlaced += blockSize;
                        placed = true;
                        break;
                    }
                }
            }

            if (!placed) {
                // Fallback: place one by one
                for (int i = 0; i < blockSize; i++) {
                    placeOneHourSmart(schedule, assignment);
                }
            }
        }

        return true;
    }

    private boolean placeOneHourSmart(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isMGMP = mgmpTeachers.contains(teacher);

        List<int[]> options = new ArrayList<>(); // [dayIdx, period, score]

        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];

            if (isMGMP && day.equals("Rabu")) {
                maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
            }

            int currentHours = countSubjectHoursOnDay(schedule, assignment, day);
            if (currentHours >= MAX_HOURS_PER_SUBJECT_PER_DAY) continue;

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);

                if (slot != null && slot.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className)) {

                    int score = 1000;
                    score += (11 - period) * 10; // Prefer earlier periods
                    if (currentHours == 0) score += 50; // Prefer distribution

                    // Bonus for consecutive with same subject
                    if (period > 1) {
                        TimeSlot prev = schedule.getSlot(day, period - 1, className);
                        if (prev != null && !prev.isEmpty() &&
                            prev.getAssignment().getSubject().equals(assignment.getSubject())) {
                            score += 100;
                        }
                    }

                    options.add(new int[]{dayIdx, period, score});
                }
            }
        }

        if (!options.isEmpty()) {
            options.sort((a, b) -> Integer.compare(b[2], a[2]));
            int[] best = options.get(0);
            String day = DAYS[best[0]];
            int period = best[1];

            TimeSlot slot = schedule.getSlot(day, period, className);
            if (slot != null) {
                slot.assign(assignment, 1);
                return true;
            }
        }

        return false;
    }

    private void completeRemainingAssignments(Schedule schedule) {
        System.out.println("   → Phase 4.1: Smart placement...");
        for (int round = 0; round < 2000; round++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) {
                System.out.println("   ✓ All assignments complete!");
                return;
            }

            boolean progress = false;

            incomplete.sort((a, b) -> {
                int aRemaining = a.getTotalHours() - schedule.getScheduledHours(a);
                int bRemaining = b.getTotalHours() - schedule.getScheduledHours(b);
                return Integer.compare(bRemaining, aRemaining);
            });

            for (Assignment a : incomplete) {
                if (isPJOKSubject(a.getSubject())) {
                    int scheduled = schedule.getScheduledHours(a);
                    if (scheduled < 3) {
                        clearAssignmentSlots(schedule, a);
                        if (placePJOKStrict21Pattern(schedule, a)) {
                            progress = true;
                        }
                    }
                } else {
                    if (placeOneHourSmart(schedule, a)) {
                        progress = true;
                    } else if (placeOneHourAnywhere(schedule, a)) {
                        progress = true;
                    }
                }
            }

            if (!progress) break;
        }

        // Phase 4.2: Aggressive swap-based completion
        System.out.println("   → Phase 4.2: Swap-based completion...");
        for (int round = 0; round < 1000; round++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) {
                System.out.println("   ✓ All assignments complete!");
                return;
            }

            boolean progress = false;
            for (Assignment a : incomplete) {
                if (isPJOKSubject(a.getSubject())) continue;

                if (swapAndPlace(schedule, a)) {
                    progress = true;
                    break;
                }
            }

            if (!progress) break;
        }

        // Phase 4.3: Force placement with relaxed constraints
        System.out.println("   → Phase 4.3: Force placement (relaxed)...");
        List<Assignment> stillIncomplete = getIncompleteAssignments(schedule);
        for (Assignment a : stillIncomplete) {
            if (isPJOKSubject(a.getSubject())) continue;

            int remaining = a.getTotalHours() - schedule.getScheduledHours(a);
            System.out.printf("      Forcing %s [%s] - needs %d more hours%n",
                a.getSubject(), a.getClassName(), remaining);

            for (int i = 0; i < remaining; i++) {
                if (!forcePlace(schedule, a)) {
                    System.out.printf("      ❌ Failed to force place %s [%s]%n",
                        a.getSubject(), a.getClassName());
                }
            }
        }

        // Phase 4.4: Ultra-aggressive - swap with overscheduled
        System.out.println("   → Phase 4.4: Ultra-aggressive completion...");
        stillIncomplete = getIncompleteAssignments(schedule);
        for (Assignment a : stillIncomplete) {
            int remaining = a.getTotalHours() - schedule.getScheduledHours(a);

            for (int i = 0; i < remaining; i++) {
                if (!swapWithOverscheduled(schedule, a)) {
                    if (!swapWithAnyAndRelocate(schedule, a)) {
                        System.out.printf("      ⚠ Could not place %s [%s] via swap%n",
                            a.getSubject(), a.getClassName());
                    }
                }
            }
        }

        // Phase 4.5: FINAL FORCE - swap dengan kelas lain jika perlu
        System.out.println("   → Phase 4.5: Final force placement...");
        stillIncomplete = getIncompleteAssignments(schedule);
        for (Assignment a : stillIncomplete) {
            int remaining = a.getTotalHours() - schedule.getScheduledHours(a);
            System.out.printf("      FINAL FORCE for %s - %s [%s] - needs %d more hours%n",
                a.getTeacher(), a.getSubject(), a.getClassName(), remaining);

            for (int i = 0; i < remaining; i++) {
                if (!finalForcePlace(schedule, a)) {
                    System.out.printf("      ❌ FINAL FORCE FAILED for %s [%s]%n",
                        a.getSubject(), a.getClassName());
                }
            }
        }

        // Phase 4.6: ULTRA FORCE - Absolute last resort dengan constraint minimal
        System.out.println("   → Phase 4.6: Ultra force placement (minimal constraints)...");
        stillIncomplete = getIncompleteAssignments(schedule);
        for (Assignment a : stillIncomplete) {
            int remaining = a.getTotalHours() - schedule.getScheduledHours(a);
            System.out.printf("      ULTRA FORCE for %s - %s [%s] - needs %d more hours%n",
                a.getTeacher(), a.getSubject(), a.getClassName(), remaining);

            for (int i = 0; i < remaining; i++) {
                if (!ultraForcePlace(schedule, a)) {
                    System.out.printf("      ❌ ULTRA FORCE FAILED for %s [%s]%n",
                        a.getSubject(), a.getClassName());
                }
            }
        }

        // Phase 4.7: ABSOLUTE FINAL - Loop sampai semua complete atau tidak ada progress
        System.out.println("   → Phase 4.7: Absolute final completion loop...");
        for (int finalRound = 0; finalRound < 100; finalRound++) {
            stillIncomplete = getIncompleteAssignments(schedule);
            if (stillIncomplete.isEmpty()) {
                System.out.println("   ✅ All assignments 100% complete!");
                return;
            }

            boolean anyProgress = false;
            for (Assignment a : stillIncomplete) {
                int before = schedule.getScheduledHours(a);
                int remaining = a.getTotalHours() - before;

                for (int i = 0; i < remaining; i++) {
                    // Try all methods in sequence
                    if (placeOneHourSmart(schedule, a)) {
                        anyProgress = true;
                    } else if (placeOneHourAnywhere(schedule, a)) {
                        anyProgress = true;
                    } else if (placeOneHourRelaxed(schedule, a)) {
                        anyProgress = true;
                    } else if (swapAndPlace(schedule, a)) {
                        anyProgress = true;
                    } else if (forcePlace(schedule, a)) {
                        anyProgress = true;
                    } else if (swapWithAnyAndRelocate(schedule, a)) {
                        anyProgress = true;
                    } else if (ultraForcePlace(schedule, a)) {
                        anyProgress = true;
                    } else if (absoluteLastResortPlace(schedule, a)) {
                        anyProgress = true;
                    }
                }

                int after = schedule.getScheduledHours(a);
                if (after > before) {
                    System.out.printf("      Round %d: %s [%s] improved %d→%d/%d%n",
                        finalRound + 1, a.getSubject(), a.getClassName(), before, after, a.getTotalHours());
                }
            }

            if (!anyProgress) {
                System.out.println("      No more progress possible, exiting loop.");
                break;
            }
        }

        // Phase 4.8: GUARANTEED COMPLETION - Force place semua incomplete dengan nuclear option
        System.out.println("   → Phase 4.8: Guaranteed completion (nuclear)...");
        stillIncomplete = getIncompleteAssignments(schedule);
        for (Assignment a : stillIncomplete) {
            int remaining = a.getTotalHours() - schedule.getScheduledHours(a);
            System.out.printf("      NUCLEAR for %s - %s [%s] - needs %d more hours%n",
                a.getTeacher(), a.getSubject(), a.getClassName(), remaining);

            for (int i = 0; i < remaining; i++) {
                if (!absoluteLastResortPlace(schedule, a)) {
                    // FINAL NUCLEAR: Just place it anywhere, even if it creates conflicts
                    guaranteedPlace(schedule, a);
                }
            }
        }

        // Phase 4.9: PERSISTENT INCOMPLETE HANDLER - Khusus untuk yang masih incomplete
        System.out.println("   → Phase 4.9: Persistent incomplete handler...");
        stillIncomplete = getIncompleteAssignments(schedule);
        for (Assignment a : stillIncomplete) {
            int remaining = a.getTotalHours() - schedule.getScheduledHours(a);
            System.out.printf("      🔥 PERSISTENT INCOMPLETE: %s - %s [%s] - needs %d more hours%n",
                a.getTeacher(), a.getSubject(), a.getClassName(), remaining);

            for (int i = 0; i < remaining; i++) {
                if (!handlePersistentIncomplete(schedule, a)) {
                    System.out.printf("      ❌ Even persistent handler failed for %s [%s]%n",
                        a.getSubject(), a.getClassName());
                }
            }
        }

        // Final check
        stillIncomplete = getIncompleteAssignments(schedule);
        if (stillIncomplete.isEmpty()) {
            System.out.println("   ✅ All assignments 100% complete!");
        } else {
            System.out.printf("   ⚠ %d assignments still incomplete%n", stillIncomplete.size());
            for (Assignment a : stillIncomplete) {
                System.out.printf("      - %s - %s [%s]: %d/%d jam%n",
                    a.getTeacher(), a.getSubject(), a.getClassName(),
                    schedule.getScheduledHours(a), a.getTotalHours());
            }
        }
    }

    /**
     * PERSISTENT INCOMPLETE HANDLER
     * Khusus menangani assignment yang tetap incomplete setelah semua phase
     * Akan mencoba semua kemungkinan termasuk yang berisiko
     */
    private boolean handlePersistentIncomplete(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        System.out.printf("         → Handling persistent incomplete: %s - %s [%s]%n",
            teacher, needsSlot.getSubject(), className);

        // STEP 1: Cari SEMUA slot di kelas ini, prioritas slot kosong
        List<TimeSlot> emptySlots = new ArrayList<>();
        List<TimeSlot> occupiedSlots = new ArrayList<>();
        List<TimeSlot> conflictSlots = new ArrayList<>();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null) continue;

                if (schedule.isTeacherAvailable(teacher, day, period, className)) {
                    if (slot.isEmpty()) {
                        emptySlots.add(slot);
                    } else if (!isPJOKSubject(slot.getAssignment().getSubject())) {
                        occupiedSlots.add(slot);
                    }
                } else {
                    // Teacher is busy elsewhere - potential for conflict resolution
                    if (!slot.isEmpty() && !isPJOKSubject(slot.getAssignment().getSubject())) {
                        conflictSlots.add(slot);
                    }
                }
            }
        }

        // Try empty slots first
        if (!emptySlots.isEmpty()) {
            TimeSlot slot = emptySlots.get(0);
            slot.assign(needsSlot, 1);
            System.out.printf("         ✓ Placed at %s P%d (found empty slot)%n",
                slot.getDay(), slot.getPeriod());
            return true;
        }

        // STEP 2: Coba occupied slots dengan aggressive relocation
        for (TimeSlot slot : occupiedSlots) {
            Assignment occupant = slot.getAssignment();

            // Check jika occupant sudah overscheduled atau complete
            int occupantScheduled = schedule.getScheduledHours(occupant);
            int occupantNeeded = occupant.getTotalHours();

            // Prioritas swap dengan yang overscheduled
            if (occupantScheduled > occupantNeeded) {
                slot.clear();
                slot.assign(needsSlot, 1);
                System.out.printf("         ✓ Placed at %s P%d (replaced overscheduled %s)%n",
                    slot.getDay(), slot.getPeriod(), occupant.getSubject());
                return true;
            }

            // Try chain relocation dengan depth maksimal
            slot.clear();
            if (chainRelocate(schedule, occupant, 20)) {
                slot.assign(needsSlot, 1);
                System.out.printf("         ✓ Placed at %s P%d (chain relocated %s)%n",
                    slot.getDay(), slot.getPeriod(), occupant.getSubject());
                return true;
            }

            // Restore if failed
            slot.assign(occupant, 1);
        }

        // STEP 3: Try finding teacher conflict and resolve it
        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot targetSlot = schedule.getSlot(day, period, className);
                if (targetSlot == null) continue;

                // Check if teacher is conflicted
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) {
                    // Find and resolve the conflict
                    if (resolveTeacherConflictAndPlace(schedule, needsSlot, day, period)) {
                        System.out.printf("         ✓ Placed at %s P%d (resolved conflict)%n", day, period);
                        return true;
                    }
                }
            }
        }

        // STEP 4: Try conflict slots - relocate teacher's other assignment
        for (TimeSlot slot : conflictSlots) {
            String day = slot.getDay();
            int period = slot.getPeriod();

            if (resolveTeacherConflictAndPlace(schedule, needsSlot, day, period)) {
                System.out.printf("         ✓ Placed at %s P%d (resolved and placed)%n", day, period);
                return true;
            }
        }

        // STEP 5: ULTRA NUCLEAR - Place and displace without relocating
        System.out.println("         → Going ULTRA NUCLEAR for persistent incomplete...");

        // Try occupied slots again but this time just displace without caring about relocation
        for (TimeSlot slot : occupiedSlots) {
            Assignment occupant = slot.getAssignment();

            // Just swap, let the occupant become incomplete temporarily
            slot.clear();
            slot.assign(needsSlot, 1);

            System.out.printf("         ✓ ULTRA NUCLEAR: Placed at %s P%d (displaced %s - %s will be incomplete)%n",
                slot.getDay(), slot.getPeriod(), occupant.getSubject(), occupant.getClassName());

            // Try to relocate occupant, but don't undo if it fails
            placeOneHourRelaxed(schedule, occupant);
            return true;
        }

        // STEP 6: ABSOLUTE NUCLEAR - Even consider conflict slots
        System.out.println("         → ABSOLUTE NUCLEAR - considering conflict slots...");
        for (TimeSlot slot : conflictSlots) {
            Assignment occupant = slot.getAssignment();

            // Clear the slot and place our assignment
            slot.clear();
            slot.assign(needsSlot, 1);

            System.out.printf("         ✓ ABSOLUTE NUCLEAR: Placed at %s P%d (may create conflicts)%n",
                slot.getDay(), slot.getPeriod());

            // Try to fix the occupant
            placeOneHourRelaxed(schedule, occupant);
            return true;
        }

        return false;
    }

    /**
     * Resolve teacher conflict and place the assignment
     */
    private boolean resolveTeacherConflictAndPlace(Schedule schedule, Assignment needsSlot,
                                                     String day, int period) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        // Find where teacher is conflicted
        for (String otherClass : schedule.getAllClasses()) {
            if (otherClass.equals(className)) continue;

            TimeSlot conflictSlot = schedule.getSlot(day, period, otherClass);
            if (conflictSlot == null || conflictSlot.isEmpty()) continue;

            Assignment conflictAssignment = conflictSlot.getAssignment();
            if (!conflictAssignment.getTeacher().equals(teacher)) continue;

            // Found the conflict! Try to relocate it
            if (isPJOKSubject(conflictAssignment.getSubject())) continue; // Don't move PJOK

            conflictSlot.clear();

            // Try to relocate with maximum effort
            if (chainRelocate(schedule, conflictAssignment, 15) ||
                placeOneHourRelaxed(schedule, conflictAssignment) ||
                placeOneHourAnywhere(schedule, conflictAssignment)) {

                // Successfully relocated conflict, now place our assignment
                TimeSlot targetSlot = schedule.getSlot(day, period, className);

                Assignment targetOccupant = null;
                if (!targetSlot.isEmpty()) {
                    targetOccupant = targetSlot.getAssignment();
                    if (isPJOKSubject(targetOccupant.getSubject())) {
                        // Can't displace PJOK, restore conflict
                        conflictSlot.assign(conflictAssignment, 1);
                        continue;
                    }
                    targetSlot.clear();
                }

                targetSlot.assign(needsSlot, 1);

                // Try to relocate displaced assignment if any
                if (targetOccupant != null) {
                    placeOneHourRelaxed(schedule, targetOccupant);
                }

                return true;
            } else {
                // Couldn't relocate conflict, but do it anyway (nuclear option)
                TimeSlot targetSlot = schedule.getSlot(day, period, className);

                Assignment targetOccupant = null;
                if (!targetSlot.isEmpty()) {
                    targetOccupant = targetSlot.getAssignment();
                    if (isPJOKSubject(targetOccupant.getSubject())) {
                        conflictSlot.assign(conflictAssignment, 1);
                        continue;
                    }
                    targetSlot.clear();
                }

                targetSlot.assign(needsSlot, 1);

                // Try to place both displaced assignments somewhere
                if (targetOccupant != null) {
                    placeOneHourRelaxed(schedule, targetOccupant);
                }
                placeOneHourRelaxed(schedule, conflictAssignment);

                return true;
            }
        }

        return false;
    }

    /**
     * FINAL FORCE: Cari slot di kelas ini, jika ada yg terisi, pindahkan ke slot lain
     */
    private boolean finalForcePlace(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null) continue;

                if (slot.isEmpty()) {
                    slot.assign(needsSlot, 1);
                    return true;
                }

                Assignment currentOccupant = slot.getAssignment();
                if (isPJOKSubject(currentOccupant.getSubject())) continue;

                slot.clear();
                boolean relocated = relocateAssignment(schedule, currentOccupant);
                if (relocated) {
                    slot.assign(needsSlot, 1);
                    return true;
                } else {
                    slot.assign(currentOccupant, 1);
                }
            }
        }
        return false;
    }

    private boolean relocateAssignment(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isMGMP = mgmpTeachers.contains(teacher);

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            if (isMGMP && day.equals("Rabu")) {
                maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
            }

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className)) {
                    slot.assign(assignment, 1);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean relocateAssignmentRelaxed(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className)) {
                    slot.assign(assignment, 1);
                    return true;
                }
            }
        }
        return chainRelocate(schedule, assignment, 3);
    }

    private boolean chainRelocate(Schedule schedule, Assignment assignment, int maxDepth) {
        if (maxDepth <= 0) return false;

        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null) continue;

                if (slot.isEmpty()) {
                    slot.assign(assignment, 1);
                    return true;
                }

                Assignment occupant = slot.getAssignment();
                if (isPJOKSubject(occupant.getSubject())) continue;

                slot.clear();
                if (chainRelocate(schedule, occupant, maxDepth - 1)) {
                    slot.assign(assignment, 1);
                    return true;
                }
                slot.assign(occupant, 1);
            }
        }
        return false;
    }

    private double evaluateFitness(Schedule schedule) {
        double score = 100000.0;
        double completionRatio = 0;
        int totalNeeded = 0, totalScheduled = 0;
        for (Assignment a : assignments) {
            totalNeeded += a.getTotalHours();
            totalScheduled += schedule.getScheduledHours(a);
        }
        if (totalNeeded > 0) completionRatio = totalScheduled / (double) totalNeeded;

        // ULTRA HIGH PRIORITY for completion
        score += completionRatio * 500000; // Increased from 300000

        // PJOK pattern penalty (high but not as high as completion)
        int pjokPatternViol = countPJOKPatternViolations(schedule);
        score -= pjokPatternViol * 30000; // Reduced from 50000

        // Teacher conflicts
        int conflicts = countTeacherConflicts(schedule);
        score -= conflicts * 25000; // Reduced from 40000

        // PJOK timing violations
        int pjokViol = countPJOKViolations(schedule);
        score -= pjokViol * 15000; // Reduced from 20000

        // MGMP violations
        int mgmpViol = countMGMPViolations(schedule);
        score -= mgmpViol * 15000; // Reduced from 20000

        // Max hours violations
        int maxHoursViol = countMaxHoursViolations(schedule);
        score -= maxHoursViol * 500;

        return score;
    }

    private double getCompletionPercentage(Schedule schedule) {
        int totalNeeded = 0, totalScheduled = 0;
        for (Assignment a : assignments) {
            totalNeeded += a.getTotalHours();
            totalScheduled += schedule.getScheduledHours(a);
        }
        return totalNeeded > 0 ? (totalScheduled * 100.0) / totalNeeded : 0;
    }

    private int countAllViolations(Schedule schedule) {
        return countTeacherConflicts(schedule) + countPJOKViolations(schedule) + countMGMPViolations(schedule);
    }

    private List<Assignment> getIncompleteAssignments(Schedule schedule) {
        List<Assignment> incomplete = new ArrayList<>();
        for (Assignment a : assignments) {
            if (schedule.getScheduledHours(a) < a.getTotalHours()) {
                incomplete.add(a);
            }
        }
        return incomplete;
    }

    private int countTeacherConflicts(Schedule schedule) {
        int conflicts = 0;
        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];
            for (int period = 1; period <= maxPeriod; period++) {
                Map<String, Integer> teacherCount = new HashMap<>();
                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        teacherCount.merge(slot.getAssignment().getTeacher(), 1, Integer::sum);
                    }
                }
                for (int count : teacherCount.values()) {
                    if (count > 1) conflicts += (count - 1);
                }
            }
        }
        return conflicts;
    }

    private int countPJOKViolations(Schedule schedule) {
        int violations = 0;
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                for (int i = 0; i < slots.size() - 1; i++) {
                    TimeSlot slot = slots.get(i);
                    if (slot.isEmpty() || !isPJOKSubject(slot.getAssignment().getSubject())) continue;
                    TimeSlot next = slots.get(i + 1);
                    if (!next.isEmpty() && isPJOKSubject(next.getAssignment().getSubject()) &&
                        next.getAssignment().getTeacher().equals(slot.getAssignment().getTeacher())) {
                        if (next.getPeriod() > PJOK_DOUBLE_MAX_END) {
                            violations += 2;
                        }
                        i++;
                    }
                }
            }
        }
        return violations;
    }

    private int countMGMPViolations(Schedule schedule) {
        int violations = 0;
        for (String className : schedule.getAllClasses()) {
            for (TimeSlot slot : schedule.getSlotsForClass("Rabu", className)) {
                if (!slot.isEmpty() && slot.getPeriod() > MGMP_MAX_PERIOD_RABU &&
                    mgmpTeachers.contains(slot.getAssignment().getTeacher())) {
                    violations++;
                }
            }
        }
        return violations;
    }

    private int countMaxHoursViolations(Schedule schedule) {
        int violations = 0;
        for (Assignment a : assignments) {
            for (String day : DAYS) {
                int hours = countSubjectHoursOnDay(schedule, a, day);
                if (hours > MAX_HOURS_PER_SUBJECT_PER_DAY) {
                    violations += (hours - MAX_HOURS_PER_SUBJECT_PER_DAY);
                }
            }
        }
        return violations;
    }

    private int countSubjectHoursOnDay(Schedule schedule, Assignment assignment, String day) {
        int count = 0;
        for (TimeSlot slot : schedule.getSlotsForClass(day, assignment.getClassName())) {
            if (!slot.isEmpty() &&
                slot.getAssignment().getSubject().equalsIgnoreCase(assignment.getSubject())) {
                count++;
            }
        }
        return count;
    }

    private int[] getDistributionPattern(Assignment assignment) {
        String subject = assignment.getSubject().toUpperCase().trim();
        int totalHours = assignment.getTotalHours();

        if (subject.contains("PJOK") && totalHours == 3) return new int[]{2, 1};
        if (subject.contains("MATEMATIKA") && totalHours == 5) return new int[]{3, 2};
        if (subject.contains("IPA") && totalHours == 5) return new int[]{3, 2};
        if ((subject.contains("BAHASA INDONESIA") || subject.contains("B. INDONESIA")) && totalHours == 6) return new int[]{2, 2, 2};
        if ((subject.contains("BAHASA INGGRIS") || subject.contains("B. INGGRIS")) && totalHours == 4) return new int[]{2, 2};
        if (subject.contains("IPS") && totalHours == 4) return new int[]{2, 2};
        if (totalHours == 3) return new int[]{2, 1};
        if (totalHours == 2) return new int[]{2};
        if (totalHours == 1) return new int[]{1};

        if (totalHours <= 3) return new int[]{totalHours};
        int sessions = (totalHours + 2) / 3;
        int[] pattern = new int[sessions];
        int remaining = totalHours;
        for (int i = 0; i < sessions; i++) {
            pattern[i] = Math.min(3, remaining);
            remaining -= pattern[i];
        }
        return pattern;
    }

    private void printDetailedReport(Schedule schedule, double seconds) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║              LAPORAN HASIL PENJADWALAN FINAL                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        int totalAssignments = assignments.size();
        int completedAssignments = 0;
        int totalHoursNeeded = 0;
        int totalHoursScheduled = 0;

        for (Assignment a : assignments) {
            int scheduled = schedule.getScheduledHours(a);
            totalHoursNeeded += a.getTotalHours();
            totalHoursScheduled += scheduled;
            if (scheduled == a.getTotalHours()) completedAssignments++;
        }

        System.out.println("\n📊 KELENGKAPAN:");
        System.out.printf("   Assignment Complete : %d/%d (%.1f%%)%n",
            completedAssignments, totalAssignments, (completedAssignments * 100.0 / totalAssignments));
        System.out.printf("   Jam Terjadwal       : %d/%d (%.1f%%)%n",
            totalHoursScheduled, totalHoursNeeded, (totalHoursScheduled * 100.0 / totalHoursNeeded));

        int conflicts = countTeacherConflicts(schedule);
        int pjokViol = countPJOKViolations(schedule);
        int pjokPatternViol = countPJOKPatternViolations(schedule);
        int mgmpViol = countMGMPViolations(schedule);
        int maxHoursViol = countMaxHoursViolations(schedule);

        System.out.println("\n⚠️  KONFLIK & CONSTRAINT:");
        System.out.printf("   Konflik Guru        : %d %s%n", conflicts, conflicts == 0 ? "✅" : "❌");
        System.out.printf("   PJOK Pattern (2-1)  : %d %s%n", pjokPatternViol, pjokPatternViol == 0 ? "✅" : "❌");
        System.out.printf("   PJOK Timing         : %d %s%n", pjokViol, pjokViol == 0 ? "✅" : "❌");
        System.out.printf("   MGMP (Rabu max 4)   : %d %s%n", mgmpViol, mgmpViol == 0 ? "✅" : "❌");
        System.out.printf("   Max 3 Jam/Hari      : %d %s%n", maxHoursViol, maxHoursViol == 0 ? "✅" : "⚠️");

        System.out.printf("\n⏱️  Waktu: %.2f detik%n", seconds);

        boolean isPerfect = conflicts == 0 && completedAssignments == totalAssignments &&
            pjokViol == 0 && pjokPatternViol == 0 && mgmpViol == 0;

        System.out.println("\n" + (isPerfect ?
            "✅✅✅ SEMPURNA! Semua constraint terpenuhi 100%! ✅✅✅" :
            "⚠️ Ada beberapa constraint yang belum optimal"));
        System.out.println("════════════════════════════════════════════════════════════════");
    }

    private void printPJOKPatternReport(Schedule schedule) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    LAPORAN PJOK PATTERN                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        for (String className : schedule.getAllClasses()) {
            Map<String, List<TimeSlot>> pjokByTeacher = new HashMap<>();

            for (String day : DAYS) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())) {
                        String teacher = slot.getAssignment().getTeacher();
                        pjokByTeacher.computeIfAbsent(teacher, k -> new ArrayList<>()).add(slot);
                    }
                }
            }

            for (List<TimeSlot> slots : pjokByTeacher.values()) {
                if (slots.isEmpty()) continue;

                slots.sort((a, b) -> {
                    int dayCompare = Arrays.asList(DAYS).indexOf(a.getDay()) - Arrays.asList(DAYS).indexOf(b.getDay());
                    if (dayCompare != 0) return dayCompare;
                    return a.getPeriod() - b.getPeriod();
                });

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("   [%s] PJOK: ", className));

                boolean hasValidPair = false;
                int maxEndPeriod = 0;

                for (int i = 0; i < slots.size(); i++) {
                    TimeSlot s = slots.get(i);
                    sb.append(String.format("%s-P%d", s.getDay(), s.getPeriod()));
                    if (i < slots.size() - 1) sb.append(", ");

                    for (int j = i + 1; j < slots.size(); j++) {
                        TimeSlot s2 = slots.get(j);
                        if (s.getDay().equals(s2.getDay()) && Math.abs(s.getPeriod() - s2.getPeriod()) == 1) {
                            int endP = Math.max(s.getPeriod(), s2.getPeriod());
                            if (endP <= PJOK_DOUBLE_MAX_END) {
                                hasValidPair = true;
                                maxEndPeriod = endP;
                            }
                        }
                    }
                }

                if (slots.size() == 3) {
                    if (hasValidPair) {
                        sb.append(String.format(" ✅ (2-1 valid, ends P%d)", maxEndPeriod));
                    } else {
                        sb.append(" ❌ (Pattern invalid!)");
                    }
                }

                System.out.println(sb.toString());
            }
        }
        System.out.println("════════════════════════════════════════════════════════════════");
    }

    private static class TabuList {
        private final Queue<Integer> tabuQueue;
        private final Set<Integer> tabuSet;
        private final int maxSize;

        TabuList(int maxSize) {
            this.maxSize = maxSize;
            this.tabuQueue = new LinkedList<>();
            this.tabuSet = new HashSet<>();
        }

        void add(Schedule schedule) {
            int hash = scheduleHash(schedule);
            if (tabuSet.contains(hash)) return;
            tabuQueue.add(hash);
            tabuSet.add(hash);
            while (tabuQueue.size() > maxSize) {
                Integer removed = tabuQueue.poll();
                if (removed != null) tabuSet.remove(removed);
            }
        }

        boolean isTabu(Schedule schedule) {
            return tabuSet.contains(scheduleHash(schedule));
        }

        private int scheduleHash(Schedule schedule) {
            int hash = 17;
            for (String className : schedule.getAllClasses()) {
                for (String day : DAYS) {
                    for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                        if (!slot.isEmpty()) {
                            hash = 31 * hash + slot.getAssignment().hashCode();
                            hash = 31 * hash + slot.getPeriod();
                        }
                    }
                }
            }
            return hash;
        }
    }

    private void clearAssignmentSlots(Schedule schedule, Assignment assignment) {
        for (String day : DAYS) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, assignment.getClassName())) {
                if (!slot.isEmpty() &&
                    slot.getAssignment().getTeacher().equals(assignment.getTeacher()) &&
                    slot.getAssignment().getSubject().equals(assignment.getSubject())) {
                    slot.clear();
                }
            }
        }
    }

    private boolean placeOneHourAnywhere(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className)) {
                    slot.assign(assignment, 1);
                    return true;
                }
            }
        }
        return false;
    }

    private void finalConstraintRepair(Schedule schedule) {
        for (int round = 0; round < 300; round++) {
            int violations = countAllViolations(schedule);
            int pjokViol = countPJOKPatternViolations(schedule);

            if (violations == 0 && pjokViol == 0) {
                System.out.println("   ✅ All constraints satisfied!");
                return;
            }

            if (pjokViol > 0) {
                repairAllPJOKPatterns(schedule);
            }

            repairMGMPViolations(schedule);
            repairTeacherConflicts(schedule);
        }
    }

    private void repairMGMPViolations(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            List<TimeSlot> slots = schedule.getSlotsForClass("Rabu", className);
            for (TimeSlot slot : slots) {
                if (!slot.isEmpty() && slot.getPeriod() > MGMP_MAX_PERIOD_RABU &&
                    mgmpTeachers.contains(slot.getAssignment().getTeacher())) {
                    Assignment a = slot.getAssignment();
                    slot.clear();
                    placeOneHourSmart(schedule, a);
                }
            }
        }
    }

    private void repairTeacherConflicts(Schedule schedule) {
        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                Map<String, List<TimeSlot>> teacherSlots = new HashMap<>();

                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        String teacher = slot.getAssignment().getTeacher();
                        teacherSlots.computeIfAbsent(teacher, k -> new ArrayList<>()).add(slot);
                    }
                }

                for (List<TimeSlot> slots : teacherSlots.values()) {
                    if (slots.size() > 1) {
                        for (int i = 1; i < slots.size(); i++) {
                            Assignment a = slots.get(i).getAssignment();
                            slots.get(i).clear();
                            placeOneHourSmart(schedule, a);
                        }
                    }
                }
            }
        }
    }

    private Schedule generateSmartNeighbor(Schedule schedule) {
        Schedule neighbor = schedule.clone();
        int moveType = random.nextInt(5);

        switch (moveType) {
            case 0: swapSameClass(neighbor); break;
            case 1: moveSingleHour(neighbor); break;
            case 2: redistributeIncomplete(neighbor); break;
            case 3: fixRandomViolation(neighbor); break;
            case 4: improvePJOKPattern(neighbor); break;
        }

        return neighbor;
    }

    private void improvePJOKPattern(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            Map<String, List<TimeSlot>> pjokByTeacher = new HashMap<>();

            for (String day : DAYS) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())) {
                        String teacher = slot.getAssignment().getTeacher();
                        pjokByTeacher.computeIfAbsent(teacher, k -> new ArrayList<>()).add(slot);
                    }
                }
            }

            for (Map.Entry<String, List<TimeSlot>> entry : pjokByTeacher.entrySet()) {
                List<TimeSlot> slots = entry.getValue();
                if (slots.size() != 3) continue;

                boolean hasValidPair = false;
                for (int i = 0; i < slots.size() && !hasValidPair; i++) {
                    for (int j = i + 1; j < slots.size(); j++) {
                        TimeSlot s1 = slots.get(i);
                        TimeSlot s2 = slots.get(j);
                        if (s1.getDay().equals(s2.getDay()) &&
                            Math.abs(s1.getPeriod() - s2.getPeriod()) == 1 &&
                            Math.max(s1.getPeriod(), s2.getPeriod()) <= PJOK_DOUBLE_MAX_END) {
                            hasValidPair = true;
                            break;
                        }
                    }
                }

                if (!hasValidPair) {
                    Assignment pjok = slots.get(0).getAssignment();
                    for (TimeSlot slot : slots) slot.clear();
                    placePJOKStrict21Pattern(schedule, pjok);
                    return;
                }
            }
        }
    }

    private void swapSameClass(Schedule schedule) {
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        if (classes.isEmpty()) return;

        String className = classes.get(random.nextInt(classes.size()));
        List<TimeSlot> filled = new ArrayList<>();

        for (String day : DAYS) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                if (!slot.isEmpty() && !isPJOKSubject(slot.getAssignment().getSubject())) {
                    filled.add(slot);
                }
            }
        }

        if (filled.size() < 2) return;

        TimeSlot slot1 = filled.get(random.nextInt(filled.size()));
        TimeSlot slot2 = filled.get(random.nextInt(filled.size()));
        if (slot1 == slot2) return;

        Assignment a1 = slot1.getAssignment();
        Assignment a2 = slot2.getAssignment();
        int s1 = slot1.getSessionNumber();
        int s2 = slot2.getSessionNumber();

        slot1.clear();
        slot2.clear();
        slot1.assign(a2, s2);
        slot2.assign(a1, s1);
    }

    private void moveSingleHour(Schedule schedule) {
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        if (classes.isEmpty()) return;

        String className = classes.get(random.nextInt(classes.size()));

        for (int attempts = 0; attempts < 10; attempts++) {
            String sourceDay = DAYS[random.nextInt(DAYS.length)];
            List<TimeSlot> sourceSlots = schedule.getSlotsForClass(sourceDay, className);

            List<TimeSlot> filled = new ArrayList<>();
            for (TimeSlot slot : sourceSlots) {
                if (!slot.isEmpty() && !isPJOKSubject(slot.getAssignment().getSubject())) {
                    filled.add(slot);
                }
            }
            if (filled.isEmpty()) continue;

            TimeSlot source = filled.get(random.nextInt(filled.size()));
            String targetDay = DAYS[random.nextInt(DAYS.length)];

            for (TimeSlot target : schedule.getSlotsForClass(targetDay, className)) {
                if (target.isEmpty() && schedule.isTeacherAvailable(
                    source.getAssignment().getTeacher(), targetDay, target.getPeriod(), className)) {

                    Assignment a = source.getAssignment();
                    int s = source.getSessionNumber();
                    source.clear();
                    target.assign(a, s);
                    return;
                }
            }
        }
    }

    private void redistributeIncomplete(Schedule schedule) {
        List<Assignment> incomplete = getIncompleteAssignments(schedule);
        if (incomplete.isEmpty()) return;

        Assignment a = incomplete.get(random.nextInt(incomplete.size()));
        if (!isPJOKSubject(a.getSubject())) {
            placeOneHourSmart(schedule, a);
        }
    }

    private void fixRandomViolation(Schedule schedule) {
        repairMGMPViolations(schedule);
        repairTeacherConflicts(schedule);
    }

    private boolean swapAndPlace(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();
        boolean isMGMP = mgmpTeachers.contains(teacher);

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            if (isMGMP && day.equals("Rabu")) {
                maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
            }

            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null) continue;

                if (slot.isEmpty()) {
                    slot.assign(needsSlot, 1);
                    return true;
                }

                Assignment occupant = slot.getAssignment();
                if (isPJOKSubject(occupant.getSubject())) continue;

                slot.clear();
                if (placeOneHourAnywhere(schedule, occupant)) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
                slot.assign(occupant, 1);
            }
        }
        return false;
    }

    private boolean forcePlace(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty()) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
            }
        }

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null || slot.isEmpty()) continue;

                Assignment occupant = slot.getAssignment();
                if (isPJOKSubject(occupant.getSubject())) continue;

                slot.clear();
                boolean relocated = relocateAssignment(schedule, occupant);
                if (relocated) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
                slot.assign(occupant, 1);
            }
        }

        return false;
    }

    private boolean swapWithOverscheduled(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        for (Assignment other : assignments) {
            if (!other.getClassName().equals(className)) continue;
            if (other.getTeacher().equals(teacher) && other.getSubject().equals(needsSlot.getSubject())) continue;

            int scheduled = schedule.getScheduledHours(other);
            int needed = other.getTotalHours();

            if (scheduled > needed) {
                for (String day : DAYS) {
                    for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                        if (slot.isEmpty()) continue;
                        if (!slot.getAssignment().getTeacher().equals(other.getTeacher())) continue;
                        if (!slot.getAssignment().getSubject().equals(other.getSubject())) continue;

                        if (!schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) continue;

                        slot.clear();
                        slot.assign(needsSlot, 1);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean swapWithAnyAndRelocate(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null || slot.isEmpty()) continue;

                Assignment occupant = slot.getAssignment();
                if (isPJOKSubject(occupant.getSubject())) continue;

                slot.clear();
                if (chainRelocate(schedule, occupant, 5)) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
                slot.assign(occupant, 1);
            }
        }

        return false;
    }

    private boolean ultraForcePlace(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null) continue;

                if (slot.isEmpty() && schedule.isTeacherAvailable(teacher, day, period, className)) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
            }
        }

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null || slot.isEmpty()) continue;

                Assignment currentOccupant = slot.getAssignment();
                if (isPJOKSubject(currentOccupant.getSubject())) continue;

                slot.clear();
                if (chainRelocate(schedule, currentOccupant, 7)) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
                slot.assign(currentOccupant, 1);
            }
        }

        return false;
    }

    private boolean absoluteLastResortPlace(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null) continue;

                if (slot.isEmpty() && schedule.isTeacherAvailable(teacher, day, period, className)) {
                    slot.assign(needsSlot, 1);
                    return true;
                }
            }
        }

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriod; period++) {
                if (!schedule.isTeacherAvailable(teacher, day, period, className)) continue;

                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot == null || slot.isEmpty()) continue;

                Assignment occupant = slot.getAssignment();
                if (isPJOKSubject(occupant.getSubject())) continue;

                slot.clear();
                slot.assign(needsSlot, 1);

                placeOneHourRelaxed(schedule, occupant);
                return true;
            }
        }

        return false;
    }

    private boolean placeOneHourRelaxed(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            int maxPeriod = getPeriodsForDay(day);

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className)) {
                    slot.assign(assignment, 1);
                    return true;
                }
            }
        }

        return chainRelocate(schedule, assignment, 10);
    }

    private void guaranteedPlace(Schedule schedule, Assignment assignment) {
        for (String day : DAYS) {
            for (int period = 1; period <= 10; period++) {
                TimeSlot slot = schedule.getSlot(day, period, assignment.getClassName());
                if (slot != null) {
                    slot.assign(assignment, 1);
                    return;
                }
            }
        }
    }
}
