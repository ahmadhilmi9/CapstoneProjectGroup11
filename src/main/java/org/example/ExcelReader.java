// File: src/main/java/org/example/ExcelReader.java
package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ExcelReader {

    public static List<Assignment> readAssignments(String path, String sheetName) throws IOException {
        List<Assignment> assignments = new ArrayList<>();

        // Menggunakan try-with-resources untuk memastikan InputStream ditutup secara otomatis.
        // Ini adalah cara yang benar untuk membaca file dari folder 'resources'.
        try (InputStream is = ExcelReader.class.getClassLoader().getResourceAsStream(path)) {

            if (is == null) {
                throw new IOException("File tidak ditemukan di folder resources: " + path);
            }

            try (Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new IOException("Sheet dengan nama '" + sheetName + "' tidak ditemukan di dalam file.");
                }

                // Asumsi dan konfigurasi tata letak sheet
                Row headerRow = sheet.getRow(1); // Baris nama-nama kelas (7A, 7B, dst.)
                int firstDataRow = 2;          // Data guru dimulai dari baris ke-3 (index 2)
                int teacherNameCol = 1;        // Kolom nama guru
                int subjectNameCol = 2;        // Kolom nama mata pelajaran
                int firstClassCol = 3;         // Kolom kelas pertama (misal: 7A)
                int lastClassCol = 25;         // Kolom kelas terakhir (sesuaikan jika perlu)

                String lastKnownTeacher = ""; // Variabel untuk menangani sel nama guru yang di-merge

                // Looping melalui setiap baris data
                for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    // Mengatasi sel nama guru yang di-merge
                    String teacherNameCell = getStringValue(row.getCell(teacherNameCol)).trim();
                    String currentTeacher;
                    if (teacherNameCell.isEmpty() || teacherNameCell.toLowerCase().contains("nip")) {
                        currentTeacher = lastKnownTeacher; // Gunakan nama guru terakhir jika sel kosong
                    } else {
                        currentTeacher = teacherNameCell;
                        lastKnownTeacher = teacherNameCell; // Perbarui nama guru terakhir
                    }

                    String subject = getStringValue(row.getCell(subjectNameCol));
                    if (subject.isEmpty() || currentTeacher.isEmpty()) continue;

                    // Looping melalui setiap kolom kelas untuk baris saat ini
                    for (int c = firstClassCol; c <= lastClassCol; c++) {
                        Cell cell = row.getCell(c);
                        // Hanya proses sel yang berisi angka (jumlah jam)
                        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                            int hours = (int) cell.getNumericCellValue();
                            if (hours > 0) {
                                String className = getStringValue(headerRow.getCell(c));
                                if (className != null && !className.isEmpty()) {
                                    // Buat objek Assignment baru dan tambahkan ke daftar
                                    assignments.add(new Assignment(currentTeacher, subject, className, hours));
                                }
                            }
                        }
                    }
                }
            }
        }
        return assignments;
    }

    /**
     * Metode bantuan untuk mengubah isi sel menjadi String secara aman,
     * menangani berbagai tipe data sel (String, Numeric, Formula).
     */
    private static String getStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}