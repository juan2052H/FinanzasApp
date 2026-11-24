package com.finanzas.ui.components;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Contenido de pagina dentro de un JScrollPane que se ajusta al ancho visible: solo hay
 * desplazamiento vertical. Sin esto el contenido se desbordaba a la derecha en ventanas de
 * 1280 px y aparecia una barra horizontal que ocultaba tarjetas y columnas.
 */
public class ViewportWidthPanel extends JPanel implements Scrollable {

    public ViewportWidthPanel() {
        super();
    }

    public ViewportWidthPanel(LayoutManager layout) {
        super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(visibleRect.height - 32, 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
