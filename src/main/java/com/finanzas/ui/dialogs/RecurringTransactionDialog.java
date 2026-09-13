package com.finanzas.ui.dialogs;

import com.finanzas.data.DataManager;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.RecurringTransaction;
import com.finanzas.model.Transaccion;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.BorderFactory;
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

public final class RecurringTransactionDialog {
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RecurringTransactionDialog() {
    }

    public static void show(Component parent, Consumer<RecurringTransaction> onSave) {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(parent) : null,
                "Nueva transaccion recurrente",
                true);
        dialog.setSize(450, 390);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 4, 7, 4);

        JComboBox<Transaccion.Tipo> typeBox = new JComboBox<Transaccion.Tipo>(Transaccion.Tipo.values());
        JComboBox<String> categoryBox = new JComboBox<String>(categoriesFor((Transaccion.Tipo) typeBox.getSelectedItem()));
        typeBox.addActionListener(e -> {
            categoryBox.removeAllItems();
            for (String category : categoriesFor((Transaccion.Tipo) typeBox.getSelectedItem())) {
                categoryBox.addItem(category);
            }
        });
        JTextField descField = new JTextField();
        JTextField amountField = new JTextField();
        JComboBox<RecurringTransaction.Frequency> frequencyBox = new JComboBox<RecurringTransaction.Frequency>(RecurringTransaction.Frequency.values());
        JTextField customDaysField = new JTextField("30");
        customDaysField.setEnabled(frequencyBox.getSelectedItem() == RecurringTransaction.Frequency.CUSTOM);
        frequencyBox.addActionListener(e ->
                customDaysField.setEnabled(frequencyBox.getSelectedItem() == RecurringTransaction.Frequency.CUSTOM));
        JTextField nextDateField = new JTextField(LocalDate.now().format(DISPLAY_DATE_FORMAT));

        addFormRow(panel, gbc, 0, "Tipo:", typeBox);
        addFormRow(panel, gbc, 1, "Categoria:", categoryBox);
        addFormRow(panel, gbc, 2, "Descripcion:", descField);
        addFormRow(panel, gbc, 3, "Monto:", amountField);
        addFormRow(panel, gbc, 4, "Frecuencia:", frequencyBox);
        addFormRow(panel, gbc, 5, "Dias si es personalizada:", customDaysField);
        addFormRow(panel, gbc, 6, "Proxima fecha:", nextDateField);

        RoundedButton saveButton = new RoundedButton("Guardar recurrencia", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                String description = descField.getText().trim();
                if (description.isEmpty()) {
                    throw new IllegalArgumentException("Ingresa una descripcion.");
                }
                BigDecimal amount = TransactionDialog.parseMoneyDecimal(amountField.getText());
                RecurringTransaction.Frequency frequency = (RecurringTransaction.Frequency) frequencyBox.getSelectedItem();
                int customDays;
                if (frequency == RecurringTransaction.Frequency.CUSTOM) {
                    customDays = Integer.parseInt(customDaysField.getText().trim());
                } else {
                    // Irrelevant for any frequency other than CUSTOM - don't force the
                    // user to fix this field just to save a Monthly/Weekly recurrence.
                    customDays = 30;
                }
                LocalDate nextDate = LocalDate.parse(nextDateField.getText().trim(), DISPLAY_DATE_FORMAT);
                RecurringTransaction recurring = new RecurringTransaction(
                        (Transaccion.Tipo) typeBox.getSelectedItem(),
                        (String) categoryBox.getSelectedItem(),
                        description,
                        amount,
                        frequency,
                        customDays,
                        nextDate);
                onSave.accept(recurring);
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Datos invalidos. Revisa monto, dias y fecha dd/MM/yyyy.", "Error", JOptionPane.ERROR_MESSAGE);
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

    private static String[] categoriesFor(Transaccion.Tipo tipo) {
        FinancialCategory.Kind kind = tipo == Transaccion.Tipo.INGRESO
                ? FinancialCategory.Kind.INCOME
                : FinancialCategory.Kind.EXPENSE;
        return DataManager.getInstance().getCategoryNames(kind);
    }
}
