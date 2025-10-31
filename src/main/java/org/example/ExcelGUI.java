package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

public class ExcelGUI {

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private File selectedFile;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Assignment> currentAssignments; // Simpan assignments untuk generate jadwal

    public ExcelGUI() {
        frame = new JFrame("Excel Scheduler GUI");
        frame.setSize(950, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center window

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // ===== Panel 1: Pilih File Excel =====
        JPanel panelFileChooser = new JPanel();
        panelFileChooser.setLayout(new BoxLayout(panelFileChooser, BoxLayout.Y_AXIS));
        panelFileChooser.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        JLabel label1 = new JLabel("Langkah 1: Pilih File Excel");
        label1.setFont(new Font("Arial", Font.BOLD, 22));
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton chooseButton = new JButton("Pilih File Excel");
        chooseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseButton.setFont(new Font("Arial", Font.PLAIN, 18));
        chooseButton.setMaximumSize(new Dimension(250, 50));
        chooseButton.setFocusPainted(false);

        panelFileChooser.add(label1);
        panelFileChooser.add(Box.createRigidArea(new Dimension(0, 30)));
        panelFileChooser.add(chooseButton);

        chooseButton.addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                JOptionPane.showMessageDialog(frame, "File dipilih: " + selectedFile.getName());
                cardLayout.show(mainPanel, "panelActionChoice");
            }
        });

        // ===== Panel 2: Pilih Aksi =====
        JPanel panelActionChoice = new JPanel();
        panelActionChoice.setLayout(new BoxLayout(panelActionChoice, BoxLayout.Y_AXIS));
        panelActionChoice.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JLabel label2 = new JLabel("Langkah 2: Pilih Aksi");
        label2.setFont(new Font("Arial", Font.BOLD, 22));
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton readButton = new JButton("Baca Excel");
        readButton.setFont(new Font("Arial", Font.PLAIN, 18));
        readButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        readButton.setMaximumSize(new Dimension(250, 50));
        readButton.setFocusPainted(false);

        JButton generateButton = new JButton("Generate Jadwal");
        generateButton.setFont(new Font("Arial", Font.PLAIN, 18));
        generateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateButton.setMaximumSize(new Dimension(250, 50));
        generateButton.setFocusPainted(false);

        panelActionChoice.add(label2);
        panelActionChoice.add(Box.createRigidArea(new Dimension(0, 30)));
        panelActionChoice.add(readButton);
        panelActionChoice.add(Box.createRigidArea(new Dimension(0, 20)));
        panelActionChoice.add(generateButton);

        // Action Baca Excel
        readButton.addActionListener((ActionEvent e) -> {
            if (selectedFile != null) {
                try {
                    currentAssignments = ExcelReader.readAssignments(selectedFile.getAbsolutePath(), "sheet1");
                    displayAssignments(currentAssignments);
                    cardLayout.show(mainPanel, "panelTable");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error membaca Excel: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        // Action Generate Jadwal
        generateButton.addActionListener((ActionEvent e) -> {
            if (selectedFile != null) {
                try {
                    // Baca assignments dari Excel
                    currentAssignments = ExcelReader.readAssignments(selectedFile.getAbsolutePath(), "sheet1");

                    // Tampilkan loading dialog
                    JDialog loadingDialog = new JDialog(frame, "Generating...", true);
                    JLabel loadingLabel = new JLabel("Sedang membuat jadwal, harap tunggu...", SwingConstants.CENTER);
                    loadingLabel.setFont(new Font("Arial", Font.PLAIN, 16));
                    loadingDialog.add(loadingLabel);
                    loadingDialog.setSize(400, 100);
                    loadingDialog.setLocationRelativeTo(frame);

                    // Generate jadwal di thread terpisah
                    SwingWorker<Schedule, Void> worker = new SwingWorker<Schedule, Void>() {
                        @Override
                        protected Schedule doInBackground() throws Exception {
                            ScheduleGenerator generator = new ScheduleGenerator(currentAssignments);
                            return generator.generate();
                        }

                        @Override
                        protected void done() {
                            loadingDialog.dispose();
                            try {
                                Schedule schedule = get();
                                // Tampilkan hasil jadwal di window baru
                                new ScheduleResultGUI(schedule);
                                JOptionPane.showMessageDialog(frame,
                                        "Jadwal berhasil di-generate!\nSilakan lihat di window baru.",
                                        "Sukses",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame,
                                        "Error saat generate jadwal: " + ex.getMessage(),
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
                                ex.printStackTrace();
                            }
                        }
                    };

                    worker.execute();
                    loadingDialog.setVisible(true);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error membaca Excel: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        // ===== Panel 3: Table =====
        JPanel panelTable = new JPanel(new BorderLayout());
        panelTable.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(table);
        panelTable.add(scrollPane, BorderLayout.CENTER);

        JButton backButton = new JButton("Kembali");
        backButton.setFont(new Font("Arial", Font.PLAIN, 16));
        backButton.setFocusPainted(false);
        backButton.addActionListener((ActionEvent e) -> cardLayout.show(mainPanel, "panelActionChoice"));
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(backButton);
        panelTable.add(bottomPanel, BorderLayout.SOUTH);

        // Tambahkan panel ke mainPanel
        mainPanel.add(panelFileChooser, "panelFileChooser");
        mainPanel.add(panelActionChoice, "panelActionChoice");
        mainPanel.add(panelTable, "panelTable");

        frame.add(mainPanel);
        cardLayout.show(mainPanel, "panelFileChooser"); // tampil panel pertama
        frame.setVisible(true);
    }

    private void displayAssignments(List<Assignment> assignments) {
        String[] columns = {"ID", "Nama Guru", "Mata Pelajaran", "Kelas", "Jam"};
        tableModel.setDataVector(new Object[0][0], columns);

        for (Assignment a : assignments) {
            Object[] row = {a.getId(), a.getTeacher(), a.getSubject(), a.getClassName(), a.getTotalHours()};
            tableModel.addRow(row);
        }
    }

}
