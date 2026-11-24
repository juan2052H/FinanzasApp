import com.finanzas.data.DataManager;
import com.finanzas.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Capturas en Windows real (Robot) para revisar el tema: secciones, un dialogo con un desplegable
 * abierto y un JOptionPane. Uso: java -cp <jar>;. CapturarTemaWindows <salida> <correo>
 * Requiere FINANZAS_API_ENABLED=true y el servidor encendido.
 */
public class CapturarTemaWindows {
    private static final String[] SECCIONES = {"inicio", "ingresos", "gastos", "presupuesto", "metas", "hogar", "facturas", "reportes", "config"};

    public static void main(String[] args) throws Exception {
        File salida = new File(args[0]);
        salida.mkdirs();
        String correo = args.length > 1 ? args[1] : "laura.lectora@finanzasapp.test";
        Robot robot = new Robot();

        DataManager data = DataManager.getInstance();
        if (!data.login(correo, "Prueba2026*")) {
            throw new IllegalStateException("No fue posible iniciar sesion: " + data.getLastErrorMessage());
        }
        AtomicReference<MainFrame> main = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> main.set(new MainFrame()));
        Thread.sleep(3000);
        JDialog inicial = dialogoVisible(main.get());
        if (inicial != null) {
            capturar(robot, inicial.getBounds(), new File(salida, "00_primeros_pasos.png"));
        }
        cerrarDialogos(main.get());
        SwingUtilities.invokeAndWait(() -> { main.get().setLocation(20, 20); main.get().toFront(); });
        Thread.sleep(800);

        for (int i = 0; i < SECCIONES.length; i++) {
            String seccion = SECCIONES[i];
            SwingUtilities.invokeAndWait(() -> main.get().showSection(seccion));
            Thread.sleep(1800);
            capturar(robot, main.get().getBounds(), new File(salida, String.format("%02d_%s.png", i + 1, seccion)));
        }

        // Dialogo de nuevo gasto con el desplegable de categorias abierto
        SwingUtilities.invokeLater(() -> main.get().showNewTransaction(com.finanzas.model.Transaccion.Tipo.GASTO));
        Thread.sleep(2000);
        JDialog dialogo = dialogoVisible(main.get());
        if (dialogo != null) {
            SwingUtilities.invokeAndWait(() -> {
                JComboBox<?> combo = primerCombo(dialogo.getContentPane());
                if (combo != null) {
                    combo.showPopup();
                }
            });
            Thread.sleep(1000);
            Rectangle zona = dialogo.getBounds();
            zona.height += 260;
            capturar(robot, zona, new File(salida, "20_dialogo_gasto_desplegable.png"));
            SwingUtilities.invokeAndWait(dialogo::dispose);
        }

        // Mensaje JOptionPane tipico
        SwingUtilities.invokeLater(() -> {
            JDialog d = new JOptionPane("Esta es una confirmacion de ejemplo.\nDesea continuar con la operacion?",
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION).createDialog(main.get(), "Confirmar");
            d.setModal(false);
            d.setVisible(true);
        });
        Thread.sleep(1500);
        JDialog mensaje = dialogoVisible(main.get());
        if (mensaje != null) {
            capturar(robot, mensaje.getBounds(), new File(salida, "21_mensaje.png"));
            SwingUtilities.invokeAndWait(mensaje::dispose);
        }
        // Cambio de tema en caliente (como en Configuracion > Tema): args[2] = LIGHT o DARK
        if (args.length > 2) {
            SwingUtilities.invokeAndWait(() -> {
                com.finanzas.ui.components.AppColors.applyTheme(args[2]);
                main.get().rebuildForThemeChange();
                main.get().showSection("gastos");
            });
            Thread.sleep(2000);
            capturar(robot, main.get().getBounds(), new File(salida, "30_tras_cambio_" + args[2].toLowerCase() + ".png"));
            SwingUtilities.invokeLater(() -> main.get().showNewTransaction(com.finanzas.model.Transaccion.Tipo.GASTO));
            Thread.sleep(2000);
            JDialog trasCambio = dialogoVisible(main.get());
            if (trasCambio != null) {
                capturar(robot, trasCambio.getBounds(), new File(salida, "31_dialogo_tras_cambio.png"));
                SwingUtilities.invokeAndWait(trasCambio::dispose);
            }
        }
        System.out.println("Capturas en " + salida.getAbsolutePath());
        System.exit(0);
    }

    private static void cerrarDialogos(Window principal) throws Exception {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && w != principal) {
                SwingUtilities.invokeAndWait(d::dispose);
            }
        }
    }

    private static JDialog dialogoVisible(Window principal) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && w != principal) {
                return d;
            }
        }
        return null;
    }

    private static JComboBox<?> primerCombo(Container raiz) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof JComboBox<?> combo && combo.isShowing()) {
                return combo;
            }
            if (c instanceof Container cont) {
                JComboBox<?> r = primerCombo(cont);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private static void capturar(Robot robot, Rectangle zona, File archivo) throws Exception {
        ImageIO.write(robot.createScreenCapture(zona), "png", archivo);
        System.out.println("  " + archivo.getName());
    }
}
