package com.finanzas.ui.dialogs;

import com.finanzas.data.DataManager;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Money;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class TransactionDialog {
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TransactionDialog() {
    }

    public static void show(Component parent, Transaccion.Tipo tipo, Transaccion existing, Consumer<Transaccion> onSave) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(parent) : null,
                isEdit ? "Editar transaccion" : (tipo == Transaccion.Tipo.INGRESO ? "Nuevo ingreso" : "Nuevo gasto"),
                true);
        dialog.setSize(430, 340);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 4, 7, 4);

        FinancialCategory.Kind categoryKind = tipo == Transaccion.Tipo.INGRESO
                ? FinancialCategory.Kind.INCOME
                : FinancialCategory.Kind.EXPENSE;
        String[] categories = DataManager.getInstance().getCategoryNames(categoryKind);

        JComboBox<String> categoryBox = new JComboBox<String>(categories);
        JTextField descField = new JTextField(isEdit ? existing.getDescripcion() : "");
        JTextField amountField = new JTextField(isEdit ? existing.getMontoDecimal().toPlainString() : "");
        JTextField dateField = new JTextField(isEdit ? existing.getFecha().format(DISPLAY_DATE_FORMAT) : LocalDate.now().format(DISPLAY_DATE_FORMAT));

        if (isEdit) {
            categoryBox.setSelectedItem(existing.getCategoria());
        }

        addFormRow(panel, gbc, 0, "Categoria:", categoryBox);
        addFormRow(panel, gbc, 1, "Descripcion:", descField);
        addFormRow(panel, gbc, 2, "Monto:", amountField);
        addFormRow(panel, gbc, 3, "Fecha (dd/MM/yyyy):", dateField);

        RoundedButton saveButton = new RoundedButton(isEdit ? "Guardar cambios" : "Guardar", tipo == Transaccion.Tipo.INGRESO ? AppColors.BTN_GREEN : AppColors.BTN_RED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                String description = descField.getText().trim();
                if (description.isEmpty()) {
                    throw new IllegalArgumentException("Ingresa una descripcion.");
                }
                BigDecimal amount = parseMoneyDecimal(amountField.getText());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("El monto debe ser mayor a cero.");
                }
                LocalDate date = LocalDate.parse(dateField.getText().trim(), DISPLAY_DATE_FORMAT);
                Transaccion transaction = new Transaccion(tipo, (String) categoryBox.getSelectedItem(), description, amount, date);
                onSave.accept(transaction);
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Datos invalidos. Usa fecha dd/MM/yyyy y un monto numerico.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(component, gbc);
    }

    public static double parseMoney(String value) {
        return Money.toDouble(parseMoneyDecimal(value));
    }

    public static BigDecimal parseMoneyDecimal(String value) {
        return Money.parseFlexible(value);
    }
}
