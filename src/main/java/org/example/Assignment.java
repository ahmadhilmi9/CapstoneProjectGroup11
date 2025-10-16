package org.example;

import java.util.*;

/**
 * Kelas ini merepresentasikan satu baris penugasan unik untuk seorang guru.
 * Contoh: "Dra. Rohmatin Afida mengajar IPA di kelas 8B selama 5 jam seminggu".
 * Setiap guru mendapat ID unik (1, 2, 3, dst) secara otomatis berdasarkan nama.
 */
public final class Assignment {
    // Menyimpan mapping nama guru -> ID unik
    private static final Map<String, Integer> teacherIds = new HashMap<>();
    private static int nextId = 1;

    private final int teacherId;
    private final String teacher;
    private final String subject;
    private final String className;
    private final int totalHours;

    public Assignment(String teacher, String subject, String className, int totalHours) {
        this.teacher = teacher;
        this.subject = subject;
        this.className = className;
        this.totalHours = totalHours;
        this.teacherId = getOrAssignTeacherId(teacher);
    }

    // Metode internal untuk memberikan ID unik ke guru
    private static int getOrAssignTeacherId(String teacher) {
        if (!teacherIds.containsKey(teacher)) {
            teacherIds.put(teacher, nextId++);
        }
        return teacherIds.get(teacher);
    }

    // Getter
    public int teacherId() {
        return teacherId;
    }

    public String teacher() {
        return teacher;
    }

    public String subject() {
        return subject;
    }

    public String className() {
        return className;
    }

    public int totalHours() {
        return totalHours;
    }

    // Override standar
    @Override
    public String toString() {
        return String.format("%-3d | %-30s | %-15s | %-6s | %2d jam",
                teacherId, teacher, subject, className, totalHours);
    }

}
