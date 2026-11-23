package sistemainventario.view;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import sistemainventario.bridge.BridgeInvoker;
import sistemainventario.bridge.JavaBridge;
import sistemainventario.bridge.SseNotifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP local (solo en {@code 127.0.0.1}) que entrega la interfaz a Edge
 * y conecta la página con Java.
 *
 * <ul>
 *   <li>{@code GET /...}: archivos de la interfaz (HTML, CSS, fuentes) desde
 *       {@code src/main/resources/html}.</li>
 *   <li>{@code POST /bridge/<metodo>}: llamada de la página a {@code javaBridge};
 *       la atiende {@link BridgeInvoker}.</li>
 *   <li>{@code GET /eventos}: flujo de eventos por el que Java avisa a la página
 *       ({@link SseNotifier}).</li>
 * </ul>
 *
 * <p><b>Seguridad:</b> escucha solo en la propia máquina y las llamadas al
 * puente exigen una clave aleatoria que se genera en cada arranque y solo
 * conoce la ventana que abre el programa.</p>
 */
public class LocalWebServer {

    private static final String RAIZ_RECURSOS = "/html/";
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "ttf", "font/ttf",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "json", "application/json; charset=utf-8",
            "txt", "text/plain; charset=utf-8");

    private final BridgeInvoker invocador;
    private final SseNotifier notificador;
    private final String clave;
    private HttpServer servidor;
    private ExecutorService hilos;

    /**
     * @param invocador   ejecuta los métodos del puente
     * @param notificador canal de eventos hacia la página
     */
    public LocalWebServer(BridgeInvoker invocador, SseNotifier notificador) {
        this.invocador = invocador;
        this.notificador = notificador;
        byte[] azar = new byte[16];
        new SecureRandom().nextBytes(azar);
        this.clave = HexFormat.of().formatHex(azar);
    }

    /**
     * Arranca el servidor en un puerto libre.
     *
     * @return dirección de la interfaz, con la clave, lista para abrir en Edge
     * @throws IOException si no se pudo abrir el puerto
     */
    public String iniciar() throws IOException {
        // Sin esto, cada respuesta pequeña espera ~40 ms (algoritmo de Nagle + ACK
        // retrasado del sistema operativo) y una venta tarda medio segundo en verse.
        System.setProperty("sun.net.httpserver.nodelay", "true");
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/bridge/", this::atenderPuente);
        servidor.createContext("/eventos", this::atenderEventos);
        servidor.createContext("/", this::atenderArchivo);
        hilos = Executors.newCachedThreadPool(r -> {
            Thread hilo = new Thread(r, "tbalance-servidor");
            hilo.setDaemon(true);
            return hilo;
        });
        servidor.setExecutor(hilos);
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/SistemaInventario.html?t=" + clave;
    }

    /** Detiene el servidor. */
    public void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
        if (hilos != null) {
            hilos.shutdownNow();
        }
    }

    private void atenderPuente(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            if (!"POST".equals(intercambio.getRequestMethod()) || !clave.equals(intercambio.getRequestHeaders().getFirst("X-TB-Token"))) {
                responder(intercambio, 403, "text/plain; charset=utf-8", "Acceso denegado".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String metodo = intercambio.getRequestURI().getPath().substring("/bridge/".length());
            String argumentos = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String resultado;
            // de a una, como en JavaFX (un solo hilo de interfaz), y sin cruzarse con el asistente
            synchronized (JavaBridge.CANDADO_DATOS) {
                resultado = invocador.invocar(metodo, argumentos);
            }
            responder(intercambio, 200, TIPOS.get("json"), resultado.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void atenderEventos(HttpExchange intercambio) throws IOException {
        String consulta = intercambio.getRequestURI().getRawQuery();
        if (consulta == null || !consulta.equals("t=" + clave)) {
            try (intercambio) {
                responder(intercambio, 403, "text/plain; charset=utf-8", new byte[0]);
            }
            return;
        }
        intercambio.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        intercambio.getResponseHeaders().set("Cache-Control", "no-cache");
        intercambio.sendResponseHeaders(200, 0);
        OutputStream salida = intercambio.getResponseBody();
        salida.write(": conectado\n\n".getBytes(StandardCharsets.UTF_8));
        salida.flush();
        // La conexión queda abierta; SseNotifier escribe en ella y la quita cuando falla.
        notificador.agregar(salida);
    }

    private void atenderArchivo(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String ruta = URLDecoder.decode(intercambio.getRequestURI().getPath(), StandardCharsets.UTF_8);
            if (ruta.equals("/")) {
                ruta = "/SistemaInventario.html";
            }
            if (ruta.contains("..") || !"GET".equals(intercambio.getRequestMethod())) {
                responder(intercambio, 404, "text/plain; charset=utf-8", new byte[0]);
                return;
            }
            try (InputStream recurso = getClass().getResourceAsStream(RAIZ_RECURSOS + ruta.substring(1))) {
                if (recurso == null) {
                    responder(intercambio, 404, "text/plain; charset=utf-8", new byte[0]);
                    return;
                }
                String extension = ruta.substring(ruta.lastIndexOf('.') + 1).toLowerCase();
                responder(intercambio, 200, TIPOS.getOrDefault(extension, "application/octet-stream"), recurso.readAllBytes());
            }
        }
    }

    private static void responder(HttpExchange intercambio, int codigo, String tipo, byte[] cuerpo) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.sendResponseHeaders(codigo, cuerpo.length == 0 ? -1 : cuerpo.length);
        if (cuerpo.length > 0) {
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        }
    }
}
