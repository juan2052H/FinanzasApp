package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Transaccion.Tipo;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.AppIcons;
import com.finanzas.ui.components.RoundedButton;
import com.finanzas.ui.dialogs.TransactionDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransaccionesPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final NumberFormat nf = NumberFormat.getInstance(data.getDisplayLocale());
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Tipo filterTipo;
    private final String pageTitle;

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JLabel totalLabel;
    private List<Transaccion> currentList;

    public TransaccionesPanel(Tipo filterTipo, String pageTitle) {
        this.filterTipo = filterTipo;
        this.pageTitle = pageTitle;
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
        data.addListener(this::refresh);
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildToolbar(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppColors.MAIN_BG);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        JLabel title = new JLabel(pageTitle);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(AppColors.TEXT_PRIMARY);
        JLabel subtitle = new JLabel(filterTipo == Tipo.INGRESO ? "Registro de todos tus ingresos" : "Registro de todos tus gastos");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(AppColors.MAIN_BG);
        left.add(title);
        left.add(subtitle);

        Color buttonColor = filterTipo == Tipo.INGRESO ? AppColors.BTN_GREEN : AppColors.BTN_RED;
        String buttonLabel = filterTipo == Tipo.INGRESO ? AppIcons.INCOME + " Nuevo Ingreso" : AppIcons.EXPENSE + " Nuevo Gasto";
        RoundedButton addButton = new RoundedButton(buttonLabel, buttonColor);
        addButton.addActionListener(e -> showFormDialog(null));

        header.add(left, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppColors.MAIN_BG);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(AppColors.MAIN_BG);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        searchField = new JTextField(22);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
        });

        totalLabel = new JLabel();
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        totalLabel.setForeground(filterTipo == Tipo.INGRESO ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED);

        toolbar.add(new JLabel("Buscar:"));
        toolbar.add(searchField);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(totalLabel);

        String[] columns = {"Tipo", "Categoria", "Descripcion", "Monto", "Fecha", "Acciones"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xEBF3FE));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xf8fafc));
        table.getTableHeader().setForeground(AppColors.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER));

        table.getColumnModel().getColumn(0).setMaxWidth(75);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setMaxWidth(100);

        Color amountColor = filterTipo == Tipo.INGRESO ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED;
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBackground(selected ? new Color(0xEBF3FE) : (row % 2 == 0 ? Color.WHITE : new Color(0xf9fafb)));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (column == 3) {
                    setForeground(amountColor);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    setForeground(AppColors.TEXT_SECONDARY);
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                return this;
            }
        });

        table.getColumn("Acciones").setCellRenderer(new ActionRenderer());
        table.getColumn("Acciones").setCellEditor(new ActionEditor());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 5 && currentList != null && row < currentList.size()) {
                    showRowMenu(e, row);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        wrapper.add(toolbar, BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        refreshTable();
        return wrapper;
    }

    private void refresh() {
        SwingUtilities.invokeLater(this::refreshTable);
    }

    private void refreshTable() {
        String query = searchField != null ? searchField.getText().trim() : "";
        currentList = data.buscarTransacciones(query, filterTipo);
        tableModel.setRowCount(0);
        double total = 0;
        for (Transaccion transaccion : currentList) {
            String sign = transaccion.getTipo() == Tipo.INGRESO ? "+" : "-";
            tableModel.addRow(new Object[]{
                    transaccion.getTipo() == Tipo.INGRESO ? "Ingreso" : "Gasto",
                    transaccion.getCategoria(),
                    transaccion.getDescripcion(),
                    sign + "$" + nf.format((long) transaccion.getMonto()),
                    transaccion.getFecha().format(dtf),
                    "⋮"
            });
            total += transaccion.getMonto();
        }
        if (totalLabel != null) {
            totalLabel.setText((filterTipo == Tipo.INGRESO ? "Total ingresos: $" : "Total gastos: $") + nf.format((long) total));
        }
    }

    private void showRowMenu(MouseEvent e, int row) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Editar");
        JMenuItem delete = new JMenuItem("Eliminar");
        edit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        delete.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        edit.addActionListener(ev -> showFormDialog(currentList.get(row)));
        delete.addActionListener(ev -> {
            int result = JOptionPane.showConfirmDialog(this, "Eliminar esta transaccion?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                data.removeTransaccion(currentList.get(row));
            }
        });
        menu.add(edit);
        menu.add(delete);
        menu.show(table, e.getX(), e.getY());
    }

    public void openNewTransactionDialog() {
        showFormDialog(null);
    }

    private void showFormDialog(Transaccion existing) {
        TransactionDialog.show(this, filterTipo, existing, nueva -> {
            if (existing == null) {
                data.addTransaccion(nueva);
            } else {
                data.updateTransaccion(existing, nueva);
            }
        });
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel view = new JLabel(label);
        view.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(view, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(component, gbc);
    }

    static class ActionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JButton button = new JButton("⋮");
            button.setFont(new Font("Segoe UI", Font.BOLD, 16));
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setForeground(AppColors.TEXT_MUTED);
            button.setFocusPainted(false);
            return button;
        }
    }

    static class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("⋮");

        ActionEditor() {
            button.setFont(new Font("Segoe UI", Font.BOLD, 16));
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setForeground(AppColors.TEXT_MUTED);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
            return button;
        }
    }
}
