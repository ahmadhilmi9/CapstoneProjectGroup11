// File: src/main/java/org/example/Assignment.java
package org.example;

import java.util.Objects;

/**
 * Kelas ini merepresentasikan satu baris penugasan unik untuk seorang guru.
 * Contoh: "Dra. Rohmatin Afida mengajar IPA di kelas 8B selama 5 jam seminggu".
 * Kita menggunakan class biasa (bukan record) untuk kompatibilitas dengan versi Java yang lebih lama.
 */
public final class Assignment {
    private final String teacher;
    private final String subject;
    private final String className;
    private final int totalHours;

    public Assignment(String teacher, String subject, String className, int totalHours) {
        this.teacher = teacher;
        this.subject = subject;
        this.className = className;
        this.totalHours = totalHours;
    }

    // Metode 'getter' untuk mengakses data private dari luar kelas
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

    // Metode standar untuk perbandingan objek, hashing, dan representasi string
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return totalHours == that.totalHours &&
                Objects.equals(teacher, that.teacher) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacher, subject, className, totalHours);
    }

    @Override
    public String toString() {
        return "Assignment[" +
                "teacher='" + teacher + '\'' +
                ", subject='" + subject + '\'' +
                ", className='" + className + '\'' +
                ", totalHours=" + totalHours +
                ']';
    }
}