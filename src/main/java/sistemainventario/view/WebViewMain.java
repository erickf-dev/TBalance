package sistemainventario.view;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import sistemainventario.AppContext;
import sistemainventario.bridge.AccessPolicy;
import sistemainventario.bridge.BridgeInvoker;
import sistemainventario.bridge.JavaBridge;
import sistemainventario.bridge.WebViewNotifier;
import sistemainventario.model.Profile;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Ventana principal: carga la interfaz HTML en un {@link WebView} y registra
 * el {@link JavaBridge} como {@code window.javaBridge}.
 *
 * <p><b>SRP:</b> solo construye la ventana y conecta la vista con el bridge;
 * no contiene lógica de negocio.</p>
 */
public class WebViewMain {

    private static final String RECURSO_HTML = "/html/SistemaInventario.html";
    private static final double ANCHO_INICIAL = 1300;
    private static final double ALTO_INICIAL = 800;
    private static final double ANCHO_MINIMO = 1000;
    private static final double ALTO_MINIMO = 650;

    private final AppContext contexto;
    private WebEngine motor;
    private WebView webView;
    /** Contenedor de la ventana: el visor web y, encima, las imágenes de las transiciones. */
    private StackPane raiz;
    /** Se conserva como atributo para que el recolector de basura no lo elimine. */
    private JavaBridge bridge;
    /** Se guarda en un campo para que JavaFX no lo libere mientras la página lo usa. */
    private WebViewBridgeAdapter puenteJs;

    public WebViewMain(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * Construye y muestra la ventana principal.
     *
     * @param ventana escenario principal de JavaFX
     */
    public void show(Stage ventana) {
        webView = new WebView();
        raiz = new StackPane(webView);
        raiz.setAlignment(Pos.TOP_LEFT);
        motor = webView.getEngine();
        motor.setOnAlert(evento -> System.out.println("[HTML Alert] " + evento.getData()));
        motor.getLoadWorker().stateProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == Worker.State.SUCCEEDED) {
                registrarBridge();
                inicializarEstadoHtml();
            }
        });
        motor.load(localizarHtml());

        ventana.setScene(new Scene(raiz, ANCHO_INICIAL, ALTO_INICIAL));
        ventana.setMinWidth(ANCHO_MINIMO);
        ventana.setMinHeight(ALTO_MINIMO);
        ventana.setOnHidden(evento -> apagarCamara());
        URL icono = getClass().getResource("/html/img/tbalance-app-256.png");
        if (icono != null) {
            ventana.getIcons().add(new Image(icono.toExternalForm()));
        }
        actualizarTitulo(ventana);
        ventana.show();
    }

    /**
     * @param ventana ventana cuyo título se actualiza con el perfil activo
     */
    public void actualizarTitulo(Stage ventana) {
        String nombre = contexto.getGestorPerfiles().getPerfilActivo()
                .map(Profile::getNombre)
                .orElse("Sin perfil activo");
        ventana.setTitle("TBalance — " + nombre);
    }

    private String localizarHtml() {
        URL url = getClass().getResource(RECURSO_HTML);
        if (url == null) {
            throw new IllegalStateException("No se encontro " + RECURSO_HTML + " en resources");
        }
        return url.toExternalForm();
    }

    /** Apaga la cámara si la ventana se cierra con el escáner de ventas abierto. */
    private void apagarCamara() {
        if (bridge != null) {
            bridge.detenerEscaneoQR();
        }
    }

    private void registrarBridge() {
        bridge = new JavaBridge(contexto, new WebViewNotifier(motor), new SnapshotTransition(raiz, webView),
                WebViewMain::enHiloDeInterfaz);
        puenteJs = new WebViewBridgeAdapter(new BridgeInvoker(bridge, new AccessPolicy(contexto)));
        JSObject ventanaJs = (JSObject) motor.executeScript("window");
        ventanaJs.setMember("__tbPuente", puenteJs);
        motor.executeScript(WebViewBridgeAdapter.SCRIPT_PUENTE);
    }

    /**
     * Ejecuta una tarea en el hilo de JavaFX y espera a que termine (el asistente
     * trabaja en otro hilo y así no toca los datos al mismo tiempo que la interfaz).
     */
    private static void enHiloDeInterfaz(Runnable tarea) {
        if (Platform.isFxApplicationThread()) {
            tarea.run();
            return;
        }
        FutureTask<Void> pendiente = new FutureTask<>(tarea, null);
        Platform.runLater(pendiente);
        try {
            pendiente.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private void inicializarEstadoHtml() {
        try {
            // tbArrancar muestra el inicio de sesión o carga los datos si ya hay sesión
            motor.executeScript("if (typeof tbArrancar === 'function') tbArrancar();");
        } catch (RuntimeException e) {
            System.err.println("[WebViewMain] Error en inicializacion: " + e.getMessage());
        }
    }
}
