package com.finanzas.ui.components;

import com.finanzas.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AvatarView extends JPanel {
    private final Usuario usuario;
    private final int size;
    private final Color fallbackColor;
    private final Color textColor;
    private final int fontSize;

    public AvatarView(Usuario usuario, int size, Color fallbackColor, Color textColor, int fontSize) {
        this.usuario = usuario;
        this.size = size;
        this.fallbackColor = fallbackColor;
        this.textColor = textColor;
        this.fontSize = fontSize;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Image image = loadProfileImage();
        Shape clip = new java.awt.geom.Ellipse2D.Double(0, 0, size, size);
        g2.setClip(clip);

        if (image != null) {
            g2.drawImage(image, 0, 0, size, size, this);
        } else {
            g2.setColor(fallbackColor);
            g2.fillOval(0, 0, size, size);
            g2.setColor(textColor);
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            String initials = buildInitials();
            FontMetrics metrics = g2.getFontMetrics();
            int x = (size - metrics.stringWidth(initials)) / 2;
            int y = ((size - metrics.getHeight()) / 2) + metrics.getAscent();
            g2.drawString(initials, x, y);
        }

        g2.dispose();
    }

    private Image loadProfileImage() {
        String imagePath = usuario.getProfileImagePath();
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }
        File file = new File(imagePath);
        if (!file.isFile()) {
            return null;
        }
        ImageIcon icon = new ImageIcon(imagePath);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        return icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    private String buildInitials() {
        StringBuilder builder = new StringBuilder();
        if (usuario.getNombre() != null && !usuario.getNombre().isEmpty()) {
            builder.append(Character.toUpperCase(usuario.getNombre().charAt(0)));
        }
        if (usuario.getApellido() != null && !usuario.getApellido().isEmpty()) {
            builder.append(Character.toUpperCase(usuario.getApellido().charAt(0)));
        }
        return builder.length() == 0 ? "U" : builder.toString();
    }
}
