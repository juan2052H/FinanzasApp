package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Money;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.CardPanel;
import com.finanzas.ui.components.FormSupport;
import com.finanzas.ui.components.PageHeader;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MetasPanel extends JPanel {
    private static final IconOption[] ICON_OPTIONS = {
            new IconOption("🏖️", "Vacaciones"),
            new IconOption("🚗", "Vehiculo"),
            new IconOption("🏠", "Vivienda"),
            new IconOption("💻", "Tecnologia"),
            new IconOption("🎓", "Estudios"),
            new IconOption("💍", "Evento"),
            new IconOption("🛡️", "Emergencia"),
            new IconOption("🧳", "Viaje"),
            new IconOption("📱", "Celular"),
            new IconOption("🎁", "Regalo")
    };

    private final DataManager data = DataManager.getInstance();
    private final NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CO"));
    private final DateTimeFormatter displayDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private JPanel cardsPanel;

    public MetasPanel() {
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
        data.addListener(() -> SwingUtilities.invokeLater(this::refreshCards));
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(AppColors.MAIN_BG);
        content.add(buildSummaryStrip(), BorderLayout.NORTH);

        cardsPanel = new JPanel(new GridLayout(0, 2, 16, 16));
        cardsPanel.setBackground(AppColors.MAIN_BG);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(AppColors.MAIN_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scrollPane, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        refreshCards();
    }

    private JPanel buildHeader() {
        JPanel header = new PageHeader("Ahorro y Metas", "Gestiona tus ahorros, aportes y objetivos financieros.");
        RoundedButton addButton = new RoundedButton("+ Nueva Meta", AppColors.ACCENT_BLUE);
        addButton.addActionListener(e -> showMetaDialog(null));
        header.add(addButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSummaryStrip() {
        JPanel wrapper = new JPanel(new GridLayout(1, 3, 12, 0));
        wrapper.setBackground(AppColors.MAIN_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        List<MetaAhorro> metas = data.getMetas();
        double totalAhorrado = metas.stream().mapToDouble(MetaAhorro::getMontoActual).sum();
        double totalObjetivo = metas.stream().mapToDouble(MetaAhorro::getMontoMeta).sum();
        long activas = metas.stream().filter(meta -> meta.getProgreso() < 100).count();

        wrapper.add(summaryMiniCard("Ahorrado total", "$" + nf.format((long) totalAhorrado), AppColors.ACCENT_GREEN));
        wrapper.add(summaryMiniCard("Objetivo total", "$" + nf.format((long) totalObjetivo), AppColors.ACCENT_BLUE));
        wrapper.add(summaryMiniCard("Metas activas", String.valueOf(activas), AppColors.ACCENT_ORANGE));
        return wrapper;
    }

    private JPanel summaryMiniCard(String label, String value, Color color) {
        JPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel labelView = new JLabel(label);
        labelView.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelView.setForeground(AppColors.TEXT_MUTED);
        labelView.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueView = new JLabel(value);
        valueView.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueView.setForeground(color);
        valueView.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(labelView);
        card.add(Box.createVerticalStrut(4));
        card.add(valueView);
        return card;
    }

    private void refreshCards() {
        cardsPanel.removeAll();
        List<MetaAhorro> metas = data.getMetas();
        if (metas.isEmpty()) {
            JLabel empty = new JLabel("No tienes metas activas. Crea la primera.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            empty.setForeground(AppColors.TEXT_MUTED);
            cardsPanel.add(empty);
        } else {
            for (MetaAhorro meta : metas) {
                cardsPanel.add(buildMetaCard(meta));
            }
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildMetaCard(MetaAhorro meta) {
        boolean completed = meta.getProgreso() >= 100;
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(completed ? new Color(0xF0FFF4) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(completed ? AppColors.ACCENT_GREEN : AppColors.BORDER);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel(meta.getIcono());
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel name = new JLabel(meta.getNombre());
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(AppColors.TEXT_PRIMARY);
        titleRow.add(icon);
        titleRow.add(name);

        JLabel progress = new JLabel(completed ? "Completada" : meta.getProgreso() + "%");
        progress.setFont(new Font("Segoe UI", Font.BOLD, 13));
        progress.setForeground(completed ? AppColors.ACCENT_GREEN : Color.decode(meta.getColor()));

        topRow.add(titleRow, BorderLayout.WEST);
        topRow.add(progress, BorderLayout.EAST);
        card.add(topRow);
        card.add(Box.createVerticalStrut(10));

        JPanel progressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE5E7EB));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                int width = (int) (getWidth() * Math.min(meta.getProgreso() / 100.0, 1.0));
                g2.setColor(completed ? AppColors.ACCENT_GREEN : Color.decode(meta.getColor()));
                g2.fill(new RoundRectangle2D.Double(0, 0, width, getHeight(), 8, 8));
                g2.dispose();
            }
        };
        progressBar.setOpaque(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        progressBar.setPreferredSize(new Dimension(0, 12));
        card.add(progressBar);
        card.add(Box.createVerticalStrut(10));

        JLabel amounts = new JLabel("Ahorrado: $" + nf.format((long) meta.getMontoActual()) + " / $" + nf.format((long) meta.getMontoMeta()));
        amounts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        amounts.setForeground(AppColors.TEXT_MUTED);
        amounts.setAlignmentX(Component.LEFT_ALIGNMENT);

        String dueText = meta.getDiasRestantes() >= 0
                ? "Fecha limite: " + meta.getFechaLimite().format(displayDateFormat) + " (" + meta.getDiasRestantes() + " dias)"
                : "Fecha limite vencida: " + meta.getFechaLimite().format(displayDateFormat);
        JLabel dueDate = new JLabel(dueText);
        dueDate.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dueDate.setForeground(meta.getDiasRestantes() >= 0 ? AppColors.TEXT_SECONDARY : AppColors.ACCENT_RED);
        dueDate.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel remaining = new JLabel(completed ? "Meta alcanzada." : "Falta: $" + nf.format((long) meta.getFaltante()));
        remaining.setFont(new Font("Segoe UI", Font.BOLD, 11));
        remaining.setForeground(completed ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED);
        remaining.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(amounts);
        card.add(Box.createVerticalStrut(4));
        card.add(dueDate);
        card.add(Box.createVerticalStrut(4));
        card.add(remaining);
        card.add(Box.createVerticalStrut(12));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        if (!completed) {
            RoundedButton depositButton = new RoundedButton("+ Agregar aporte", AppColors.ACCENT_GREEN);
            depositButton.setPreferredSize(new Dimension(150, 30));
            depositButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
            depositButton.addActionListener(e -> showDepositDialog(meta));
            actions.add(depositButton);
        }

        RoundedButton editButton = new RoundedButton("Editar", new Color(0x607D8B));
        editButton.setPreferredSize(new Dimension(90, 30));
        editButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        editButton.addActionListener(e -> showMetaDialog(meta));
        actions.add(editButton);

        JButton deleteButton = new JButton("X");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.setBorderPainted(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setForeground(AppColors.ACCENT_RED);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, "Eliminar meta '" + meta.getNombre() + "'?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                data.removeMeta(meta);
            }
        });
        actions.add(deleteButton);

        card.add(actions);
        return card;
    }

    private void showDepositDialog(MetaAhorro meta) {
        String input = JOptionPane.showInputDialog(
                this,
                "Cuanto deseas aportar a '" + meta.getNombre() + "'?\nDisponible: $" + nf.format((long) meta.getFaltante()),
                "Registrar aporte",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }

        try {
            BigDecimal amount = Money.parseFlexible(input);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
            if (amount.compareTo(meta.getFaltanteDecimal()) > 0) {
                JOptionPane.showMessageDialog(this, "El aporte supera el monto restante.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (data.depositarMeta(meta, amount.doubleValue())) {
                JOptionPane.showMessageDialog(this, "Aporte registrado.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, data.getLastErrorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Monto invalido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openNewMetaDialog() {
        showMetaDialog(null);
    }

    private void showMetaDialog(MetaAhorro existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(this) : null,
                isEdit ? "Editar Meta" : "Nueva Meta de Ahorro",
                true);
        dialog.setSize(470, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = FormSupport.baseConstraints();

        JTextField nameField = new JTextField(isEdit ? existing.getNombre() : "");
        JComboBox<IconOption> iconBox = new JComboBox<IconOption>(ICON_OPTIONS);
        iconBox.setRenderer(new IconOptionRenderer());
        selectExistingIcon(iconBox, isEdit ? existing.getIcono() : null);
        JTextField currentField = new JTextField(isEdit ? String.valueOf((long) existing.getMontoActual()) : "0");
        JTextField targetField = new JTextField(isEdit ? String.valueOf((long) existing.getMontoMeta()) : "");
        JTextField dueDateField = new JTextField(isEdit ? existing.getFechaLimite().format(displayDateFormat) : LocalDate.now().plusMonths(3).format(displayDateFormat));
        JTextField colorField = new JTextField(isEdit ? existing.getColor() : "#1a73e8");

        FormSupport.addFormRow(panel, gbc, 0, "Nombre:", nameField);
        FormSupport.addFormRow(panel, gbc, 1, "Icono:", iconBox);
        FormSupport.addFormRow(panel, gbc, 2, "Monto ahorrado ($):", currentField);
        FormSupport.addFormRow(panel, gbc, 3, "Monto objetivo ($):", targetField);
        FormSupport.addFormRow(panel, gbc, 4, "Fecha limite:", dueDateField);
        FormSupport.addFormRow(panel, gbc, 5, "Color (hex):", colorField);

        RoundedButton saveButton = new RoundedButton(isEdit ? "Guardar cambios" : "Crear meta", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                IconOption selectedIcon = (IconOption) iconBox.getSelectedItem();
                BigDecimal currentAmount = Money.parseFlexible(currentField.getText());
                BigDecimal targetAmount = Money.parseFlexible(targetField.getText());
                LocalDate dueDate = LocalDate.parse(dueDateField.getText().trim(), displayDateFormat);
                String color = colorField.getText().trim();

                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Debes ingresar el nombre de la meta.");
                }
                if (!dueDate.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("La fecha limite de la meta debe ser posterior a hoy.");
                }
                if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("El monto objetivo debe ser mayor a cero.");
                }
                if (currentAmount.compareTo(BigDecimal.ZERO) < 0 || currentAmount.compareTo(targetAmount) > 0) {
                    throw new IllegalArgumentException("El monto ahorrado debe estar entre 0 y el objetivo.");
                }

                String icon = selectedIcon != null ? selectedIcon.icon : "🎯";
                if (isEdit) {
                    existing.setNombre(name);
                    existing.setIcono(icon);
                    existing.setMontoActualDecimal(currentAmount);
                    existing.setMontoMetaDecimal(targetAmount);
                    existing.setFechaLimite(dueDate);
                    existing.setColor(color);
                    data.updateMeta(existing);
                } else {
                    data.addMeta(new MetaAhorro(name, icon, currentAmount, targetAmount, color, dueDate));
                }
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Datos invalidos. Usa fecha dd/MM/yyyy y montos numericos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void selectExistingIcon(JComboBox<IconOption> iconBox, String currentIcon) {
        if (currentIcon == null) {
            iconBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < iconBox.getItemCount(); i++) {
            IconOption option = iconBox.getItemAt(i);
            if (option.icon.equals(currentIcon)) {
                iconBox.setSelectedIndex(i);
                return;
            }
        }
        iconBox.setSelectedIndex(0);
    }

    private static final class IconOption {
        private final String icon;
        private final String label;

        private IconOption(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }

        @Override
        public String toString() {
            return icon + " " + label;
        }
    }

    private static final class IconOptionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof IconOption) {
                IconOption option = (IconOption) value;
                label.setText(option.icon + "  " + option.label);
                label.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            }
            return label;
        }
    }
}
