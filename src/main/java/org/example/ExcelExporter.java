package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Class untuk export jadwal ke Excel dengan format:
 * - Satu minggu penuh (tidak dipotong per hari)
 * - Kolom: Hari, Waktu, Jam Ke, lalu semua kelas
 * - Isi jadwal hanya ID guru
 * - Menyertakan sholat dhuha, istirahat, sholat zuhur/kultum/jumat
 */
public class ExcelExporter {
    private final Schedule schedule;

    // Waktu per jam pelajaran
    private static final String[][] TIME_SLOTS = {
        {"06.30 - 07.15", "Sholat Dhuha"},
        {"07.15 - 07.55", "1"},
        {"07.55 - 08.35", "2"},
        {"08.35 - 09.15", "3"},
        {"09.15 - 09.55", "4"},
        {"09.55 - 10.25", "Istirahat"},
        {"10.25 - 11.05", "5"},
        {"11.05 - 11.45", "6"},
        {"11.45 - 12.20", "Sholat Zuhur dan Kultum"}, // Akan diganti untuk Jumat
        {"12.20 - 13.00", "7"},
        {"13.00 - 13.40", "8"},
        {"13.40 - 14.20", "9"},
        {"14.20 - 15.00", "10"}
    };

    // Jam pelajaran per hari
    private static final Map<String, Integer> PERIODS_PER_DAY = new HashMap<>();
    static {
        PERIODS_PER_DAY.put("Senin", 10);
        PERIODS_PER_DAY.put("Selasa", 10);
        PERIODS_PER_DAY.put("Rabu", 10);
        PERIODS_PER_DAY.put("Kamis", 9);
        PERIODS_PER_DAY.put("Jumat", 8);
    }

    public ExcelExporter(Schedule schedule) {
        this.schedule = schedule;
    }

    public void exportToExcel(String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Jadwal Pelajaran");

        // Buat styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dayStyle = createDayStyle(workbook);
        CellStyle timeStyle = createTimeStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle specialStyle = createSpecialStyle(workbook);
        CellStyle centerStyle = createCenterStyle(workbook);

        // Get sorted classes
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        Collections.sort(classes);

        // Buat header
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);

        // Kolom: Hari, Waktu, Jam Ke, lalu semua kelas
        Cell cell = headerRow.createCell(0);
        cell.setCellValue("HARI");
        cell.setCellStyle(headerStyle);

        cell = headerRow.createCell(1);
        cell.setCellValue("WAKTU");
        cell.setCellStyle(headerStyle);

        cell = headerRow.createCell(2);
        cell.setCellValue("JAM KE");
        cell.setCellStyle(headerStyle);

        for (int i = 0; i < classes.size(); i++) {
            cell = headerRow.createCell(3 + i);
            cell.setCellValue(classes.get(i));
            cell.setCellStyle(headerStyle);
        }

        // Isi data per hari
        List<String> days = schedule.getDays();

        for (String day : days) {
            int maxPeriods = PERIODS_PER_DAY.get(day);
            int startRowForDay = rowNum;

            // Iterasi semua time slots
            for (String[] timeSlot : TIME_SLOTS) {
                String time = timeSlot[0];
                String jamKe = timeSlot[1];

                // Skip jika melebihi maksimal period untuk hari ini
                if (jamKe.matches("\\d+")) { // Jika ini jam pelajaran (bukan sholat/istirahat)
                    int period = Integer.parseInt(jamKe);
                    if (period > maxPeriods) {
                        continue; // Skip jam ini untuk hari ini
                    }
                }

                Row dataRow = sheet.createRow(rowNum++);

                // Kolom Hari (akan di-merge nanti)
                cell = dataRow.createCell(0);
                cell.setCellValue(day);
                cell.setCellStyle(dayStyle);

                // Kolom Waktu
                cell = dataRow.createCell(1);
                cell.setCellValue(time);
                cell.setCellStyle(timeStyle);

                // Kolom Jam Ke
                cell = dataRow.createCell(2);
                // Khusus untuk Jumat, ganti "Sholat Zuhur dan Kultum" menjadi "Sholat Jumat"
                if (day.equals("Jumat") && jamKe.equals("Sholat Zuhur dan Kultum")) {
                    cell.setCellValue("Sholat Jumat");
                } else {
                    cell.setCellValue(jamKe);
                }
                cell.setCellStyle(centerStyle);

                // Isi data untuk setiap kelas
                boolean isSpecialTime = !jamKe.matches("\\d+"); // Sholat atau istirahat

                for (int classIdx = 0; classIdx < classes.size(); classIdx++) {
                    cell = dataRow.createCell(3 + classIdx);

                    if (isSpecialTime) {
                        // Untuk sholat/istirahat, beri tanda "-"
                        cell.setCellValue("-");
                        cell.setCellStyle(specialStyle);
                    } else {
                        // Untuk jam pelajaran, ambil ID guru
                        String className = classes.get(classIdx);
                        int period = Integer.parseInt(jamKe);

                        TimeSlot slot = schedule.getSlot(day, period, className);
                        if (slot != null && !slot.isEmpty()) {
                            Assignment assignment = slot.getAssignment();
                            cell.setCellValue(assignment.getId());
                            cell.setCellStyle(dataStyle);
                        } else {
                            cell.setCellValue("");
                            cell.setCellStyle(dataStyle);
                        }
                    }
                }
            }

            // Merge cells untuk kolom Hari
            int endRowForDay = rowNum - 1;
            if (endRowForDay > startRowForDay) {
                sheet.addMergedRegion(new CellRangeAddress(startRowForDay, endRowForDay, 0, 0));
            }
        }

        // Set column widths
        sheet.setColumnWidth(0, 3000);  // Hari
        sheet.setColumnWidth(1, 4500);  // Waktu
        sheet.setColumnWidth(2, 5500);  // Jam Ke
        for (int i = 0; i < classes.size(); i++) {
            sheet.setColumnWidth(3 + i, 3000); // Kelas
        }

        // Write to file
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }

        workbook.close();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDayStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSpecialStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
