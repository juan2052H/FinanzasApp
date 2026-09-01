package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.AppIcons;
import com.finanzas.ui.components.AvatarView;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final JLabel userName = new JLabel();
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
    private final JPanel rightWrapper = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("  Gestion de Finanzas Personales y del Hogar");

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
            String workspaceName = data.getBackendWorkspaces().isEmpty() ? "" : data.getBackendWorkspaces().get(0).getNombre();
            JLabel apiBadge = new JLabel(workspaceName == null || workspaceName.trim().isEmpty() ? "API" : "API - " + workspaceName);
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
}
