package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * GUI untuk menampilkan hasil jadwal PER HARI
 * Format: Kolom = Kelas, Baris = Jam
 */
public class ScheduleResultGUI extends JFrame {
    private Schedule schedule;
    private JTabbedPane tabbedPane;

    public ScheduleResultGUI(Schedule schedule) {
        this.schedule = schedule;
        initUI();
    }

    private void initUI() {
        setTitle("Hasil Jadwal Pelajaran - Format Per Hari");
        setSize(1400, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        // Buat tab untuk setiap HARI (bukan per kelas)
        List<String> days = schedule.getDays();
        for (String day : days) {
            JPanel dayPanel = createDaySchedulePanel(day);
            tabbedPane.addTab(day, dayPanel);
        }

        // Tab statistik
        JPanel statsPanel = createStatisticsPanel();
        tabbedPane.addTab("📊 Statistik", statsPanel);

        // Tab validasi
        JPanel validationPanel = createValidationPanel();
        tabbedPane.addTab("✓ Validasi", validationPanel);

        add(tabbedPane);
        setVisible(true);
    }

    /**
     * Buat panel jadwal per hari
     * Format: Baris = Jam, Kolom = Kelas
     */
    private JPanel createDaySchedulePanel(String day) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header dengan info hari
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("JADWAL " + day.toUpperCase(), SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setForeground(new Color(0, 102, 204));

        int maxPeriods = schedule.getPeriodsForDay(day);
        JLabel infoLabel = new JLabel("Total: " + maxPeriods + " Jam Pelajaran", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoLabel.setForeground(Color.GRAY);

        headerPanel.add(headerLabel, BorderLayout.CENTER);
        headerPanel.add(infoLabel, BorderLayout.SOUTH);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Siapkan kolom: Jam + semua kelas
        List<String> classes = new ArrayList<>(schedule.getAllClasses());
        Collections.sort(classes); // Sort kelas: 7A, 7B, 7C, dst

        String[] columnNames = new String[classes.size() + 1];
        columnNames[0] = "JAM";
        for (int i = 0; i < classes.size(); i++) {
            columnNames[i + 1] = classes.get(i);
        }

        // Buat table model
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Isi data per jam
        int maxPeriodsForDay = schedule.getPeriodsForDay(day);
        for (int period = 1; period <= maxPeriodsForDay; period++) {
            Object[] row = new Object[classes.size() + 1];
            row[0] = String.format("Jam %d", period);

            for (int classIdx = 0; classIdx < classes.size(); classIdx++) {
                String className = classes.get(classIdx);
                TimeSlot slot = schedule.getSlot(day, period, className);

                if (slot != null && !slot.isEmpty()) {
                    Assignment a = slot.getAssignment();
                    // Format: Mapel + ID Guru
                    String cellText = String.format("<html><center><b>%s</b><br><font size='2'>ID: %s</font></center></html>",
                        truncate(a.getSubject(), 15), a.getId());
                    row[classIdx + 1] = cellText;
                } else {
                    row[classIdx + 1] = "<html><center><font color='lightgray'>-</font></center></html>";
                }
            }
            tableModel.addRow(row);
        }

        // Buat table dengan styling
        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Footer dengan legend
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footerPanel.add(new JLabel("📌 Format: Mata Pelajaran (ID Guru)"));
        panel.add(footerPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Style table untuk tampilan yang bagus
     */
    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 11));
        table.setRowHeight(60);
        table.setGridColor(new Color(220, 220, 220));
        table.setShowGrid(true);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 35));

        // Column width
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(0).setMaxWidth(100);

        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        // Custom renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (column == 0) {
                    // Kolom jam - background abu
                    c.setBackground(new Color(240, 240, 240));
                    setFont(new Font("Arial", Font.BOLD, 12));
                    setHorizontalAlignment(CENTER);
                } else {
                    // Kolom kelas - background putih atau hijau muda jika ada mapel
                    if (value != null && !value.toString().contains("lightgray")) {
                        c.setBackground(new Color(230, 255, 230)); // Hijau muda
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                    setHorizontalAlignment(CENTER);
                    setVerticalAlignment(TOP);
                }

                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    /**
     * Panel statistik lengkap
     */
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("STATISTIK JADWAL", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Split menjadi 2 bagian
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Atas: Ringkasan per guru
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Ringkasan Per Guru"));

        String[] columnNames1 = {"ID", "Nama Guru", "Mata Pelajaran", "Kelas", "Total Jam", "Terjadwal", "Status"};
        DefaultTableModel model1 = new DefaultTableModel(columnNames1, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Hitung jam terjadwal
        Map<String, Integer> scheduledHours = new HashMap<>();
        for (String day : schedule.getDays()) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty()) {
                        Assignment a = slot.getAssignment();
                        String key = a.getTeacher() + "|" + a.getSubject() + "|" + a.getClassName();
                        scheduledHours.put(key, scheduledHours.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }

        // Tampilkan data
        Set<String> processed = new HashSet<>();
        int totalComplete = 0;
        int totalIncomplete = 0;

        for (String day : schedule.getDays()) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty()) {
                        Assignment a = slot.getAssignment();
                        String key = a.getTeacher() + "|" + a.getSubject() + "|" + a.getClassName();

                        if (!processed.contains(key)) {
                            processed.add(key);
                            int scheduled = scheduledHours.getOrDefault(key, 0);
                            int total = a.getTotalHours();
                            boolean complete = (scheduled == total);

                            if (complete) totalComplete++;
                            else totalIncomplete++;

                            String status = complete ? "✓ Complete" : "✗ Incomplete";

                            Object[] row = {
                                a.getId(),
                                a.getTeacher(),
                                a.getSubject(),
                                a.getClassName(),
                                total,
                                scheduled,
                                status
                            };
                            model1.addRow(row);
                        }
                    }
                }
            }
        }

        JTable table1 = new JTable(model1);
        table1.setFont(new Font("Arial", Font.PLAIN, 11));
        table1.setRowHeight(25);
        JScrollPane scroll1 = new JScrollPane(table1);
        topPanel.add(scroll1, BorderLayout.CENTER);

        // Info summary
        JLabel summaryLabel = new JLabel(String.format(
            "  Total: %d assignments  |  Complete: %d  |  Incomplete: %d",
            totalComplete + totalIncomplete, totalComplete, totalIncomplete));
        summaryLabel.setFont(new Font("Arial", Font.BOLD, 12));
        topPanel.add(summaryLabel, BorderLayout.SOUTH);

        // Bawah: Statistik per hari
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Statistik Per Hari"));

        String[] columnNames2 = {"Hari", "Total Jam", "Jam Terisi", "Jam Kosong", "Persentase"};
        DefaultTableModel model2 = new DefaultTableModel(columnNames2, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String day : schedule.getDays()) {
            int totalSlots = 0;
            int filledSlots = 0;

            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                totalSlots += slots.size();
                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty()) filledSlots++;
                }
            }

            int emptySlots = totalSlots - filledSlots;
            double percentage = (filledSlots * 100.0) / totalSlots;

            Object[] row = {
                day,
                totalSlots,
                filledSlots,
                emptySlots,
                String.format("%.1f%%", percentage)
            };
            model2.addRow(row);
        }

        JTable table2 = new JTable(model2);
        table2.setFont(new Font("Arial", Font.PLAIN, 12));
        table2.setRowHeight(30);
        JScrollPane scroll2 = new JScrollPane(table2);
        bottomPanel.add(scroll2, BorderLayout.CENTER);

        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        splitPane.setDividerLocation(400);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Panel validasi untuk cek konflik
     */
    private JPanel createValidationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("VALIDASI JADWAL", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea validationText = new JTextArea();
        validationText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        validationText.setEditable(false);
        validationText.setMargin(new Insets(10, 10, 10, 10));

        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    HASIL VALIDASI JADWAL                       ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        // Cek konflik guru
        sb.append("1. CEK KONFLIK GURU (Tabrakan):\n");
        sb.append("   ").append("─".repeat(60)).append("\n");
        int conflicts = 0;
        for (String day : schedule.getDays()) {
            int maxPeriod = schedule.getPeriodsForDay(day);
            for (int period = 1; period <= maxPeriod; period++) {
                Map<String, List<String>> teacherClasses = new HashMap<>();

                for (String className : schedule.getAllClasses()) {
                    TimeSlot slot = schedule.getSlot(day, period, className);
                    if (slot != null && !slot.isEmpty()) {
                        String teacher = slot.getAssignment().getTeacher();
                        teacherClasses.computeIfAbsent(teacher, k -> new ArrayList<>()).add(className);
                    }
                }

                for (Map.Entry<String, List<String>> entry : teacherClasses.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        conflicts++;
                        sb.append(String.format("   ✗ KONFLIK: %s - %s Jam %d - Kelas: %s\n",
                            day, entry.getKey(), period, String.join(", ", entry.getValue())));
                    }
                }
            }
        }

        if (conflicts == 0) {
            sb.append("   ✓ TIDAK ADA KONFLIK GURU!\n");
        } else {
            sb.append(String.format("   Total Konflik: %d\n", conflicts));
        }

        // Cek assignment yang belum terjadwal lengkap
        sb.append("\n2. CEK KELENGKAPAN PENJADWALAN:\n");
        sb.append("   ").append("─".repeat(60)).append("\n");
        int incomplete = 0;

        Set<String> processed = new HashSet<>();
        for (String day : schedule.getDays()) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                for (TimeSlot slot : slots) {
                    if (!slot.isEmpty()) {
                        Assignment a = slot.getAssignment();
                        String key = a.getTeacher() + "|" + a.getSubject() + "|" + a.getClassName();

                        if (!processed.contains(key)) {
                            processed.add(key);
                            int scheduled = schedule.getScheduledHours(a);
                            int expected = a.getTotalHours();

                            if (scheduled != expected) {
                                incomplete++;
                                sb.append(String.format("   ✗ %s - %s [%s]: %d/%d jam\n",
                                    a.getTeacher(), a.getSubject(), a.getClassName(), scheduled, expected));
                            }
                        }
                    }
                }
            }
        }

        if (incomplete == 0) {
            sb.append("   ✓ SEMUA ASSIGNMENT TERJADWAL LENGKAP!\n");
        } else {
            sb.append(String.format("   Total Incomplete: %d\n", incomplete));
        }

        // Cek constraint PJOK
        sb.append("\n3. CEK CONSTRAINT PJOK (Maksimal Jam ke-5 untuk 2 jam):\n");
        sb.append("   ").append("─".repeat(60)).append("\n");
        int pjokViolations = 0;

        for (String day : schedule.getDays()) {
            for (String className : schedule.getAllClasses()) {
                List<TimeSlot> slots = schedule.getSlotsForClass(day, className);
                for (int i = 0; i < slots.size() - 1; i++) {
                    TimeSlot slot = slots.get(i);
                    if (slot.isEmpty()) continue;

                    if (slot.getAssignment().getSubject().toUpperCase().contains("PJOK")) {
                        TimeSlot next = slots.get(i + 1);
                        if (!next.isEmpty() && next.getAssignment().getSubject().toUpperCase().contains("PJOK")) {
                            if (slot.getPeriod() > 4) {
                                pjokViolations++;
                                sb.append(String.format("   ✗ PJOK 2 jam di %s [%s] Jam %d-%d (harusnya max jam 4-5)\n",
                                    day, className, slot.getPeriod(), next.getPeriod()));
                            }
                        }
                    }
                }
            }
        }

        if (pjokViolations == 0) {
            sb.append("   ✓ CONSTRAINT PJOK TERPENUHI!\n");
        }

        // Cek constraint MGMP
        sb.append("\n4. CEK CONSTRAINT MGMP (Rabu setelah istirahat):\n");
        sb.append("   ").append("─".repeat(60)).append("\n");
        int mgmpViolations = 0;

        Set<String> mgmpSubjects = new HashSet<>(Arrays.asList(
            "SKI", "B.ARAB", "AQIDAH AKHLAK", "QURDITS", "FIQIH"));

        for (String className : schedule.getAllClasses()) {
            List<TimeSlot> slots = schedule.getSlotsForClass("Rabu", className);
            for (TimeSlot slot : slots) {
                if (slot.isEmpty()) continue;
                if (slot.getPeriod() >= 6) {
                    String subject = slot.getAssignment().getSubject().toUpperCase();
                    for (String mgmp : mgmpSubjects) {
                        if (subject.contains(mgmp)) {
                            mgmpViolations++;
                            sb.append(String.format("   ✗ %s [%s] di Rabu Jam %d (seharusnya < jam 6)\n",
                                subject, className, slot.getPeriod()));
                            break;
                        }
                    }
                }
            }
        }

        if (mgmpViolations == 0) {
            sb.append("   ✓ CONSTRAINT MGMP TERPENUHI!\n");
        }

        // Summary
        sb.append("\n").append("═".repeat(66)).append("\n");
        sb.append("SUMMARY:\n");
        int totalViolations = conflicts + incomplete + pjokViolations + mgmpViolations;

        if (totalViolations == 0) {
            sb.append("✓✓✓ JADWAL SEMPURNA! TIDAK ADA MASALAH! ✓✓✓\n");
        } else {
            sb.append(String.format("Total Violations: %d\n", totalViolations));
            sb.append("  - Konflik Guru: " + conflicts + "\n");
            sb.append("  - Incomplete: " + incomplete + "\n");
            sb.append("  - PJOK Violations: " + pjokViolations + "\n");
            sb.append("  - MGMP Violations: " + mgmpViolations + "\n");
        }
        sb.append("═".repeat(66)).append("\n");

        validationText.setText(sb.toString());
        validationText.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(validationText);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

