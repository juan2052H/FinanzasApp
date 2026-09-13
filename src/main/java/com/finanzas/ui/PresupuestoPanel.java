package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.Presupuesto;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.CardPanel;
import com.finanzas.ui.components.FormSupport;
import com.finanzas.ui.components.PageHeader;
import com.finanzas.ui.components.RoundedButton;
import com.finanzas.ui.dialogs.TransactionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;

public class PresupuestoPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final NumberFormat nf = NumberFormat.getInstance(data.getDisplayLocale());
    private JPanel cardsPanel;

    public PresupuestoPanel() {
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
        data.addListener(() -> SwingUtilities.invokeLater(this::refreshCards));
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(AppColors.MAIN_BG);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 20, 20));

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppColors.MAIN_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refreshCards();
    }

    private JPanel buildHeader() {
        JPanel header = new PageHeader("Presupuesto mensual", "Controla tus limites de gasto por categoria");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        RoundedButton savingsButton = new RoundedButton("Configurar ahorro", AppColors.TEXT_MUTED);
        savingsButton.addActionListener(e -> showSavingsRecommendationDialog());
        RoundedButton addButton = new RoundedButton("+ Nuevo presupuesto", AppColors.ACCENT_BLUE);
        addButton.addActionListener(e -> showFormDialog(null));
        actions.add(savingsButton);
        actions.add(addButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private void refreshCards() {
        cardsPanel.removeAll();
        List<Presupuesto> presupuestos = data.getPresupuestos();
        double totalPresupuesto = presupuestos.stream().mapToDouble(Presupuesto::getMontoPresupuestado).sum();
        double totalGastado = presupuestos.stream().mapToDouble(Presupuesto::getMontoGastado).sum();

        cardsPanel.add(buildSummaryBar(totalPresupuesto, totalGastado));
        cardsPanel.add(Box.createVerticalStrut(14));

        for (Presupuesto presupuesto : presupuestos) {
            cardsPanel.add(buildCard(presupuesto));
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildSummaryBar(double totalPresupuesto, double totalGastado) {
        JPanel panel = new JPanel(new GridLayout(1, 4, 16, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        panel.add(miniCard("Presupuesto total", "$" + nf.format((long) totalPresupuesto), AppColors.ACCENT_BLUE));
        panel.add(miniCard("Total gastado", "$" + nf.format((long) totalGastado), AppColors.ACCENT_RED));
        panel.add(miniCard("Disponible", "$" + nf.format((long) (totalPresupuesto - totalGastado)), AppColors.ACCENT_GREEN));
        panel.add(miniCard("Ahorro recomendado", data.getSavingsRecommendationText(), AppColors.ACCENT_GREEN));
        return panel;
    }

    private JPanel miniCard(String label, String value, Color color) {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel labelView = new JLabel(label);
        labelView.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelView.setForeground(AppColors.TEXT_MUTED);
        labelView.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueView = new JLabel(value);
        valueView.setFont(new Font("Segoe UI", Font.BOLD, value != null && value.length() > 24 ? 11 : 16));
        valueView.setForeground(color);
        valueView.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(labelView);
        card.add(Box.createVerticalStrut(4));
        card.add(valueView);
        return card;
    }

    private JPanel buildCard(Presupuesto presupuesto) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel categoryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        categoryRow.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(presupuesto.getColor()));
                g2.fillOval(0, 3, 12, 12);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 18));

        JLabel categoryLabel = new JLabel(presupuesto.getCategoria());
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        categoryLabel.setForeground(AppColors.TEXT_PRIMARY);
        categoryRow.add(dot);
        categoryRow.add(categoryLabel);

        double percentage = presupuesto.getPorcentajeUsado();
        String badge = percentage >= 100 ? "EXCEDIDO" : percentage >= 90 ? "CRITICO" : percentage >= 75 ? "ATENCION" : "OK";
        Color badgeColor = percentage >= 100
                ? new Color(0xb71c1c)
                : percentage >= 90 ? AppColors.ACCENT_RED
                : percentage >= 75 ? AppColors.ACCENT_YELLOW
                : AppColors.ACCENT_GREEN;

        JLabel badgeLabel = new JLabel(" " + badge + " ");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setBackground(badgeColor);
        badgeLabel.setOpaque(true);
        badgeLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JPanel percentagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        percentagePanel.setOpaque(false);
        JLabel percentageLabel = new JLabel(String.format("%.0f%%", percentage));
        percentageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        percentageLabel.setForeground(badgeColor);
        percentagePanel.add(badgeLabel);
        percentagePanel.add(percentageLabel);

        top.add(categoryRow, BorderLayout.WEST);
        top.add(percentagePanel, BorderLayout.EAST);

        JPanel progressBg = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xe5e7eb));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                int progressWidth = (int) (getWidth() * Math.min(percentage / 100.0, 1.0));
                g2.setColor(badgeColor);
                g2.fill(new RoundRectangle2D.Double(0, 0, progressWidth, getHeight(), 8, 8));
                g2.dispose();
            }
        };
        progressBg.setOpaque(false);
        progressBg.setPreferredSize(new Dimension(0, 10));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel amounts = new JLabel(
                "Gastado: $" + nf.format((long) presupuesto.getMontoGastado())
                        + " / Presupuesto: $" + nf.format((long) presupuesto.getMontoPresupuestado())
                        + " / Disponible: $" + nf.format((long) presupuesto.getSaldo()));
        amounts.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        amounts.setForeground(AppColors.TEXT_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        JButton editButton = iconBtn("Editar", AppColors.ACCENT_BLUE);
        JButton deleteButton = iconBtn("Borrar", AppColors.ACCENT_RED);
        editButton.addActionListener(e -> showFormDialog(presupuesto));
        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, "Eliminar presupuesto de " + presupuesto.getCategoria() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                data.removePresupuesto(presupuesto);
            }
        });
        actions.add(editButton);
        actions.add(deleteButton);
        bottom.add(amounts, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(progressBg, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    public void openNewBudgetDialog() {
        showFormDialog(null);
    }

    private void showSavingsRecommendationDialog() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(this) : null,
                "Configurar ahorro recomendado",
                true);
        dialog.setSize(420, 280);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = FormSupport.baseConstraints();

        JCheckBox enabled = new JCheckBox("Activar recomendacion");
        enabled.setBackground(Color.WHITE);
        enabled.setSelected(data.isSavingsRecommendationEnabled());
        JComboBox<String> modeBox = new JComboBox<String>(new String[]{"Conservador", "Equilibrado", "Agresivo", "Personalizado", "Monto fijo"});
        modeBox.setSelectedItem(data.getSavingsRecommendationMode());
        JTextField percentField = new JTextField(data.getSavingsRecommendationPercent().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        JTextField fixedField = new JTextField(data.getSavingsRecommendationFixedAmount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());

        FormSupport.addFormRow(panel, gbc, 0, "Estado:", enabled);
        FormSupport.addFormRow(panel, gbc, 1, "Modo:", modeBox);
        FormSupport.addFormRow(panel, gbc, 2, "Porcentaje (%):", percentField);
        FormSupport.addFormRow(panel, gbc, 3, "Monto fijo:", fixedField);

        RoundedButton saveButton = new RoundedButton("Guardar recomendacion", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                BigDecimal percent = percentField.getText().trim().isEmpty()
                        ? BigDecimal.ZERO
                        : TransactionDialog.parseMoneyDecimal(percentField.getText());
                BigDecimal fixed = fixedField.getText().trim().isEmpty()
                        ? BigDecimal.ZERO
                        : TransactionDialog.parseMoneyDecimal(fixedField.getText());
                data.configureSavingsRecommendation(enabled.isSelected(), (String) modeBox.getSelectedItem(), percent, fixed);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showFormDialog(Presupuesto existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(this) : null,
                isEdit ? "Editar presupuesto" : "Nuevo presupuesto",
                true);
        dialog.setSize(400, isEdit ? 290 : 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = FormSupport.baseConstraints();

        JComboBox<String> categoryBox = new JComboBox<String>(data.getCategoryNames(FinancialCategory.Kind.EXPENSE));
        if (isEdit) {
            categoryBox.setSelectedItem(existing.getCategoria());
        }
        JTextField budgetField = new JTextField(isEdit ? String.valueOf((long) existing.getMontoPresupuestado()) : "");
        JTextField colorField = new JTextField(isEdit ? existing.getColor() : "#1a73e8");

        Object[][] rows = {
                {"Categoria:", categoryBox},
                {"Presupuesto ($):", budgetField},
                {"Color (hex):", colorField}
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0.4;
            FormSupport.addFormRow(panel, gbc, i, (String) rows[i][0], (Component) rows[i][1]);
        }

        int nextRow = rows.length;
        if (isEdit) {
            JLabel spentLabel = new JLabel("Ya gastado este mes: $" + nf.format((long) existing.getMontoGastado())
                    + " (disponible: $" + nf.format((long) existing.getSaldo()) + ")");
            spentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            spentLabel.setForeground(AppColors.TEXT_MUTED);
            gbc.gridx = 0;
            gbc.gridy = nextRow;
            gbc.gridwidth = 2;
            panel.add(spentLabel, gbc);
            gbc.gridwidth = 1;
            nextRow++;
        }

        RoundedButton saveButton = new RoundedButton(isEdit ? "Guardar cambios" : "Crear presupuesto", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = nextRow;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                BigDecimal presupuesto = TransactionDialog.parseMoneyDecimal(budgetField.getText());
                String categoria = (String) categoryBox.getSelectedItem();
                String color = colorField.getText().trim();
                if (presupuesto.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("El presupuesto debe ser mayor a cero.");
                }

                if (isEdit) {
                    existing.setCategoria(categoria);
                    data.updatePresupuesto(existing, presupuesto, color);
                } else {
                    data.addPresupuesto(new Presupuesto(categoria, presupuesto, BigDecimal.ZERO, color));
                }
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Datos invalidos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private JButton iconBtn(String text, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setForeground(foreground);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel createCard() {
        JPanel card = new CardPanel();
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return card;
    }
}
