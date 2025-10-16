// File: src/main/java/org/example/DataVerifier.java
package org.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataVerifier {

    public static void main(String[] args) {
        // --- PENGATURAN ---
        // Ganti nama file dan sheet yang ingin Anda periksa di sini
        String filePath = "jadwal.xlsx";
        String sheetName = "sheet1"; // Ganti ini menjadi "JADWAL-1" atau nama sheet lain untuk memeriksanya

        System.out.println("======================================================");
        System.out.println("MEMBACA DATA DARI FILE: " + filePath);
        System.out.println("SHEET YANG DIPERIKSA: " + sheetName);
        System.out.println("======================================================");

        try {
            // Memanggil ExcelReader untuk membaca data
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