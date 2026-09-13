package com.finanzas.ui.dialogs;

import com.finanzas.data.DataManager;
import com.finanzas.domain.simulation.SimulationResult;
import com.finanzas.domain.simulation.WhatIfSimulatorService;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Money;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SimulationDialog {
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(new Locale("es", "CO"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SimulationDialog() {
    }

    public static void show(Component parent) {
        DataManager data = DataManager.getInstance();
        WhatIfSimulatorService simulator = new WhatIfSimulatorService();

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(parent) : null,
                "Simulador financiero",
                true);
        dialog.setSize(560, 430);
        dialog.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Que pasa si...");
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        JComboBox<String> scenarioBox = new JComboBox<String>(new String[]{
                "Ahorrar un monto adicional cada mes",
                "Reducir gastos en porcentaje"
        });
        JTextField valueField = new JTextField("300000");
        JTextField monthsField = new JTextField("12");
        JTextField currentSavedField = new JTextField(defaultCurrentSaved(data));
        JTextField goalAmountField = new JTextField(defaultGoalAmount(data));

        addRow(form, gbc, 0, "Escenario:", scenarioBox);
        addRow(form, gbc, 1, "Monto adicional o porcentaje:", valueField);
        addRow(form, gbc, 2, "Meses a proyectar:", monthsField);
        addRow(form, gbc, 3, "Ahorro actual:", currentSavedField);
        addRow(form, gbc, 4, "Meta objetivo:", goalAmountField);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setRows(6);
        resultArea.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        resultArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        resultArea.setText("Configura un escenario y presiona Simular. No se modificaran tus datos reales.");

        RoundedButton simulate = new RoundedButton("Simular", AppColors.ACCENT_BLUE);
        simulate.addActionListener(e -> {
            try {
                BigDecimal currentIncome = Money.of(data.getIngresosMesActual());
                BigDecimal currentExpenses = Money.of(data.getGastosMesActual());
                BigDecimal scenarioValue = TransactionDialog.parseMoneyDecimal(valueField.getText());
                BigDecimal currentSaved = TransactionDialog.parseMoneyDecimal(currentSavedField.getText());
                BigDecimal goalAmount = TransactionDialog.parseMoneyDecimal(goalAmountField.getText());
                int months = Integer.parseInt(monthsField.getText().trim());
                if (months < 1) {
                    throw new NumberFormatException("Los meses a proyectar deben ser al menos 1.");
                }

                SimulationResult result;
                if (scenarioBox.getSelectedIndex() == 0) {
                    result = simulator.simulateAdditionalMonthlySavings(currentIncome, currentExpenses, scenarioValue, currentSaved, goalAmount, months);
                } else {
                    result = simulator.simulateExpenseReduction(currentIncome, currentExpenses, scenarioValue, currentSaved, goalAmount, months);
                }
                resultArea.setText(formatResult(result, months));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Revisa montos, porcentaje y meses.", "Datos invalidos", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("El simulador es de solo lectura hasta que confirmes cambios manualmente en la app.");
        note.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        note.setForeground(AppColors.TEXT_MUTED);
        footer.add(note, BorderLayout.CENTER);
        footer.add(simulate, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.add(resultArea, BorderLayout.CENTER);
        bottom.add(footer, BorderLayout.SOUTH);

        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private static void addRow(JPanel form, GridBagConstraints gbc, int row, String label, Component input) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel labelView = new JLabel(label);
        labelView.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        form.add(labelView, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.65;
        form.add(input, gbc);
    }

    private static String defaultCurrentSaved(DataManager data) {
        return String.valueOf(Math.round(data.getAhorros()));
    }

    private static String defaultGoalAmount(DataManager data) {
        if (!data.getMetas().isEmpty()) {
            MetaAhorro meta = data.getMetas().get(0);
            return meta.getMontoMetaDecimal().toPlainString();
        }
        return "10000000";
    }

    private static String formatResult(SimulationResult result, int months) {
        String date = result.getProjectedGoalDate() == null
                ? "No alcanzable con el ahorro mensual simulado"
                : result.getProjectedGoalDate().format(DATE_FORMAT);
        return "Ahorro mensual actual: " + money(result.getCurrentMonthlySavings()) + "\n"
                + "Ahorro mensual simulado: " + money(result.getSimulatedMonthlySavings()) + "\n"
                + "Diferencia mensual: " + money(result.getDifference()) + "\n"
                + "Monto proyectado en " + months + " meses: " + money(result.getProjectedAmount()) + "\n"
                + "Fecha estimada para la meta: " + date;
    }

    private static String money(BigDecimal value) {
        return "$" + MONEY_FORMAT.format(Money.normalize(value));
    }
}
