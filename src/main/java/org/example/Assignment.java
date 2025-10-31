package org.example;

import java.util.*;

/**
 * Setiap guru punya ID dasar (1, 2, 3, dst).
 * Jika guru mengajar lebih dari satu mapel:
 *   - mapel pertama → 1a
 *   - mapel kedua → 1b
 *   - mapel ketiga → 1c
 * Kalau hanya satu mapel → 1 saja.
 */
public final class Assignment {
    private static final Map<String, Integer> teacherIds = new HashMap<>();
    private static final Map<String, LinkedHashMap<String, String>> teacherSubjectLetters = new HashMap<>();
    private static int nextTeacherId = 1;

    private final String teacher;
    private final String subject;
    private final String className;
    private final int totalHours;
    private final int baseId;

    public Assignment(String teacher, String subject, String className, int totalHours) {
        this.teacher = teacher.trim();
        this.subject = subject.trim();
        this.className = className.trim();
        this.totalHours = totalHours;

        // Dapatkan ID dasar guru (hindari lambda yang tidak menggunakan param sehingga memperkecil warning)
        if (!teacherIds.containsKey(this.teacher)) {
            teacherIds.put(this.teacher, nextTeacherId++);
        }
        this.baseId = teacherIds.get(this.teacher);

        // Inisialisasi daftar mapel per guru
        teacherSubjectLetters.putIfAbsent(this.teacher, new LinkedHashMap<>());
        LinkedHashMap<String, String> mapelHuruf = teacherSubjectLetters.get(this.teacher);

        // Tambahkan huruf baru jika mapel belum ada (menghasilkan label seperti a..z, aa, ab ...)
        if (!mapelHuruf.containsKey(this.subject)) {
            String huruf = indexToLetters(mapelHuruf.size());
            mapelHuruf.put(this.subject, huruf);
        }

        // Note: tidak menyimpan ID tetap di konstruktor.
        // ID sekarang dihitung dinamis di getId() sehingga jika guru menambah mapel
        // setelah assignment pertama dibuat, semua assignment akan menampilkan sufiks huruf.
    }

    // Menghasilkan ID saat diminta, berdasarkan jumlah mapel yang terdaftar untuk guru ini.
    public String getId() {
        LinkedHashMap<String, String> mapelHuruf = teacherSubjectLetters.get(this.teacher);
        if (mapelHuruf == null) {
            return String.valueOf(baseId);
        }
        String huruf = mapelHuruf.get(this.subject);
        boolean lebihDariSatu = mapelHuruf.size() > 1;
        if (lebihDariSatu) {
            return baseId + huruf;
        } else {
            return String.valueOf(baseId);
        }
    }

    private static String indexToLetters(int index) {
        // index 0 -> "a", 1 -> "b", ..., 25 -> "z", 26 -> "aa", 27 -> "ab" ...
        StringBuilder sb = new StringBuilder();
        int i = index;
        while (true) {
            int rem = i % 26;
            sb.append((char) ('a' + rem));
            i = i / 26 - 1;
            if (i < 0) break;
        }
        return sb.reverse().toString();
    }

    public String getTeacher() {
        return teacher;
    }

    public String getSubject() {
        return subject;
    }

    public String getClassName() {
        return className;
    }

    public int getTotalHours() {
        return totalHours;
    }

    @Override
    public String toString() {
        return String.format("%-3s | %-30s | %-15s | %-6s | %2d jam",
                getId(), teacher, subject, className, totalHours);
    }
}