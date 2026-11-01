import com.finanzas.data.DataManager;
import com.finanzas.ui.LoginFrame;
import com.finanzas.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Container;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Genera capturas reales de las pantallas de FinanzasApp (hoja "7. Mockups").
 * Inicia sesion contra el backend con el usuario de prueba Ana y recorre cada seccion.
 * Requiere FINANZAS_API_ENABLED=true y FINANZAS_API_BASE_URL; ver scripts/capturas/capturar.sh.
 */
public class CapturarPantallas {
    private static final String HOGAR = "20000000-0000-0000-0000-000000000010";
    private static final String[] SECCIONES = {"inicio", "ingresos", "gastos", "presupuesto", "metas", "hogar", "facturas", "reportes", "config"};

    public static void main(String[] args) throws Exception {
        File salida = new File(args.length > 0 ? args[0] : "capturas");
        salida.mkdirs();

        AtomicReference<JFrame> login = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
            login.set(frame);
        });
        Thread.sleep(1500);
        capturar(login.get(), new File(salida, "MK-001_acceso.png"));
        SwingUtilities.invokeAndWait(() -> login.get().dispose());

        DataManager data = DataManager.getInstance();
        if (!data.login("ana.duena@finanzasapp.test", "Prueba2026*")) {
            throw new IllegalStateException("No fue posible iniciar sesion contra el backend.");
        }
        if (!data.selectBackendWorkspace(HOGAR)) {
            throw new IllegalStateException("No fue posible seleccionar el workspace Hogar Gomez.");
        }

        AtomicReference<MainFrame> main = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> main.set(new MainFrame()));
        Thread.sleep(2500);
        cerrarDialogos(main.get(), salida);

        for (int i = 0; i < SECCIONES.length; i++) {
            String seccion = SECCIONES[i];
            SwingUtilities.invokeAndWait(() -> main.get().showSection(seccion));
            Thread.sleep(2000);
            capturar(main.get(), new File(salida, String.format("pantalla_%02d_%s.png", i + 1, seccion)));
        }
        // CP-014: buscar "Arriendo" en Gastos debe dejar solo los arriendos.
        SwingUtilities.invokeAndWait(() -> main.get().showSection("gastos"));
        Thread.sleep(1000);
        SwingUtilities.invokeAndWait(() -> {
            JTextField buscador = primerCampoVisible(main.get().getContentPane());
            if (buscador == null) {
                throw new IllegalStateException("No se encontro el buscador de Gastos.");
            }
            buscador.setText("Arriendo");
        });
        Thread.sleep(1500);
        capturar(main.get(), new File(salida, "CP-014_busqueda_gastos.png"));
        SwingUtilities.invokeAndWait(() -> {
            JTable tabla = primeraTablaVisible(main.get().getContentPane());
            System.out.println("CP-014 filas visibles con 'Arriendo': " + (tabla == null ? "sin tabla" : tabla.getRowCount()));
        });

        System.out.println("Capturas generadas en " + salida.getAbsolutePath());
        System.exit(0);
    }

    private static void cerrarDialogos(Window principal, File salida) throws Exception {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing() && window != principal) {
                capturar(dialog, new File(salida, "MK-002_onboarding.png"));
                SwingUtilities.invokeAndWait(dialog::dispose);
            }
        }
    }

    private static JTextField primerCampoVisible(Container raiz) {
        for (Component c : raiz.getComponents()) {
            if (!c.isShowing()) {
                continue;
            }
            if (c instanceof JTextField campo && campo.isEditable()) {
                return campo;
            }
            if (c instanceof Container contenedor) {
                JTextField encontrado = primerCampoVisible(contenedor);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private static JTable primeraTablaVisible(Container raiz) {
        for (Component c : raiz.getComponents()) {
            if (!c.isShowing()) {
                continue;
            }
            if (c instanceof JTable tabla) {
                return tabla;
            }
            if (c instanceof Container contenedor) {
                JTable encontrada = primeraTablaVisible(contenedor);
                if (encontrada != null) {
                    return encontrada;
                }
            }
        }
        return null;
    }

    private static void capturar(Component component, File archivo) throws Exception {
        AtomicReference<BufferedImage> imagen = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            component.printAll(g);
            g.dispose();
            imagen.set(image);
        });
        ImageIO.write(imagen.get(), "png", archivo);
        System.out.println("  " + archivo.getName());
    }
}
