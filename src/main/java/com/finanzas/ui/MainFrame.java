package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.data.SearchResult;
import com.finanzas.model.Transaccion;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.Sidebar;
import com.finanzas.ui.dialogs.OnboardingDialog;
import com.finanzas.ui.dialogs.RecurringTransactionDialog;
import com.finanzas.ui.dialogs.SimulationDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class MainFrame extends JFrame {
    private static final String[] CARDS = {"inicio", "ingresos", "gastos", "presupuesto", "metas", "hogar", "reportes", "config"};

    private JPanel contentArea;
    private CardLayout cardLayout;
    private Sidebar sidebar;
    private TransaccionesPanel ingresosPanel;
    private TransaccionesPanel gastosPanel;
    private PresupuestoPanel presupuestoPanel;
    private MetasPanel metasPanel;
    private FinanzasHogarPanel hogarPanel;

    public MainFrame() {
        setTitle("Gestion de Finanzas Personales y del Hogar");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).fine("No fue posible aplicar look and feel del sistema: " + ex.getMessage());
        }
        buildUI();
        installCommandPaletteShortcut();
        setVisible(true);
        SwingUtilities.invokeLater(this::showOnboardingIfNeeded);
    }

    private void buildUI() {
        add(new HeaderPanel(), BorderLayout.NORTH);

        sidebar = new Sidebar(this::onMenuSelect);
        add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(AppColors.MAIN_BG);

        ingresosPanel = new TransaccionesPanel(Transaccion.Tipo.INGRESO, "Ingresos");
        gastosPanel = new TransaccionesPanel(Transaccion.Tipo.GASTO, "Gastos");
        presupuestoPanel = new PresupuestoPanel();
        metasPanel = new MetasPanel();
        hogarPanel = new FinanzasHogarPanel();

        contentArea.add(new DashboardPanel(), "inicio");
        contentArea.add(ingresosPanel, "ingresos");
        contentArea.add(gastosPanel, "gastos");
        contentArea.add(presupuestoPanel, "presupuesto");
        contentArea.add(metasPanel, "metas");
        contentArea.add(hogarPanel, "hogar");
        contentArea.add(new ReportesPanel(), "reportes");
        contentArea.add(new ConfiguracionPanel(), "config");

        add(contentArea, BorderLayout.CENTER);
    }

    private void onMenuSelect(int index) {
        if (index >= 0 && index < CARDS.length) {
            cardLayout.show(contentArea, CARDS[index]);
        }
    }

    public void showSection(String section) {
        for (int i = 0; i < CARDS.length; i++) {
            if (CARDS[i].equals(section)) {
                cardLayout.show(contentArea, CARDS[i]);
                sidebar.setActiveIndex(i);
                return;
            }
        }
    }

    public void showNewTransaction(Transaccion.Tipo tipo) {
        if (tipo == Transaccion.Tipo.INGRESO) {
            showSection("ingresos");
            ingresosPanel.openNewTransactionDialog();
        } else {
            showSection("gastos");
            gastosPanel.openNewTransactionDialog();
        }
    }

    public void showNewGoal() {
        showSection("metas");
        metasPanel.openNewMetaDialog();
    }

    public void showNewBudget() {
        showSection("presupuesto");
        presupuestoPanel.openNewBudgetDialog();
    }

    public void showNewHouseholdExpense() {
        showSection("hogar");
        hogarPanel.openNewHouseholdExpenseDialog();
    }

    private void installCommandPaletteShortcut() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "commandPalette");
        getRootPane().getActionMap().put("commandPalette", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showCommandPalette();
            }
        });
    }

    private void showCommandPalette() {
        JDialog dialog = new JDialog(this, "Acciones rapidas", true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(Color.WHITE);

        addPaletteAction(panel, dialog, "Nuevo ingreso", () -> showNewTransaction(Transaccion.Tipo.INGRESO));
        addPaletteAction(panel, dialog, "Nuevo gasto", () -> showNewTransaction(Transaccion.Tipo.GASTO));
        addPaletteAction(panel, dialog, "Nueva meta", this::showNewGoal);
        addPaletteAction(panel, dialog, "Nuevo presupuesto", this::showNewBudget);
        addPaletteAction(panel, dialog, "Gasto compartido", this::showNewHouseholdExpense);
        addPaletteAction(panel, dialog, "Transaccion recurrente", this::showNewRecurringTransaction);
        addPaletteAction(panel, dialog, "Busqueda global", this::showGlobalSearch);
        addPaletteAction(panel, dialog, "Simulador financiero", this::showSimulationDialog);
        addPaletteAction(panel, dialog, "Generar reporte", () -> showSection("reportes"));

        dialog.setContentPane(panel);
        dialog.setSize(360, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showOnboardingIfNeeded() {
        if (DataManager.getInstance().needsOnboarding()) {
            OnboardingDialog.show(this);
        }
    }

    private void addPaletteAction(JPanel panel, JDialog dialog, String label, Runnable action) {
        JButton button = new JButton(label);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.addActionListener(e -> {
            dialog.dispose();
            action.run();
        });
        panel.add(button);
        panel.add(Box.createVerticalStrut(6));
    }

    private void showNewRecurringTransaction() {
        RecurringTransactionDialog.show(this, DataManager.getInstance()::addRecurringTransaction);
    }

    private void showSimulationDialog() {
        SimulationDialog.show(this);
    }

    private void showGlobalSearch() {
        JDialog dialog = new JDialog(this, "Busqueda global", true);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(Color.WHITE);

        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        DefaultListModel<SearchResult> model = new DefaultListModel<SearchResult>();
        JList<SearchResult> resultsList = new JList<SearchResult>(model);
        resultsList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultsList.setFixedCellHeight(54);
        resultsList.setSelectionBackground(new Color(0xEBF3FE));
        resultsList.setCellRenderer((list, value, index, selected, focus) -> {
            JPanel row = new JPanel(new BorderLayout(8, 2));
            row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            row.setBackground(selected ? new Color(0xEBF3FE) : Color.WHITE);
            JLabel title = new JLabel(value.getType() + " | " + value.getTitle());
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title.setForeground(AppColors.TEXT_PRIMARY);
            JLabel detail = new JLabel(value.getDetail());
            detail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            detail.setForeground(AppColors.TEXT_MUTED);
            row.add(title, BorderLayout.NORTH);
            row.add(detail, BorderLayout.CENTER);
            return row;
        });

        Runnable refreshResults = () -> {
            model.clear();
            List<SearchResult> results = DataManager.getInstance().searchGlobal(searchField.getText());
            for (int i = 0; i < Math.min(30, results.size()); i++) {
                model.addElement(results.get(i));
            }
        };

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshResults.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshResults.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshResults.run(); }
        });

        resultsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    openSearchResult(dialog, resultsList.getSelectedValue());
                }
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton openButton = new JButton("Abrir");
        JButton closeButton = new JButton("Cerrar");
        openButton.addActionListener(e -> openSearchResult(dialog, resultsList.getSelectedValue()));
        closeButton.addActionListener(e -> dialog.dispose());
        actions.add(closeButton);
        actions.add(openButton);

        JLabel hint = new JLabel("Busca transacciones, presupuestos, metas o miembros.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(AppColors.TEXT_MUTED);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(searchField, BorderLayout.CENTER);
        top.add(hint, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultsList), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setSize(520, 460);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private void openSearchResult(JDialog dialog, SearchResult result) {
        if (result == null) {
            return;
        }
        dialog.dispose();
        showSection(result.getSection());
    }
}
