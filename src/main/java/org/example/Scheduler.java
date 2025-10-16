// File: src/main/java/org/example/Scheduler.java
package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class Scheduler {

    private static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
    private static final int[] HOURS_PER_DAY = {10, 10, 10, 9, 8};

    private static final String[] CLASSES = {
            "7A", "7B", "7C", "7D", "7E", "7F", "7G", "7H",
            "8A", "8B", "8C", "8D", "8E", "8F", "8G", "8H",
            "9A", "9B", "9C", "9D", "9E", "9F", "9G"
    };

    private final Map<String, int[][]> schedule = new LinkedHashMap<>();
    private final List<Assignment> assignments;

    // Melacak kapan guru sudah mengajar (per hari & jam)
    // teacherOccupied.get(id)[day][hour] == true -> guru id sibuk di hari/jam itu
    private final Map<Integer, boolean[][]> teacherOccupied = new HashMap<>();

    public Scheduler(List<Assignment> assignments) {
        this.assignments = assignments;
        initializeEmptySchedule();
        initializeTeacherAvailability();
        fillReligiousTeacherSchedule();
    }

    private void initializeEmptySchedule() {
        for (String className : CLASSES) {
            int[][] classSchedule = new int[DAYS.length][];
            for (int i = 0; i < DAYS.length; i++) {
                classSchedule[i] = new int[HOURS_PER_DAY[i]];
                Arrays.fill(classSchedule[i], 0);
            }
            schedule.put(className, classSchedule);
        }
    }

    private void initializeTeacherAvailability() {
        for (Assignment a : assignments) {
            teacherOccupied.putIfAbsent(a.teacherId(), new boolean[DAYS.length][]);
            for (int d = 0; d < DAYS.length; d++) {
                teacherOccupied.get(a.teacherId())[d] = new boolean[HOURS_PER_DAY[d]];
            }
        }
    }

    public List<Assignment> findReligiousAssignments() {
        List<String> religiousSubjects = Arrays.asList(
                "SKI", "B.ARAB", "AQIDAH A.", "QURDITS", "FIQIH"
        );

        return assignments.stream()
                .filter(a -> religiousSubjects.stream()
                        .anyMatch(sub -> a.subject().toUpperCase().contains(sub)))
                .collect(Collectors.toList());
    }

    private void fillReligiousTeacherSchedule() {
        List<Assignment> religiousAssignments = findReligiousAssignments();

        // Urutkan assignments agar hasil deterministik (misal berdasarkan teacherId atau class)
        religiousAssignments.sort(Comparator.comparingInt(Assignment::teacherId));

        for (Assignment a : religiousAssignments) {
            String className = a.className();
            int teacherId = a.teacherId();
            int need = a.totalHours();

            int[][] classSchedule = schedule.get(className);
            if (classSchedule == null) {
                System.out.printf("⚠ Kelas %s tidak dikenal, lewati assignment %s (ID %d)%n",
                        className, a.subject(), teacherId);
                continue;
            }

            boolean[][] teacherBusy = teacherOccupied.get(teacherId);
            if (teacherBusy == null) {
                // seharusnya tidak terjadi karena sudah di-init, tapi guard tetap ada
                teacherOccupied.putIfAbsent(teacherId, new boolean[DAYS.length][]);
                for (int d = 0; d < DAYS.length; d++) {
                    teacherOccupied.get(teacherId)[d] = new boolean[HOURS_PER_DAY[d]];
                }
                teacherBusy = teacherOccupied.get(teacherId);
            }

            int placed = 0;

            // Strategy: coba tempatkan sebanyak mungkin blok 2 jika need >=2, agar durasi 2 jadi contiguous.
            // Lakukan scan hari->jam secara sekuensial.
            outer:
            for (int d = 0; d < DAYS.length; d++) {
                for (int h = 0; h < HOURS_PER_DAY[d]; h++) {

                    // aturan Rabu: tidak boleh mengajar setelah jam ke-4 -> index >=4 dilarang
                    if (d == 2 && h >= 4) continue;

                    // Jika kita masih butuh >=2 jam, coba tempatkan blok 2 jam
                    if (need - placed >= 2) {
                        // pastikan ada slot h+1 di hari tersebut
                        if (h + 1 >= HOURS_PER_DAY[d]) continue;
                        // cek slot kelas kosong
                        if (classSchedule[d][h] != 0 || classSchedule[d][h + 1] != 0) continue;
                        // cek guru tidak sibuk di kedua jam
                        if (teacherBusy[d][h] || teacherBusy[d][h + 1]) continue;

                        // tempatkan blok 2 jam berurutan
                        classSchedule[d][h] = teacherId;
                        classSchedule[d][h + 1] = teacherId;
                        teacherBusy[d][h] = true;
                        teacherBusy[d][h + 1] = true;
                        placed += 2;
                        // lompat satu jam karena sudah isi h and h+1
                        h++; // safe karena h+1 ada
                        // jika sudah terpenuhi, break
                        if (placed >= need) break outer;
                        else continue; // lanjut cari slot tambahan
                    }

                    // jika sisanya 1 jam, coba tempatkan single slot
                    if (need - placed == 1) {
                        if (classSchedule[d][h] != 0) continue;
                        if (teacherBusy[d][h]) continue;

                        classSchedule[d][h] = teacherId;
                        teacherBusy[d][h] = true;
                        placed += 1;
                        if (placed >= need) break outer;
                    }
                }
            }
        }
    }

    public void printSchedule() {
        for (String className : CLASSES) {
            System.out.println("\n=== Jadwal Kelas " + className + " ===");
            int[][] classSchedule = schedule.get(className);
            for (int i = 0; i < DAYS.length; i++) {
                System.out.printf("%-7s: ", DAYS[i]);
                for (int j = 0; j < HOURS_PER_DAY[i]; j++) {
                    System.out.printf("%3d ", classSchedule[i][j]);
                }
                System.out.println();
            }
        }
    }

    public void printReligiousTeacherIds() {
        List<Assignment> religiousAssignments = findReligiousAssignments();

        Set<Integer> uniqueIds = new HashSet<>();
        for (Assignment a : religiousAssignments) {
            uniqueIds.add(a.teacherId());
        }

        System.out.println("Guru dengan mapel SKI, B. Arab, Aqidah A., Qurdits, Fiqih:");
        for (int id : uniqueIds) {
            System.out.printf("ID: %-3d ", id);
        }
        System.out.println();
    }

    public Map<String, int[][]> getSchedule() {
        return schedule;
    }
}
