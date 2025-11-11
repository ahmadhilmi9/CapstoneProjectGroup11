package org.example;

import java.util.*;

/**
 * Class untuk menyimpan dan mengelola jadwal pelajaran
 */
public class Schedule {
    private final Map<String, Map<String, List<TimeSlot>>> schedule; // day -> className -> List<TimeSlot>
    private final List<String> days = Arrays.asList("Senin", "Selasa", "Rabu", "Kamis", "Jumat");
    private final Map<String, Integer> periodsPerDay; // Jumlah jam per hari
    private final Set<String> allClasses;

    public Schedule(Set<String> classes) {
        this.schedule = new LinkedHashMap<>();
        this.allClasses = new HashSet<>(classes);

        // Setup jumlah jam per hari
        periodsPerDay = new HashMap<>();
        periodsPerDay.put("Senin", 10);
        periodsPerDay.put("Selasa", 10);
        periodsPerDay.put("Rabu", 10);
        periodsPerDay.put("Kamis", 9);
        periodsPerDay.put("Jumat", 8);

        // Inisialisasi slot untuk semua hari dan kelas
        for (String day : days) {
            Map<String, List<TimeSlot>> daySchedule = new LinkedHashMap<>();
            for (String className : classes) {
                List<TimeSlot> slots = new ArrayList<>();
                int periods = periodsPerDay.get(day);
                for (int period = 1; period <= periods; period++) {
                    slots.add(new TimeSlot(day, period, className));
                }
                daySchedule.put(className, slots);
            }
            schedule.put(day, daySchedule);
        }
    }

    public TimeSlot getSlot(String day, int period, String className) {
        if (!schedule.containsKey(day)) return null;
        if (!schedule.get(day).containsKey(className)) return null;
        List<TimeSlot> slots = schedule.get(day).get(className);
        if (period < 1 || period > slots.size()) return null;
        return slots.get(period - 1);
    }

    public List<TimeSlot> getSlotsForClass(String day, String className) {
        if (!schedule.containsKey(day)) return new ArrayList<>();
        return schedule.get(day).getOrDefault(className, new ArrayList<>());
    }

    public List<String> getDays() {
        return new ArrayList<>(days);
    }

    public Set<String> getAllClasses() {
        return new HashSet<>(allClasses);
    }

    public int getPeriodsForDay(String day) {
        return periodsPerDay.getOrDefault(day, 0);
    }

    public Map<String, Map<String, List<TimeSlot>>> getFullSchedule() {
        return schedule;
    }

    /**
     * Cek apakah guru sudah mengajar di jam dan hari tersebut di kelas lain (bentrok)
     */
    public boolean isTeacherAvailable(String teacher, String day, int period, String excludeClass) {
        for (String className : allClasses) {
            if (className.equals(excludeClass)) continue;
            TimeSlot slot = getSlot(day, period, className);
            if (slot != null && !slot.isEmpty()) {
                if (slot.getAssignment().getTeacher().equals(teacher)) {
                    return false; // Guru sudah mengajar di kelas lain
                }
            }
        }
        return true;
    }
    // Mengembalikan semua teacher id yang muncul di schedule (tidak duplikat)
    public Set<String> getAllTeachers() {
        Set<String> teachers = new HashSet<>();
        for (String day : days) {
            for (String className : allClasses) {
                List<TimeSlot> slots = getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty() && slot.getAssignment() != null) {
                        String teacher = slot.getAssignment().getTeacher();
                        if (teacher != null && !teacher.isEmpty()) {
                            teachers.add(teacher);
                        }
                    }
                }
            }
        }
        return teachers;
    }

    // Mencari nama kelas yang diajar oleh teacher pada hari+period tertentu.
// Mengembalikan nama kelas pertama yang ditemukan, atau empty string jika tidak ada.
    public String getClassTaughtByTeacherAt(String day, int period, String teacher) {
        if (teacher == null || teacher.isEmpty()) return "";
        for (String className : allClasses) {
            TimeSlot slot = getSlot(day, period, className);
            if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                if (teacher.equals(slot.getAssignment().getTeacher())) {
                    return className;
                }
            }
        }
        return "";
    }

    // Kembalikan list TimeSlot yang diajar teacher pada hari tertentu (dapat dipakai untuk sheet per-guru)
    public List<TimeSlot> getSlotsForTeacher(String day, String teacher) {
        List<TimeSlot> result = new ArrayList<>();
        if (teacher == null || teacher.isEmpty()) return result;
        for (String className : allClasses) {
            List<TimeSlot> slots = getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (!slot.isEmpty() && slot.getAssignment() != null &&
                        teacher.equals(slot.getAssignment().getTeacher())) {
                    result.add(slot);
                }
            }
        }
        return result;
    }


    /**
     * Hitung berapa jam guru sudah mengajar pada hari tertentu
     */
    public int getTeacherHoursOnDay(String teacher, String day) {
        int hours = 0;
        for (String className : allClasses) {
            List<TimeSlot> slots = getSlotsForClass(day, className);
            for (TimeSlot slot : slots) {
                if (!slot.isEmpty() && slot.getAssignment().getTeacher().equals(teacher)) {
                    hours++;
                }
            }
        }
        return hours;
    }

    /**
     * Cek apakah assignment sudah terpenuhi semua jamnya
     */
    public int getScheduledHours(Assignment assignment) {
        int scheduled = 0;
        for (String day : days) {
            for (String className : allClasses) {
                if (!className.equals(assignment.getClassName())) continue;
                List<TimeSlot> slots = getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty() &&
                        slot.getAssignment().getTeacher().equals(assignment.getTeacher()) &&
                        slot.getAssignment().getSubject().equals(assignment.getSubject()) &&
                        slot.getAssignment().getClassName().equals(assignment.getClassName())) {
                        scheduled++;
                    }
                }
            }
        }
        return scheduled;
    }

    public void printSchedule() {
        for (String day : days) {
            System.out.println("\n========== " + day + " ==========");
            for (String className : allClasses) {
                System.out.println("\n  Kelas: " + className);
                List<TimeSlot> slots = getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    System.out.println("    " + slot);
                }
            }
        }
    }

    /**
     * Clone schedule untuk digunakan dalam algoritma ILS
     */
    @Override
    public Schedule clone() {
        Schedule cloned = new Schedule(this.allClasses);

        // Copy all assignments
        for (String day : days) {
            for (String className : allClasses) {
                List<TimeSlot> originalSlots = this.getSlotsForClass(day, className);
                List<TimeSlot> clonedSlots = cloned.getSlotsForClass(day, className);

                for (int i = 0; i < originalSlots.size(); i++) {
                    TimeSlot original = originalSlots.get(i);
                    TimeSlot clonedSlot = clonedSlots.get(i);

                    if (!original.isEmpty()) {
                        clonedSlot.assign(original.getAssignment(), original.getSessionNumber());
                    }
                }
            }
        }

        return cloned;
    }
}
