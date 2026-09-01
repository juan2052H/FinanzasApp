package com.finanzas.ui;

import com.finanzas.api.BackendWorkspace;
import com.finanzas.data.DataManager;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.AppIcons;
import com.finanzas.ui.components.AvatarView;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HeaderPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final JLabel userName = new JLabel();
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
    private final JPanel rightWrapper = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("  Gestion de Finanzas Personales y del Hogar");
    private boolean workspaceActionRunning;

    public HeaderPanel() {
        setBackground(AppColors.HEADER_BG);
        setPreferredSize(new Dimension(0, 56));
        setLayout(new BorderLayout());
        data.addListener(() -> SwingUtilities.invokeLater(this::refreshUserInfo));
        buildUI();
    }

    private void buildUI() {
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        rightPanel.setBackground(AppColors.HEADER_BG);
        userName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userName.setForeground(Color.WHITE);
        userName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rightWrapper.setBackground(AppColors.HEADER_BG);
        rightWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        add(title, BorderLayout.WEST);
        add(rightWrapper, BorderLayout.EAST);
        refreshUserInfo();
    }

    private void refreshUserInfo() {
        rightPanel.removeAll();
        String currentUserName = data.getUsuario().getNombreCompleto();
        if (currentUserName == null || currentUserName.trim().isEmpty()) {
            currentUserName = "Usuario";
        }

        userName.setText(currentUserName + "  " + AppIcons.ARROW);
        if (data.isBackendSessionActive()) {
            rightPanel.add(buildWorkspaceSelector());
            JLabel apiBadge = new JLabel("API");
            apiBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            apiBadge.setForeground(Color.WHITE);
            apiBadge.setOpaque(true);
            apiBadge.setBackground(new Color(0x16a34a));
            apiBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            rightPanel.add(apiBadge);
        }
        rightPanel.add(new AvatarView(data.getUsuario(), 32, new Color(0x4a90d9), Color.WHITE, 13));
        rightPanel.add(userName);
        rightWrapper.removeAll();
        rightWrapper.add(rightPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JComponent buildWorkspaceSelector() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setOpaque(false);
        List<BackendWorkspace> workspaces = data.getBackendWorkspaces();
        JComboBox<BackendWorkspace> selector = new JComboBox<BackendWorkspace>(workspaces.toArray(new BackendWorkspace[0]));
        selector.setPreferredSize(new Dimension(220, 30));
        selector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selector.setFocusable(false);
        selector.setEnabled(!workspaceActionRunning && !workspaces.isEmpty());
        selector.setToolTipText("Workspace activo");
        selector.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "Sin workspace" : workspaceLabel(value));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label.setBackground(selected ? new Color(0xEBF3FE) : Color.WHITE);
            label.setForeground(AppColors.TEXT_PRIMARY);
            return label;
        });
        BackendWorkspace active = data.getActiveBackendWorkspace();
        if (active != null) {
            selector.setSelectedItem(active);
        }
        selector.addActionListener(e -> {
            BackendWorkspace selected = (BackendWorkspace) selector.getSelectedItem();
            BackendWorkspace current = data.getActiveBackendWorkspace();
            if (selected == null || current != null && selected.getId().equals(current.getId())) {
                return;
            }
            runWorkspaceAction(() -> data.selectBackendWorkspace(selected.getId()), "No fue posible cambiar el workspace.");
        });

        JButton create = new JButton("+");
        create.setPreferredSize(new Dimension(30, 30));
        create.setToolTipText("Crear workspace");
        create.setFocusPainted(false);
        create.setEnabled(!workspaceActionRunning);
        create.addActionListener(e -> showCreateWorkspaceDialog());

        panel.add(selector);
        panel.add(create);
        return panel;
    }

    private String workspaceLabel(BackendWorkspace workspace) {
        String name = workspace.getNombre() == null || workspace.getNombre().trim().isEmpty()
                ? "Workspace"
                : workspace.getNombre().trim();
        String role = workspace.getRole() == null || workspace.getRole().trim().isEmpty()
                ? "MEMBER"
                : workspace.getRole().trim();
        return name + " - " + role;
    }

    private void showCreateWorkspaceDialog() {
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        JTextField nameField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<String>(new String[]{"PERSONAL", "HOUSEHOLD", "BUSINESS"});
        form.add(new JLabel("Nombre:"));
        form.add(nameField);
        form.add(new JLabel("Tipo:"));
        form.add(typeBox);

        int result = JOptionPane.showConfirmDialog(this, form, "Crear workspace", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runWorkspaceAction(
                () -> data.createBackendWorkspace(nameField.getText().trim(), (String) typeBox.getSelectedItem()),
                "No fue posible crear el workspace.");
    }

    private void runWorkspaceAction(WorkspaceAction action, String fallbackMessage) {
        if (workspaceActionRunning) {
            return;
        }
        workspaceActionRunning = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        refreshUserInfo();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return action.run();
            }

            @Override
            protected void done() {
                workspaceActionRunning = false;
                setCursor(Cursor.getDefaultCursor());
                try {
                    if (!get()) {
                        String message = data.getLastErrorMessage();
                        JOptionPane.showMessageDialog(HeaderPanel.this,
                                message == null || message.trim().isEmpty() ? fallbackMessage : message,
                                "Workspace",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HeaderPanel.this, fallbackMessage, "Workspace", JOptionPane.ERROR_MESSAGE);
                }
                refreshUserInfo();
            }
        }.execute();
    }

    private interface WorkspaceAction {
        boolean run();
    }
}
