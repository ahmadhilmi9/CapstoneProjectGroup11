package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {

    public static List<Assignment> readAssignments(String filePath, String sheetName) throws IOException {
        List<Assignment> assignments = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);

            // Asumsi dan konfigurasi tata letak sheet
            Row headerRow = sheet.getRow(1); // Baris nama-nama kelas (7A, 7B, dst.)
            int firstDataRow = 2;          // Data guru dimulai dari baris ke-3 (index 2)
            int teacherNameCol = 1;        // Kolom nama guru
            int subjectNameCol = 2;        // Kolom mata pelajaran
            int firstClassCol = 3;         // Kolom kelas pertama
            int lastClassCol = 25;         // Kolom kelas terakhir

            String lastKnownTeacher = ""; // untuk sel nama guru yang di-merge

            for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Tangani sel nama guru yang di-merge
                String teacherNameCell = getStringValue(row.getCell(teacherNameCol)).trim();
                String currentTeacher;
                if (teacherNameCell.isEmpty() || teacherNameCell.toLowerCase().contains("nip")) {
                    currentTeacher = lastKnownTeacher;
                } else {
                    currentTeacher = teacherNameCell;
                    lastKnownTeacher = teacherNameCell;
                }

                String subject = getStringValue(row.getCell(subjectNameCol));
                if (subject.isEmpty() || currentTeacher.isEmpty()) continue;

                for (int c = firstClassCol; c <= lastClassCol; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        int hours = (int) cell.getNumericCellValue();
                        if (hours > 0) {
                            String className = getStringValue(headerRow.getCell(c));
                            if (className != null && !className.isEmpty()) {
                                assignments.add(new Assignment(currentTeacher, subject, className, hours));
                            }
                        }
                    }
                }
            }
        }
        return assignments;
    }

    private static String getStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            default: return "";
        }
    }
}
