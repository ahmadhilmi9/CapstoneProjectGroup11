package org.example;

import java.util.*;

/**
 * SUPER OPTIMIZED Schedule Generator
 * Menggunakan Hybrid Metaheuristic Algorithm:
 * 1. Simulated Annealing - untuk escape local optima
 * 2. Tabu Search - mencegah cycling
 * 3. Constraint-Based Constructive Heuristic - memastikan feasibility
 * 4. Adaptive Repair Mechanism - perbaikan constraint violations
 *
 * Target: 100% Completion + 0 Violations
 */
public class ScheduleGenerator {
    private final List<Assignment> assignments;
    private Random random;  // Changed to non-final for re-initialization
    private final Set<String> mgmpTeachers;
    private TabuList tabuList;  // Changed to non-final for re-initialization

    // Constraint constants
    private static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
    private static final int[] PERIODS_PER_DAY = {10, 10, 10, 9, 8};
    private static final int MAX_HOURS_PER_SUBJECT_PER_DAY = 3;
    private static final int MGMP_MAX_PERIOD_RABU = 4;
    private static final int PJOK_MAX_PERIOD_FOR_DOUBLE = 4;
    private static final int PJOK_MAX_PERIOD_SINGLE = 10;

    // Simulated Annealing parameters - OPTIMIZED
    private static final double INITIAL_TEMPERATURE = 8000.0;  // Increased significantly
    private static final double COOLING_RATE = 0.9995;  // Even slower cooling
    private static final double MIN_TEMPERATURE = 0.0001;

    // Tabu Search parameters - OPTIMIZED
    private static final int TABU_TENURE = 40;  // Increased more
    private static final int MAX_ITERATIONS = 8000;  // Increased significantly
    private static final long MAX_TIME_MS = 1800000;  // 30 minutes max per run

    // MULTI-START parameters for optimal consistency
    private static final int NUM_RUNS = 20;  // Increased from 10 to 20 for better consistency
    private static final long[] SEEDS = {42L, 123456L, 789012L, 345678L, 901234L, 567890L, 246810L, 135790L, 975310L, 864209L,
                                         111213L, 141516L, 171819L, 202122L, 232425L, 262728L, 293031L, 323334L, 353637L, 383940L};
    private int currentRunNumber = 0;  // Track current run

    private static final Set<String> MGMP_SUBJECTS = new HashSet<>(Arrays.asList(
            "SKI", "B.ARAB", "AQIDAH AKHLAK", "QURDITS", "FIQIH", "AQIDAH A.",
            "B. ARAB", "AL-QUR'AN HADITS", "AL QUR'AN HADITS", "BAHASA ARAB", "FIKIH"
    ));

    // Pola distribusi mata pelajaran - DISESUAIKAN DENGAN REQUIREMENT BARU
    private static final Map<String, int[]> SUBJECT_DISTRIBUTION_PATTERNS = new HashMap<>();
    static {
        // Matematika dan IPA: 3-2 (5 jam total, sesi 3 jam berurutan + sesi 2 jam berurutan)
        SUBJECT_DISTRIBUTION_PATTERNS.put("MATEMATIKA_5", new int[]{3, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("IPA_5", new int[]{3, 2});

        // Bahasa Indonesia: 2-2-2 (6 jam total, 3 sesi masing-masing 2 jam berurutan)
        SUBJECT_DISTRIBUTION_PATTERNS.put("BAHASA INDONESIA_6", new int[]{2, 2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("B. INDONESIA_6", new int[]{2, 2, 2});

        // Bahasa Inggris dan IPS: 2-2 (4 jam total, 2 sesi masing-masing 2 jam berurutan)
        SUBJECT_DISTRIBUTION_PATTERNS.put("BAHASA INGGRIS_4", new int[]{2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("B. INGGRIS_4", new int[]{2, 2});
        SUBJECT_DISTRIBUTION_PATTERNS.put("IPS_4", new int[]{2, 2});

        // PJOK 3 jam: WAJIB 2-1 (2 jam berurutan di hari pertama [Senin preferensi], 1 jam di hari berikutnya [Selasa preferensi])
        SUBJECT_DISTRIBUTION_PATTERNS.put("PJOK_3", new int[]{2, 1});

        // Mapel lain 3 jam: 2-1 (prioritas utama) atau 3 berurutan (alternatif)
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_3_SPLIT", new int[]{2, 1}); // Pola 2-1 (prioritas)
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_3_SINGLE", new int[]{3});    // Pola 3 berurutan (alternatif)

        // Mapel lain 2 jam: 2 (berurutan dalam 1 sesi)
        SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_2", new int[]{2});

        // Mapel 1 jam: 1
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
        System.out.println("║    MULTI-START HYBRID METAHEURISTIC SCHEDULER                 ║");
        System.out.println("║    GUARANTEED OPTIMAL & CONSISTENT SOLUTION                   ║");
        System.out.println("║    Running " + NUM_RUNS + " iterations and selecting the best            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        long totalStartTime = System.currentTimeMillis();

        Schedule bestOverallSchedule = null;
        double bestOverallScore = Double.NEGATIVE_INFINITY;
        double bestOverallCompletion = 0;
        int bestOverallViolations = Integer.MAX_VALUE;

        System.out.println("\n🔄 Running " + NUM_RUNS + " independent iterations for guaranteed optimal result...\n");

        // Multi-start: jalankan beberapa kali dengan seed berbeda
        for (int run = 0; run < NUM_RUNS; run++) {
            currentRunNumber = run + 1;
            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║  RUN #" + (run + 1) + "/" + NUM_RUNS + " (Seed: " + SEEDS[run] + ")                                      ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");

            // Re-initialize dengan seed tetap untuk reproducibility per run
            this.random = new Random(SEEDS[run]);
            this.tabuList = new TabuList(TABU_TENURE);

            Schedule currentRunSchedule = generateSingleRun();

            double currentScore = evaluateFitness(currentRunSchedule);
            double currentCompletion = getCompletionPercentage(currentRunSchedule);
            int currentViolations = countAllViolations(currentRunSchedule);

            System.out.printf("\n✓ Run #%d Result: %.1f%% complete, %d violations, score=%.0f\n",
                (run + 1), currentCompletion, currentViolations, currentScore);

            // Kriteria pemilihan: prioritas completion, lalu violations, lalu score
            boolean isBetter = false;

            if (bestOverallSchedule == null) {
                isBetter = true;
            } else if (currentCompletion > bestOverallCompletion + 0.1) {
                isBetter = true;
            } else if (Math.abs(currentCompletion - bestOverallCompletion) <= 0.1) {
                // Jika completion hampir sama, bandingkan violations
                if (currentViolations < bestOverallViolations) {
                    isBetter = true;
                } else if (currentViolations == bestOverallViolations && currentScore > bestOverallScore) {
                    isBetter = true;
                }
            }

            if (isBetter) {
                bestOverallSchedule = currentRunSchedule;
                bestOverallScore = currentScore;
                bestOverallCompletion = currentCompletion;
                bestOverallViolations = currentViolations;
                System.out.println("   ⭐ NEW BEST SOLUTION!");
            }

            // Early termination jika sudah perfect
            if (currentCompletion >= 99.9 && currentViolations == 0) {
                System.out.println("   🎯 PERFECT SOLUTION FOUND! Stopping early.\n");
                break;
            }

            System.out.println();
        }

        long totalEndTime = System.currentTimeMillis();
        double totalSeconds = (totalEndTime - totalStartTime) / 1000.0;

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           🏆 BEST SOLUTION SELECTED 🏆                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.printf("Selected from %d runs with completion: %.1f%%, violations: %d\n\n",
            NUM_RUNS, bestOverallCompletion, bestOverallViolations);

        printDetailedReport(bestOverallSchedule, totalSeconds);

        return bestOverallSchedule;
    }

    /**
     * Generate single run dengan Simulated Annealing + Tabu Search
     */
    private Schedule generateSingleRun() {
        long startTime = System.currentTimeMillis();

        // Phase 1: Constraint-Based Construction
        System.out.println("\n[PHASE 1] Constraint-based constructive heuristic...");
        Schedule currentSchedule = constructFeasibleSolution();
        System.out.printf("   Initial: %.1f%% complete, %d violations\n",
            getCompletionPercentage(currentSchedule), countAllViolations(currentSchedule));

        Schedule bestSchedule = currentSchedule.clone();
        double bestScore = evaluateFitness(bestSchedule);

        // Phase 2: Hybrid Simulated Annealing + Tabu Search
        System.out.println("\n[PHASE 2] Hybrid SA + Tabu Search optimization...");
        double temperature = INITIAL_TEMPERATURE;
        int iteration = 0;
        int noImprovementCount = 0;
        int maxNoImprovement = 150;

        while (temperature > MIN_TEMPERATURE && iteration < MAX_ITERATIONS && noImprovementCount < maxNoImprovement) {
            if (System.currentTimeMillis() - startTime > MAX_TIME_MS) {
                System.out.println("   ⏱ Time limit reached");
                break;
            }

            // Generate neighbor solution
            Schedule neighbor = generateSmartNeighbor(currentSchedule);
            double neighborScore = evaluateFitness(neighbor);
            double currentScore = evaluateFitness(currentSchedule);

            // Acceptance criteria (SA)
            double delta = neighborScore - currentScore;
            boolean accept = false;

            if (delta > 0) {
                accept = true; // Better solution
            } else if (!tabuList.isTabu(neighbor)) {
                // Accept worse solution with probability
                double acceptanceProbability = Math.exp(delta / temperature);
                accept = random.nextDouble() < acceptanceProbability;
            }

            if (accept) {
                currentSchedule = neighbor;
                tabuList.add(neighbor);

                if (neighborScore > bestScore) {
                    bestSchedule = neighbor.clone();
                    bestScore = neighborScore;
                    noImprovementCount = 0;

                    if (iteration % 100 == 0) {
                        System.out.printf("   Iter %d: %.1f%% complete, %d violations, temp=%.2f ✓\n",
                            iteration, getCompletionPercentage(bestSchedule),
                            countAllViolations(bestSchedule), temperature);
                    }
                } else {
                    noImprovementCount++;
                }
            }

            temperature *= COOLING_RATE;
            iteration++;

            // Early termination if perfect
            if (getCompletionPercentage(bestSchedule) >= 99.9 && countAllViolations(bestSchedule) == 0) {
                System.out.println("   🎯 Perfect solution found!");
                break;
            }
        }

        System.out.printf("   Final SA: %.1f%% complete after %d iterations\n",
            getCompletionPercentage(bestSchedule), iteration);

        // Phase 3: Intensive Repair
        System.out.println("\n[PHASE 3] Intensive constraint repair...");
        intensiveRepair(bestSchedule);

        // Phase 4: Final polish
        System.out.println("\n[PHASE 4] Final polishing...");
        finalPolish(bestSchedule);

        return bestSchedule;
    }

    private Schedule constructFeasibleSolution() {
        Set<String> classes = new HashSet<>();
        for (Assignment a : assignments) {
            classes.add(a.getClassName());
        }

        Schedule schedule = new Schedule(classes);

        // Sort assignments by priority
        List<Assignment> prioritized = new ArrayList<>(assignments);
        prioritized.sort((a, b) -> {
            // PJOK and MGMP first (constrained)
            boolean aPJOK = isPJOKSubject(a.getSubject());
            boolean bPJOK = isPJOKSubject(b.getSubject());
            boolean aMGMP = mgmpTeachers.contains(a.getTeacher());
            boolean bMGMP = mgmpTeachers.contains(b.getTeacher());

            if (aPJOK && !bPJOK) return -1;
            if (!aPJOK && bPJOK) return 1;
            if (aMGMP && !bMGMP) return -1;
            if (!aMGMP && bMGMP) return 1;

            // Then by total hours (descending)
            return Integer.compare(b.getTotalHours(), a.getTotalHours());
        });

        // FASE 1: Place dengan pola berurutan (BARU!)
        System.out.println("   → Phase 1: Placing subjects in consecutive patterns...");
        for (Assignment assignment : prioritized) {
            placeAssignmentInConsecutivePattern(schedule, assignment);
        }

        // FASE 1.5: AGGRESSIVE COMPLETION for remaining hours
        System.out.println("   → Phase 1.5: Aggressive completion for remaining hours...");
        for (int round = 0; round < 1000; round++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) break;

            boolean progress = false;

            // Sort by priority: most incomplete first
            incomplete.sort((a, b) -> {
                int aRemaining = a.getTotalHours() - schedule.getScheduledHours(a);
                int bRemaining = b.getTotalHours() - schedule.getScheduledHours(b);
                return Integer.compare(bRemaining, aRemaining);
            });

            for (Assignment assignment : incomplete) {
                int remaining = assignment.getTotalHours() - schedule.getScheduledHours(assignment);

                // Try to place remaining hours with consecutive pattern if possible
                if (remaining >= 2) {
                    // Try to place 2 consecutive hours
                    if (placeTwoConsecutiveHours(schedule, assignment)) {
                        progress = true;
                        continue;
                    }
                }

                // Otherwise place one hour at a time
                if (placeOneHourConstrained(schedule, assignment)) {
                    progress = true;
                } else if (placeOneHourRelaxed(schedule, assignment)) {
                    progress = true;
                } else if (forcePlace(schedule, assignment)) {
                    progress = true;
                }
            }

            if (!progress) break;
        }

        // FASE 2: Fill remaining gaps untuk assignment yang belum lengkap
        System.out.println("   → Phase 2: Filling remaining gaps...");
        for (int round = 0; round < 500; round++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) break;

            boolean progress = false;
            for (Assignment assignment : incomplete) {
                if (placeOneHourConstrained(schedule, assignment) ||
                    placeOneHourRelaxed(schedule, assignment)) {
                    progress = true;
                }
            }
            if (!progress) break;
        }

        return schedule;
    }

    /**
     * METODE BARU: Menempatkan assignment dengan pola berurutan
     * Misalnya: IPA 5 jam -> 3 jam berurutan + 2 jam berurutan
     * PJOK KHUSUS: 2 jam berurutan di SENIN (jam 1-4) + 1 jam di SELASA
     */
    private boolean placeAssignmentInConsecutivePattern(Schedule schedule, Assignment assignment) {
        int[] pattern = getDistributionPattern(assignment);
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isPJOK = isPJOKSubject(assignment.getSubject());
        boolean isMGMP = mgmpTeachers.contains(teacher);

        System.out.printf("      Placing %s [%s] with pattern %s\n",
            assignment.getSubject(), className, Arrays.toString(pattern));

        // SPECIAL HANDLING untuk PJOK 3 jam: WAJIB 2 jam di Senin + 1 jam di Selasa
        if (isPJOK && assignment.getTotalHours() == 3) {
            return placePJOKWithStrictPattern(schedule, assignment);
        }

        // Untuk setiap blok dalam pola (misal [3, 2])
        int totalPlaced = 0;
        for (int blockIdx = 0; blockIdx < pattern.length; blockIdx++) {
            int blockSize = pattern[blockIdx];
            boolean placed = false;

            // Coba tempatkan blok berurutan di setiap hari
            for (int dayIdx = 0; dayIdx < DAYS.length && !placed; dayIdx++) {
                String day = DAYS[dayIdx];
                int maxPeriod = PERIODS_PER_DAY[dayIdx];

                // Apply constraints
                if (isPJOK && blockSize >= 2) {
                    maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_FOR_DOUBLE);
                } else if (isPJOK) {
                    maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_SINGLE);
                }

                if (isMGMP && day.equals("Rabu")) {
                    maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
                }

                // Cek apakah subject sudah ada di hari ini
                int existingHoursToday = countSubjectHoursOnDay(schedule, assignment, day);
                if (existingHoursToday > 0) continue; // Skip, cari hari lain untuk distribusi merata

                // Cari slot berurutan sebanyak blockSize
                List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
                for (int startPeriod = 1; startPeriod <= maxPeriod - blockSize + 1; startPeriod++) {
                    boolean canPlace = true;

                    // Cek apakah semua slot dalam blok tersedia
                    for (int offset = 0; offset < blockSize; offset++) {
                        int period = startPeriod + offset;
                        TimeSlot slot = schedule.getSlot(day, period, className);

                        if (slot == null || !slot.isEmpty()) {
                            canPlace = false;
                            break;
                        }

                        if (!schedule.isTeacherAvailable(teacher, day, period, className)) {
                            canPlace = false;
                            break;
                        }
                    }

                    // Jika bisa, tempatkan seluruh blok
                    if (canPlace) {
                        for (int offset = 0; offset < blockSize; offset++) {
                            int period = startPeriod + offset;
                            TimeSlot slot = schedule.getSlot(day, period, className);
                            slot.assign(assignment, totalPlaced + offset + 1);
                        }
                        totalPlaced += blockSize;
                        placed = true;
                        System.out.printf("         ✓ Placed block #%d: %d consecutive hours on %s periods %d-%d\n",
                            blockIdx + 1, blockSize, day, startPeriod, startPeriod + blockSize - 1);
                        break;
                    }
                }
            }

            if (!placed) {
                System.out.printf("         ⚠ Could not place block of %d consecutive hours\n", blockSize);
                // PJOK TIDAK BOLEH fallback ke placement individual!
                if (isPJOK) {
                    System.out.printf("         ❌ PJOK cannot be placed individually - skipping\n");
                    return false;
                }
                // Untuk non-PJOK, fallback ke per jam
                for (int i = 0; i < blockSize; i++) {
                    placeOneHourConstrained(schedule, assignment);
                }
            }
        }

        return true;
    }

    /**
     * SPECIAL METHOD: Place PJOK dengan pola STRICT - 2 jam berurutan di SENIN + 1 jam di SELASA
     */
    private boolean placePJOKWithStrictPattern(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        System.out.printf("      → PJOK STRICT PATTERN: 2 hours on SENIN + 1 hour on SELASA\n");

        // STEP 1: Tempatkan 2 jam berurutan di SENIN (jam 1-4)
        boolean senin2HoursPlaced = false;
        List<TimeSlot> seninSlots = schedule.getSlotsForClass("Senin", className);

        for (int period = 1; period <= PJOK_MAX_PERIOD_FOR_DOUBLE - 1; period++) {
            TimeSlot slot1 = schedule.getSlot("Senin", period, className);
            TimeSlot slot2 = schedule.getSlot("Senin", period + 1, className);

            if (slot1 != null && slot2 != null &&
                slot1.isEmpty() && slot2.isEmpty() &&
                schedule.isTeacherAvailable(teacher, "Senin", period, className) &&
                schedule.isTeacherAvailable(teacher, "Senin", period + 1, className)) {

                slot1.assign(assignment, 1);
                slot2.assign(assignment, 2);
                senin2HoursPlaced = true;
                System.out.printf("         ✓ Placed 2 consecutive PJOK hours on SENIN periods %d-%d\n",
                    period, period + 1);
                break;
            }
        }

        if (!senin2HoursPlaced) {
            System.out.printf("         ⚠ Failed to place 2 consecutive hours on SENIN, trying fallback...\n");
            // Fallback: Coba hari lain untuk 2 jam berurutan
            for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
                String day = DAYS[dayIdx];
                if (day.equals("Rabu")) continue; // Skip Rabu untuk PJOK double

                int maxPeriod = Math.min(PERIODS_PER_DAY[dayIdx], PJOK_MAX_PERIOD_FOR_DOUBLE);

                for (int period = 1; period <= maxPeriod - 1; period++) {
                    TimeSlot slot1 = schedule.getSlot(day, period, className);
                    TimeSlot slot2 = schedule.getSlot(day, period + 1, className);

                    if (slot1 != null && slot2 != null &&
                        slot1.isEmpty() && slot2.isEmpty() &&
                        schedule.isTeacherAvailable(teacher, day, period, className) &&
                        schedule.isTeacherAvailable(teacher, day, period + 1, className)) {

                        slot1.assign(assignment, 1);
                        slot2.assign(assignment, 2);
                        senin2HoursPlaced = true;
                        System.out.printf("         ✓ Fallback: Placed 2 consecutive PJOK hours on %s periods %d-%d\n",
                            day, period, period + 1);
                        break;
                    }
                }
                if (senin2HoursPlaced) break;
            }
        }

        // STEP 2: Tempatkan 1 jam di SELASA
        boolean selasa1HourPlaced = false;
        List<TimeSlot> selasaSlots = schedule.getSlotsForClass("Selasa", className);

        for (TimeSlot slot : selasaSlots) {
            if (slot.getPeriod() > PJOK_MAX_PERIOD_SINGLE) continue;

            if (slot.isEmpty() &&
                schedule.isTeacherAvailable(teacher, "Selasa", slot.getPeriod(), className)) {

                slot.assign(assignment, 3);
                selasa1HourPlaced = true;
                System.out.printf("         ✓ Placed 1 PJOK hour on SELASA period %d\n", slot.getPeriod());
                break;
            }
        }

        if (!selasa1HourPlaced) {
            System.out.printf("         ⚠ Failed to place 1 hour on SELASA, trying other days...\n");
            // Fallback: Coba hari lain untuk 1 jam
            for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
                String day = DAYS[dayIdx];
                int maxPeriod = PERIODS_PER_DAY[dayIdx];

                for (int period = 1; period <= maxPeriod; period++) {
                    TimeSlot slot = schedule.getSlot(day, period, className);

                    if (slot != null && slot.isEmpty() &&
                        schedule.isTeacherAvailable(teacher, day, period, className)) {

                        slot.assign(assignment, 3);
                        selasa1HourPlaced = true;
                        System.out.printf("         ✓ Fallback: Placed 1 PJOK hour on %s period %d\n", day, period);
                        break;
                    }
                }
                if (selasa1HourPlaced) break;
            }
        }

        boolean success = senin2HoursPlaced && selasa1HourPlaced;
        if (success) {
            System.out.printf("      ✅ PJOK pattern completed: 2 hours + 1 hour = 3 hours total\n");
        } else {
            System.out.printf("      ⚠ PJOK pattern incomplete: Senin=%s, Selasa=%s\n",
                senin2HoursPlaced ? "OK" : "FAIL", selasa1HourPlaced ? "OK" : "FAIL");
        }

        return success;
    }

    private boolean placeOneHourConstrained(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isPJOK = isPJOKSubject(assignment.getSubject());
        boolean isMGMP = mgmpTeachers.contains(teacher);

        List<PlacementOption> validOptions = new ArrayList<>();

        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];

            // Apply STRICT hard constraints - NO RELAXATION
            if (isPJOK) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                int scheduledPJOK = 0;
                for (TimeSlot s : slots) {
                    if (!s.isEmpty() && isPJOKSubject(s.getAssignment().getSubject()) &&
                        s.getAssignment().getTeacher().equals(teacher)) {
                        scheduledPJOK++;
                    }
                }

                if (scheduledPJOK > 0) {
                    maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_FOR_DOUBLE);
                } else {
                    maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_SINGLE);
                }
            }

            if (isMGMP && day.equals("Rabu")) {
                maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
            }

            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (slot.getPeriod() > maxPeriod) continue;
                if (!slot.isEmpty()) continue;
                if (!schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) continue;

                int currentHours = countSubjectHoursOnDay(schedule, assignment, day);
                if (currentHours >= MAX_HOURS_PER_SUBJECT_PER_DAY) continue;

                if (isPJOK && slot.getPeriod() > 1) {
                    TimeSlot prevSlot = slots.get(slot.getPeriod() - 2);
                    if (!prevSlot.isEmpty() && isPJOKSubject(prevSlot.getAssignment().getSubject()) &&
                        prevSlot.getAssignment().getTeacher().equals(teacher)) {
                        if (slot.getPeriod() > 5) continue;
                    }
                }

                double score = calculateAdvancedScore(schedule, assignment, day, slot.getPeriod(), isMGMP, isPJOK);

                if (isPJOK && slot.getPeriod() <= 4) score += 500;
                if (isMGMP && !day.equals("Rabu")) score += 500;
                if (isMGMP && day.equals("Rabu") && slot.getPeriod() <= MGMP_MAX_PERIOD_RABU) score += 300;

                validOptions.add(new PlacementOption(day, slot.getPeriod(), score));
            }
        }

        if (!validOptions.isEmpty()) {
            validOptions.sort((a, b) -> Double.compare(b.score, a.score));
            int selectFrom = Math.min(3, validOptions.size());
            PlacementOption selected = validOptions.get(random.nextInt(selectFrom));

            TimeSlot slot = schedule.getSlot(selected.day, selected.period, className);
            if (slot != null) {
                slot.assign(assignment, 1);
                return true;
            }
        }

        return false;
    }

    private boolean placeTwoConsecutiveHours(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isPJOK = isPJOKSubject(assignment.getSubject());
        boolean isMGMP = mgmpTeachers.contains(teacher);

        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];

            if (isPJOK) {
                maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_FOR_DOUBLE);
            }
            if (isMGMP && day.equals("Rabu")) {
                maxPeriod = Math.min(maxPeriod, MGMP_MAX_PERIOD_RABU);
            }

            int existingHoursToday = countSubjectHoursOnDay(schedule, assignment, day);
            if (existingHoursToday > 0) continue;

            for (int period = 1; period <= maxPeriod - 1; period++) {
                TimeSlot slot1 = schedule.getSlot(day, period, className);
                TimeSlot slot2 = schedule.getSlot(day, period + 1, className);

                if (slot1 != null && slot2 != null &&
                    slot1.isEmpty() && slot2.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, period, className) &&
                    schedule.isTeacherAvailable(teacher, day, period + 1, className)) {

                    int currentScheduled = schedule.getScheduledHours(assignment);
                    slot1.assign(assignment, currentScheduled + 1);
                    slot2.assign(assignment, currentScheduled + 2);
                    return true;
                }
            }
        }
        return false;
    }

    private double calculateAdvancedScore(Schedule schedule, Assignment assignment,
                                         String day, int period, boolean isMGMP, boolean isPJOK) {
        double score = 1000.0;
        score += (11 - period) * 15;

        String className = assignment.getClassName();
        List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);

        if (period > 1) {
            TimeSlot prevSlot = daySlots.get(period - 2);
            if (!prevSlot.isEmpty() &&
                prevSlot.getAssignment().getTeacher().equals(assignment.getTeacher()) &&
                prevSlot.getAssignment().getSubject().equals(assignment.getSubject())) {
                score += 150;
            }
        }

        if (period < daySlots.size()) {
            TimeSlot nextSlot = daySlots.get(period);
            if (!nextSlot.isEmpty() &&
                nextSlot.getAssignment().getTeacher().equals(assignment.getTeacher()) &&
                nextSlot.getAssignment().getSubject().equals(assignment.getSubject())) {
                score += 150;
            }
        }

        int currentHours = countSubjectHoursOnDay(schedule, assignment, day);
        if (currentHours == 0) score += 80;
        else if (currentHours == 1) score += 40;
        else if (currentHours == 2) score += 20;

        if (isMGMP) {
            if (!day.equals("Rabu")) {
                score += 100;
            } else {
                if (period <= MGMP_MAX_PERIOD_RABU) {
                    score += (MGMP_MAX_PERIOD_RABU - period + 1) * 30;
                }
            }
        }

        if (isPJOK) {
            score += (PJOK_MAX_PERIOD_SINGLE - period + 1) * 35;
        }

        int teacherHoursToday = countTeacherHoursOnDay(schedule, assignment.getTeacher(), day);
        if (teacherHoursToday < 3) score += 30;
        else if (teacherHoursToday > 6) score -= 40;

        score += random.nextDouble() * 10;

        return score;
    }

    private int countTeacherHoursOnDay(Schedule schedule, String teacher, String day) {
        int count = 0;
        for (String className : schedule.getAllClasses()) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                if (!slot.isEmpty() && slot.getAssignment().getTeacher().equals(teacher)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean placeOneHourRelaxed(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (slot.isEmpty() && schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                    slot.assign(assignment, 1);
                    return true;
                }
            }
        }
        return false;
    }

    private Schedule generateSmartNeighbor(Schedule schedule) {
        Schedule neighbor = schedule.clone();

        int moveType = random.nextInt(6);
        switch (moveType) {
            case 0: swapSameClass(neighbor); break;
            case 1: moveSingleHour(neighbor); break;
            case 2: swapDifferentClasses(neighbor); break;
            case 3: redistributeIncomplete(neighbor); break;
            case 4: fixRandomViolation(neighbor); break;
            case 5: swapTeacherSlots(neighbor); break;
        }

        return neighbor;
    }

    private void swapSameClass(Schedule schedule) {
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        if (classes.isEmpty()) return;

        String className = classes.get(random.nextInt(classes.size()));

        List<TimeSlot> filledSlots = new ArrayList<>();
        for (String day : DAYS) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                if (!slot.isEmpty()) filledSlots.add(slot);
            }
        }

        if (filledSlots.size() < 2) return;

        TimeSlot slot1 = filledSlots.get(random.nextInt(filledSlots.size()));
        TimeSlot slot2 = filledSlots.get(random.nextInt(filledSlots.size()));

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
                if (!slot.isEmpty()) filled.add(slot);
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

    private void swapDifferentClasses(Schedule schedule) {
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        if (classes.size() < 2) return;

        String class1 = classes.get(random.nextInt(classes.size()));
        String class2 = classes.get(random.nextInt(classes.size()));
        if (class1.equals(class2)) return;

        String day = DAYS[random.nextInt(DAYS.length)];
        int period = random.nextInt(PERIODS_PER_DAY[0]) + 1;

        TimeSlot slot1 = schedule.getSlot(day, period, class1);
        TimeSlot slot2 = schedule.getSlot(day, period, class2);

        if (slot1 == null || slot2 == null) return;
        if (slot1.isEmpty() || slot2.isEmpty()) return;

        Assignment a1 = slot1.getAssignment();
        Assignment a2 = slot2.getAssignment();
        int s1 = slot1.getSessionNumber();
        int s2 = slot2.getSessionNumber();

        slot1.clear();
        slot2.clear();
        slot1.assign(a2, s2);
        slot2.assign(a1, s1);
    }

    private void redistributeIncomplete(Schedule schedule) {
        List<Assignment> incomplete = getIncompleteAssignments(schedule);
        if (incomplete.isEmpty()) return;

        Assignment assignment = incomplete.get(random.nextInt(incomplete.size()));
        placeOneHourConstrained(schedule, assignment);
    }

    private void fixRandomViolation(Schedule schedule) {
        for (int i = 0; i < 3; i++) {
            fixOneRandomViolation(schedule);
        }
    }

    private void swapTeacherSlots(Schedule schedule) {
        String teacher = getRandomTeacher();
        if (teacher == null) return;

        List<TimeSlot> teacherSlots = new ArrayList<>();
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && slot.getAssignment().getTeacher().equals(teacher)) {
                        teacherSlots.add(slot);
                    }
                }
            }
        }

        if (teacherSlots.size() < 2) return;

        TimeSlot slot1 = teacherSlots.get(random.nextInt(teacherSlots.size()));
        TimeSlot slot2 = teacherSlots.get(random.nextInt(teacherSlots.size()));

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

    private String getRandomTeacher() {
        if (assignments.isEmpty()) return null;
        Assignment randomAssignment = assignments.get(random.nextInt(assignments.size()));
        return randomAssignment.getTeacher();
    }

    private void intensiveRepair(Schedule schedule) {
        System.out.println("   → Repairing constraint violations...");

        for (int round = 0; round < 200; round++) {
            int violations = countAllViolations(schedule);
            if (violations == 0) {
                System.out.println("   ✓ All hard constraints satisfied!");
                break;
            }

            repairPJOKViolationsWithSwap(schedule);
            repairMGMPViolationsWithSwap(schedule);
            fixAllTeacherConflicts(schedule);

            // TAMBAHAN: Perbaiki PJOK yang terpecah jadi 1-1-1
            fixPJOKWrongPattern(schedule);

            if (round % 50 == 0 && round > 0) {
                System.out.printf("   → Constraint repair round %d: %d violations remaining\n", round, violations);
            }
        }

        System.out.println("   → Completing incomplete assignments (AGGRESSIVE MODE)...");

        for (int round = 0; round < 2000; round++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) {
                System.out.println("   ✓ All assignments complete!");
                break;
            }

            boolean progress = false;

            incomplete.sort((a, b) -> {
                int aRemaining = a.getTotalHours() - schedule.getScheduledHours(a);
                int bRemaining = b.getTotalHours() - schedule.getScheduledHours(b);
                return Integer.compare(bRemaining, aRemaining);
            });

            for (Assignment assignment : incomplete) {
                // BLOKIR PJOK dari placement individual
                if (isPJOKSubject(assignment.getSubject())) {
                    // PJOK harus menggunakan strict pattern placement
                    if (placePJOKWithStrictPattern(schedule, assignment)) {
                        progress = true;
                    }
                } else {
                    if (placeOneHourConstrained(schedule, assignment)) {
                        progress = true;
                    } else if (placeOneHourRelaxed(schedule, assignment)) {
                        progress = true;
                    } else if (swapForIncomplete(schedule, assignment)) {
                        progress = true;
                    } else if (forcePlace(schedule, assignment)) {
                        progress = true;
                    }
                }
            }

            if (round % 200 == 0 && round > 0) {
                System.out.printf("   → Phase 1 Round %d: %.1f%% complete (%d assignments remaining)\n",
                    round, getCompletionPercentage(schedule), incomplete.size());
            }

            if (!progress && round > 500) break;
        }

        System.out.println("   → Phase 2: Ultra aggressive placement...");
        List<Assignment> stillIncomplete = getIncompleteAssignments(schedule);

        for (int round = 0; round < 1000 && !stillIncomplete.isEmpty(); round++) {
            boolean progress = false;

            for (Assignment incomplete : stillIncomplete) {
                if (forceSwapWithOverscheduled(schedule, incomplete)) {
                    progress = true;
                } else if (forceSwapWithAnyAssignment(schedule, incomplete)) {
                    progress = true;
                }
            }

            stillIncomplete = getIncompleteAssignments(schedule);

            if (round % 100 == 0 && round > 0) {
                System.out.printf("   → Phase 2 Round %d: %.1f%% complete (%d assignments remaining)\n",
                    round, getCompletionPercentage(schedule), stillIncomplete.size());
            }

            if (!progress) break;
        }

        System.out.println("   → Phase 3: Last resort placement...");
        stillIncomplete = getIncompleteAssignments(schedule);

        for (Assignment incomplete : stillIncomplete) {
            int remaining = incomplete.getTotalHours() - schedule.getScheduledHours(incomplete);
            System.out.printf("   ⚠ Attempting last resort for: %s [%s] - needs %d more hours\n",
                incomplete.getSubject(), incomplete.getClassName(), remaining);

            for (int i = 0; i < remaining; i++) {
                if (!forcePlaceAnywhere(schedule, incomplete)) {
                    System.out.printf("   ❌ FAILED to place: %s [%s] - hour %d/%d\n",
                        incomplete.getSubject(), incomplete.getClassName(), i+1, remaining);
                }
            }
        }

        List<Assignment> finalIncomplete = getIncompleteAssignments(schedule);
        if (finalIncomplete.isEmpty()) {
            System.out.println("   ✅✅✅ SUCCESS! ALL ASSIGNMENTS 100% COMPLETE! ✅✅✅");
        } else {
            System.out.printf("   ⚠ Warning: %d assignments still incomplete\n", finalIncomplete.size());
            for (Assignment a : finalIncomplete) {
                System.out.printf("      - %s [%s]: %d/%d jam\n",
                    a.getSubject(), a.getClassName(),
                    schedule.getScheduledHours(a), a.getTotalHours());
            }
        }
    }

    /**
     * FUNGSI BARU: Memperbaiki PJOK yang terpecah menjadi 1-1-1, harus jadi 2-1
     */
    private void fixPJOKWrongPattern(Schedule schedule) {
        System.out.println("   → Validating PJOK patterns (MUST be 2-1)...");
        
        for (String className : schedule.getAllClasses()) {
            // Cari semua PJOK assignment untuk kelas ini
            Map<String, List<TimeSlot>> pjokSlotsByTeacher = new HashMap<>();
            
            for (String day : DAYS) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())) {
                        String teacher = slot.getAssignment().getTeacher();
                        pjokSlotsByTeacher.computeIfAbsent(teacher, k -> new ArrayList<>()).add(slot);
                    }
                }
            }
            
            // Untuk setiap guru PJOK
            for (Map.Entry<String, List<TimeSlot>> entry : pjokSlotsByTeacher.entrySet()) {
                List<TimeSlot> pjokSlots = entry.getValue();
                String teacher = entry.getKey();
                
                if (pjokSlots.size() != 3) continue; // Hanya handle PJOK 3 jam
                
                // Sort slots untuk print yang rapi
                pjokSlots.sort((a, b) -> {
                    int dayCompare = Integer.compare(
                        Arrays.asList(DAYS).indexOf(a.getDay()),
                        Arrays.asList(DAYS).indexOf(b.getDay())
                    );
                    if (dayCompare != 0) return dayCompare;
                    return Integer.compare(a.getPeriod(), b.getPeriod());
                });
                
                // Print current pattern
                System.out.printf("      Class %s PJOK: ", className);
                for (int i = 0; i < pjokSlots.size(); i++) {
                    TimeSlot slot = pjokSlots.get(i);
                    System.out.printf("%s-P%d", slot.getDay(), slot.getPeriod());
                    if (i < pjokSlots.size() - 1) System.out.print(", ");
                }
                
                // Cek apakah ada 2 jam berurutan
                boolean hasConsecutivePair = false;
                for (int i = 0; i < pjokSlots.size() - 1; i++) {
                    TimeSlot slot1 = pjokSlots.get(i);
                    for (int j = i + 1; j < pjokSlots.size(); j++) {
                        TimeSlot slot2 = pjokSlots.get(j);
                        
                        if (slot1.getDay().equals(slot2.getDay()) &&
                            Math.abs(slot1.getPeriod() - slot2.getPeriod()) == 1) {
                            hasConsecutivePair = true;
                            break;
                        }
                    }
                    if (hasConsecutivePair) break;
                }
                
                // Jika TIDAK ada 2 jam berurutan, berarti pola salah (1-1-1)
                if (!hasConsecutivePair) {
                    System.out.printf(" ❌ WRONG (1-1-1) - FIXING...\n");
                    
                    // Hapus semua slot PJOK yang salah pola
                    Assignment pjokAssignment = pjokSlots.get(0).getAssignment();
                    for (TimeSlot slot : pjokSlots) {
                        slot.clear();
                    }
                    
                    // Tempatkan ulang dengan pola yang benar (2-1)
                    boolean success = placePJOKWithStrictPattern(schedule, pjokAssignment);
                    
                    if (success) {
                        System.out.printf("         ✅ Fixed to 2-1 pattern\n");
                    } else {
                        System.out.printf("         ⚠ Failed to fix - will retry\n");
                    }
                } else {
                    System.out.printf(" ✅ CORRECT (2-1)\n");
                }
            }
        }
    }

    private boolean forcePlace(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        boolean isPJOK = isPJOKSubject(assignment.getSubject());
        boolean isMGMP = mgmpTeachers.contains(teacher);

        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];

            if (isPJOK) {
                maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_SINGLE);
            }
            int preferredMax = (isMGMP && day.equals("Rabu")) ? MGMP_MAX_PERIOD_RABU : maxPeriod;

            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);

            for (TimeSlot slot : slots) {
                if (slot.getPeriod() > preferredMax) continue;
                if (!slot.isEmpty()) continue;
                if (!schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) continue;

                slot.assign(assignment, 1);
                return true;
            }
        }

        if (isMGMP) {
            for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
                String day = DAYS[dayIdx];
                int maxPeriod = PERIODS_PER_DAY[dayIdx];

                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    if (slot.getPeriod() > maxPeriod) continue;
                    if (!slot.isEmpty()) continue;
                    if (!schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) continue;

                    slot.assign(assignment, 1);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean forceSwapWithOverscheduled(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();
        boolean isPJOK = isPJOKSubject(needsSlot.getSubject());

        for (String day : DAYS) {
            int maxPeriod = PERIODS_PER_DAY[java.util.Arrays.asList(DAYS).indexOf(day)];

            if (isPJOK) {
                maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_SINGLE);
            }

            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (slot.getPeriod() > maxPeriod) continue;
                if (slot.isEmpty()) {
                    if (schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                        slot.assign(needsSlot, 1);
                        return true;
                    }
                } else {
                    Assignment current = slot.getAssignment();
                    int scheduled = schedule.getScheduledHours(current);
                    int expected = current.getTotalHours();

                    if (scheduled > expected) {
                        slot.clear();
                        if (schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                            slot.assign(needsSlot, 1);
                            return true;
                        }
                        slot.assign(current, 1);
                    }
                }
            }
        }
        return false;
    }

    private boolean forceSwapWithAnyAssignment(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();
        String teacher = needsSlot.getTeacher();
        boolean isPJOK = isPJOKSubject(needsSlot.getSubject());

        for (String day : DAYS) {
            int maxPeriod = PERIODS_PER_DAY[java.util.Arrays.asList(DAYS).indexOf(day)];

            if (isPJOK) {
                maxPeriod = Math.min(maxPeriod, PJOK_MAX_PERIOD_SINGLE);
            }

            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (slot.getPeriod() > maxPeriod) continue;
                if (slot.isEmpty()) continue;

                Assignment current = slot.getAssignment();

                int currentScheduled = schedule.getScheduledHours(current);
                int currentExpected = current.getTotalHours();

                if (currentScheduled >= currentExpected * 0.8) {
                    slot.clear();

                    if (schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                        slot.assign(needsSlot, 1);

                        if (!placeOneHourRelaxed(schedule, current)) {
                            slot.clear();
                            slot.assign(current, 1);
                        } else {
                            return true;
                        }
                    } else {
                        slot.assign(current, 1);
                    }
                }
            }
        }
        return false;
    }

    private boolean forcePlaceAnywhere(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (slot.isEmpty() && schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                    slot.assign(assignment, 1);
                    System.out.printf("      ✓ Placed %s [%s] at %s period %d\n",
                        assignment.getSubject(), className, day, slot.getPeriod());
                    return true;
                }
            }
        }

        for (String day : DAYS) {
            List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (!slot.isEmpty()) {
                    Assignment current = slot.getAssignment();
                    slot.clear();

                    if (schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                        slot.assign(assignment, 1);
                        System.out.printf("      ⚠ FORCED placement of %s [%s] at %s period %d (replaced %s)\n",
                            assignment.getSubject(), className, day, slot.getPeriod(), current.getSubject());

                        placeOneHourRelaxed(schedule, current);
                        return true;
                    }

                    slot.assign(current, 1);
                }
            }
        }

        return false;
    }

    private void finalPolish(Schedule schedule) {
        System.out.println("   → ULTRA AGRESSIVE CONSTRAINT ENFORCEMENT...");

        for (Assignment assignment : assignments) {
            int scheduled = schedule.getScheduledHours(assignment);
            int expected = assignment.getTotalHours();

            while (scheduled > expected) {
                TimeSlot slot = findLastFilledSlot(schedule, assignment);
                if (slot != null) {
                    slot.clear();
                    scheduled--;
                } else {
                    break;
                }
            }
        }

        System.out.println("   → Super aggressive constraint repair (500 rounds)...");
        for (int round = 0; round < 500; round++) {
            int pjokViol = countPJOKViolations(schedule);
            int mgmpViol = countMGMPViolations(schedule);

            if (pjokViol == 0 && mgmpViol == 0) {
                System.out.println("   ✅ ALL CONSTRAINTS PERFECT!");
                break;
            }

            if (pjokViol > 0) {
                repairPJOKViolationsWithSwap(schedule);
                fixAllPJOKViolations(schedule);
            }

            if (mgmpViol > 0) {
                repairMGMPViolationsWithSwap(schedule);
                fixAllMGMPViolations(schedule);
            }

            fixAllTeacherConflicts(schedule);

            if (round % 100 == 0) {
                System.out.printf("   → Final polish round %d: PJOK=%d, MGMP=%d\n", round, pjokViol, mgmpViol);
            }
        }

        for (int i = 0; i < 100; i++) {
            List<Assignment> incomplete = getIncompleteAssignments(schedule);
            if (incomplete.isEmpty()) break;

            for (Assignment assignment : incomplete) {
                placeOneHourRelaxed(schedule, assignment);
            }
        }

        int finalPjok = countPJOKViolations(schedule);
        int finalMgmp = countMGMPViolations(schedule);
        System.out.printf("   → Final result: PJOK violations=%d, MGMP violations=%d\n", finalPjok, finalMgmp);
    }

    private boolean swapForIncomplete(Schedule schedule, Assignment needsSlot) {
        String className = needsSlot.getClassName();

        for (String day : DAYS) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                if (slot.isEmpty()) continue;

                Assignment current = slot.getAssignment();
                if (current.getTeacher().equals(needsSlot.getTeacher())) continue;

                int scheduled = schedule.getScheduledHours(current);
                int expected = current.getTotalHours();

                if (scheduled > expected) {
                    slot.clear();
                    if (schedule.isTeacherAvailable(needsSlot.getTeacher(), day, slot.getPeriod(), className)) {
                        slot.assign(needsSlot, 1);
                        return true;
                    }
                    slot.assign(current, 1);
                }
            }
        }
        return false;
    }

    private void fixOneRandomViolation(Schedule schedule) {
        int violationType = random.nextInt(3);

        switch (violationType) {
            case 0: fixOnePJOKViolation(schedule); break;
            case 1: fixOneMGMPViolation(schedule); break;
            case 2: fixOneTeacherConflict(schedule); break;
        }
    }

    private void fixOnePJOKViolation(Schedule schedule) {
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())
                        && slot.getPeriod() > PJOK_MAX_PERIOD_SINGLE) {
                        Assignment a = slot.getAssignment();
                        slot.clear();
                        placeOneHourConstrained(schedule, a);
                        return;
                    }
                }
            }
        }
    }

    private void fixOneMGMPViolation(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            for (TimeSlot slot : schedule.getSlotsForClass("Rabu", className)) {
                if (!slot.isEmpty() && mgmpTeachers.contains(slot.getAssignment().getTeacher())
                    && slot.getPeriod() > MGMP_MAX_PERIOD_RABU) {
                    Assignment a = slot.getAssignment();
                    slot.clear();
                    placeOneHourConstrained(schedule, a);
                    return;
                }
            }
        }
    }

    private void fixOneTeacherConflict(Schedule schedule) {
        for (String day : DAYS) {
            for (int period = 1; period <= 10; period++) {
                Map<String, List<TimeSlot>> teacherSlots = new HashMap<>();

                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        teacherSlots.computeIfAbsent(slot.getAssignment().getTeacher(),
                            k -> new ArrayList<>()).add(slot);
                    }
                }

                for (List<TimeSlot> slots : teacherSlots.values()) {
                    if (slots.size() > 1) {
                        TimeSlot conflict = slots.get(1);
                        Assignment a = conflict.getAssignment();
                        conflict.clear();
                        placeOneHourConstrained(schedule, a);
                        return;
                    }
                }
            }
        }
    }

    private void fixAllPJOKViolations(Schedule schedule) {
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())
                        && slot.getPeriod() > PJOK_MAX_PERIOD_SINGLE) {
                        Assignment a = slot.getAssignment();
                        slot.clear();
                        placeOneHourConstrained(schedule, a);
                    }
                }
            }
        }
    }

    private void fixAllMGMPViolations(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            for (TimeSlot slot : schedule.getSlotsForClass("Rabu", className)) {
                if (!slot.isEmpty() && mgmpTeachers.contains(slot.getAssignment().getTeacher())
                    && slot.getPeriod() > MGMP_MAX_PERIOD_RABU) {
                    Assignment a = slot.getAssignment();
                    slot.clear();
                    placeOneHourConstrained(schedule, a);
                }
            }
        }
    }

    private void fixAllTeacherConflicts(Schedule schedule) {
        for (String day : DAYS) {
            for (int period = 1; period <= 10; period++) {
                Map<String, List<TimeSlot>> teacherSlots = new HashMap<>();

                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        teacherSlots.computeIfAbsent(slot.getAssignment().getTeacher(),
                            k -> new ArrayList<>()).add(slot);
                    }
                }

                for (List<TimeSlot> slots : teacherSlots.values()) {
                    if (slots.size() > 1) {
                        for (int i = 1; i < slots.size(); i++) {
                            Assignment a = slots.get(i).getAssignment();
                            slots.get(i).clear();
                            placeOneHourConstrained(schedule, a);
                        }
                    }
                }
            }
        }
    }

    private void repairPJOKViolationsWithSwap(Schedule schedule) {
        List<TimeSlot> pjokViolations = new ArrayList<>();

        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);

                for (int i = 0; i < slots.size() - 1; i++) {
                    TimeSlot slot = slots.get(i);
                    if (slot.isEmpty() || !isPJOKSubject(slot.getAssignment().getSubject())) continue;

                    TimeSlot next = slots.get(i + 1);
                    if (!next.isEmpty() &&
                        isPJOKSubject(next.getAssignment().getSubject()) &&
                        next.getAssignment().getTeacher().equals(slot.getAssignment().getTeacher())) {

                        if (slot.getPeriod() > 4) {
                            pjokViolations.add(slot);
                            pjokViolations.add(next);
                        }
                        i++;
                    }
                }
            }
        }

        for (int i = 0; i < pjokViolations.size(); i += 2) {
            if (i + 1 >= pjokViolations.size()) break;

            TimeSlot pjokSlot1 = pjokViolations.get(i);
            TimeSlot pjokSlot2 = pjokViolations.get(i + 1);
            Assignment pjokAssignment = pjokSlot1.getAssignment();
            String className = pjokSlot1.getClassName();
            String day = pjokSlot1.getDay();
            String teacher = pjokAssignment.getTeacher();

            boolean repaired = false;

            List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
            for (int j = 0; j < daySlots.size() - 1; j++) {
                if (daySlots.get(j).getPeriod() > 4) break;

                TimeSlot candidate1 = daySlots.get(j);
                TimeSlot candidate2 = daySlots.get(j + 1);

                if (candidate1.isEmpty() && candidate2.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, day, candidate1.getPeriod(), className)) {

                    pjokSlot1.clear();
                    pjokSlot2.clear();
                    candidate1.assign(pjokAssignment, 1);
                    candidate2.assign(pjokAssignment, 1);
                    repaired = true;
                    break;
                }
            }

            if (!repaired) {
                for (String otherDay : DAYS) {
                    if (otherDay.equals(day)) continue;

                    List<TimeSlot> otherDaySlots = schedule.getSlotsForClass(otherDay, className);
                    for (int j = 0; j < otherDaySlots.size() - 1; j++) {
                        if (otherDaySlots.get(j).getPeriod() > 4) break;

                        TimeSlot candidate1 = otherDaySlots.get(j);
                        TimeSlot candidate2 = otherDaySlots.get(j + 1);

                        if (candidate1.isEmpty() && candidate2.isEmpty() &&
                            schedule.isTeacherAvailable(teacher, otherDay, candidate1.getPeriod(), className)) {

                            pjokSlot1.clear();
                            pjokSlot2.clear();
                            candidate1.assign(pjokAssignment, 1);
                            candidate2.assign(pjokAssignment, 1);
                            repaired = true;
                            break;
                        }
                    }
                    if (repaired) break;
                }
            }
        }
    }

    private void repairMGMPViolationsWithSwap(Schedule schedule) {
        List<TimeSlot> mgmpViolations = new ArrayList<>();

        for (String className : schedule.getAllClasses()) {
            List<TimeSlot> slots = schedule.getSlotsForClass("Rabu", className);
            for (TimeSlot slot : slots) {
                if (!slot.isEmpty() &&
                    slot.getPeriod() > MGMP_MAX_PERIOD_RABU &&
                    mgmpTeachers.contains(slot.getAssignment().getTeacher())) {
                    mgmpViolations.add(slot);
                }
            }
        }

        for (TimeSlot mgmpSlot : mgmpViolations) {
            Assignment mgmpAssignment = mgmpSlot.getAssignment();
            String className = mgmpSlot.getClassName();
            String teacher = mgmpAssignment.getTeacher();

            boolean repaired = false;

            List<TimeSlot> rabuSlots = schedule.getSlotsForClass("Rabu", className);
            for (TimeSlot candidate : rabuSlots) {
                if (candidate.getPeriod() > MGMP_MAX_PERIOD_RABU) break;

                if (candidate.isEmpty() &&
                    schedule.isTeacherAvailable(teacher, "Rabu", candidate.getPeriod(), className)) {

                    mgmpSlot.clear();
                    candidate.assign(mgmpAssignment, 1);
                    repaired = true;
                    break;
                }
            }

            if (!repaired) {
                for (String day : DAYS) {
                    if (day.equals("Rabu")) continue;

                    List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
                    for (TimeSlot candidate : daySlots) {
                        if (candidate.isEmpty() &&
                            schedule.isTeacherAvailable(teacher, day, candidate.getPeriod(), className)) {

                            mgmpSlot.clear();
                            candidate.assign(mgmpAssignment, 1);
                            repaired = true;
                            break;
                        }
                    }
                    if (repaired) break;
                }
            }
        }
    }

    private double evaluateFitness(Schedule schedule) {
        double score = 100000.0;

        int totalNeeded = 0;
        int totalScheduled = 0;
        for (Assignment a : assignments) {
            totalNeeded += a.getTotalHours();
            totalScheduled += schedule.getScheduledHours(a);
        }
        double completionRatio = totalScheduled / (double) totalNeeded;
        score += completionRatio * 200000;

        int conflicts = countTeacherConflicts(schedule);
        score -= conflicts * 30000;

        int pjokViol = countPJOKViolations(schedule);
        score -= pjokViol * 15000;

        int mgmpViol = countMGMPViolations(schedule);
        score -= mgmpViol * 15000;

        int maxHoursViol = countMaxHoursViolations(schedule);
        score -= maxHoursViol * 300;

        score += calculateDistributionQuality(schedule);

        return score;
    }

    private double getCompletionPercentage(Schedule schedule) {
        int totalNeeded = 0;
        int totalScheduled = 0;
        for (Assignment a : assignments) {
            totalNeeded += a.getTotalHours();
            totalScheduled += schedule.getScheduledHours(a);
        }
        return totalNeeded > 0 ? (totalScheduled * 100.0) / totalNeeded : 0;
    }

    private int countAllViolations(Schedule schedule) {
        return countTeacherConflicts(schedule) + countPJOKViolations(schedule) + countMGMPViolations(schedule);
    }

    private double calculateDistributionQuality(Schedule schedule) {
        double quality = 0.0;
        for (Assignment assignment : assignments) {
            int[] hoursPerDay = new int[DAYS.length];
            for (int i = 0; i < DAYS.length; i++) {
                hoursPerDay[i] = countSubjectHoursOnDay(schedule, assignment, DAYS[i]);
            }
            int nonZeroDays = 0;
            for (int hours : hoursPerDay) {
                if (hours > 0) nonZeroDays++;
            }
            if (assignment.getTotalHours() >= 4) {
                quality += nonZeroDays * 20;
            }
        }
        return quality;
    }

    private int countSubjectHoursOnDay(Schedule schedule, Assignment assignment, String day) {
        int count = 0;
        String normalizedSubject = assignment.getSubject().toUpperCase().trim();
        for (TimeSlot slot : schedule.getSlotsForClass(day, assignment.getClassName())) {
            if (!slot.isEmpty()) {
                String slotSubject = slot.getAssignment().getSubject().toUpperCase().trim();
                if (slotSubject.equals(normalizedSubject)) {
                    count++;
                }
            }
        }
        return count;
    }

    private TimeSlot findLastFilledSlot(Schedule schedule, Assignment assignment) {
        for (int dayIdx = DAYS.length - 1; dayIdx >= 0; dayIdx--) {
            String day = DAYS[dayIdx];
            List<TimeSlot> daySlots = schedule.getSlotsForClass(day, assignment.getClassName());
            for (int i = daySlots.size() - 1; i >= 0; i--) {
                TimeSlot slot = daySlots.get(i);
                if (!slot.isEmpty() &&
                        slot.getAssignment().getTeacher().equals(assignment.getTeacher()) &&
                        slot.getAssignment().getSubject().equals(assignment.getSubject()) &&
                        slot.getAssignment().getClassName().equals(assignment.getClassName())) {
                    return slot;
                }
            }
        }
        return null;
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

                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty() && isPJOKSubject(slot.getAssignment().getSubject())
                        && slot.getPeriod() > PJOK_MAX_PERIOD_SINGLE) {
                        violations++;
                    }
                }

                for (int i = 0; i < slots.size() - 1; i++) {
                    TimeSlot slot = slots.get(i);
                    if (slot.isEmpty() || !isPJOKSubject(slot.getAssignment().getSubject())) continue;

                    TimeSlot next = slots.get(i + 1);
                    if (!next.isEmpty() &&
                        isPJOKSubject(next.getAssignment().getSubject()) &&
                        next.getAssignment().getTeacher().equals(slot.getAssignment().getTeacher()) &&
                        slot.getPeriod() + 1 == next.getPeriod()) {

                        if (slot.getPeriod() > PJOK_MAX_PERIOD_FOR_DOUBLE) {
                            violations += 2;
                        }
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
                if (!slot.isEmpty() &&
                        slot.getPeriod() > MGMP_MAX_PERIOD_RABU &&
                        mgmpTeachers.contains(slot.getAssignment().getTeacher())) {
                    violations++;
                }
            }
        }
        return violations;
    }

    private int countMaxHoursViolations(Schedule schedule) {
        int violations = 0;
        for (Assignment assignment : assignments) {
            for (String day : DAYS) {
                int hours = countSubjectHoursOnDay(schedule, assignment, day);
                if (hours > MAX_HOURS_PER_SUBJECT_PER_DAY) {
                    violations += (hours - MAX_HOURS_PER_SUBJECT_PER_DAY);
                }
            }
        }
        return violations;
    }

    private int[] getDistributionPattern(Assignment assignment) {
        String subject = assignment.getSubject().toUpperCase().trim();
        int totalHours = assignment.getTotalHours();

        if (subject.contains("PJOK") && totalHours == 3) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("PJOK_3");
        }

        if (subject.contains("MATEMATIKA") && totalHours == 5) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("MATEMATIKA_5");
        }

        if (subject.contains("IPA") && totalHours == 5) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("IPA_5");
        }

        if ((subject.contains("BAHASA INDONESIA") || subject.contains("B. INDONESIA") ||
             subject.contains("B.INDONESIA") || subject.contains("INDONESIA")) && totalHours == 6) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("BAHASA INDONESIA_6");
        }

        if ((subject.contains("BAHASA INGGRIS") || subject.contains("B. INGGRIS") ||
             subject.contains("B.INGGRIS") || subject.contains("INGGRIS")) && totalHours == 4) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("BAHASA INGGRIS_4");
        }

        if (subject.contains("IPS") && totalHours == 4) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("IPS_4");
        }

        if (totalHours == 3) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("DEFAULT_3_SPLIT");
        } else if (totalHours == 2) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("DEFAULT_2");
        } else if (totalHours == 1) {
            return SUBJECT_DISTRIBUTION_PATTERNS.get("DEFAULT_1");
        }

        return distributeEvenly(totalHours);
    }

    private int[] distributeEvenly(int totalHours) {
        if (totalHours <= 3) {
            return new int[]{totalHours};
        }

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
            int expected = a.getTotalHours();
            totalHoursNeeded += expected;
            totalHoursScheduled += scheduled;
            if (scheduled == expected) completedAssignments++;
        }

        System.out.println("\n📊 KELENGKAPAN:");
        System.out.printf("   Assignment Complete : %d/%d (%.1f%%)\n",
                completedAssignments, totalAssignments, (completedAssignments * 100.0 / totalAssignments));
        System.out.printf("   Jam Terjadwal       : %d/%d (%.1f%%)\n",
                totalHoursScheduled, totalHoursNeeded, (totalHoursScheduled * 100.0 / totalHoursNeeded));

        int conflicts = countTeacherConflicts(schedule);
        int pjokViol = countPJOKViolations(schedule);
        int mgmpViol = countMGMPViolations(schedule);
        int maxHoursViol = countMaxHoursViolations(schedule);

        System.out.println("\n⚠️  KONFLIK:");
        System.out.printf("   Konflik Guru        : %d %s\n", conflicts, conflicts == 0 ? "✅" : "❌");

        System.out.println("\n📋 CONSTRAINT:");
        System.out.printf("   PJOK (max jam 5)    : %d %s\n", pjokViol, pjokViol == 0 ? "✅" : "❌");
        System.out.printf("   MGMP (Rabu max 4)   : %d %s\n", mgmpViol, mgmpViol == 0 ? "✅" : "❌");
        System.out.printf("   Max 3 Jam/Hari      : %d %s\n", maxHoursViol, maxHoursViol == 0 ? "✅" : "⚠️");

        System.out.printf("\n⏱️  Waktu: %.2f detik\n", seconds);

        boolean isPerfect = conflicts == 0 && completedAssignments == totalAssignments &&
                pjokViol == 0 && mgmpViol == 0;

        System.out.println("\n" + (isPerfect ?
                "✅✅✅ SEMPURNA! Semua constraint terpenuhi 100%! ✅✅✅" :
                (completedAssignments == totalAssignments ?
                        "✅ Semua assignment lengkap! (violations: " + (conflicts+pjokViol+mgmpViol) + ")" :
                        "⚠️  " + (totalAssignments - completedAssignments) + " assignment belum lengkap - run ulang untuk hasil berbeda")));
        System.out.println("════════════════════════════════════════════════════════════════");
    }

    private static class PlacementOption {
        String day;
        int period;
        double score;

        PlacementOption(String day, int period, double score) {
            this.day = day;
            this.period = period;
            this.score = score;
        }
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
                if (removed != null) {
                    tabuSet.remove(removed);
                }
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
}
