// File: src/main/java/org/example/Main.java
package org.example;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            List<Assignment> assignments = ExcelReader.readAssignments("jadwal.xlsx", "Sheet1");
            for (Assignment a : assignments) {
                System.out.println(a);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
