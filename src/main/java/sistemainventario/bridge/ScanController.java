package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.dispositivo.Camera;
import sistemainventario.dispositivo.CameraProvider;
import sistemainventario.dispositivo.PhoneLink;
import sistemainventario.util.QrGenerator;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.service.QrSaleScanner;
import sistemainventario.util.ImageEncoder;
import sistemainventario.util.QrReader;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final CameraProvider camaras;
    private volatile String camaraElegida;
    private final ViewNotifier vista;
    private final PhoneLink celular;

    private AtomicBoolean sesionActiva;
    private Thread hiloActual;

    /**
     * @param contexto contexto de la aplicación
     * @param camaras  cámaras disponibles (webcam o celular)
     * @param vista    canal para avisar a la página
     * @param celular  conexión con el celular por Wi-Fi
     */
    public ScanController(AppContext contexto, CameraProvider camaras, ViewNotifier vista, PhoneLink celular) {
        this.contexto = contexto;
        this.camaras = camaras;
        this.vista = vista;
        this.celular = celular;
    }

    /**
     * Prepara la conexión con el celular: abre el servidor en la red local y
     * devuelve el código QR que el celular debe escanear para conectarse.
     *
     * @return JSON {@code {direccion, qr}} (qr es una imagen PNG en Base64), o un error
     */
    public String prepararCelular() {
        return BridgeResponse.ejecutar(() -> {
            contexto.requerirServiciosActivos();
            String direccion = celular.iniciar();
            BufferedImage codigo = new QrGenerator().generar(direccion, 260);
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("direccion", direccion);
            datos.put("qr", ImageEncoder.aPngDataUri(codigo));
            return BridgeResponse.json(datos);
        });
    }

    /** @return {@code true} si el celular está enviando imágenes */
    public boolean celularConectado() {
        return celular.celularConectado();
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
     * Enciende una cámara concreta (por ejemplo, el celular usado como cámara web).
     *
     * @param nombre nombre de la cámara, o vacío para la predeterminada
     * @return {@code "OK"}, o un error si no hay perfil activo
     */
    public synchronized String iniciarEscaneoQRCon(String nombre) {
        String elegida = nombre == null || nombre.isBlank() || "null".equals(nombre) ? null : nombre;
        if (!java.util.Objects.equals(elegida, camaraElegida) && sesionActiva != null) {
            // Cambio de cámara: se apaga la actual y la nueva espera a que la suelte.
            sesionActiva.set(false);
        }
        camaraElegida = elegida;
        return iniciarEscaneoQR();
    }

    /**
     * Busca las cámaras en segundo plano (puede tardar unos segundos) y avisa a la
     * vista con {@code tbCamarasDisponibles(["...", ...])}.
     *
     * @return {@code "OK"}
     */
    public String listarCamaras() {
        Thread hilo = new Thread(() -> {
            try {
                avisar("tbCamarasDisponibles(" + BridgeResponse.json(camaras.listar()) + ")");
            } catch (RuntimeException e) {
                avisar("tbCamarasDisponibles([])");
            }
        }, "buscar-camaras");
        hilo.setDaemon(true);
        hilo.start();
        return BridgeResponse.OK;
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
        Camera camara = camaras.crear(camaraElegida);
        QrSaleScanner escaner = new QrSaleScanner(new QrReader()).conNombresDeNegocio(
                id -> contexto.getGestorPerfiles().buscarPorId(id).map(p -> p.getNombre()));
        try {
            camara.abrir();
            avisarSiEsActual(sesion, "tbCamaraLista()");
            while (sesion.get()) {
                BufferedImage cuadro = camara.capturar();
                if (cuadro != null) {
                    avisar("tbCuadroCamara(" + BridgeResponse.literalJs(
                            ImageEncoder.aJpegDataUri(cuadro, ANCHO_VISTA_PREVIA)) + ")");
                    buscarProducto(escaner, cuadro, camara);
                }
                Thread.sleep(PAUSA_ENTRE_CUADROS_MS);
            }
        } catch (ValidationException e) {
            avisarSiEsActual(sesion, "tbErrorCamara(" + BridgeResponse.literalJs(e.getMessage()) + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            System.err.println("[EscaneoController] " + e);
            avisarSiEsActual(sesion, "tbErrorCamara(" + BridgeResponse.literalJs("Error con la camara: " + e.getMessage()) + ")");
        } finally {
            camara.cerrar();
            sesion.set(false);
            avisarSiEsActual(sesion, "tbCamaraDetenida()");
        }
    }

    private void buscarProducto(QrSaleScanner escaner, BufferedImage cuadro, Camera camara) {
        try {
            escaner.procesar(cuadro, contexto.requerirServiciosActivos().inventario(), contexto.getPerfilActivoId())
                    .ifPresent(lectura -> {
                        avisar("tbProductoEscaneado(" + BridgeResponse.json(resumen(lectura.producto())) + ")");
                        camara.avisar(lectura.producto().getNombre() + " agregado", true);
                        if (lectura.aviso() != null) {
                            avisar("tbAvisoEscaneo(" + BridgeResponse.literalJs(lectura.aviso()) + ")");
                            camara.avisar(lectura.aviso(), false);
                        }
                    });
        } catch (ValidationException e) {
            avisar("tbAvisoEscaneo(" + BridgeResponse.literalJs(e.getMessage()) + ")");
            camara.avisar(e.getMessage(), false);
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

    /**
     * Avisa a la vista solo si la sesión sigue siendo la actual: al cambiar de
     * cámara, la sesión anterior puede terminar después de que la nueva ya
     * arrancó, y su "cámara detenida" apagaría la vista de la nueva.
     */
    private void avisarSiEsActual(AtomicBoolean sesion, String script) {
        synchronized (this) {
            if (sesionActiva != sesion) {
                return;
            }
        }
        avisar(script);
    }

    private void avisar(String script) {
        vista.ejecutarScript(script);
    }
}
