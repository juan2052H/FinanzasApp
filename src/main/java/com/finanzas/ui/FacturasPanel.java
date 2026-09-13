package com.finanzas.ui;

import com.finanzas.api.BackendInvoice;
import com.finanzas.data.DataManager;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.PageHeader;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel de gestion de Facturas / Comprobantes fiscales.
 * Funciona con el backend cuando hay una sesion activa; de lo contrario
 * las facturas y sus adjuntos se guardan localmente en el equipo.
 */
public class FacturasPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(FacturasPanel.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLUMNS = {
            "N.Factura", "Proveedor", "NIT/RUT", "Fecha", "Subtotal", "IVA", "Total", "Notas"
    };

    private final DataManager dm = DataManager.getInstance();
    private final InvoiceTableModel tableModel = new InvoiceTableModel();
    private JTable table;
    private JLabel statusLabel;

    public FacturasPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(new PageHeader("Facturas / Comprobantes", "Gestiona tus facturas y soportes fiscales"), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);
        dm.addListener(() -> SwingUtilities.invokeLater(this::refresh));
    }

    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.setOpaque(false);
        filterBar.add(new JLabel("Desde anio:"));
        JSpinner fromSpinner = yearSpinner(LocalDate.now().getYear());
        JSpinner toSpinner = yearSpinner(LocalDate.now().getYear());
        filterBar.add(fromSpinner);
        filterBar.add(new JLabel("Hasta anio:"));
        filterBar.add(toSpinner);
        RoundedButton filterBtn = new RoundedButton("Filtrar", AppColors.TEXT_MUTED);
        filterBtn.addActionListener(e -> {
            int fromYear = (Integer) fromSpinner.getValue();
            int toYear = (Integer) toSpinner.getValue();
            loadInvoices(LocalDate.of(fromYear, 1, 1), LocalDate.of(toYear, 12, 31));
        });
        filterBar.add(filterBtn);
        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        filterBar.add(statusLabel);
        panel.add(filterBar, BorderLayout.NORTH);

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(0xEBF3FE));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 4; i <= 6; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);

        RoundedButton addBtn = new RoundedButton("+ Nueva Factura", AppColors.ACCENT_BLUE);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> openInvoiceDialog(null));
        bar.add(addBtn);

        RoundedButton editBtn = new RoundedButton("Editar", AppColors.TEXT_MUTED);
        editBtn.addActionListener(e -> {
            BackendInvoice sel = tableModel.get(table.getSelectedRow());
            if (sel != null) openInvoiceDialog(sel);
        });
        bar.add(editBtn);

        RoundedButton delBtn = new RoundedButton("Eliminar", AppColors.TEXT_MUTED);
        delBtn.addActionListener(e -> deleteSelected());
        bar.add(delBtn);

        RoundedButton uploadBtn = new RoundedButton("Adjuntar PDF/Img", AppColors.TEXT_MUTED);
        uploadBtn.addActionListener(e -> uploadAttachment());
        bar.add(uploadBtn);

        RoundedButton downloadBtn = new RoundedButton("Ver Adjunto", AppColors.TEXT_MUTED);
        downloadBtn.addActionListener(e -> downloadAttachment());
        bar.add(downloadBtn);

        return bar;
    }

    private JSpinner yearSpinner(int initial) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, 2000, 2099, 1);
        JSpinner sp = new JSpinner(model);
        sp.setPreferredSize(new Dimension(70, 28));
        return sp;
    }

    public void refresh() {
        loadInvoices(null, null);
    }

    private void loadInvoices(LocalDate from, LocalDate to) {
        statusLabel.setText("Cargando...");
        SwingWorker<List<BackendInvoice>, Void> worker = new SwingWorker<List<BackendInvoice>, Void>() {
            @Override
            protected List<BackendInvoice> doInBackground() throws Exception {
                return dm.listInvoices(from, to);
            }
            @Override
            protected void done() {
                try {
                    List<BackendInvoice> result = get();
                    tableModel.setData(result);
                    statusLabel.setText(result.size() + " factura(s).");
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + causeMessage(ex));
                    LOGGER.log(Level.WARNING, "Error al cargar facturas", ex);
                }
            }
        };
        worker.execute();
    }

    private void openInvoiceDialog(BackendInvoice existing) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), existing == null ? "Nueva Factura" : "Editar Factura", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField numField = field(existing == null ? "" : existing.getInvoiceNumber());
        JTextField merchantField = field(existing == null ? "" : existing.getMerchantName());
        JTextField taxIdField = field(existing == null ? "" : existing.getTaxId());
        JTextField dateField = field(existing == null ? LocalDate.now().toString() : (existing.getIssueDate() == null ? "" : existing.getIssueDate().toString()));
        JTextField subtotalField = field(existing == null ? "" : (existing.getSubtotal() == null ? "" : existing.getSubtotal().toPlainString()));
        JTextField ivaField = field(existing == null ? "" : (existing.getTaxAmount() == null ? "" : existing.getTaxAmount().toPlainString()));
        JTextField totalField = field(existing == null ? "" : (existing.getTotalAmount() == null ? "" : existing.getTotalAmount().toPlainString()));
        JTextField notesField = field(existing == null ? "" : existing.getNotes());
        JTextField txField = field(existing == null ? "" : existing.getTransactionId());

        addRow(form, gbc, 0, "N. Factura:", numField);
        addRow(form, gbc, 1, "Proveedor:", merchantField);
        addRow(form, gbc, 2, "NIT/RUT:", taxIdField);
        addRow(form, gbc, 3, "Fecha (YYYY-MM-DD):", dateField);
        addRow(form, gbc, 4, "Subtotal:", subtotalField);
        addRow(form, gbc, 5, "IVA:", ivaField);
        addRow(form, gbc, 6, "Total:", totalField);
        addRow(form, gbc, 7, "Notas:", notesField);
        addRow(form, gbc, 8, "ID Transaccion (opc):", txField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton(existing == null ? "Crear" : "Guardar");
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            try {
                LocalDate issueDate = dateField.getText().trim().isEmpty() ? LocalDate.now() : LocalDate.parse(dateField.getText().trim());
                BigDecimal subtotal = parseDec(subtotalField.getText());
                BigDecimal iva = parseDec(ivaField.getText());
                BigDecimal total = parseDec(totalField.getText());
                if (existing == null) {
                    dm.createInvoice(txField.getText().trim(), numField.getText().trim(), merchantField.getText().trim(),
                            taxIdField.getText().trim(), issueDate, subtotal, iva, total, notesField.getText().trim());
                } else {
                    dm.updateInvoice(existing.getId(), txField.getText().trim(), numField.getText().trim(),
                            merchantField.getText().trim(), taxIdField.getText().trim(), issueDate, subtotal, iva, total, notesField.getText().trim());
                }
                dialog.dispose();
                loadInvoices(null, null);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        actions.add(cancel);
        actions.add(save);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteSelected() {
        BackendInvoice sel = tableModel.get(table.getSelectedRow());
        if (sel == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Eliminar factura " + sel.getInvoiceNumber() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (!dm.deleteInvoice(sel.getId())) {
                JOptionPane.showMessageDialog(this, dm.getLastErrorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                loadInvoices(null, null);
            }
        }
    }

    private void uploadAttachment() {
        BackendInvoice sel = tableModel.get(table.getSelectedRow());
        if (sel == null) { JOptionPane.showMessageDialog(this, "Selecciona una factura primero.", "Aviso", JOptionPane.INFORMATION_MESSAGE); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar PDF o imagen");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            SwingWorker<BackendInvoice, Void> worker = new SwingWorker<BackendInvoice, Void>() {
                @Override protected BackendInvoice doInBackground() throws Exception { return dm.uploadInvoiceAttachment(sel.getId(), file); }
                @Override protected void done() {
                    try { get(); JOptionPane.showMessageDialog(FacturasPanel.this, "Adjunto cargado.", "Exito", JOptionPane.INFORMATION_MESSAGE); loadInvoices(null, null); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(FacturasPanel.this, "Error: " + causeMessage(ex), "Error", JOptionPane.ERROR_MESSAGE); }
                }
            };
            worker.execute();
        }
    }

    private void downloadAttachment() {
        BackendInvoice sel = tableModel.get(table.getSelectedRow());
        if (sel == null) { JOptionPane.showMessageDialog(this, "Selecciona una factura primero.", "Aviso", JOptionPane.INFORMATION_MESSAGE); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar adjunto como...");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            SwingWorker<byte[], Void> worker = new SwingWorker<byte[], Void>() {
                @Override protected byte[] doInBackground() throws Exception { return dm.downloadInvoiceAttachment(sel.getId()); }
                @Override protected void done() {
                    try { byte[] data = get(); java.nio.file.Files.write(target.toPath(), data); JOptionPane.showMessageDialog(FacturasPanel.this, "Guardado en: " + target.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(FacturasPanel.this, "Error: " + causeMessage(ex), "Error", JOptionPane.ERROR_MESSAGE); }
                }
            };
            worker.execute();
        }
    }

    private static String causeMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() == null || cause.getMessage().trim().isEmpty()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }

    private JTextField field(String value) {
        JTextField f = new JTextField(value, 20);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return f;
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(field, gbc);
    }

    private BigDecimal parseDec(String text) {
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(text.replace(",", "."));
    }

    private static class InvoiceTableModel extends AbstractTableModel {
        private List<BackendInvoice> data = new ArrayList<>();

        void setData(List<BackendInvoice> d) { this.data = d == null ? new ArrayList<>() : d; fireTableDataChanged(); }
        BackendInvoice get(int row) { if (row < 0 || row >= data.size()) return null; return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            BackendInvoice inv = data.get(r);
            switch (c) {
                case 0: return inv.getInvoiceNumber();
                case 1: return inv.getMerchantName();
                case 2: return inv.getTaxId();
                case 3: return inv.getIssueDate() == null ? "" : inv.getIssueDate().format(DATE_FMT);
                case 4: return inv.getSubtotal() == null ? "" : "$" + inv.getSubtotal().toPlainString();
                case 5: return inv.getTaxAmount() == null ? "" : "$" + inv.getTaxAmount().toPlainString();
                case 6: return inv.getTotalAmount() == null ? "" : "$" + inv.getTotalAmount().toPlainString();
                case 7: return inv.getNotes();
                default: return "";
            }
        }
    }
}
