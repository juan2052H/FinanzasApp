package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.data.FinancialHealthSnapshot;
import com.finanzas.data.NotificationItem;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.RecurringTransaction;
import com.finanzas.model.Transaccion;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.AppIcons;
import com.finanzas.ui.components.BarChartPanel;
import com.finanzas.ui.components.CardPanel;
import com.finanzas.ui.components.DonutChartPanel;
import com.finanzas.ui.components.RoundedButton;
import com.finanzas.ui.components.SummaryCard;
import com.finanzas.ui.dialogs.SimulationDialog;
import com.finanzas.ui.dialogs.TransactionDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final NumberFormat nf = NumberFormat.getInstance(data.getDisplayLocale());
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DashboardPanel() {
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
        data.addListener(() -> SwingUtilities.invokeLater(this::refresh));
    }

    private void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void buildUI() {
        add(buildPageHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setBackground(AppColors.MAIN_BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
        content.add(buildSummaryRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildMiddleRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildBottomRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildUpcomingRecurringCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildActionButtons());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(AppColors.MAIN_BG);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildPageHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppColors.MAIN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 4, 20));

        JLabel title = new JLabel("Inicio");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Resumen de tu situacion financiera");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(AppColors.MAIN_BG);
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitle);

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "CO")));
        JLabel dateLabel = new JLabel(AppIcons.CALENDAR + "  " + currentDate);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateLabel.setForeground(AppColors.TEXT_SECONDARY);

        panel.add(left, BorderLayout.WEST);
        panel.add(dateLabel, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setBackground(AppColors.MAIN_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        row.add(new SummaryCard("Saldo Actual", data.getSaldoActual(), data.getBalanceVariationText(), AppIcons.BALANCE, AppColors.CARD_SALDO));
        row.add(new SummaryCard("Ingresos del mes", data.getIngresosMesActual(), data.getIncomeVariationText(), AppIcons.INCOME, AppColors.CARD_INGRESOS));
        row.add(new SummaryCard("Gastos del mes", data.getGastosMesActual(), data.getExpenseVariationText(), AppIcons.EXPENSE, AppColors.CARD_GASTOS));
        row.add(new SummaryCard("Ahorro del mes", data.getAhorroMesActual(), String.format(Locale.US, "Tasa %.1f%%", data.getTasaAhorroMesActual()), AppIcons.SAVINGS, AppColors.CARD_AHORROS));
        return row;
    }

    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setBackground(AppColors.MAIN_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));
        row.add(buildDonutCard());
        row.add(buildBarCard());
        row.add(buildMetasCard());
        return row;
    }

    private JPanel buildDonutCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.add(cardTitle("Distribucion de gastos", AppIcons.DONUT), BorderLayout.NORTH);

        if (data.getCategorias().length == 0) {
            card.add(emptyState("Todavia no existen gastos para analizar."), BorderLayout.CENTER);
            return card;
        }

        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setOpaque(false);
        DonutChartPanel donut = new DonutChartPanel(data.getCategorias(), data.getPorcentajes(), AppColors.DONUT_COLORS);
        donut.setPreferredSize(new Dimension(160, 200));
        body.add(donut, BorderLayout.WEST);
        body.add(buildDonutLegend(), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDonutLegend() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        String[] categories = data.getCategorias();
        double[] percentages = data.getPorcentajes();
        Color[] colors = AppColors.DONUT_COLORS;
        for (int i = 0; i < categories.length; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            row.setOpaque(false);
            JLabel dot = new JLabel("o");
            dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
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

    private JPanel buildBarCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.add(cardTitle("Ingresos vs gastos", AppIcons.CHART + "  Ultimos 6 meses"), BorderLayout.NORTH);
        if (hasChartData(data.getIngresosUltimos6Meses()) || hasChartData(data.getGastosUltimos6Meses())) {
            card.add(new BarChartPanel(data.getMesesLabels(), data.getIngresosUltimos6Meses(), data.getGastosUltimos6Meses()), BorderLayout.CENTER);
        } else {
            card.add(emptyState("Registra tu primer ingreso o gasto para ver el flujo de caja."), BorderLayout.CENTER);
        }
        return card;
    }

    private JPanel buildMetasCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        JPanel header = cardTitle("Metas de ahorro", AppIcons.PLUS);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showMetasSection();
            }
        });
        card.add(header, BorderLayout.NORTH);

        JPanel metasPanel = new JPanel();
        metasPanel.setLayout(new BoxLayout(metasPanel, BoxLayout.Y_AXIS));
        metasPanel.setOpaque(false);
        List<MetaAhorro> metas = data.getMetas();
        if (metas.isEmpty()) {
            metasPanel.add(emptyState("Define una meta y empieza a construir tu futuro."));
        } else {
            for (int i = 0; i < Math.min(2, metas.size()); i++) {
                metasPanel.add(buildMetaItem(metas.get(i)));
                metasPanel.add(Box.createVerticalStrut(10));
            }
        }

        JLabel viewAll = linkLabel(metas.isEmpty() ? "Crear primera meta  " + AppIcons.ARROW : "Ver todas mis metas  " + AppIcons.ARROW);
        viewAll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showMetasSection();
            }
        });
        metasPanel.add(Box.createVerticalStrut(8));
        metasPanel.add(viewAll);
        card.add(metasPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildMetaItem(MetaAhorro meta) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);

        JLabel name = new JLabel(meta.getIcono() + "  " + meta.getNombre());
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(AppColors.TEXT_PRIMARY);
        JLabel pct = new JLabel(meta.getProgreso() + "%");
        pct.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pct.setForeground(Color.decode(meta.getColor()));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(pct, BorderLayout.EAST);

        JLabel amounts = new JLabel("$" + nf.format((long) meta.getMontoActual()) + " / $" + nf.format((long) meta.getMontoMeta()) + "  Faltan $" + nf.format((long) meta.getFaltante()));
        amounts.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        amounts.setForeground(AppColors.TEXT_MUTED);
        panel.add(top, BorderLayout.NORTH);
        panel.add(amounts, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setBackground(AppColors.MAIN_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        row.add(buildTransaccionesCard());
        row.add(buildFinancialHealthCard());
        row.add(buildAlertsCard());
        return row;
    }

    private JPanel buildFinancialHealthCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        FinancialHealthSnapshot health = data.getFinancialHealthSnapshot();

        JLabel title = new JLabel("Salud Financiera");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(AppColors.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel score = new JLabel(health.getScore() + "/100 - " + health.getLabel());
        score.setFont(new Font("Segoe UI", Font.BOLD, 24));
        score.setForeground(health.getScore() >= 65 ? AppColors.ACCENT_GREEN : health.getScore() >= 45 ? AppColors.ACCENT_YELLOW : AppColors.ACCENT_RED);
        score.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(score);
        card.add(Box.createVerticalStrut(10));

        addHealthLine(card, "Fortaleza", firstOrDefault(health.getStrengths(), "Aun no hay suficientes datos para detectar fortalezas."));
        card.add(Box.createVerticalStrut(6));
        addHealthLine(card, "Oportunidad", firstOrDefault(health.getOpportunities(), "Mantener tus datos actualizados mejora el diagnostico."));
        return card;
    }

    private JPanel buildTransaccionesCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        JPanel header = cardTitle("Transacciones recientes", "Ver todas");
        header.getComponent(1).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.getComponent(1).addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showSection("gastos");
            }
        });
        card.add(header, BorderLayout.NORTH);

        List<Transaccion> transacciones = data.getTransacciones();
        if (transacciones.isEmpty()) {
            card.add(emptyState("No tienes movimientos todavia."), BorderLayout.CENTER);
            return card;
        }

        Object[][] tableData = new Object[Math.min(5, transacciones.size())][5];
        for (int i = 0; i < tableData.length; i++) {
            Transaccion transaccion = transacciones.get(i);
            tableData[i][0] = transaccion.getTipo() == Transaccion.Tipo.INGRESO ? "Ingreso" : "Gasto";
            tableData[i][1] = transaccion.getCategoria();
            tableData[i][2] = transaccion.getDescripcion();
            tableData[i][3] = (transaccion.getTipo() == Transaccion.Tipo.INGRESO ? "+" : "-") + "$" + nf.format((long) transaccion.getMonto());
            tableData[i][4] = transaccion.getFecha().format(dtf);
        }

        JTable table = new JTable(tableData, new String[]{"Tipo", "Categoria", "Descripcion", "Monto", "Fecha"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xEBF3FE));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.getTableHeader().setBackground(AppColors.MAIN_BG);
        table.getTableHeader().setForeground(AppColors.TEXT_SECONDARY);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                String tipo = (String) table.getValueAt(row, 0);
                setForeground(column == 3 || column == 0
                        ? ("Ingreso".equals(tipo) ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED)
                        : AppColors.TEXT_SECONDARY);
                setFont(new Font("Segoe UI", column == 3 ? Font.BOLD : Font.PLAIN, 11));
                setBackground(selected ? new Color(0xEBF3FE) : Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAlertsCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(AppIcons.ALERT + "  Alertas y notificaciones");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(AppColors.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        List<NotificationItem> alerts = data.getNotifications();
        if (alerts.isEmpty()) {
            card.add(buildAlertItem(AppIcons.INFO, "Sin alertas criticas", "No hay eventos financieros que requieran atencion.", AppColors.ACCENT_BLUE));
        } else {
            for (int i = 0; i < Math.min(4, alerts.size()); i++) {
                NotificationItem alert = alerts.get(i);
                card.add(buildAlertItem(notificationIcon(alert), alert.getTitle(), alert.getMessage(), notificationColor(alert)));
                card.add(Box.createVerticalStrut(6));
            }
        }

        card.add(Box.createVerticalStrut(12));
        JLabel tipTitle = new JLabel(AppIcons.IDEA + "  Consejo financiero");
        tipTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tipTitle.setForeground(AppColors.TEXT_PRIMARY);
        tipTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tipTitle);
        card.add(Box.createVerticalStrut(8));
        JLabel tip = new JLabel("<html><body style='width:240px;color:#6b7280;font-size:11px;'>" + buildDynamicTip() + "</body></html>");
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tip);
        return card;
    }

    private JPanel buildUpcomingRecurringCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        card.add(cardTitle("Proximos movimientos", "30 dias"), BorderLayout.NORTH);

        List<RecurringTransaction> upcoming = data.getUpcomingRecurringTransactions(30);
        if (upcoming.isEmpty()) {
            card.add(emptyState("No hay pagos o ingresos recurrentes previstos."), BorderLayout.CENTER);
            return card;
        }

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        for (int i = 0; i < Math.min(4, upcoming.size()); i++) {
            RecurringTransaction recurring = upcoming.get(i);
            rows.add(buildRecurringRow(recurring));
            if (i < Math.min(4, upcoming.size()) - 1) {
                rows.add(Box.createVerticalStrut(4));
            }
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecurringRow(RecurringTransaction recurring) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        String sign = recurring.getTipo() == Transaccion.Tipo.INGRESO ? "+" : "-";
        JLabel label = new JLabel(recurring.getNextDate().format(dtf) + "  " + recurring.getDescripcion());
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(AppColors.TEXT_SECONDARY);

        JLabel amount = new JLabel(sign + "$" + nf.format((long) recurring.getMonto()));
        amount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        amount.setForeground(recurring.getTipo() == Transaccion.Tipo.INGRESO ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED);

        JButton apply = new JButton("Registrar");
        apply.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        apply.setFocusPainted(false);
        apply.addActionListener(e -> data.materializeRecurringTransaction(recurring));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(amount);
        right.add(apply);

        row.add(label, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void addHealthLine(JPanel card, String label, String value) {
        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setForeground(AppColors.TEXT_MUTED);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel text = new JLabel("<html><body style='width:210px;color:#374151;font-size:10px;'>" + value + "</body></html>");
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(2));
        card.add(text);
    }

    private String firstOrDefault(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }

    private String buildDynamicTip() {
        List<String> insights = data.getRuleBasedInsights();
        return insights.isEmpty() ? "Mantener tus datos actualizados mejora el diagnostico." : insights.get(0);
    }

    private String notificationIcon(NotificationItem item) {
        if (item.getSeverity() == NotificationItem.Severity.CRITICAL || item.getSeverity() == NotificationItem.Severity.WARNING) {
            return AppIcons.ALERT;
        }
        if (item.getSeverity() == NotificationItem.Severity.SUCCESS) {
            return AppIcons.INFO;
        }
        return AppIcons.INFO;
    }

    private Color notificationColor(NotificationItem item) {
        if (item.getSeverity() == NotificationItem.Severity.CRITICAL) {
            return AppColors.ACCENT_RED;
        }
        if (item.getSeverity() == NotificationItem.Severity.WARNING) {
            return new Color(0xFF9800);
        }
        if (item.getSeverity() == NotificationItem.Severity.SUCCESS) {
            return AppColors.ACCENT_GREEN;
        }
        return AppColors.ACCENT_BLUE;
    }

    private JPanel buildAlertItem(String icon, String title, String desc, Color color) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconLabel.setForeground(color);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(AppColors.TEXT_PRIMARY);
        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        descLabel.setForeground(AppColors.TEXT_MUTED);
        textPanel.add(titleLabel);
        textPanel.add(descLabel);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(AppColors.MAIN_BG);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        RoundedButton incomeButton = new RoundedButton(AppIcons.INCOME + " Nuevo Ingreso", AppColors.BTN_GREEN);
        RoundedButton expenseButton = new RoundedButton(AppIcons.EXPENSE + " Nuevo Gasto", AppColors.BTN_RED);
        RoundedButton simulatorButton = new RoundedButton(AppIcons.IDEA + " Simulador", AppColors.ACCENT_YELLOW);
        RoundedButton reportButton = new RoundedButton(AppIcons.EXPORT + " Generar Reporte", AppColors.BTN_BLUE);
        incomeButton.addActionListener(e -> TransactionDialog.show(this, Transaccion.Tipo.INGRESO, null, data::addTransaccion));
        expenseButton.addActionListener(e -> TransactionDialog.show(this, Transaccion.Tipo.GASTO, null, data::addTransaccion));
        simulatorButton.addActionListener(e -> SimulationDialog.show(this));
        reportButton.addActionListener(e -> showSection("reportes"));
        panel.add(incomeButton);
        panel.add(expenseButton);
        panel.add(simulatorButton);
        panel.add(reportButton);
        return panel;
    }

    private JPanel cardTitle(String leftText, String rightText) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel left = new JLabel(leftText);
        left.setFont(new Font("Segoe UI", Font.BOLD, 13));
        left.setForeground(AppColors.TEXT_PRIMARY);
        JLabel right = new JLabel(rightText);
        right.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        right.setForeground(AppColors.ACCENT_BLUE);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JLabel linkLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(AppColors.ACCENT_BLUE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return label;
    }

    private JLabel emptyState(String message) {
        JLabel label = new JLabel("<html><div style='text-align:center;color:#6b7280;width:190px;'>" + message + "</div></html>", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(AppColors.TEXT_MUTED);
        return label;
    }

    private boolean hasChartData(double[] values) {
        for (double value : values) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    private void showMetasSection() {
        showSection("metas");
    }

    private void showSection(String section) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainFrame) {
            ((MainFrame) window).showSection(section);
        }
    }

    private JPanel createCard() {
        JPanel card = new CardPanel();
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return card;
    }
}
