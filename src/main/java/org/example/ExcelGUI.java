package org.example;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class ExcelGUI {
    private JFrame frame = new JFrame("Excel Scheduler GUI");
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private File selectedFile;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Assignment> currentAssignments;

    public ExcelGUI() {
        this.frame.setSize(950, 550);
        this.frame.setDefaultCloseOperation(3);
        this.frame.setLocationRelativeTo((Component)null);
        this.cardLayout = new CardLayout();
        this.mainPanel = new JPanel(this.cardLayout);
        
        // PANEL 1: File Chooser
        JPanel panelFileChooser = new JPanel();
        panelFileChooser.setLayout(new BoxLayout(panelFileChooser, 1));
        panelFileChooser.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        JLabel label1 = new JLabel("Langkah 1: Pilih File Excel");
        label1.setFont(new Font("Arial", 1, 22));
        label1.setAlignmentX(0.5F);
        JButton chooseButton = new JButton("Pilih File Excel");
        chooseButton.setAlignmentX(0.5F);
        chooseButton.setFont(new Font("Arial", 0, 18));
        chooseButton.setMaximumSize(new Dimension(250, 50));
        chooseButton.setFocusPainted(false);
        panelFileChooser.add(label1);
        panelFileChooser.add(Box.createRigidArea(new Dimension(0, 30)));
        panelFileChooser.add(chooseButton);
        
        chooseButton.addActionListener((e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", new String[]{"xlsx"}));
            int option = fileChooser.showOpenDialog(this.frame);
            if (option == 0) {
                this.selectedFile = fileChooser.getSelectedFile();
                JOptionPane.showMessageDialog(this.frame, "File dipilih: " + this.selectedFile.getName());
                this.cardLayout.show(this.mainPanel, "panelActionChoice");
            }
        });
        
        // PANEL 2: Action Choice
        JPanel panelActionChoice = new JPanel();
        panelActionChoice.setLayout(new BoxLayout(panelActionChoice, 1));
        panelActionChoice.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        JLabel label2 = new JLabel("Langkah 2: Pilih Aksi");
        label2.setFont(new Font("Arial", 1, 22));
        label2.setAlignmentX(0.5F);
        
        JButton readButton = new JButton("Baca Excel");
        readButton.setFont(new Font("Arial", 0, 18));
        readButton.setAlignmentX(0.5F);
        readButton.setMaximumSize(new Dimension(250, 50));
        readButton.setFocusPainted(false);
        
        JButton generateButton = new JButton("Generate Jadwal");
        generateButton.setFont(new Font("Arial", 0, 18));
        generateButton.setAlignmentX(0.5F);
        generateButton.setMaximumSize(new Dimension(250, 50));
        generateButton.setFocusPainted(false);
        
        // TOMBOL KEMBALI KE LANGKAH 1
        JButton backToStep1Button = new JButton("← Kembali ke Langkah 1");
        backToStep1Button.setFont(new Font("Arial", 0, 16));
        backToStep1Button.setAlignmentX(0.5F);
        backToStep1Button.setMaximumSize(new Dimension(250, 40));
        backToStep1Button.setFocusPainted(false);
        backToStep1Button.addActionListener((e) -> {
            this.cardLayout.show(this.mainPanel, "panelFileChooser");
        });
        
        panelActionChoice.add(label2);
        panelActionChoice.add(Box.createRigidArea(new Dimension(0, 30)));
        panelActionChoice.add(readButton);
        panelActionChoice.add(Box.createRigidArea(new Dimension(0, 20)));
        panelActionChoice.add(generateButton);
        panelActionChoice.add(Box.createRigidArea(new Dimension(0, 40))); // Spacing
        panelActionChoice.add(backToStep1Button); // Tombol kembali
        
        readButton.addActionListener((e) -> {
            if (this.selectedFile != null) {
                try {
                    this.currentAssignments = ExcelReader.readAssignments(this.selectedFile.getAbsolutePath(), "sheet1");
                    this.displayAssignments(this.currentAssignments);
                    this.cardLayout.show(this.mainPanel, "panelTable");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this.frame, "Error membaca Excel: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        
        generateButton.addActionListener((e) -> {
            if (this.selectedFile != null) {
                try {
                    this.currentAssignments = ExcelReader.readAssignments(this.selectedFile.getAbsolutePath(), "sheet1");
                    final JDialog loadingDialog = new JDialog(this.frame, "Generating...", true);
                    JLabel loadingLabel = new JLabel("Sedang membuat jadwal, harap tunggu...", 0);
                    loadingLabel.setFont(new Font("Arial", 0, 16));
                    loadingDialog.add(loadingLabel);
                    loadingDialog.setSize(400, 100);
                    loadingDialog.setLocationRelativeTo(this.frame);
                    
                    SwingWorker<Schedule, Void> worker = new SwingWorker<Schedule, Void>() {
                        private ScheduleGenerator generator;

                        protected Schedule doInBackground() throws Exception {
                            this.generator = new ScheduleGenerator(ExcelGUI.this.currentAssignments);
                            return this.generator.generate();
                        }

                        protected void done() {
                            loadingDialog.dispose();
                            try {
                                Schedule schedule = (Schedule)this.get();
                                new ScheduleResultGUI(schedule, this.generator);
                                JOptionPane.showMessageDialog(ExcelGUI.this.frame, "Jadwal berhasil di-generate!\nSilakan lihat di window baru.", "Sukses", 1);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ExcelGUI.this.frame, "Error saat generate jadwal: " + ex.getMessage(), "Error", 0);
                                ex.printStackTrace();
                            }
                        }
                    };
                    worker.execute();
                    loadingDialog.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this.frame, "Error membaca Excel: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        
        // PANEL 3: Table
        JPanel panelTable = new JPanel(new BorderLayout());
        panelTable.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        this.tableModel = new DefaultTableModel();
        this.table = new JTable(this.tableModel);
        this.table.setFont(new Font("Arial", 0, 14));
        this.table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(this.table);
        panelTable.add(scrollPane, "Center");
        
        JButton backButton = new JButton("Kembali");
        backButton.setFont(new Font("Arial", 0, 16));
        backButton.setFocusPainted(false);
        backButton.addActionListener((e) -> this.cardLayout.show(this.mainPanel, "panelActionChoice"));
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(backButton);
        panelTable.add(bottomPanel, "South");
        
        // Add all panels to main panel
        this.mainPanel.add(panelFileChooser, "panelFileChooser");
        this.mainPanel.add(panelActionChoice, "panelActionChoice");
        this.mainPanel.add(panelTable, "panelTable");
        
        this.frame.add(this.mainPanel);
        this.cardLayout.show(this.mainPanel, "panelFileChooser");
        this.frame.setVisible(true);
    }

    private void displayAssignments(List<Assignment> assignments) {
        String[] columns = new String[]{"ID", "Nama Guru", "Mata Pelajaran", "Kelas", "Jam"};
        this.tableModel.setDataVector(new Object[0][0], columns);

        for(Assignment a : assignments) {
            Object[] row = new Object[]{a.getId(), a.getTeacher(), a.getSubject(), a.getClassName(), a.getTotalHours()};
            this.tableModel.addRow(row);
        }
    }
}
