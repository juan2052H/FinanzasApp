package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;

public class PageHeader extends JPanel {
    public PageHeader(String titleText, String subtitleText) {
        setLayout(new BorderLayout());
        setBackground(AppColors.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(AppColors.MAIN_BG);
        left.add(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            JLabel subtitle = new JLabel(subtitleText);
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subtitle.setForeground(AppColors.TEXT_SECONDARY);
            left.add(subtitle);
        }

        add(left, BorderLayout.WEST);
    }
}
