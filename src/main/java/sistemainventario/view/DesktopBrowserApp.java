package sistemainventario.view;

import sistemainventario.AppContext;
import sistemainventario.bridge.BridgeInvoker;
import sistemainventario.bridge.JavaBridge;
import sistemainventario.bridge.SseNotifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Arranca TBalance mostrando la interfaz en una ventana de Edge (modo
 * aplicación) en lugar del visor de JavaFX.
 *
 * <p>Motivo: el visor de JavaFX dibuja la página con el procesador y las
 * animaciones grandes van a tirones; Edge la dibuja con la tarjeta gráfica.
 * La interfaz (HTML), el puente ({@link JavaBridge}) y la lógica son los
 * mismos: solo cambia quién dibuja la ventana.</p>
 *
 * <p>Si no hay Edge ni Chrome, o se ejecuta con {@code -Dtbalance.visor=javafx},
 * el programa usa el visor de JavaFX ({@link WebViewMain}).</p>
 */
public final class DesktopBrowserApp {

    /** Segundos sin ninguna página conectada para dar la ventana por cerrada. */
    private static final int ESPERA_CIERRE = 4;

    private DesktopBrowserApp() {
    }

    /**
     * Intenta abrir la interfaz en Edge.
     *
     * @param contexto servicios de la aplicación
     * @return {@code true} si la ventana se abrió; {@code false} para usar JavaFX
     */
    public static boolean iniciar(AppContext contexto) {
        if ("javafx".equalsIgnoreCase(System.getProperty("tbalance.visor"))) {
            return false;
        }
        Optional<String> navegador = BrowserWindow.buscarNavegador();
        if (navegador.isEmpty()) {
            return false;
        }
        SseNotifier notificador = new SseNotifier();
        JavaBridge puente = new JavaBridge(contexto, notificador, null, tarea -> {
            synchronized (JavaBridge.CANDADO_DATOS) {
                tarea.run();
            }
        });
        LocalWebServer servidor = new LocalWebServer(new BridgeInvoker(puente), notificador);
        Path carpetaVentana = Path.of(System.getProperty("user.home"), ".inventariopro", "ventana");
        BrowserWindow ventana = new BrowserWindow(carpetaVentana);
        try {
            String url = servidor.iniciar();
            ventana.abrir(navegador.get(), url);
            WindowsTaskbarIcon.aplicar(carpetaVentana);
        } catch (IOException e) {
            System.err.println("[TBalance] No se pudo abrir la ventana en Edge, se usa JavaFX: " + e.getMessage());
            servidor.detener();
            return false;
        }
        vigilarCierre(servidor, ventana, notificador);
        return true;
    }

    /**
     * Termina el programa cuando la ventana se cierra, es decir, cuando ninguna
     * página sigue conectada por unos segundos (una recarga se reconecta antes).
     */
    private static void vigilarCierre(LocalWebServer servidor, BrowserWindow ventana, SseNotifier notificador) {
        Runnable salir = () -> {
            ventana.cerrar();
            servidor.detener();
            System.exit(0);
        };
        ScheduledExecutorService reloj = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread hilo = new Thread(r, "tbalance-vigia");
            hilo.setDaemon(false);
            return hilo;
        });
        int[] segundosSinPagina = {0};
        boolean[] yaConecto = {false};
        reloj.scheduleAtFixedRate(() -> {
            notificador.latido();
            if (notificador.conectadas() > 0) {
                yaConecto[0] = true;
                segundosSinPagina[0] = 0;
            } else if (yaConecto[0] && ++segundosSinPagina[0] >= ESPERA_CIERRE) {
                salir.run();
            } else if (!yaConecto[0] && ++segundosSinPagina[0] >= 60) {
                // la ventana nunca llegó a conectarse
                salir.run();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
