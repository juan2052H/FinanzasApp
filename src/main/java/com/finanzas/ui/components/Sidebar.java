package com.finanzas.ui.components;

import com.finanzas.data.DataManager;
import com.finanzas.model.Presupuesto;
import com.finanzas.model.Transaccion;
import com.finanzas.ui.dialogs.TransactionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

public class Sidebar extends JPanel {

    private final String[] menuItems = {"Inicio", "Ingresos", "Gastos", "Presupuesto", "Ahorro y Metas", "Finanzas del Hogar", "Facturas", "Reportes", "Configuracion"};
    private final String[] menuIcons = {
            AppIcons.HOME,
            AppIcons.INCOME,
            AppIcons.EXPENSE,
            AppIcons.BUDGET,
            AppIcons.GOALS,
            AppIcons.HOUSEHOLD,
            "\uD83D\uDCC4",
            AppIcons.REPORTS,
            AppIcons.SETTINGS
    };
    private int activeIndex = 0;
    private int hoveredIndex = -1;
    private final Consumer<Integer> onSelect;

    public Sidebar(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        setBackground(AppColors.SIDEBAR_BG);
        setPreferredSize(new Dimension(200, 0));
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        logoPanel.setBackground(AppColors.SIDEBAR_BG);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel logoIcon = new JLabel(AppIcons.APP);
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        JLabel logoText = new JLabel("<html><span style='color:#fff;font-size:13px;font-weight:bold;'>FinanzasApp</span></html>");
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(AppColors.SIDEBAR_BG);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (int i = 0; i < menuItems.length; i++) {
            menuPanel.add(createMenuItem(i));
            menuPanel.add(Box.createVerticalStrut(2));
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        buttonPanel.setBackground(AppColors.SIDEBAR_BG);

        RoundedButton newButton = new RoundedButton(AppIcons.PLUS + " Nuevo Ingreso", AppColors.ACCENT_BLUE);
        newButton.setPreferredSize(new Dimension(176, 38));
        newButton.addActionListener(e -> showNewTransactionDialog("INGRESO"));
        buttonPanel.add(newButton);

        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        logoutPanel.setBackground(new Color(0x141e35));
        logoutPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel logoutIcon = new JLabel(AppIcons.LOGOUT);
        logoutIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        JLabel logoutText = new JLabel("Cerrar sesion");
        logoutText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutText.setForeground(AppColors.SIDEBAR_TEXT);
        logoutPanel.add(logoutIcon);
        logoutPanel.add(logoutText);
        logoutPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                com.finanzas.data.DataManager.getInstance().logout();
                new com.finanzas.ui.LoginFrame().setVisible(true);
                SwingUtilities.getWindowAncestor(logoutPanel).dispose();
            }
        });

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(AppColors.SIDEBAR_BG);
        topWrapper.add(logoPanel, BorderLayout.NORTH);
        topWrapper.add(menuPanel, BorderLayout.CENTER);
        topWrapper.add(buttonPanel, BorderLayout.SOUTH);

        add(topWrapper, BorderLayout.CENTER);
        add(logoutPanel, BorderLayout.SOUTH);
    }

    private JPanel createMenuItem(int index) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (activeIndex == index) {
                    g2.setColor(AppColors.SIDEBAR_ACTIVE);
                } else if (hoveredIndex == index) {
                    g2.setColor(AppColors.SIDEBAR_HOVER);
                } else {
                    g2.setColor(AppColors.SIDEBAR_BG);
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel iconLabel = new JLabel(menuIcons[index]);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));

        JLabel textLabel = new JLabel(menuItems[index]);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textLabel.setForeground(activeIndex == index ? Color.WHITE : AppColors.SIDEBAR_TEXT);

        int budgetRiskCount = budgetRiskCount();
        if (index == 3 && budgetRiskCount > 0) {
            JLabel badge = new JLabel(String.valueOf(budgetRiskCount));
            badge.setOpaque(false);
            badge.setForeground(Color.WHITE);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            JPanel badgePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AppColors.ACCENT_RED);
                    g2.fillOval(0, 0, 18, 18);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badgePanel.setOpaque(false);
            badgePanel.setPreferredSize(new Dimension(18, 18));
            badgePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 2));
            badgePanel.add(badge);
            item.add(iconLabel);
            item.add(textLabel);
            item.add(badgePanel);
        } else {
            item.add(iconLabel);
            item.add(textLabel);
        }

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoveredIndex = index;
                item.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                item.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                activeIndex = index;
                updateTextColors();
                repaintAll();
                if (onSelect != null) {
                    onSelect.accept(index);
                }
            }
        });

        return item;
    }

    private void repaintAll() {
        for (Component component : ((JPanel) ((JPanel) getComponent(0)).getComponent(1)).getComponents()) {
            component.repaint();
        }
    }

    private void updateTextColors() {
        JPanel menuPanel = (JPanel) ((JPanel) getComponent(0)).getComponent(1);
        for (int i = 0; i < menuPanel.getComponentCount(); i++) {
            Component component = menuPanel.getComponent(i);
            if (component instanceof JPanel) {
                JPanel item = (JPanel) component;
                for (Component child : item.getComponents()) {
                    if (child instanceof JLabel) {
                        JLabel label = (JLabel) child;
                        if (!label.getText().matches("\\d+")) {
                            int idx = i / 2;
                            label.setForeground(activeIndex == idx ? Color.WHITE : AppColors.SIDEBAR_TEXT);
                        }
                    }
                }
            }
        }
    }

    private void showNewTransactionDialog(String type) {
        TransactionDialog.show(this, Transaccion.Tipo.INGRESO, null, DataManager.getInstance()::addTransaccion);
    }

    public void setActiveIndex(int index) {
        this.activeIndex = index;
        repaint();
    }

    private int budgetRiskCount() {
        int count = 0;
        for (Presupuesto presupuesto : DataManager.getInstance().getPresupuestos()) {
            if (presupuesto.getPorcentajeUsado() >= 85) {
                count++;
            }
        }
        return count;
    }
}
