package org.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        String filePath = "jadwal.xlsx";
        String sheetName = "sheet1";
        try {
            List<Assignment> assignments = ExcelReader.readAssignments(filePath, sheetName);

            if (assignments.isEmpty()) {
                System.out.println("Tidak ada data yang ditemukan di sheet '" + sheetName + "'.");
            } else {
                // Mencetak setiap baris data yang berhasil dibaca
                AtomicInteger count = new AtomicInteger(1);
                assignments.forEach(a -> {
                    System.out.printf("%d. GURU: %s | MAPEL: %s | KELAS: %s | JAM: %d%n",
                            count.getAndIncrement(),
                            a.teacher(),
                            a.subject(),
                            a.className(),
                            a.totalHours());
                });

                System.out.println("------------------------------------------------------");
                System.out.println("✅ SUKSES: " + assignments.size() + " total baris data berhasil dibaca dari sheet '" + sheetName + "'.");
            }

        } catch (IOException e) {
            System.err.println("❌ GAGAL MEMBACA FILE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}