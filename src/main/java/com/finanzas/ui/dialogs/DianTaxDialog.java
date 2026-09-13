package com.finanzas.ui.dialogs;

import com.finanzas.api.BackendTaxSummary;
import com.finanzas.data.DataManager;
import com.finanzas.ui.components.AppColors;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialogo de Preparacion Tributaria DIAN.
 * Muestra resumen de obligacion y permite configurar umbrales de UVT.
 */
public class DianTaxDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(DianTaxDialog.class.getName());
    private final DataManager dm = DataManager.getInstance();
    private int selectedYear = LocalDate.now().getYear();

    private JLabel obligationLabel;
    private JTextArea reasonsArea;
    private JLabel incomeLabel, expensesLabel, wealthLabel, depositsLabel;
    private JSpinner yearSpinner;
    private JTextField uvtField, incomeUvtField, purchasesUvtField, depositsUvtField, wealthUvtField, estimatedWealthField;

    private DianTaxDialog(Window owner) {
        super(owner, "Preparacion Tributaria DIAN", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(0, 0));
        setSize(680, 600);
        setResizable(true);
        setLocationRelativeTo(owner);
        buildUI();
        loadSummary();
    }

    public static void show(Window owner) {
        new DianTaxDialog(owner).setVisible(true);
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        header.setBackground(AppColors.ACCENT_BLUE);
        JLabel title = new JLabel("Declaracion de Renta DIAN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.addTab("Resumen", buildSummaryTab());
        tabs.addTab("Configurar UVT", buildConfigTab());
        add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        JButton close = new JButton("Cerrar");
        close.addActionListener(e -> dispose());
        footer.add(close);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildSummaryTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setBackground(Color.WHITE);

        // Year selector
        JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        yearPanel.setOpaque(false);
        yearPanel.add(new JLabel("Ano fiscal:"));
        yearSpinner = new JSpinner(new SpinnerNumberModel(selectedYear, 2000, 2099, 1));
        yearSpinner.setPreferredSize(new Dimension(80, 28));
        yearPanel.add(yearSpinner);
        JButton refreshBtn = new JButton("Consultar");
        refreshBtn.addActionListener(e -> {
            selectedYear = (Integer) yearSpinner.getValue();
            loadSummary();
        });
        yearPanel.add(refreshBtn);
        panel.add(yearPanel);
        panel.add(Box.createVerticalStrut(16));

        // Obligation
        obligationLabel = new JLabel("Consultando...");
        obligationLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        obligationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(obligationLabel);
        panel.add(Box.createVerticalStrut(10));

        // Metrics
        incomeLabel = metricLabel("Ingresos del anio:");
        expensesLabel = metricLabel("Compras/Gastos:");
        depositsLabel = metricLabel("Depositos/Ahorro:");
        wealthLabel = metricLabel("Patrimonio bruto estimado:");
        panel.add(incomeLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(expensesLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(depositsLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(wealthLabel);
        panel.add(Box.createVerticalStrut(14));

        // Reasons
        panel.add(new JLabel("Razones de obligacion:"));
        panel.add(Box.createVerticalStrut(4));
        reasonsArea = new JTextArea(5, 50);
        reasonsArea.setEditable(false);
        reasonsArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        reasonsArea.setLineWrap(true);
        reasonsArea.setWrapStyleWord(true);
        reasonsArea.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        panel.add(new JScrollPane(reasonsArea));

        // Disclaimer
        panel.add(Box.createVerticalStrut(10));
        JLabel disclaimer = new JLabel("<html><i>AVISO LEGAL: Esta herramienta es solo orientativa. Consulta con un contador certificado para tu declaracion de renta.</i></html>");
        disclaimer.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        disclaimer.setForeground(AppColors.TEXT_MUTED);
        panel.add(disclaimer);

        return panel;
    }

    private JPanel buildConfigTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        uvtField = field("47065");
        incomeUvtField = field("1400");
        purchasesUvtField = field("1400");
        depositsUvtField = field("1400");
        wealthUvtField = field("4500");
        estimatedWealthField = field("0");

        addCfgRow(panel, gbc, 0, "Valor UVT ($):", uvtField);
        addCfgRow(panel, gbc, 1, "Umbral ingresos (UVT):", incomeUvtField);
        addCfgRow(panel, gbc, 2, "Umbral compras (UVT):", purchasesUvtField);
        addCfgRow(panel, gbc, 3, "Umbral depositos (UVT):", depositsUvtField);
        addCfgRow(panel, gbc, 4, "Umbral patrimonio (UVT):", wealthUvtField);
        addCfgRow(panel, gbc, 5, "Patrimonio bruto estimado ($):", estimatedWealthField);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JButton saveBtn = new JButton("Guardar Configuracion");
        saveBtn.setBackground(AppColors.ACCENT_BLUE);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveConfig());
        panel.add(saveBtn, gbc);

        return panel;
    }

    private JLabel metricLabel(String prefix) {
        JLabel label = new JLabel(prefix + " --");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField field(String def) {
        JTextField f = new JTextField(def, 16);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return f;
    }

    private void addCfgRow(JPanel form, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(field, gbc);
    }

    private void loadSummary() {
        obligationLabel.setText("Consultando resumen tributario...");
        reasonsArea.setText("");
        incomeLabel.setText("Ingresos del anio: --");
        expensesLabel.setText("Compras/Gastos: --");
        depositsLabel.setText("Depositos/Ahorro: --");
        wealthLabel.setText("Patrimonio bruto estimado: --");

        SwingWorker<BackendTaxSummary, Void> worker = new SwingWorker<BackendTaxSummary, Void>() {
            @Override
            protected BackendTaxSummary doInBackground() throws Exception {
                return dm.getTaxSummary(selectedYear);
            }
            @Override
            protected void done() {
                try {
                    BackendTaxSummary s = get();
                    applyToUI(s);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() == null || cause.getMessage().trim().isEmpty()
                            ? "No fue posible consultar el resumen tributario."
                            : cause.getMessage();
                    obligationLabel.setText("Error al consultar: " + message);
                    obligationLabel.setForeground(Color.RED);
                    LOGGER.log(Level.WARNING, "Error al cargar resumen DIAN", ex);
                }
            }
        };
        worker.execute();
    }

    private void applyToUI(BackendTaxSummary s) {
        if (s == null) { obligationLabel.setText("Sin datos."); return; }
        boolean oblig = s.isObligationToDeclare();
        obligationLabel.setText(oblig ? "SI DEBE DECLARAR RENTA" : "NO OBLIGADO A DECLARAR (segun datos registrados)");
        obligationLabel.setForeground(oblig ? new Color(0xC0392B) : new Color(0x27AE60));

        String fmt = "%s $%s / umbral $%s %s";
        BigDecimal inc = orZero(s.getTotalIncome());
        BigDecimal incThresh = orZero(s.getIncomeThresholdAmount());
        incomeLabel.setText(String.format(fmt, "Ingresos:", inc.toPlainString(), incThresh.toPlainString(), s.isExceedsIncomeThreshold() ? "[SUPERA]" : ""));
        BigDecimal exp = orZero(s.getTotalExpenses());
        BigDecimal expThresh = orZero(s.getPurchasesThresholdAmount());
        expensesLabel.setText(String.format(fmt, "Compras/Gastos:", exp.toPlainString(), expThresh.toPlainString(), s.isExceedsPurchasesThreshold() ? "[SUPERA]" : ""));
        BigDecimal dep = orZero(s.getTotalBankDepositsOrSavings());
        BigDecimal depThresh = orZero(s.getDepositsThresholdAmount());
        depositsLabel.setText(String.format(fmt, "Depositos/Ahorro:", dep.toPlainString(), depThresh.toPlainString(), s.isExceedsDepositsThreshold() ? "[SUPERA]" : ""));
        BigDecimal wealth = orZero(s.getEstimatedGrossWealth());
        BigDecimal wThresh = orZero(s.getWealthThresholdAmount());
        wealthLabel.setText(String.format(fmt, "Patrimonio:", wealth.toPlainString(), wThresh.toPlainString(), s.isExceedsWealthThreshold() ? "[SUPERA]" : ""));

        if (s.getObligationReasons() != null && !s.getObligationReasons().isEmpty()) {
            reasonsArea.setText(String.join("\n", s.getObligationReasons()));
        } else {
            reasonsArea.setText(oblig ? "Ver detalles de cada indicador arriba." : "Ninguna de las condiciones de obligacion fue superada.");
        }
    }

    private void saveConfig() {
        final int year;
        final BigDecimal uvt;
        final int incomeUvt;
        final int purchasesUvt;
        final int depositsUvt;
        final int wealthUvt;
        final BigDecimal estWealth;
        try {
            year = selectedYear;
            uvt = new BigDecimal(uvtField.getText().trim().replace(",", "."));
            incomeUvt = Integer.parseInt(incomeUvtField.getText().trim());
            purchasesUvt = Integer.parseInt(purchasesUvtField.getText().trim());
            depositsUvt = Integer.parseInt(depositsUvtField.getText().trim());
            wealthUvt = Integer.parseInt(wealthUvtField.getText().trim());
            estWealth = new BigDecimal(estimatedWealthField.getText().trim().replace(",", "."));
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Valores invalidos. Verifica los numeros ingresados.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dm.updateTaxConfig(year, uvt, incomeUvt, purchasesUvt, depositsUvt, wealthUvt, estWealth);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(DianTaxDialog.this, "Configuracion guardada. Consultando nuevo resumen...", "Exito", JOptionPane.INFORMATION_MESSAGE);
                    loadSummary();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() == null || cause.getMessage().trim().isEmpty()
                            ? "No fue posible guardar la configuracion tributaria."
                            : cause.getMessage();
                    JOptionPane.showMessageDialog(DianTaxDialog.this, "Error: " + message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private BigDecimal orZero(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
