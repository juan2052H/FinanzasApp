import com.finanzas.data.DataManager;
import com.finanzas.ui.LoginFrame;
import com.finanzas.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Capturas del manual de instalacion (docs/REQUISITOS_E_INSTALACION.md):
 * instalacion_03 = pantalla de acceso con la cuenta de Ana escrita,
 * instalacion_04 = primer inicio en "Hogar Gomez" con el dialogo "Primeros pasos".
 * Requiere datos recien sembrados (el dialogo solo aparece si el usuario no lo ha completado).
 */
public class CapturarInstalacion {
    private static final String HOGAR = "20000000-0000-0000-0000-000000000010";

    public static void main(String[] args) throws Exception {
        File salida = new File(args.length > 0 ? args[0] : "capturas");
        salida.mkdirs();

        AtomicReference<LoginFrame> login = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
            login.set(frame);
        });
        Thread.sleep(1500);
        SwingUtilities.invokeAndWait(() -> {
            JTextField correo = buscar(login.get().getContentPane(), JTextField.class, false);
            JPasswordField clave = buscar(login.get().getContentPane(), JPasswordField.class, true);
            correo.setText("ana.duena@finanzasapp.test");
            clave.setText("Prueba2026*");
        });
        Thread.sleep(500);
        ImageIO.write(pintar(login.get()), "png", new File(salida, "instalacion_03_inicio_sesion.png"));
        SwingUtilities.invokeAndWait(() -> login.get().dispose());

        DataManager data = DataManager.getInstance();
        if (!data.login("ana.duena@finanzasapp.test", "Prueba2026*") || !data.selectBackendWorkspace(HOGAR)) {
            throw new IllegalStateException("No fue posible entrar a Hogar Gomez con Ana.");
        }
        AtomicReference<MainFrame> main = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> main.set(new MainFrame()));
        Thread.sleep(3000);

        JDialog dialogo = null;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing()) {
                dialogo = d;
            }
        }
        if (dialogo == null) {
            throw new IllegalStateException("No aparecio el dialogo Primeros pasos (reinicie los datos con docker compose down -v).");
        }
        BufferedImage fondo = pintar(main.get());
        BufferedImage frente = pintar(dialogo);
        Point pf = main.get().getRootPane().getLocationOnScreen();
        Point pd = dialogo.getRootPane().getLocationOnScreen();
        Graphics2D g = fondo.createGraphics();
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(0, 0, fondo.getWidth(), fondo.getHeight());
        int x = pd.x - pf.x;
        int y = pd.y - pf.y;
        g.drawImage(frente, x, y, null);
        g.setColor(new Color(0, 0, 0, 90));
        g.drawRect(x - 1, y - 1, frente.getWidth() + 1, frente.getHeight() + 1);
        g.dispose();
        ImageIO.write(fondo, "png", new File(salida, "instalacion_04_primeros_pasos.png"));
        System.out.println("Capturas de instalacion generadas en " + salida.getAbsolutePath());
        System.exit(0);
    }

    private static <T extends Component> T buscar(Container raiz, Class<T> tipo, boolean exacto) {
        for (Component c : raiz.getComponents()) {
            if (c.isShowing() && (exacto ? tipo.isInstance(c) : c.getClass() == tipo)) {
                return tipo.cast(c);
            }
            if (c instanceof Container cont) {
                T t = buscar(cont, tipo, exacto);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static BufferedImage pintar(Component ventana) throws Exception {
        // Solo el contenido: sin la barra de titulo que Xvfb dibuja encima del encabezado.
        Component component = ventana instanceof javax.swing.RootPaneContainer r ? r.getRootPane() : ventana;
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
        return imagen.get();
    }
}
