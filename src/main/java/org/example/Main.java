package org.example;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            List<Assignment> assignments = ExcelReader.readAssignments("jadwal.xlsx", "Sheet1");

            System.out.println("=== DATA ASSIGNMENT ===");
            for (Assignment a : assignments) {
                System.out.println(a);
            }
            System.out.printf("Total assignment: %d%n%n", assignments.size());

            Scheduler scheduler = new Scheduler(assignments);

            System.out.println("\n=== JADWAL TERISI (ID GURU) ===");
            scheduler.printSchedule();
            scheduler.printReligiousTeacherIds();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
