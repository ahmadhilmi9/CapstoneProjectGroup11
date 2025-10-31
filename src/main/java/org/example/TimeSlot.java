package org.example;

/**
 * Representasi slot waktu dalam jadwal
 */
public class TimeSlot {
    private final String day;        // Senin, Selasa, Rabu, Kamis, Jumat
    private final int period;        // Jam ke-berapa (1-10)
    private final String className;  // Kelas (7A, 7B, dst)
    private Assignment assignment;   // Assignment yang dijadwalkan
    private int sessionNumber;       // Untuk tracking sesi (misal Matematika 3-2, ini sesi ke-1 atau ke-2)

    public TimeSlot(String day, int period, String className) {
        this.day = day;
        this.period = period;
        this.className = className;
        this.assignment = null;
        this.sessionNumber = 0;
    }

    public boolean isEmpty() {
        return assignment == null;
    }

    public void assign(Assignment assignment, int sessionNumber) {
        this.assignment = assignment;
        this.sessionNumber = sessionNumber;
    }

    public void clear() {
        this.assignment = null;
        this.sessionNumber = 0;
    }

    public String getDay() {
        return day;
    }

    public int getPeriod() {
        return period;
    }

    public String getClassName() {
        return className;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public String getDisplayText() {
        if (isEmpty()) {
            return "-";
        }
        return assignment.getSubject() + "\n(" + assignment.getTeacher() + ")";
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return String.format("%s Jam-%d [%s]: KOSONG", day, period, className);
        }
        return String.format("%s Jam-%d [%s]: %s - %s (Sesi %d)",
            day, period, className, assignment.getSubject(), assignment.getTeacher(), sessionNumber);
    }
}

