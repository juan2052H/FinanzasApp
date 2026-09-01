package com.finanzas.ui.dialogs;

import com.finanzas.data.DataManager;
import com.finanzas.model.FinancialCategory;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;

public final class OnboardingDialog {
    private OnboardingDialog() {
    }

    public static void show(JFrame owner) {
        JDialog dialog = new JDialog(owner, "Configurar FinanzasApp", true);
        dialog.setSize(520, 470);
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel("Primeros pasos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppColors.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Configura tu espacio sin crear dinero ficticio.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_SECONDARY);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(subtitle);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1;

        JComboBox<String> accountType = new JComboBox<String>(new String[]{"Personal", "Hogar", "Negocio"});
        JComboBox<String> currency = new JComboBox<String>(new String[]{"COP", "USD", "EUR", "MXN"});
        JTextField estimatedIncome = new JTextField();
        JComboBox<String> goal = new JComboBox<String>(new String[]{"Ahorrar", "Reducir gastos", "Salir de deudas", "Organizar hogar", "Controlar negocio"});
        String[] expenseCategories = DataManager.getInstance().getCategoryNames(FinancialCategory.Kind.EXPENSE);
        String[] budgetOptions = new String[expenseCategories.length + 1];
        budgetOptions[0] = "";
        System.arraycopy(expenseCategories, 0, budgetOptions, 1, expenseCategories.length);
        JComboBox<String> budgetCategory = new JComboBox<String>(budgetOptions);
        JTextField budgetAmount = new JTextField();

        addRow(form, gbc, 0, "Que quieres administrar", accountType);
        addRow(form, gbc, 1, "Moneda", currency);
        addRow(form, gbc, 2, "Ingreso mensual aproximado (opcional)", estimatedIncome);
        addRow(form, gbc, 3, "Objetivo principal", goal);
        addRow(form, gbc, 4, "Primer presupuesto sugerido (opcional)", budgetCategory);
        addRow(form, gbc, 5, "Monto del presupuesto", budgetAmount);

        RoundedButton start = new RoundedButton("Empezar", AppColors.ACCENT_BLUE);
        start.setPreferredSize(new Dimension(140, 38));
        start.addActionListener(e -> {
            try {
                BigDecimal income = parseOptionalMoney(estimatedIncome.getText());
                BigDecimal budget = parseOptionalMoney(budgetAmount.getText());
                DataManager.getInstance().completeOnboarding(
                        (String) accountType.getSelectedItem(),
                        (String) currency.getSelectedItem(),
                        income,
                        (String) goal.getSelectedItem(),
                        (String) budgetCategory.getSelectedItem(),
                        budget);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Revisa los montos ingresados.", "Datos invalidos", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Los ingresos aproximados no se registran como transacciones.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(AppColors.TEXT_MUTED);
        footer.add(note, BorderLayout.WEST);
        footer.add(start, BorderLayout.EAST);

        root.add(heading, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private static void addRow(JPanel form, GridBagConstraints gbc, int row, String label, Component input) {
        gbc.gridx = 0;
        gbc.gridy = row * 2;
        JLabel view = new JLabel(label);
        view.setFont(new Font("Segoe UI", Font.BOLD, 12));
        view.setForeground(AppColors.TEXT_PRIMARY);
        form.add(view, gbc);
        gbc.gridy = row * 2 + 1;
        form.add(input, gbc);
    }

    private static BigDecimal parseOptionalMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return TransactionDialog.parseMoneyDecimal(value);
    }
}
