package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * ExcelExporter versi terbaru:
 * - Sheet "Jadwal Pelajaran" (ID guru) — seperti semula
 * - Sheet "Per Kelas" (satu sheet untuk semua kelas; isi = NAMA GURU)
 * - Sheet "Per Guru" (satu sheet untuk semua guru; isi = MATA PELAJARAN)
 * - Tambahan: buat satu sheet per guru di dalam workbook (file tetap disimpan ke filePath).
 *   Nama file/paths dikendalikan oleh caller; sheet per-guru dinamai "Jadwal Mengajar Guru - <Guru>".
 */
public class ExcelExporter {
    private final Schedule schedule;

    private static final String[][] TIME_SLOTS = {
            {"06.30 - 07.15", "Sholat Dhuha"},
            {"07.15 - 07.55", "1"},
            {"07.55 - 08.35", "2"},
            {"08.35 - 09.15", "3"},
            {"09.15 - 09.55", "4"},
            {"09.55 - 10.25", "Istirahat"},
            {"10.25 - 11.05", "5"},
            {"11.05 - 11.45", "6"},
            {"11.45 - 12.20", "Sholat Zuhur dan Kultum"},
            {"12.20 - 13.00", "7"},
            {"13.00 - 13.40", "8"},
            {"13.40 - 14.20", "9"},
            {"14.20 - 15.00", "10"}
    };

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

        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dayStyle = createDayStyle(workbook);
        CellStyle timeStyle = createTimeStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle specialStyle = createSpecialStyle(workbook);
        CellStyle centerStyle = createCenterStyle(workbook);

        // sorted classes
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        Collections.sort(classes);

        // collect teachers from schedule (use teacher name if available, else ID)
        List<String> teachers = collectAllTeachers();
        Collections.sort(teachers);

        // 1) main sheet (ID guru) — existing format
        Sheet mainSheet = workbook.createSheet("Jadwal Pelajaran (ID)");
        fillMainSheet(mainSheet, classes, headerStyle, dayStyle, timeStyle, dataStyle, specialStyle, centerStyle);

        // 2) single sheet "Per Kelas" (kolom per kelas, isi = nama guru)
        //Sheet perKelasSheet = workbook.createSheet("Jadwal Guru");
        //fillPerKelasSheet(perKelasSheet, classes, headerStyle, dayStyle, timeStyle, dataStyle, specialStyle, centerStyle);

        // 3) single sheet "Per Guru" (kolom per kelas; isi = MATA PELAJARAN per kelas)
        //Sheet perGuruSheet = workbook.createSheet("Jadwal Pelajaran");
        //fillPerGuruSheet(perGuruSheet, teachers, classes, headerStyle, dayStyle, timeStyle, dataStyle, specialStyle, centerStyle);

        // 4) sheets per teacher — each sheet named after teacher, contents: HARI | JAM KE | WAKTU | KELAS | MATA PELAJARAN
        createPerTeacherSheets(workbook, teachers, classes, headerStyle, dayStyle, timeStyle, dataStyle);

        //5) per kelas
        createPerClassSheets(workbook, classes, headerStyle, dayStyle, timeStyle, dataStyle, specialStyle);

        // write file
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            workbook.write(out);
        }
        workbook.close();
    }

    // ---------- sheet utama (ID guru) ----------
    private void fillMainSheet(Sheet sheet, List<String> classes,
                               CellStyle headerStyle, CellStyle dayStyle, CellStyle timeStyle,
                               CellStyle dataStyle, CellStyle specialStyle, CellStyle centerStyle) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        createHeaderCells(headerRow, headerStyle, "HARI", "WAKTU", "JAM KE");
        for (int i = 0; i < classes.size(); i++) {
            Cell c = headerRow.createCell(3 + i);
            c.setCellValue(classes.get(i));
            c.setCellStyle(headerStyle);
        }

        List<String> days = schedule.getDays();
        for (String day : days) {
            int maxPeriods = PERIODS_PER_DAY.getOrDefault(day, 0);
            int startRowForDay = rowNum;

            for (String[] timeSlot : TIME_SLOTS) {
                String time = timeSlot[0];
                String jamKe = timeSlot[1];

                if (jamKe.matches("\\d+")) {
                    int p = Integer.parseInt(jamKe);
                    if (p > maxPeriods) continue;
                }

                Row r = sheet.createRow(rowNum++);
                Cell cell = r.createCell(0);
                cell.setCellValue(day); cell.setCellStyle(dayStyle);

                cell = r.createCell(1);
                cell.setCellValue(time); cell.setCellStyle(timeStyle);

                cell = r.createCell(2);
                if (day.equals("Jumat") && jamKe.equals("Sholat Zuhur dan Kultum")) {
                    cell.setCellValue("Sholat Jumat");
                } else {
                    cell.setCellValue(jamKe);
                }
                cell.setCellStyle(centerStyle);

                boolean isSpecial = !jamKe.matches("\\d+");
                for (int ci = 0; ci < classes.size(); ci++) {
                    Cell dataCell = r.createCell(3 + ci);
                    if (isSpecial) {
                        dataCell.setCellValue("-"); dataCell.setCellStyle(specialStyle);
                    } else {
                        int period = Integer.parseInt(jamKe);
                        TimeSlot slot = schedule.getSlot(day, period, classes.get(ci));
                        if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                            dataCell.setCellValue(slot.getAssignment().getId());
                        } else {
                            dataCell.setCellValue("");
                        }
                        dataCell.setCellStyle(dataStyle);
                    }
                }
            }

            int endRow = rowNum - 1;
            if (endRow > startRowForDay) {
                sheet.addMergedRegion(new CellRangeAddress(startRowForDay, endRow, 0, 0));
            }
        }

        // column widths
        sheet.setColumnWidth(0, 3000); sheet.setColumnWidth(1, 4500); sheet.setColumnWidth(2, 5500);
        for (int i = 0; i < classes.size(); i++) sheet.setColumnWidth(3 + i, 3000);
    }

    // ---------- sheet "Per Kelas" (satu sheet, kolom per kelas; isi = NAMA GURU) ----------
    private void fillPerKelasSheet(Sheet sheet, List<String> classes,
                                   CellStyle headerStyle, CellStyle dayStyle, CellStyle timeStyle,
                                   CellStyle dataStyle, CellStyle specialStyle, CellStyle centerStyle) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        createHeaderCells(headerRow, headerStyle, "HARI", "WAKTU", "JAM KE");
        for (int i = 0; i < classes.size(); i++) {
            Cell c = headerRow.createCell(3 + i);
            c.setCellValue(classes.get(i));
            c.setCellStyle(headerStyle);
        }

        List<String> days = schedule.getDays();
        for (String day : days) {
            int maxPeriods = PERIODS_PER_DAY.getOrDefault(day, 0);
            int startRowForDay = rowNum;

            for (String[] timeSlot : TIME_SLOTS) {
                String time = timeSlot[0];
                String jamKe = timeSlot[1];

                if (jamKe.matches("\\d+")) {
                    int p = Integer.parseInt(jamKe);
                    if (p > maxPeriods) continue;
                }

                Row r = sheet.createRow(rowNum++);
                Cell cell = r.createCell(0);
                cell.setCellValue(day); cell.setCellStyle(dayStyle);

                cell = r.createCell(1);
                cell.setCellValue(time); cell.setCellStyle(timeStyle);

                cell = r.createCell(2);
                if (day.equals("Jumat") && jamKe.equals("Sholat Zuhur dan Kultum")) {
                    cell.setCellValue("Sholat Jumat");
                } else {
                    cell.setCellValue(jamKe);
                }
                cell.setCellStyle(centerStyle);

                boolean isSpecial = !jamKe.matches("\\d+");
                for (int ci = 0; ci < classes.size(); ci++) {
                    Cell dataCell = r.createCell(3 + ci);
                    if (isSpecial) {
                        dataCell.setCellValue("-"); dataCell.setCellStyle(specialStyle);
                    } else {
                        int period = Integer.parseInt(jamKe);
                        String className = classes.get(ci);
                        TimeSlot slot = schedule.getSlot(day, period, className);
                        if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                            Assignment a = slot.getAssignment();
                            String teacherName = a.getTeacher();
                            if (teacherName == null || teacherName.trim().isEmpty()) teacherName = a.getId();
                            dataCell.setCellValue(teacherName);
                        } else {
                            dataCell.setCellValue("");
                        }
                        dataCell.setCellStyle(dataStyle);
                    }
                }
            }

            int endRow = rowNum - 1;
            if (endRow > startRowForDay) {
                sheet.addMergedRegion(new CellRangeAddress(startRowForDay, endRow, 0, 0));
            }
        }

        // widths
        sheet.setColumnWidth(0, 3000); sheet.setColumnWidth(1, 4500); sheet.setColumnWidth(2, 4000);
        for (int i = 0; i < classes.size(); i++) sheet.setColumnWidth(3 + i, 6000);
    }

    // ---------- sheet "Per Guru" (diubah: satu sheet, kolom per kelas; isi = MATA PELAJARAN per kelas) ----------
    private void fillPerGuruSheet(Sheet sheet, List<String> teachers, List<String> classes,
                                  CellStyle headerStyle, CellStyle dayStyle, CellStyle timeStyle,
                                  CellStyle dataStyle, CellStyle specialStyle, CellStyle centerStyle) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        createHeaderCells(headerRow, headerStyle, "HARI", "WAKTU", "JAM KE");
        for (int i = 0; i < classes.size(); i++) {
            Cell c = headerRow.createCell(3 + i);
            c.setCellValue(classes.get(i));
            c.setCellStyle(headerStyle);
        }

        List<String> days = schedule.getDays();
        for (String day : days) {
            int maxPeriods = PERIODS_PER_DAY.getOrDefault(day, 0);
            int startRowForDay = rowNum;

            for (String[] timeSlot : TIME_SLOTS) {
                String time = timeSlot[0];
                String jamKe = timeSlot[1];

                if (jamKe.matches("\\d+")) {
                    int p = Integer.parseInt(jamKe);
                    if (p > maxPeriods) continue;
                }

                Row r = sheet.createRow(rowNum++);
                Cell cell = r.createCell(0);
                cell.setCellValue(day); cell.setCellStyle(dayStyle);

                cell = r.createCell(1);
                cell.setCellValue(time); cell.setCellStyle(timeStyle);

                cell = r.createCell(2);
                if (day.equals("Jumat") && jamKe.equals("Sholat Zuhur dan Kultum")) {
                    cell.setCellValue("Sholat Jumat");
                } else {
                    cell.setCellValue(jamKe);
                }
                cell.setCellStyle(centerStyle);

                boolean isSpecial = !jamKe.matches("\\d+");
                for (int ci = 0; ci < classes.size(); ci++) {
                    Cell dataCell = r.createCell(3 + ci);
                    if (isSpecial) {
                        dataCell.setCellValue("-"); dataCell.setCellStyle(specialStyle);
                    } else {
                        int period = Integer.parseInt(jamKe);
                        String className = classes.get(ci);
                        TimeSlot slot = schedule.getSlot(day, period, className);
                        if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                            Assignment a = slot.getAssignment();
                            String subject = a.getSubject();
                            dataCell.setCellValue(subject != null ? subject : "");
                        } else {
                            dataCell.setCellValue("");
                        }
                        dataCell.setCellStyle(dataStyle);
                    }
                }
            }

            int endRow = rowNum - 1;
            if (endRow > startRowForDay) {
                sheet.addMergedRegion(new CellRangeAddress(startRowForDay, endRow, 0, 0));
            }
        }

        // widths
        sheet.setColumnWidth(0, 3000); sheet.setColumnWidth(1, 4500); sheet.setColumnWidth(2, 4000);
        for (int i = 0; i < classes.size(); i++) sheet.setColumnWidth(3 + i, 6000);
    }

    // ---------- new: create one sheet per teacher (include special slots) with merged HARI column ----------
    private void createPerTeacherSheets(Workbook workbook, List<String> teachers, List<String> classes,
                                        CellStyle headerStyle, CellStyle dayStyle, CellStyle timeStyle, CellStyle dataStyle) {
        List<String> days = schedule.getDays();

        for (String teacher : teachers) {
            String sheetBaseName = "Guru - " + teacher;
            String sheetName = sanitizeSheetName(sheetBaseName);
            Sheet sheet = workbook.createSheet(sheetName);

            // header: HARI | JAM KE | WAKTU | KELAS | MATA PELAJARAN
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            Cell hc = headerRow.createCell(0); hc.setCellValue("HARI"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(1); hc.setCellValue("JAM KE"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(2); hc.setCellValue("WAKTU"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(3); hc.setCellValue("KELAS"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(4); hc.setCellValue("MATA PELAJARAN"); hc.setCellStyle(headerStyle);

            // iterate days and time slots
            for (String day : days) {
                int maxPeriods = PERIODS_PER_DAY.getOrDefault(day, 0);
                int startRowForDay = rowNum; // remember first row for this day

                boolean firstRowOfDay = true;

                for (String[] timeSlot : TIME_SLOTS) {
                    String time = timeSlot[0];
                    String jamKe = timeSlot[1];

                    // numeric slot: include only up to maxPeriods
                    boolean isNumeric = jamKe.matches("\\d+");
                    if (isNumeric) {
                        int period = Integer.parseInt(jamKe);
                        if (period > maxPeriods) continue;

                        // for numeric slots: check each class for assignment by this teacher
                        for (String className : classes) {
                            TimeSlot slot = schedule.getSlot(day, period, className);
                            if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                                Assignment a = slot.getAssignment();
                                String teacherName = a.getTeacher();
                                String teacherId = a.getId();
                                String teacherKey = (teacherName != null && !teacherName.trim().isEmpty()) ? teacherName : teacherId;
                                if (teacherKey == null) continue;
                                if (teacherKey.equals(teacher)) {
                                    Row r = sheet.createRow(rowNum++);
                                    // HARI: only set on first row of this day (will be merged later)
                                    Cell c;
                                    if (firstRowOfDay) {
                                        c = r.createCell(0); c.setCellValue(day); c.setCellStyle(dayStyle);
                                        firstRowOfDay = false;
                                    } else {
                                        c = r.createCell(0); c.setCellStyle(dayStyle);
                                    }

                                    c = r.createCell(1); c.setCellValue(jamKe); c.setCellStyle(timeStyle);
                                    c = r.createCell(2); c.setCellValue(time); c.setCellStyle(timeStyle);
                                    c = r.createCell(3); c.setCellValue(className); c.setCellStyle(dataStyle);
                                    c = r.createCell(4); c.setCellValue(a.getSubject() != null ? a.getSubject() : ""); c.setCellStyle(dataStyle);
                                }
                            }
                        }
                    } else {
                        // non-numeric (special) slots: include one row (kelas = "-"; mata pelajaran = label slot)
                        Row r = sheet.createRow(rowNum++);
                        Cell c;
                        if (firstRowOfDay) {
                            c = r.createCell(0); c.setCellValue(day); c.setCellStyle(dayStyle);
                            firstRowOfDay = false;
                        } else {
                            c = r.createCell(0); c.setCellStyle(dayStyle);
                        }

                        c = r.createCell(1); c.setCellValue(jamKe); c.setCellStyle(timeStyle);
                        String jamLabel = (day.equals("Jumat") && jamKe.equals("Sholat Zuhur dan Kultum")) ? "Sholat Jumat" : time;
                        c = r.createCell(2); c.setCellValue(jamLabel); c.setCellStyle(timeStyle);
                        c = r.createCell(3); c.setCellValue("-"); c.setCellStyle(dataStyle);
                        c = r.createCell(4); c.setCellValue(jamKe); c.setCellStyle(dataStyle);
                    }
                }

                int endRowForDay = rowNum - 1;
                if (endRowForDay >= startRowForDay) {
                    // merge HARI column (col 0) vertically for this day's rows
                    sheet.addMergedRegion(new CellRangeAddress(startRowForDay, endRowForDay, 0, 0));
                    // ensure top cell of merged region has the dayStyle (already set), others keep style but no text
                } else {
                    // jika tidak ada baris untuk hari ini (mis. semua period > max), buat satu baris kosong dengan HARI digabung 1 baris
                    Row r = sheet.createRow(rowNum++);
                    Cell c = r.createCell(0); c.setCellValue(day); c.setCellStyle(dayStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 0));
                }
            }

            // set sensible column widths
            sheet.setColumnWidth(0, 4000); // HARI
            sheet.setColumnWidth(1, 3000); // JAM KE
            sheet.setColumnWidth(2, 6000); // WAKTU
            sheet.setColumnWidth(3, 5000); // KELAS
            sheet.setColumnWidth(4, 8000); // MATA PELAJARAN
        }
    }

    private void createPerClassSheets(Workbook workbook, List<String> classes,
                                      CellStyle headerStyle, CellStyle dayStyle,
                                      CellStyle timeStyle, CellStyle dataStyle, CellStyle specialStyle) {
        List<String> days = schedule.getDays();

        for (String className : classes) {
            String sheetnam = "Kelas - " + className;
            String sheetName = sanitizeSheetName(sheetnam);
            Sheet sheet = workbook.createSheet(sheetName);

            // header: HARI | JAM KE | WAKTU | MATA PELAJARAN | NAMA GURU
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            Cell hc = headerRow.createCell(0); hc.setCellValue("HARI"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(1); hc.setCellValue("JAM KE"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(2); hc.setCellValue("WAKTU"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(3); hc.setCellValue("MATA PELAJARAN"); hc.setCellStyle(headerStyle);
            hc = headerRow.createCell(4); hc.setCellValue("NAMA GURU"); hc.setCellStyle(headerStyle);

            // iterate days and time slots
            for (String day : days) {
                int maxPeriods = PERIODS_PER_DAY.getOrDefault(day, 0);
                int startRowForDay = rowNum;
                boolean firstRowOfDay = true;

                for (String[] timeSlot : TIME_SLOTS) {
                    String time = timeSlot[0];
                    String jamKe = timeSlot[1];

                    boolean isNumeric = jamKe.matches("\\d+");
                    if (isNumeric) {
                        int period = Integer.parseInt(jamKe);
                        if (period > maxPeriods) continue;

                        TimeSlot slot = schedule.getSlot(day, period, className);
                        if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                            Assignment a = slot.getAssignment();
                            Row r = sheet.createRow(rowNum++);
                            Cell c;
                            if (firstRowOfDay) {
                                c = r.createCell(0); c.setCellValue(day); c.setCellStyle(dayStyle);
                                firstRowOfDay = false;
                            } else {
                                c = r.createCell(0); c.setCellStyle(dayStyle);
                            }

                            c = r.createCell(1); c.setCellValue(jamKe); c.setCellStyle(timeStyle);
                            c = r.createCell(2); c.setCellValue(time); c.setCellStyle(timeStyle);
                            c = r.createCell(3); c.setCellValue(a.getSubject() != null ? a.getSubject() : ""); c.setCellStyle(dataStyle);
                            String teacherName = a.getTeacher();
                            if (teacherName == null || teacherName.trim().isEmpty()) teacherName = a.getId();
                            c = r.createCell(4); c.setCellValue(teacherName != null ? teacherName : ""); c.setCellStyle(dataStyle);
                        }
                    } else {
                        // special slot (mis. Sholat, Istirahat, kultum)
                        Row r = sheet.createRow(rowNum++);
                        Cell c;
                        if (firstRowOfDay) {
                            c = r.createCell(0); c.setCellValue(day); c.setCellStyle(dayStyle);
                            firstRowOfDay = false;
                        } else {
                            c = r.createCell(0); c.setCellStyle(dayStyle);
                        }

                        c = r.createCell(1); c.setCellValue(jamKe); c.setCellStyle(timeStyle);
                        String jamLabel = (day.equals("Jumat") && jamKe.equals("Sholat Zuhur dan Kultum")) ? "Sholat Jumat" : time;
                        c = r.createCell(2); c.setCellValue(jamLabel); c.setCellStyle(timeStyle);
                        c = r.createCell(3); c.setCellValue(jamKe); c.setCellStyle(specialStyle);
                        c = r.createCell(4); c.setCellValue("-"); c.setCellStyle(specialStyle);
                    }
                }

                int endRowForDay = rowNum - 1;
                if (endRowForDay >= startRowForDay) {
                    sheet.addMergedRegion(new CellRangeAddress(startRowForDay, endRowForDay, 0, 0));
                } else {
                    // jika tidak ada baris untuk hari ini, buat satu baris dengan HARI saja
                    Row r = sheet.createRow(rowNum++);
                    Cell c = r.createCell(0); c.setCellValue(day); c.setCellStyle(dayStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 0));
                }
            }

            // set column widths
            sheet.setColumnWidth(0, 4000); // HARI
            sheet.setColumnWidth(1, 3000); // JAM KE
            sheet.setColumnWidth(2, 6000); // WAKTU
            sheet.setColumnWidth(3, 8000); // MATA PELAJARAN
            sheet.setColumnWidth(4, 6000); // NAMA GURU
        }
    }

    // ---------- utilities ----------
    private void createHeaderCells(Row headerRow, CellStyle headerStyle, String col0, String col1, String col2) {
        Cell c = headerRow.createCell(0);
        c.setCellValue(col0); c.setCellStyle(headerStyle);
        c = headerRow.createCell(1);
        c.setCellValue(col1); c.setCellStyle(headerStyle);
        c = headerRow.createCell(2);
        c.setCellValue(col2); c.setCellStyle(headerStyle);
    }

    // collect all teachers (prefer nama guru, fallback ke id)
    private List<String> collectAllTeachers() {
        Set<String> set = new HashSet<>();
        Map<String, Map<String, List<TimeSlot>>> full = schedule.getFullSchedule();
        for (Map<String, List<TimeSlot>> dayMap : full.values()) {
            for (List<TimeSlot> slots : dayMap.values()) {
                for (TimeSlot slot : slots) {
                    if (slot != null && !slot.isEmpty() && slot.getAssignment() != null) {
                        Assignment a = slot.getAssignment();
                        String teacherName = a.getTeacher();
                        String id = a.getId();
                        if (teacherName != null && !teacherName.trim().isEmpty()) set.add(teacherName);
                        else if (id != null && !id.trim().isEmpty()) set.add(id);
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    private String sanitizeSheetName(String name) {
        if (name == null) return "sheet";
        String s = name.replaceAll("[\\\\/?*\\[\\]]", "_");
        if (s.length() > 31) s = s.substring(0, 31);
        return s;
    }

    // ---------- styles ----------
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