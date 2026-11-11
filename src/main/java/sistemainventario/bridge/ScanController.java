package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.dispositivo.Camera;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.service.QrSaleScanner;
import sistemainventario.util.ImageEncoder;
import sistemainventario.util.QrReader;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * <b>GRASP Controller</b> del caso de uso "registrar venta por QR".
 *
 * <p>Enciende la cámara en un hilo propio para no congelar la ventana. Por
 * cada cuadro envía una vista previa a la pantalla y, cuando reconoce una
 * etiqueta, avisa a la vista con el producto para agregarlo a la venta.
 * Funciones de la vista que se invocan: {@code tbCamaraLista()},
 * {@code tbCuadroCamara(imagen)}, {@code tbProductoEscaneado(producto)},
 * {@code tbAvisoEscaneo(mensaje)}, {@code tbErrorCamara(mensaje)} y
 * {@code tbCamaraDetenida()}.</p>
 *
 * <p><b>Bajo acoplamiento:</b> recibe la cámara mediante una fábrica de
 * {@link Camera} y avisa a la vista mediante {@link ViewNotifier}, así
 * que no depende de la webcam ni de JavaFX.</p>
 */
public class ScanController {

    /** Pausa entre cuadros: unas 10 lecturas por segundo. */
    private static final long PAUSA_ENTRE_CUADROS_MS = 90;
    /** Ancho de la vista previa que se envía a la pantalla. */
    private static final int ANCHO_VISTA_PREVIA = 480;
    /** Espera máxima a que una sesión anterior suelte la cámara. */
    private static final long ESPERA_SESION_ANTERIOR_MS = 3000;

    private final AppContext contexto;
    private final Supplier<Camera> fabricaCamara;
    private final ViewNotifier vista;

    private AtomicBoolean sesionActiva;
    private Thread hiloActual;

    public ScanController(AppContext contexto, Supplier<Camera> fabricaCamara, ViewNotifier vista) {
        this.contexto = contexto;
        this.fabricaCamara = fabricaCamara;
        this.vista = vista;
    }

    /**
     * Enciende la cámara y empieza a buscar etiquetas QR.
     *
     * @return {@code "OK"}, o un error si no hay perfil activo
     */
    public synchronized String iniciarEscaneoQR() {
        return BridgeResponse.ejecutarOk(() -> {
            contexto.requerirServiciosActivos();
            if (sesionActiva != null && sesionActiva.get()) {
                return;
            }
            AtomicBoolean sesion = new AtomicBoolean(true);
            Thread anterior = hiloActual;
            hiloActual = new Thread(() -> escanear(sesion, anterior), "escaner-qr");
            hiloActual.setDaemon(true);
            sesionActiva = sesion;
            hiloActual.start();
        });
    }

    /**
     * Apaga la cámara. No falla si ya estaba apagada.
     *
     * @return {@code "OK"}
     */
    public synchronized String detenerEscaneoQR() {
        if (sesionActiva != null) {
            sesionActiva.set(false);
        }
        return BridgeResponse.OK;
    }

    /** @return {@code true} mientras la cámara está encendida o encendiéndose */
    public synchronized boolean estaEscaneando() {
        return sesionActiva != null && sesionActiva.get();
    }

    private void escanear(AtomicBoolean sesion, Thread anterior) {
        esperarA(anterior);
        Camera camara = fabricaCamara.get();
        QrSaleScanner escaner = new QrSaleScanner(new QrReader());
        try {
            camara.abrir();
            avisar("tbCamaraLista()");
            while (sesion.get()) {
                BufferedImage cuadro = camara.capturar();
                if (cuadro != null) {
                    avisar("tbCuadroCamara(" + BridgeResponse.literalJs(
                            ImageEncoder.aJpegDataUri(cuadro, ANCHO_VISTA_PREVIA)) + ")");
                    buscarProducto(escaner, cuadro);
                }
                Thread.sleep(PAUSA_ENTRE_CUADROS_MS);
            }
        } catch (ValidationException e) {
            avisar("tbErrorCamara(" + BridgeResponse.literalJs(e.getMessage()) + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            System.err.println("[EscaneoController] " + e);
            avisar("tbErrorCamara(" + BridgeResponse.literalJs("Error con la camara: " + e.getMessage()) + ")");
        } finally {
            camara.cerrar();
            sesion.set(false);
            avisar("tbCamaraDetenida()");
        }
    }

    private void buscarProducto(QrSaleScanner escaner, BufferedImage cuadro) {
        try {
            escaner.procesar(cuadro, contexto.requerirServiciosActivos().inventario())
                    .ifPresent(producto -> avisar("tbProductoEscaneado(" + BridgeResponse.json(resumen(producto)) + ")"));
        } catch (ValidationException e) {
            avisar("tbAvisoEscaneo(" + BridgeResponse.literalJs(e.getMessage()) + ")");
        }
    }

    /** Datos que la vista necesita para agregar el producto a la venta. */
    private static Map<String, Object> resumen(Product producto) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", producto.getId());
        datos.put("nombre", producto.getNombre());
        datos.put("precioVenta", producto.getPrecioVenta());
        datos.put("stock", producto.getStock());
        return datos;
    }

    private static void esperarA(Thread anterior) {
        if (anterior == null) {
            return;
        }
        try {
            anterior.join(ESPERA_SESION_ANTERIOR_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void avisar(String script) {
        vista.ejecutarScript(script);
    }
}
