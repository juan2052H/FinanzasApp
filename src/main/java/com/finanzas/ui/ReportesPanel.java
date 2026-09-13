package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.data.ExportService;
import com.finanzas.data.ReportPeriod;
import com.finanzas.data.ReportSnapshot;
import com.finanzas.model.Money;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.BarChartPanel;
import com.finanzas.ui.components.CardPanel;
import com.finanzas.ui.components.DonutChartPanel;
import com.finanzas.ui.components.PageHeader;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class ReportesPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final ExportService exportService = ExportService.getInstance();
    private final NumberFormat nf = NumberFormat.getInstance(data.getDisplayLocale());
    private final DateTimeFormatter displayDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private JPanel statsPanel;
    private JPanel chartArea;
    private JPanel categoryArea;
    private JComboBox<String> periodoBox;
    private JTextField fromField;
    private JTextField toField;

    public ReportesPanel() {
        nf.setMaximumFractionDigits(0);
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
        data.addListener(() -> SwingUtilities.invokeLater(this::refresh));
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(AppColors.MAIN_BG);
        content.setBorder(BorderFactory.createEmptyBorder(8, 20, 20, 20));

        statsPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        statsPanel.setBackground(AppColors.MAIN_BG);
        statsPanel.setPreferredSize(new Dimension(0, 100));

        chartArea = new JPanel(new GridLayout(1, 2, 16, 0));
        chartArea.setBackground(AppColors.MAIN_BG);

        categoryArea = card();
        categoryArea.setPreferredSize(new Dimension(0, 220));

        content.add(statsPanel, BorderLayout.NORTH);
        content.add(chartArea, BorderLayout.CENTER);
        content.add(categoryArea, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new PageHeader("Reportes financieros", "Analisis por periodo con datos reales");
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(AppColors.MAIN_BG);

        periodoBox = new JComboBox<String>(ReportPeriod.labels());
        periodoBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        fromField = dateField();
        toField = dateField();
        updateDateFieldsFromSelection();

        periodoBox.addActionListener(e -> {
            updateDateFieldsFromSelection();
            refresh();
        });

        RoundedButton applyButton = new RoundedButton("Aplicar", AppColors.TEXT_MUTED);
        applyButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
        applyButton.addActionListener(e -> refresh());

        RoundedButton exportButton = new RoundedButton("Exportar reporte", AppColors.ACCENT_BLUE);
        exportButton.addActionListener(e -> mostrarOpcionesExportacion());

        RoundedButton dianButton = new RoundedButton("Preparacion DIAN", new java.awt.Color(0x27AE60));
        dianButton.setForeground(java.awt.Color.WHITE);
        dianButton.addActionListener(e -> {
            com.finanzas.ui.dialogs.DianTaxDialog.show(SwingUtilities.getWindowAncestor(this));
        });

        right.add(new JLabel("Periodo:"));
        right.add(periodoBox);
        right.add(new JLabel("Desde:"));
        right.add(fromField);
        right.add(new JLabel("Hasta:"));
        right.add(toField);
        right.add(applyButton);
        right.add(exportButton);
        right.add(dianButton);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JTextField dateField() {
        JTextField field = new JTextField(8);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }

    private void updateDateFieldsFromSelection() {
        if (periodoBox == null || fromField == null || toField == null) {
            return;
        }
        String selected = (String) periodoBox.getSelectedItem();
        boolean custom = ReportPeriod.CUSTOM.equals(selected);
        fromField.setEnabled(custom);
        toField.setEnabled(custom);
        if (!custom) {
            ReportPeriod period = ReportPeriod.fromLabel(selected);
            fromField.setText(period.getStartDate() == null ? "" : period.getStartDate().format(displayDateFormat));
            toField.setText(period.getEndDate() == null ? "" : period.getEndDate().format(displayDateFormat));
        } else if (fromField.getText().trim().isEmpty() || toField.getText().trim().isEmpty()) {
            LocalDate today = LocalDate.now();
            fromField.setText(today.minusDays(29).format(displayDateFormat));
            toField.setText(today.format(displayDateFormat));
        }
    }

    private ReportSnapshot resolveSnapshot(boolean showErrors) {
        try {
            String selected = (String) periodoBox.getSelectedItem();
            if (ReportPeriod.CUSTOM.equals(selected)) {
                LocalDate start = LocalDate.parse(fromField.getText().trim(), displayDateFormat);
                LocalDate end = LocalDate.parse(toField.getText().trim(), displayDateFormat);
                return data.getReportSnapshot(selected, start, end);
            }
            return data.getReportSnapshot(selected);
        } catch (Exception ex) {
            if (showErrors) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Rango invalido", JOptionPane.ERROR_MESSAGE);
            }
            return data.getReportSnapshot(ReportPeriod.ALL_HISTORY);
        }
    }

    private void mostrarOpcionesExportacion() {
        ReportSnapshot snapshot = resolveSnapshot(true);
        boolean backend = data.isBackendSessionActive();
        String[] options = backend ? new String[]{"CSV", "PDF", "Excel", "Cancelar"} : new String[]{"PDF", "Excel", "Cancelar"};
        int selected = JOptionPane.showOptionDialog(
                this,
                "Selecciona el formato del reporte para " + snapshot.getPeriod().getDisplayLabel(),
                "Exportar reporte",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (backend && selected == 0) {
            exportarCsv(snapshot);
            return;
        }
        int pdfIndex = backend ? 1 : 0;
        int excelIndex = backend ? 2 : 1;
        if (selected == pdfIndex) {
            exportarPdf(snapshot);
            return;
        }
        if (selected == excelIndex) {
            exportarExcel(snapshot);
        }
    }

    private void exportarCsv(ReportSnapshot snapshot) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte CSV");
        chooser.setSelectedFile(new File("reporte_" + sanitizeFileName(snapshot.getPeriod().getLabel()) + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File exported = data.exportBackendReportCsv(chooser.getSelectedFile(), snapshot);
                JOptionPane.showMessageDialog(this, "Reporte CSV exportado en:\n" + exported.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarPdf(ReportSnapshot snapshot) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte PDF");
        chooser.setSelectedFile(new File("reporte_" + sanitizeFileName(snapshot.getPeriod().getLabel()) + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File exported;
                if (data.isBackendSessionActive()) {
                    exported = data.exportBackendReportPdf(chooser.getSelectedFile(), snapshot);
                } else {
                    exported = exportService.exportPdf(chooser.getSelectedFile(), data, snapshot);
                }
                JOptionPane.showMessageDialog(this, "Reporte PDF exportado en:\n" + exported.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarExcel(ReportSnapshot snapshot) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte Excel");
        chooser.setSelectedFile(new File("reporte_" + sanitizeFileName(snapshot.getPeriod().getLabel()) + ".xlsx"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xlsx)", "xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File exported;
                if (data.isBackendSessionActive()) {
                    exported = data.exportBackendReportXlsx(chooser.getSelectedFile(), snapshot);
                } else {
                    exported = exportService.exportExcel(chooser.getSelectedFile(), data, snapshot);
                }
                JOptionPane.showMessageDialog(this, "Reporte Excel exportado en:\n" + exported.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar Excel: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String sanitizeFileName(String value) {
        return value == null ? "reporte" : value.toLowerCase(Locale.ROOT).replace(" ", "_").replace("/", "_");
    }

    private void refresh() {
        ReportSnapshot snapshot = resolveSnapshot(false);
        refreshStats(snapshot);
        refreshCharts(snapshot);
        refreshCategoryBreakdown(snapshot);
    }

    private void refreshStats(ReportSnapshot snapshot) {
        statsPanel.removeAll();
        statsPanel.add(statCard("Ingresos", money(snapshot.getIncome()), AppColors.ACCENT_GREEN, snapshot.getPeriod().getDisplayLabel()));
        statsPanel.add(statCard("Gastos", money(snapshot.getExpenses()), AppColors.ACCENT_RED, snapshot.getPeriod().getDisplayLabel()));
        statsPanel.add(statCard("Balance", money(snapshot.getBalance()), snapshot.getBalance().compareTo(BigDecimal.ZERO) >= 0 ? AppColors.ACCENT_BLUE : AppColors.ACCENT_RED, snapshot.hasTransactions() ? "Calculado con movimientos reales" : "Sin movimientos en el periodo"));
        statsPanel.add(statCard("Tasa de ahorro", snapshot.getSavingsRatePercent() + "%", snapshot.getSavingsRatePercent().compareTo(BigDecimal.valueOf(20)) >= 0 ? AppColors.ACCENT_GREEN : AppColors.ACCENT_YELLOW, snapshot.getIncome().compareTo(BigDecimal.ZERO) > 0 ? "Ahorro / ingresos" : "Sin ingresos para calcular"));
        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private JPanel statCard(String label, String value, Color color, String note) {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel labelView = new JLabel(label);
        labelView.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelView.setForeground(AppColors.TEXT_MUTED);
        labelView.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueView = new JLabel(value);
        valueView.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valueView.setForeground(color);
        valueView.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel noteView = new JLabel(note);
        noteView.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        noteView.setForeground(AppColors.TEXT_MUTED);
        noteView.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(labelView);
        card.add(Box.createVerticalStrut(4));
        card.add(valueView);
        card.add(Box.createVerticalStrut(2));
        card.add(noteView);
        return card;
    }

    private void refreshCharts(ReportSnapshot snapshot) {
        chartArea.removeAll();

        JPanel barCard = card();
        barCard.setLayout(new BorderLayout(0, 8));
        JLabel barTitle = new JLabel("Ingresos vs gastos - " + snapshot.getPeriod().getDisplayLabel());
        barTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        barTitle.setForeground(AppColors.TEXT_PRIMARY);
        barCard.add(barTitle, BorderLayout.NORTH);
        if (hasChartData(snapshot.getMonthlyIncomeTotals()) || hasChartData(snapshot.getMonthlyExpenseTotals())) {
            barCard.add(new BarChartPanel(snapshot.getMonthLabels(), snapshot.getMonthlyIncomeTotals(), snapshot.getMonthlyExpenseTotals()), BorderLayout.CENTER);
        } else {
            barCard.add(emptyLabel("No hay movimientos para graficar en este periodo."), BorderLayout.CENTER);
        }

        JPanel donutCard = card();
        donutCard.setLayout(new BorderLayout(0, 8));
        JLabel donutTitle = new JLabel("Distribucion de gastos");
        donutTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        donutTitle.setForeground(AppColors.TEXT_PRIMARY);

        JPanel donutBody = new JPanel(new BorderLayout(8, 0));
        donutBody.setOpaque(false);
        if (snapshot.getCategoryLabels().length == 0) {
            donutBody.add(emptyLabel("Todavia no existen gastos para analizar en este periodo."), BorderLayout.CENTER);
        } else {
            DonutChartPanel donutChart = new DonutChartPanel(snapshot.getCategoryLabels(), snapshot.getCategoryPercentages(), AppColors.DONUT_COLORS);
            donutChart.setPreferredSize(new Dimension(180, 180));
            donutBody.add(donutChart, BorderLayout.WEST);
            donutBody.add(buildLegend(snapshot), BorderLayout.CENTER);
        }

        donutCard.add(donutTitle, BorderLayout.NORTH);
        donutCard.add(donutBody, BorderLayout.CENTER);

        chartArea.add(barCard);
        chartArea.add(donutCard);
        chartArea.revalidate();
        chartArea.repaint();
    }

    private JPanel buildLegend(ReportSnapshot snapshot) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        String[] categories = snapshot.getCategoryLabels();
        double[] percentages = snapshot.getCategoryPercentages();
        Color[] colors = AppColors.DONUT_COLORS;

        for (int i = 0; i < categories.length; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            row.setOpaque(false);

            JLabel dot = new JLabel("o");
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            dot.setForeground(colors[i % colors.length]);

            JLabel text = new JLabel(categories[i]);
            text.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            text.setForeground(AppColors.TEXT_SECONDARY);

            JLabel pct = new JLabel(String.format(Locale.US, "%.0f%%", percentages[i]));
            pct.setFont(new Font("Segoe UI", Font.BOLD, 10));
            pct.setForeground(AppColors.TEXT_PRIMARY);

            row.add(dot);
            row.add(text);
            row.add(pct);
            panel.add(row);
        }
        return panel;
    }

    private void refreshCategoryBreakdown(ReportSnapshot snapshot) {
        categoryArea.removeAll();
        categoryArea.setLayout(new BorderLayout(0, 10));

        JLabel title = new JLabel("Gastos por categoria - detalle");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);

        if (snapshot.getExpenseByCategory().isEmpty()) {
            rows.add(emptyLabel("Todavia no existen gastos para analizar en este periodo."));
        }

        for (Map.Entry<String, BigDecimal> entry : snapshot.getExpenseByCategory().entrySet()) {
            BigDecimal percentage = snapshot.getExpenses().compareTo(BigDecimal.ZERO) <= 0
                    ? Money.ZERO
                    : entry.getValue().multiply(BigDecimal.valueOf(100)).divide(snapshot.getExpenses(), 2, Money.ROUNDING);
            final double percentageValue = percentage.doubleValue();

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

            JLabel category = new JLabel(entry.getKey());
            category.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            category.setForeground(AppColors.TEXT_SECONDARY);
            category.setPreferredSize(new Dimension(130, 18));

            JPanel barBg = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xf0f4f8));
                    g2.fill(new RoundRectangle2D.Double(0, 3, getWidth(), 10, 6, 6));
                    g2.setColor(AppColors.ACCENT_BLUE);
                    g2.fill(new RoundRectangle2D.Double(0, 3, (int) (getWidth() * percentageValue / 100), 10, 6, 6));
                    g2.dispose();
                }
            };
            barBg.setOpaque(false);

            JLabel amount = new JLabel(String.format(Locale.US, "%s (%.0f%%)", money(entry.getValue()), percentageValue));
            amount.setFont(new Font("Segoe UI", Font.BOLD, 11));
            amount.setForeground(AppColors.TEXT_PRIMARY);
            amount.setPreferredSize(new Dimension(150, 18));

            row.add(category, BorderLayout.WEST);
            row.add(barBg, BorderLayout.CENTER);
            row.add(amount, BorderLayout.EAST);
            rows.add(row);
            rows.add(Box.createVerticalStrut(3));
        }

        JScrollPane scrollPane = new JScrollPane(rows);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        categoryArea.add(title, BorderLayout.NORTH);
        categoryArea.add(scrollPane, BorderLayout.CENTER);
        categoryArea.revalidate();
        categoryArea.repaint();
    }

    private JPanel card() {
        JPanel card = new CardPanel();
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return card;
    }

    private boolean hasChartData(double[] values) {
        for (double value : values) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    private JLabel emptyLabel(String message) {
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(AppColors.TEXT_MUTED);
        return label;
    }

    private String money(BigDecimal value) {
        return "$" + nf.format(Money.normalize(value));
    }
}
