package sistemainventario.view;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import sistemainventario.AppContext;
import sistemainventario.bridge.JavaBridge;
import sistemainventario.bridge.WebViewNotifier;
import sistemainventario.model.Profile;

import java.net.URL;

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
    /** Se conserva como atributo para que el recolector de basura no lo elimine. */
    private JavaBridge bridge;

    public WebViewMain(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * Construye y muestra la ventana principal.
     *
     * @param ventana escenario principal de JavaFX
     */
    public void show(Stage ventana) {
        WebView webView = new WebView();
        motor = webView.getEngine();
        motor.setOnAlert(evento -> System.out.println("[HTML Alert] " + evento.getData()));
        motor.getLoadWorker().stateProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == Worker.State.SUCCEEDED) {
                registrarBridge();
                inicializarEstadoHtml();
            }
        });
        motor.load(localizarHtml());

        ventana.setScene(new Scene(webView, ANCHO_INICIAL, ALTO_INICIAL));
        ventana.setMinWidth(ANCHO_MINIMO);
        ventana.setMinHeight(ALTO_MINIMO);
        ventana.setOnHidden(evento -> apagarCamara());
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
        bridge = new JavaBridge(contexto, new WebViewNotifier(motor));
        JSObject ventanaJs = (JSObject) motor.executeScript("window");
        ventanaJs.setMember("javaBridge", bridge);
    }

    private void inicializarEstadoHtml() {
        try {
            motor.executeScript("if (typeof cargarEstadoDesdeJava === 'function') cargarEstadoDesdeJava();");
        } catch (RuntimeException e) {
            System.err.println("[WebViewMain] Error en inicializacion: " + e.getMessage());
        }
    }
}
