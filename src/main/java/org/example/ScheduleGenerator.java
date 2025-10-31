package org.example;

import java.util.*;

/**
 * Generator jadwal OPTIMAL dengan Hill Climbing + Iterated Local Search (ILS)
 * Menggunakan multiple neighborhood operators dan perturbation untuk escape local optima
 */
public class ScheduleGenerator {
    private List<Assignment> assignments;
    private Random random = new Random();

    private static final Set<String> MGMP_SUBJECTS = new HashSet<>(Arrays.asList(
            "SKI", "B.ARAB", "AQIDAH AKHLAK", "QURDITS", "FIQIH",
            "B. ARAB", "AQIDAH A.", "AL-QUR'AN HADITS", "AL QUR'AN HADITS",
            "AQIDAH A."
    ));

    private static final Map<String, int[]> SUBJECT_PATTERNS = new HashMap<>();
    static {
        SUBJECT_PATTERNS.put("MATEMATIKA", new int[]{3, 2});
        SUBJECT_PATTERNS.put("IPA", new int[]{3, 2});
        SUBJECT_PATTERNS.put("B.INDONESIA", new int[]{2, 2, 2});
        SUBJECT_PATTERNS.put("B. INDONESIA", new int[]{2, 2, 2});
        SUBJECT_PATTERNS.put("B.INGGRIS", new int[]{2, 2});
        SUBJECT_PATTERNS.put("B. INGGRIS", new int[]{2, 2});
        SUBJECT_PATTERNS.put("IPS", new int[]{2, 2});
    }

    private static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
    private static final int[] PERIODS_PER_DAY = {10, 10, 10, 9, 8};

    public ScheduleGenerator(List<Assignment> assignments) {
        this.assignments = new ArrayList<>(assignments);
    }

    public Schedule generate() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║   ALGORITMA OPTIMAL - HILL CLIMBING + ITERATED LOCAL SEARCH  ║");
        System.out.println("║          DENGAN COMPLETE COVERAGE GUARANTEE                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println("Total assignments: " + assignments.size());

        Schedule bestSchedule = null;
        int bestFitness = Integer.MIN_VALUE;
        long startTime = System.currentTimeMillis();

        // PHASE 1: GREEDY CONSTRUCTION dengan constraint-aware placement
        System.out.println("\n[PHASE 1] Greedy Construction with Constraint-Aware Placement...");
        Schedule current = buildConstraintAwareGreedy();

        // PHASE 1.5: FORCE COMPLETE ALL ASSIGNMENTS
        System.out.println("\n[PHASE 1.5] Forcing Complete Coverage for All Assignments...");
        forceCompleteAllAssignments(current);

        int currentFitness = evaluateFitness(current);
        bestSchedule = current.clone();
        bestFitness = currentFitness;

        printScheduleStatus("Initial", current);

        // PHASE 2: HILL CLIMBING dengan Complete Coverage Priority
        System.out.println("\n[PHASE 2] Hill Climbing with Complete Coverage Priority...");
        int noImprovementCount = 0;
        int maxNoImprovement = 500;
        int iteration = 0;
        int hillClimbingImprovements = 0;

        while (noImprovementCount < maxNoImprovement && iteration < 15000) {
            iteration++;
            boolean improved = false;

            // Generate more neighbors with focus on complete coverage
            Schedule[] neighbors = generateCompleteCoverageNeighbors(current, 20);

            for (Schedule neighbor : neighbors) {
                int neighborFitness = evaluateFitness(neighbor);

                // Hill Climbing: Accept only improvements
                if (neighborFitness > currentFitness) {
                    current = neighbor;
                    currentFitness = neighborFitness;
                    improved = true;
                    hillClimbingImprovements++;
                    noImprovementCount = 0;

                    if (currentFitness > bestFitness) {
                        bestSchedule = current.clone();
                        bestFitness = currentFitness;

                        if (iteration % 300 == 0 || getPenalty(bestSchedule) == 0) {
                            printScheduleStatus("Iteration " + iteration, bestSchedule);
                        }

                        if (getPenalty(bestSchedule) == 0) {
                            System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
                            System.out.println("║              ✓✓✓ SOLUSI SEMPURNA! ✓✓✓                       ║");
                            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
                            break;
                        }
                    }
                    break; // Accept first improvement
                }
            }

            if (!improved) {
                noImprovementCount++;

                // Every 30 iterations, force complete all assignments
                if (noImprovementCount % 30 == 0) {
                    Schedule forced = current.clone();
                    forceCompleteAllAssignments(forced);
                    int forcedFitness = evaluateFitness(forced);
                    if (forcedFitness > currentFitness) {
                        current = forced;
                        currentFitness = forcedFitness;
                        improved = true;
                        noImprovementCount = 0;
                    }
                }

                // Try aggressive swapping every 50 iterations without improvement
                if (noImprovementCount % 50 == 0) {
                    Schedule swapped = current.clone();
                    aggressiveSwapOptimization(swapped);
                    forceCompleteAllAssignments(swapped);
                    int swappedFitness = evaluateFitness(swapped);
                    if (swappedFitness > currentFitness) {
                        current = swapped;
                        currentFitness = swappedFitness;
                        improved = true;
                        noImprovementCount = 0;
                    }
                }
            }
        }

        System.out.printf("\nHill Climbing completed: %d improvements in %d iterations\n",
                hillClimbingImprovements, iteration);

        // PHASE 3: ITERATED LOCAL SEARCH with Complete Coverage
        if (getPenalty(bestSchedule) > 0) {
            System.out.println("\n[PHASE 3] Iterated Local Search with Complete Coverage Enforcement...");
            int ilsIterations = 40;
            int ilsImprovements = 0;

            for (int ils = 0; ils < ilsIterations; ils++) {
                // Perturbation: shake the current solution with varying strength
                int perturbStrength = 3 + (ils / 5);
                Schedule perturbed = perturbSolution(bestSchedule, perturbStrength);

                // Force complete coverage after perturbation
                forceCompleteAllAssignments(perturbed);

                // Apply aggressive swapping
                aggressiveSwapOptimization(perturbed);

                // Ensure still complete
                forceCompleteAllAssignments(perturbed);

                // Local search on perturbed solution
                Schedule optimized = intensiveLocalSearchWithCompletion(perturbed, 400);
                int optimizedFitness = evaluateFitness(optimized);

                if (optimizedFitness > bestFitness) {
                    bestSchedule = optimized.clone();
                    bestFitness = optimizedFitness;
                    ilsImprovements++;

                    System.out.printf("[ILS %d] NEW BEST! Penalty=%d (Incomplete=%d)\n",
                            ils + 1, getPenalty(bestSchedule), countIncompleteHours(bestSchedule));
                    printScheduleStatus("ILS " + (ils + 1), bestSchedule);

                    if (getPenalty(bestSchedule) == 0) {
                        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
                        System.out.println("║              ✓✓✓ SOLUSI SEMPURNA! ✓✓✓                       ║");
                        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
                        break;
                    }
                }
            }

            System.out.printf("ILS completed: %d improvements\n", ilsImprovements);
        }

        // PHASE 4: FINAL COMPLETE COVERAGE OPTIMIZATION
        if (getPenalty(bestSchedule) > 0) {
            System.out.println("\n[PHASE 4] Final Complete Coverage Optimization...");
            bestSchedule = finalCompleteCoverageOptimization(bestSchedule);
            bestFitness = evaluateFitness(bestSchedule);
        }

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                        HASIL AKHIR                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.printf("Best Fitness      : %d\n", bestFitness);
        System.out.printf("Penalty           : %d\n", getPenalty(bestSchedule));
        System.out.printf("Incomplete Hours  : %d\n", countIncompleteHours(bestSchedule));
        System.out.printf("Waktu Eksekusi    : %.2f detik\n", seconds);

        printDetailedMetrics(bestSchedule);

        return bestSchedule;
    }

    /**
     * FORCE COMPLETE ALL ASSIGNMENTS - Pastikan setiap assignment terjadwal lengkap
     */
    private void forceCompleteAllAssignments(Schedule schedule) {
        int maxAttempts = 3;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean allComplete = true;

            // First pass: Add missing hours
            for (Assignment assignment : assignments) {
                int scheduled = schedule.getScheduledHours(assignment);
                int expected = assignment.getTotalHours();

                if (scheduled < expected) {
                    allComplete = false;
                    int needed = expected - scheduled;

                    for (int i = 0; i < needed; i++) {
                        TimeSlot slot = findAnyAvailableSlot(schedule, assignment);
                        if (slot != null) {
                            slot.assign(assignment, 99);
                        } else {
                            // No empty slot available, need to swap
                            boolean swapped = swapToMakeRoom(schedule, assignment);
                            if (swapped) {
                                // Try again after swap
                                slot = findAnyAvailableSlot(schedule, assignment);
                                if (slot != null) {
                                    slot.assign(assignment, 99);
                                }
                            }
                        }
                    }
                }
            }

            // Second pass: Remove excess hours
            for (Assignment assignment : assignments) {
                int scheduled = schedule.getScheduledHours(assignment);
                int expected = assignment.getTotalHours();

                if (scheduled > expected) {
                    int excess = scheduled - expected;
                    for (int i = 0; i < excess; i++) {
                        TimeSlot slot = findWorstFilledSlot(schedule, assignment);
                        if (slot != null) {
                            slot.clear();
                        }
                    }
                }
            }

            if (allComplete) {
                break;
            }
        }
    }

    /**
     * Find any available slot (relaxed constraints for forcing completion)
     */
    private TimeSlot findAnyAvailableSlot(Schedule schedule, Assignment assignment) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        String subject = assignment.getSubject().toUpperCase();

        // First try: slots without constraint violations
        List<TimeSlot> candidates = new ArrayList<>();
        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty()) {
                    if (schedule.isTeacherAvailable(teacher, day, period, className)) {
                        // Check hard constraints
                        boolean valid = true;
                        if (day.equals("Rabu") && isMGMPSubject(subject) && period >= 6) {
                            valid = false;
                        }
                        if (valid) {
                            candidates.add(slot);
                        }
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            // Prefer earlier slots
            candidates.sort((a, b) -> {
                int dayDiff = Arrays.asList(DAYS).indexOf(a.getDay()) - Arrays.asList(DAYS).indexOf(b.getDay());
                if (dayDiff != 0) return dayDiff;
                return Integer.compare(a.getPeriod(), b.getPeriod());
            });
            return candidates.get(0);
        }

        // Second try: any empty slot where teacher is available (ignore MGMP constraint if desperate)
        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];

            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty()) {
                    if (schedule.isTeacherAvailable(teacher, day, period, className)) {
                        return slot;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Swap to make room for an assignment that needs slots
     */
    private boolean swapToMakeRoom(Schedule schedule, Assignment needsSlot) {
        String needsClassName = needsSlot.getClassName();
        String needsTeacher = needsSlot.getTeacher();

        // Find assignments in the same class that are over-scheduled or can be moved
        for (Assignment candidate : assignments) {
            if (!candidate.getClassName().equals(needsClassName)) {
                continue;
            }

            int scheduled = schedule.getScheduledHours(candidate);
            int expected = candidate.getTotalHours();

            // If this assignment is over-scheduled, remove one instance
            if (scheduled > expected) {
                TimeSlot slot = findWorstFilledSlot(schedule, candidate);
                if (slot != null) {
                    slot.clear();
                    return true;
                }
            }
        }

        // Try to swap with assignments from other classes
        for (String day : DAYS) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, needsClassName)) {
                if (!slot.isEmpty()) {
                    Assignment current = slot.getAssignment();

                    // Try to find alternative slot for current assignment
                    TimeSlot alternative = findAlternativeSlot(schedule, current, slot);
                    if (alternative != null) {
                        int sessionNum = slot.getSessionNumber();
                        slot.clear();
                        alternative.assign(current, sessionNum);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Find alternative slot for an assignment (for swapping)
     */
    private TimeSlot findAlternativeSlot(Schedule schedule, Assignment assignment, TimeSlot excludeSlot) {
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();

        for (String day : DAYS) {
            List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
            for (TimeSlot slot : daySlots) {
                if (slot.equals(excludeSlot)) continue;
                if (slot.isEmpty() && schedule.isTeacherAvailable(teacher, day, slot.getPeriod(), className)) {
                    return slot;
                }
            }
        }

        return null;
    }

    /**
     * Generate neighbors with complete coverage priority
     */
    private Schedule[] generateCompleteCoverageNeighbors(Schedule current, int count) {
        Schedule[] neighbors = new Schedule[count];

        // Identify incomplete assignments
        List<Assignment> incomplete = new ArrayList<>();
        for (Assignment a : assignments) {
            int scheduled = current.getScheduledHours(a);
            int expected = a.getTotalHours();
            if (scheduled != expected) {
                incomplete.add(a);
            }
        }

        for (int i = 0; i < count; i++) {
            Schedule neighbor = current.clone();

            // Apply different strategies
            int strategy = i % 8;
            switch (strategy) {
                case 0:
                case 1:
                    // Focus on completing incomplete assignments
                    if (!incomplete.isEmpty()) {
                        Assignment toComplete = incomplete.get(random.nextInt(incomplete.size()));
                        completeAssignment(neighbor, toComplete);
                    }
                    break;
                case 2:
                    moveRandomAssignment(neighbor);
                    break;
                case 3:
                    swapTwoAssignments(neighbor);
                    break;
                case 4:
                    fixTeacherConflicts(neighbor);
                    break;
                case 5:
                    fixConstraintViolations(neighbor);
                    break;
                case 6:
                    balanceHours(neighbor);
                    break;
                case 7:
                    compactSchedule(neighbor);
                    break;
            }

            // Ensure completeness
            forceCompleteAllAssignments(neighbor);

            neighbors[i] = neighbor;
        }

        return neighbors;
    }

    /**
     * Complete a specific assignment
     */
    private void completeAssignment(Schedule schedule, Assignment assignment) {
        int scheduled = schedule.getScheduledHours(assignment);
        int expected = assignment.getTotalHours();

        if (scheduled < expected) {
            // Add missing hours
            int needed = expected - scheduled;
            for (int i = 0; i < needed; i++) {
                TimeSlot slot = findAnyAvailableSlot(schedule, assignment);
                if (slot != null) {
                    slot.assign(assignment, 99);
                } else {
                    swapToMakeRoom(schedule, assignment);
                    slot = findAnyAvailableSlot(schedule, assignment);
                    if (slot != null) {
                        slot.assign(assignment, 99);
                    }
                }
            }
        } else if (scheduled > expected) {
            // Remove excess hours
            int excess = scheduled - expected;
            for (int i = 0; i < excess; i++) {
                TimeSlot slot = findWorstFilledSlot(schedule, assignment);
                if (slot != null) {
                    slot.clear();
                }
            }
        }
    }

    /**
     * Intensive local search with completion guarantee
     */
    private Schedule intensiveLocalSearchWithCompletion(Schedule initial, int maxIterations) {
        Schedule current = initial.clone();
        int currentFitness = evaluateFitness(current);

        for (int i = 0; i < maxIterations; i++) {
            Schedule[] neighbors = generateCompleteCoverageNeighbors(current, 15);

            boolean improved = false;
            for (Schedule neighbor : neighbors) {
                int neighborFitness = evaluateFitness(neighbor);
                if (neighborFitness > currentFitness) {
                    current = neighbor;
                    currentFitness = neighborFitness;
                    improved = true;
                    break;
                }
            }

            if (!improved) {
                // Try more aggressive moves with forced completion
                Schedule aggressive = current.clone();
                aggressiveSwapOptimization(aggressive);
                forceCompleteAllAssignments(aggressive);
                fixConstraintViolations(aggressive);
                forceCompleteAllAssignments(aggressive);

                int aggressiveFitness = evaluateFitness(aggressive);
                if (aggressiveFitness > currentFitness) {
                    current = aggressive;
                    currentFitness = aggressiveFitness;
                    improved = true;
                }
            }

            if (!improved) {
                break; // Local optimum reached
            }
        }

        return current;
    }

    /**
     * Final complete coverage optimization - last resort to complete everything
     */
    private Schedule finalCompleteCoverageOptimization(Schedule schedule) {
        Schedule optimized = schedule.clone();

        System.out.println("   → Forcing complete coverage for all assignments...");

        // Multiple passes to ensure completeness
        for (int pass = 0; pass < 100; pass++) {
            int incompleteBefore = countIncompleteHours(optimized);

            // Force complete all
            forceCompleteAllAssignments(optimized);

            // Fix any conflicts that might have been created
            fixTeacherConflicts(optimized);

            // Force complete again
            forceCompleteAllAssignments(optimized);

            // Fix constraints
            fixConstraintViolations(optimized);

            // Final force complete
            forceCompleteAllAssignments(optimized);

            int incompleteAfter = countIncompleteHours(optimized);

            if (incompleteAfter == 0) {
                System.out.println("   ✓ All assignments completed!");
                break;
            }

            if (incompleteAfter >= incompleteBefore && pass > 10) {
                // Try aggressive swapping
                aggressiveSwapOptimization(optimized);
                forceCompleteAllAssignments(optimized);
            }

            if (pass % 20 == 0) {
                System.out.printf("   → Pass %d: %d incomplete hours remaining\n", pass, incompleteAfter);
            }
        }

        return optimized;
    }

    /**
     * BUILD CONSTRAINT-AWARE GREEDY - Initial solution dengan perhatian khusus pada constraints
     */
    private Schedule buildConstraintAwareGreedy() {
        Set<String> classes = new HashSet<>();
        for (Assignment a : assignments) {
            classes.add(a.getClassName());
        }

        Schedule schedule = new Schedule(classes);

        // Expand ke sessions
        List<SessionSlot> sessions = expandToSessions();

        // Sort by priority: PJOK first, then MGMP, then by difficulty
        sessions.sort((a, b) -> Integer.compare(b.priority, a.priority));

        // Place sessions with constraint awareness
        for (SessionSlot session : sessions) {
            placeSessionWithConstraints(schedule, session);
        }

        // Fill remaining hours exactly
        fillRemainingHoursExactly(schedule);

        return schedule;
    }

    private List<SessionSlot> expandToSessions() {
        List<SessionSlot> sessions = new ArrayList<>();
        for (Assignment assignment : assignments) {
            int totalHours = assignment.getTotalHours();
            int[] pattern = getOptimalPattern(assignment);
            int patternSum = Arrays.stream(pattern).sum();
            if (patternSum != totalHours) {
                pattern = createExactPattern(totalHours);
            }
            for (int i = 0; i < pattern.length; i++) {
                int priority = calculatePriority(assignment, pattern[i]);
                sessions.add(new SessionSlot(assignment, i + 1, pattern[i], priority));
            }
        }
        return sessions;
    }

    private int[] createExactPattern(int totalHours) {
        if (totalHours == 6) return new int[]{2, 2, 2};
        if (totalHours == 5) return new int[]{3, 2};
        if (totalHours == 4) return new int[]{2, 2};
        if (totalHours == 3) return new int[]{2, 1};
        if (totalHours == 2) return new int[]{1, 1};
        if (totalHours == 1) return new int[]{1};
        List<Integer> parts = new ArrayList<>();
        int remaining = totalHours;
        while (remaining > 0) {
            if (remaining >= 3) { parts.add(3); remaining -= 3; }
            else if (remaining >= 2) { parts.add(2); remaining -= 2; }
            else { parts.add(1); remaining -= 1; }
        }
        return parts.stream().mapToInt(Integer::intValue).toArray();
    }

    private int calculatePriority(Assignment assignment, int sessionLength) {
        int priority = 0;
        String subject = assignment.getSubject().toUpperCase();
        if (subject.contains("PJOK") && sessionLength == 2) priority += 10000;
        else if (subject.contains("PJOK")) priority += 5000;
        if (isMGMPSubject(subject)) priority += 3000;
        priority += sessionLength * 100;
        if (sessionLength >= 3) priority += 500;
        return priority;
    }

    private void placeSessionWithConstraints(Schedule schedule, SessionSlot session) {
        List<PlacementOption> validOptions = new ArrayList<>();
        String subject = session.assignment.getSubject().toUpperCase();
        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];
            for (int period = 1; period <= maxPeriod - session.length + 1; period++) {
                if (!canPlaceSession(schedule, session, day, period)) continue;
                if (subject.contains("PJOK") && session.length == 2 && period + session.length - 1 > 5) continue;
                if (day.equals("Rabu") && isMGMPSubject(subject) && period + session.length - 1 >= 6) continue;
                int score = evaluatePlacementQuality(schedule, session, day, period);
                validOptions.add(new PlacementOption(day, period, score));
            }
        }
        if (validOptions.isEmpty()) validOptions = findRelaxedPlacements(schedule, session);
        if (!validOptions.isEmpty()) {
            validOptions.sort((a, b) -> Integer.compare(b.score, a.score));
            PlacementOption best = validOptions.get(0);
            placeSession(schedule, session, best.day, best.startPeriod);
        }
    }

    private boolean canPlaceSession(Schedule schedule, SessionSlot session, String day, int startPeriod) {
        String className = session.assignment.getClassName();
        String teacher = session.assignment.getTeacher();

        // Batasi semua mata pelajaran maksimum 3 jam per hari
        String normalizedSubject = session.assignment.getSubject().toUpperCase().trim();
        int existingSubjectHours = 0;
        for (TimeSlot s : schedule.getSlotsForClass(day, className)) {
            if (!s.isEmpty()) {
                String sSubj = s.getAssignment().getSubject().toUpperCase().trim();
                if (sSubj.equals(normalizedSubject) || sSubj.contains(normalizedSubject) || normalizedSubject.contains(sSubj)) {
                    existingSubjectHours++;
                }
            }
        }
        if (existingSubjectHours + session.length > 3) {
            return false;
        }

        for (int i = 0; i < session.length; i++) {
            TimeSlot slot = schedule.getSlot(day, startPeriod + i, className);
            if (slot == null || !slot.isEmpty()) return false;
            if (!schedule.isTeacherAvailable(teacher, day, startPeriod + i, className)) return false;
        }
        return true;
    }

    private void placeSession(Schedule schedule, SessionSlot session, String day, int startPeriod) {
        String className = session.assignment.getClassName();
        for (int i = 0; i < session.length; i++) {
            TimeSlot slot = schedule.getSlot(day, startPeriod + i, className);
            if (slot != null) slot.assign(session.assignment, session.sessionNumber);
        }
    }

    private List<PlacementOption> findRelaxedPlacements(Schedule schedule, SessionSlot session) {
        List<PlacementOption> options = new ArrayList<>();
        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];
            for (int period = 1; period <= maxPeriod - session.length + 1; period++) {
                if (canPlaceSession(schedule, session, day, period)) {
                    int score = evaluatePlacementQuality(schedule, session, day, period);
                    options.add(new PlacementOption(day, period, score));
                }
            }
        }
        return options;
    }

    private int evaluatePlacementQuality(Schedule schedule, SessionSlot session, String day, int period) {
        int score = 100;
        String subject = session.assignment.getSubject().toUpperCase();
        if (subject.contains("PJOK")) score += (10 - period) * 20;
        if (day.equals("Rabu") && isMGMPSubject(subject)) score += (6 - period) * 15;
        if (session.length >= 3) score += (10 - period) * 5;
        TimeSlot prevSlot = schedule.getSlot(day, period - 1, session.assignment.getClassName());
        if (prevSlot != null && prevSlot.isEmpty() && period > 1) score -= 10;
        int classHoursThisDay = 0;
        for (TimeSlot slot : schedule.getSlotsForClass(day, session.assignment.getClassName())) {
            if (!slot.isEmpty()) classHoursThisDay++;
        }
        score -= classHoursThisDay * 2;
        return score;
    }

    private void fillRemainingHoursExactly(Schedule schedule) {
        for (Assignment assignment : assignments) {
            int scheduled = schedule.getScheduledHours(assignment);
            int expected = assignment.getTotalHours();
            while (scheduled < expected) {
                TimeSlot slot = findBestEmptySlot(schedule, assignment);
                if (slot != null) { slot.assign(assignment, 99); scheduled++; }
                else break;
            }
            while (scheduled > expected) {
                TimeSlot slot = findWorstFilledSlot(schedule, assignment);
                if (slot != null) { slot.clear(); scheduled--; }
                else break;
            }
        }
    }

    private TimeSlot findBestEmptySlot(Schedule schedule, Assignment assignment) {
        List<TimeSlot> candidates = new ArrayList<>();
        String className = assignment.getClassName();
        String teacher = assignment.getTeacher();
        String subject = assignment.getSubject().toUpperCase();
        for (int dayIdx = 0; dayIdx < DAYS.length; dayIdx++) {
            String day = DAYS[dayIdx];
            int maxPeriod = PERIODS_PER_DAY[dayIdx];
            for (int period = 1; period <= maxPeriod; period++) {
                TimeSlot slot = schedule.getSlot(day, period, className);
                if (slot != null && slot.isEmpty() && schedule.isTeacherAvailable(teacher, day, period, className)) {
                    boolean valid = true;
                    if (day.equals("Rabu") && isMGMPSubject(subject) && period >= 6) valid = false;
                    if (valid) candidates.add(slot);
                }
            }
        }
        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> {
            int dayDiff = Arrays.asList(DAYS).indexOf(a.getDay()) - Arrays.asList(DAYS).indexOf(b.getDay());
            if (dayDiff != 0) return dayDiff;
            return Integer.compare(a.getPeriod(), b.getPeriod());
        });
        return candidates.get(0);
    }

    private TimeSlot findWorstFilledSlot(Schedule schedule, Assignment assignment) {
        List<TimeSlot> slots = findAllSlotsForAssignment(schedule, assignment);
        if (slots.isEmpty()) return null;
        slots.sort((a, b) -> {
            int dayDiff = Arrays.asList(DAYS).indexOf(b.getDay()) - Arrays.asList(DAYS).indexOf(a.getDay());
            if (dayDiff != 0) return dayDiff;
            return Integer.compare(b.getPeriod(), a.getPeriod());
        });
        return slots.get(0);
    }

    private List<TimeSlot> findAllSlotsForAssignment(Schedule schedule, Assignment assignment) {
        List<TimeSlot> result = new ArrayList<>();
        for (String day : DAYS) {
            for (TimeSlot slot : schedule.getSlotsForClass(day, assignment.getClassName())) {
                if (!slot.isEmpty() &&
                        slot.getAssignment().getTeacher().equals(assignment.getTeacher()) &&
                        slot.getAssignment().getSubject().equals(assignment.getSubject()) &&
                        slot.getAssignment().getClassName().equals(assignment.getClassName())) {
                    result.add(slot);
                }
            }
        }
        return result;
    }

    private void moveRandomAssignment(Schedule schedule) {
        List<TimeSlot> filledSlots = getAllFilledSlots(schedule);
        if (filledSlots.isEmpty()) return;
        TimeSlot source = filledSlots.get(random.nextInt(filledSlots.size()));
        Assignment assignment = source.getAssignment();
        int sessionNumber = source.getSessionNumber();
        TimeSlot target = findBestEmptySlot(schedule, assignment);
        if (target != null && !violatesHardConstraints(schedule, assignment, target.getDay(), target.getPeriod())) {
            source.clear();
            target.assign(assignment, sessionNumber);
        }
    }

    private void swapTwoAssignments(Schedule schedule) {
        List<TimeSlot> filledSlots = getAllFilledSlots(schedule);
        if (filledSlots.size() < 2) return;
        TimeSlot slot1 = filledSlots.get(random.nextInt(filledSlots.size()));
        TimeSlot slot2 = filledSlots.get(random.nextInt(filledSlots.size()));
        if (slot1.equals(slot2)) return;
        Assignment temp = slot1.getAssignment();
        int tempSession = slot1.getSessionNumber();
        slot1.assign(slot2.getAssignment(), slot2.getSessionNumber());
        slot2.assign(temp, tempSession);
        if (hasTeacherConflict(schedule, slot1) || hasTeacherConflict(schedule, slot2) ||
                violatesHardConstraints(schedule, slot1.getAssignment(), slot1.getDay(), slot1.getPeriod()) ||
                violatesHardConstraints(schedule, slot2.getAssignment(), slot2.getDay(), slot2.getPeriod())) {
            slot1.assign(temp, tempSession);
            slot2.assign(slot2.getAssignment(), slot2.getSessionNumber());
        }
    }

    private void fixTeacherConflicts(Schedule schedule) {
        for (String day : DAYS) {
            int maxPeriod = PERIODS_PER_DAY[Arrays.asList(DAYS).indexOf(day)];
            for (int period = 1; period <= maxPeriod; period++) {
                Map<String, List<TimeSlot>> teacherSlots = new HashMap<>();
                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        teacherSlots.computeIfAbsent(slot.getAssignment().getTeacher(), k -> new ArrayList<>()).add(slot);
                    }
                }
                for (List<TimeSlot> slots : teacherSlots.values()) {
                    if (slots.size() > 1) {
                        for (int i = 1; i < slots.size(); i++) {
                            TimeSlot conflict = slots.get(i);
                            Assignment assignment = conflict.getAssignment();
                            int sessionNumber = conflict.getSessionNumber();
                            conflict.clear();
                            TimeSlot alternative = findAnyAvailableSlot(schedule, assignment);
                            if (alternative != null) alternative.assign(assignment, sessionNumber);
                        }
                    }
                }
            }
        }
    }

    private void fixConstraintViolations(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            List<TimeSlot> rabuSlots = schedule.getSlotsForClass("Rabu", className);
            for (TimeSlot slot : rabuSlots) {
                if (!slot.isEmpty() && slot.getPeriod() >= 6 && isMGMPSubject(slot.getAssignment().getSubject())) {
                    Assignment assignment = slot.getAssignment();
                    int sessionNumber = slot.getSessionNumber();
                    slot.clear();
                    for (int period = 1; period < 6; period++) {
                        TimeSlot alt = schedule.getSlot("Rabu", period, className);
                        if (alt != null && alt.isEmpty() && schedule.isTeacherAvailable(assignment.getTeacher(), "Rabu", period, className)) {
                            alt.assign(assignment, sessionNumber);
                            break;
                        }
                    }
                    if (schedule.getScheduledHours(assignment) < assignment.getTotalHours()) {
                        TimeSlot alt = findAnyAvailableSlot(schedule, assignment);
                        if (alt != null) alt.assign(assignment, sessionNumber);
                    }
                }
            }
        }
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
                for (int i = 0; i < daySlots.size() - 1; i++) {
                    TimeSlot slot1 = daySlots.get(i);
                    TimeSlot slot2 = daySlots.get(i + 1);
                    if (!slot1.isEmpty() && !slot2.isEmpty()) {
                        Assignment a1 = slot1.getAssignment();
                        Assignment a2 = slot2.getAssignment();
                        if (a1.getSubject().toUpperCase().contains("PJOK") && a2.getSubject().toUpperCase().contains("PJOK") &&
                                a1.getTeacher().equals(a2.getTeacher()) && a1.getClassName().equals(a2.getClassName()) && slot1.getPeriod() > 4) {
                            slot1.clear();
                            slot2.clear();
                            boolean placed = false;
                            for (int period = 1; period <= 4; period++) {
                                TimeSlot t1 = schedule.getSlot(day, period, className);
                                TimeSlot t2 = schedule.getSlot(day, period + 1, className);
                                if (t1 != null && t2 != null && t1.isEmpty() && t2.isEmpty() &&
                                        schedule.isTeacherAvailable(a1.getTeacher(), day, period, className) &&
                                        schedule.isTeacherAvailable(a1.getTeacher(), day, period + 1, className)) {
                                    t1.assign(a1, slot1.getSessionNumber());
                                    t2.assign(a2, slot2.getSessionNumber());
                                    placed = true;
                                    break;
                                }
                            }
                            if (!placed) {
                                for (String otherDay : DAYS) {
                                    if (placed) break;
                                    for (int period = 1; period <= 4; period++) {
                                        TimeSlot t1 = schedule.getSlot(otherDay, period, className);
                                        TimeSlot t2 = schedule.getSlot(otherDay, period + 1, className);
                                        if (t1 != null && t2 != null && t1.isEmpty() && t2.isEmpty() &&
                                                schedule.isTeacherAvailable(a1.getTeacher(), otherDay, period, className) &&
                                                schedule.isTeacherAvailable(a1.getTeacher(), otherDay, period + 1, className)) {
                                            t1.assign(a1, slot1.getSessionNumber());
                                            t2.assign(a2, slot2.getSessionNumber());
                                            placed = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void balanceHours(Schedule schedule) {
        for (Assignment assignment : assignments) {
            int scheduled = schedule.getScheduledHours(assignment);
            int expected = assignment.getTotalHours();
            if (scheduled < expected) {
                int needed = expected - scheduled;
                for (int i = 0; i < needed; i++) {
                    TimeSlot slot = findAnyAvailableSlot(schedule, assignment);
                    if (slot != null) slot.assign(assignment, 99);
                }
            } else if (scheduled > expected) {
                int excess = scheduled - expected;
                for (int i = 0; i < excess; i++) {
                    TimeSlot slot = findWorstFilledSlot(schedule, assignment);
                    if (slot != null) slot.clear();
                }
            }
        }
    }

    private void compactSchedule(Schedule schedule) {
        for (String className : schedule.getAllClasses()) {
            for (String day : DAYS) {
                List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
                for (int i = 0; i < daySlots.size() - 1; i++) {
                    TimeSlot current = daySlots.get(i);
                    if (current.isEmpty()) {
                        for (int j = i + 1; j < daySlots.size(); j++) {
                            TimeSlot later = daySlots.get(j);
                            if (!later.isEmpty()) {
                                Assignment assignment = later.getAssignment();
                                if (schedule.isTeacherAvailable(assignment.getTeacher(), day, current.getPeriod(), className)) {
                                    current.assign(assignment, later.getSessionNumber());
                                    later.clear();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Schedule perturbSolution(Schedule schedule, int strength) {
        Schedule perturbed = schedule.clone();
        for (int i = 0; i < strength * 5; i++) {
            int operator = random.nextInt(3);
            switch (operator) {
                case 0: swapTwoAssignments(perturbed); break;
                case 1: moveRandomAssignment(perturbed); break;
                case 2: shuffleClassDay(perturbed); break;
            }
        }
        return perturbed;
    }

    private void shuffleClassDay(Schedule schedule) {
        if (schedule.getAllClasses().isEmpty()) return;
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        String className = classes.get(random.nextInt(classes.size()));
        String day = DAYS[random.nextInt(DAYS.length)];
        List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
        List<Assignment> assignmentList = new ArrayList<>();
        List<Integer> sessionNumbers = new ArrayList<>();
        for (TimeSlot slot : slots) {
            if (!slot.isEmpty()) {
                assignmentList.add(slot.getAssignment());
                sessionNumbers.add(slot.getSessionNumber());
                slot.clear();
            }
        }
        Collections.shuffle(assignmentList, random);
        int idx = 0;
        for (TimeSlot slot : slots) {
            if (idx < assignmentList.size()) {
                Assignment assignment = assignmentList.get(idx);
                if (schedule.isTeacherAvailable(assignment.getTeacher(), day, slot.getPeriod(), className)) {
                    slot.assign(assignment, sessionNumbers.get(idx));
                    idx++;
                }
            }
        }
        while (idx < assignmentList.size()) {
            TimeSlot emptySlot = findAnyAvailableSlot(schedule, assignmentList.get(idx));
            if (emptySlot != null) emptySlot.assign(assignmentList.get(idx), sessionNumbers.get(idx));
            idx++;
        }
    }

    private void aggressiveSwapOptimization(Schedule schedule) {
        List<TimeSlot> filledSlots = getAllFilledSlots(schedule);
        if (filledSlots.size() < 2) return;
        for (TimeSlot slot1 : filledSlots) {
            if (slot1.isEmpty()) continue;
            Assignment a1 = slot1.getAssignment();
            String day1 = slot1.getDay();
            int period1 = slot1.getPeriod();
            for (TimeSlot slot2 : filledSlots) {
                if (slot2.isEmpty() || slot1.equals(slot2)) continue;
                Assignment a2 = slot2.getAssignment();
                String day2 = slot2.getDay();
                int period2 = slot2.getPeriod();
                if (a1.equals(a2) || (a1.getSubject().toUpperCase().contains("PJOK") &&
                        a2.getSubject().toUpperCase().contains("PJOK") &&
                        a1.getTeacher().equals(a2.getTeacher()) &&
                        !violatesHardConstraints(schedule, a1, day2, period2) &&
                        !violatesHardConstraints(schedule, a2, day1, period1))) {
                    slot1.assign(slot2.getAssignment(), slot2.getSessionNumber());
                    slot2.assign(a1, slot1.getSessionNumber());
                    if (hasTeacherConflict(schedule, slot1) || hasTeacherConflict(schedule, slot2) ||
                            violatesHardConstraints(schedule, slot1.getAssignment(), slot1.getDay(), slot1.getPeriod()) ||
                            violatesHardConstraints(schedule, slot2.getAssignment(), slot2.getDay(), slot2.getPeriod())) {
                        slot1.assign(a1, slot1.getSessionNumber());
                        slot2.assign(slot2.getAssignment(), slot2.getSessionNumber());
                    }
                }
            }
        }
    }

    // ==================== FITNESS & PENALTY EVALUATION ====================

    /**
     * Evaluate fitness (higher is better)
     */
    private int evaluateFitness(Schedule schedule) {
        return -getPenalty(schedule);
    }

    /**
     * Get total penalty (lower is better)
     */
    private int getPenalty(Schedule schedule) {
        int penalty = 0;

        // Critical: Teacher conflicts (same teacher, same time, different class)
        penalty += countTeacherConflicts(schedule) * 100000;

        // Critical: Incomplete scheduling (hours don't match requirements)
        penalty += countIncompleteHours(schedule) * 10000;

        // Important: Hard constraints (PJOK timing, MGMP timing)
        penalty += countPJOKViolations(schedule) * 5000;
        penalty += countMGMPViolations(schedule) * 5000;

        // Minor: Schedule quality
        penalty += countEmptySlots(schedule) * 1;
        penalty += countGaps(schedule) * 10;

        return penalty;
    }

    private int countTeacherConflicts(Schedule schedule) {
        int conflicts = 0;
        for (String day : DAYS) {
            int maxPeriod = PERIODS_PER_DAY[Arrays.asList(DAYS).indexOf(day)];
            for (int period = 1; period <= maxPeriod; period++) {
                Map<String, Integer> teacherCount = new HashMap<>();
                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        teacherCount.merge(slot.getAssignment().getTeacher(), 1, Integer::sum);
                    }
                }
                for (int count : teacherCount.values()) {
                    if (count > 1) {
                        conflicts += (count - 1);
                    }
                }
            }
        }
        return conflicts;
    }

    private int countIncompleteHours(Schedule schedule) {
        int incomplete = 0;
        for (Assignment assignment : assignments) {
            int scheduled = schedule.getScheduledHours(assignment);
            int expected = assignment.getTotalHours();
            incomplete += Math.abs(expected - scheduled);
        }
        return incomplete;
    }

    private int countPJOKViolations(Schedule schedule) {
        int violations = 0;
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);

                for (int i = 0; i < daySlots.size() - 1; i++) {
                    TimeSlot slot1 = daySlots.get(i);
                    TimeSlot slot2 = daySlots.get(i + 1);

                    if (!slot1.isEmpty() && !slot2.isEmpty()) {
                        Assignment a1 = slot1.getAssignment();
                        Assignment a2 = slot2.getAssignment();

                        if (a1.getSubject().toUpperCase().contains("PJOK") &&
                                a2.getSubject().toUpperCase().contains("PJOK") &&
                                a1.getTeacher().equals(a2.getTeacher()) &&
                                a1.getClassName().equals(a2.getClassName())) {

                            // 2-hour PJOK session must end by period 5
                            if (slot1.getPeriod() > 4) {
                                violations++;
                            }
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
            List<TimeSlot> rabuSlots = schedule.getSlotsForClass("Rabu", className);
            for (TimeSlot slot : rabuSlots) {
                if (!slot.isEmpty() && slot.getPeriod() >= 6) {
                    if (isMGMPSubject(slot.getAssignment().getSubject())) {
                        violations++;
                    }
                }
            }
        }
        return violations;
    }

    private int countEmptySlots(Schedule schedule) {
        int count = 0;
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (slot.isEmpty()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private int countGaps(Schedule schedule) {
        int gaps = 0;
        for (String className : schedule.getAllClasses()) {
            for (String day : DAYS) {
                List<TimeSlot> daySlots = schedule.getSlotsForClass(day, className);
                boolean seenFilled = false;
                boolean seenEmpty = false;

                for (TimeSlot slot : daySlots) {
                    if (!slot.isEmpty()) {
                        if (seenEmpty && seenFilled) {
                            gaps++;
                        }
                        seenFilled = true;
                        seenEmpty = false;
                    } else {
                        if (seenFilled) {
                            seenEmpty = true;
                        }
                    }
                }
            }
        }
        return gaps;
    }

    // ==================== HELPER METHODS ====================

    private List<TimeSlot> getAllFilledSlots(Schedule schedule) {
        List<TimeSlot> filled = new ArrayList<>();
        for (String day : DAYS) {
            for (String className : schedule.getAllClasses()) {
                for (TimeSlot slot : schedule.getSlotsForClass(day, className)) {
                    if (!slot.isEmpty()) {
                        filled.add(slot);
                    }
                }
            }
        }
        return filled;
    }

    private boolean hasTeacherConflict(Schedule schedule, TimeSlot slot) {
        if (slot.isEmpty()) return false;

        String teacher = slot.getAssignment().getTeacher();
        String day = slot.getDay();
        int period = slot.getPeriod();

        int count = 0;
        for (String className : schedule.getAllClasses()) {
            TimeSlot otherSlot = schedule.getSlot(day, period, className);
            if (otherSlot != null && !otherSlot.isEmpty()) {
                if (otherSlot.getAssignment().getTeacher().equals(teacher)) {
                    count++;
                }
            }
        }

        return count > 1;
    }

    private boolean violatesHardConstraints(Schedule schedule, Assignment assignment, String day, int period) {
        String subject = assignment.getSubject().toUpperCase();

        // MGMP constraint
        if (day.equals("Rabu") && isMGMPSubject(subject) && period >= 6) {
            return true;
        }

        // Note: PJOK constraint is checked for 2-hour sessions, single slots are OK

        return false;
    }

    private int[] getOptimalPattern(Assignment assignment) {
        String subject = assignment.getSubject().toUpperCase().trim();

        for (Map.Entry<String, int[]> entry : SUBJECT_PATTERNS.entrySet()) {
            if (subject.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return createExactPattern(assignment.getTotalHours());
    }

    private boolean isMGMPSubject(String subject) {
        String upperSubject = subject.toUpperCase().trim();
        for (String mgmpSubject : MGMP_SUBJECTS) {
            if (upperSubject.contains(mgmpSubject.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private void printScheduleStatus(String label, Schedule schedule) {
        int conflicts = countTeacherConflicts(schedule);
        int incomplete = countIncompleteHours(schedule);
        int pjokViol = countPJOKViolations(schedule);
        int mgmpViol = countMGMPViolations(schedule);
        int penalty = getPenalty(schedule);

        System.out.printf("[%s] Penalty=%d (Conflicts=%d, Incomplete=%d, PJOK=%d, MGMP=%d)\n",
                label, penalty, conflicts, incomplete, pjokViol, mgmpViol);
    }

    private void printDetailedMetrics(Schedule schedule) {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                    DETAIL METRICS                           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Teacher Conflicts : %-40d│\n", countTeacherConflicts(schedule));
        System.out.printf("│ Incomplete Hours  : %-40d│\n", countIncompleteHours(schedule));
        System.out.printf("│ PJOK Violations   : %-40d│\n", countPJOKViolations(schedule));
        System.out.printf("│ MGMP Violations   : %-40d│\n", countMGMPViolations(schedule));
        System.out.printf("│ Empty Slots       : %-40d│\n", countEmptySlots(schedule));
        System.out.printf("│ Schedule Gaps     : %-40d│\n", countGaps(schedule));
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        // Detailed assignment check
        int perfect = 0, over = 0, under = 0;
        for (Assignment a : assignments) {
            int scheduled = schedule.getScheduledHours(a);
            int expected = a.getTotalHours();
            if (scheduled == expected) perfect++;
            else if (scheduled > expected) over++;
            else under++;
        }

        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                 ASSIGNMENT STATUS                           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Perfect Match     : %-40d│\n", perfect);
        System.out.printf("│ Over-scheduled    : %-40d│\n", over);
        System.out.printf("│ Under-scheduled   : %-40d│\n", under);
        System.out.printf("│ Total             : %-40d│\n", assignments.size());
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    // ==================== INNER CLASSES ====================

    private static class SessionSlot {
        Assignment assignment;
        int sessionNumber;
        int length;
        int priority;

        SessionSlot(Assignment assignment, int sessionNumber, int length, int priority) {
            this.assignment = assignment;
            this.sessionNumber = sessionNumber;
            this.length = length;
            this.priority = priority;
        }
    }

    private static class PlacementOption {
        String day;
        int startPeriod;
        int score;

        PlacementOption(String day, int startPeriod, int score) {
            this.day = day;
            this.startPeriod = startPeriod;
            this.score = score;
        }
    }
}

