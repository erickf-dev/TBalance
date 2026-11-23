package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.dispositivo.Camera;
import sistemainventario.dispositivo.CameraProvider;
import sistemainventario.dispositivo.PhoneCamera;
import sistemainventario.dispositivo.PhoneLink;
import sistemainventario.dispositivo.WebcamCamera;

import java.util.List;

import java.util.concurrent.Executor;

/**
 * Objeto expuesto a JavaScript como {@code window.javaBridge}.
 *
 * <p><b>GRASP Controller (fachada):</b> es el único punto de entrada de los
 * eventos de la vista web. En P2 esta clase tenía 338 líneas y mezclaba
 * conversión JSON, reglas de negocio, generación de IDs y manejo de
 * errores. Ahora solo delega en controladores por caso de uso
 * ({@link ProfileController}, {@link InventoryController},
 * {@link CatalogController}, {@link TransactionController},
 * {@link BackupController}, {@link StorageController}, {@link LabelController},
 * {@link ScanController}), cada uno con
 * <b>alta cohesión</b>.</p>
 *
 * <p>Los nombres y firmas de los métodos públicos se mantienen idénticos a
 * P2 porque el HTML los invoca por nombre.</p>
 */
public class JavaBridge {

    /**
     * Candado de los datos: el servidor de la ventana de Edge atiende cada llamada
     * de la vista con este candado, y el asistente lo usa para leer o cambiar el
     * inventario desde su hilo sin cruzarse con la interfaz.
     */
    public static final Object CANDADO_DATOS = new Object();

    private final ProfileController perfiles;
    private final InventoryController inventario;
    private final CatalogController catalogo;
    private final TransactionController transacciones;
    private final BackupController respaldos;
    private final StorageController almacenamiento;
    private final LabelController etiquetas;
    private final ReportController informes;
    private final AssistantController asistente;
    private final ScanController escaneo;
    private final CostController costos;
    private final CreditController fiados;
    private final SessionController sesion;
    private final ViewNotifier notificador;
    /** Transiciones grandes de la ventana; es {@code null} si la vista no las ofrece. */
    private final ScreenTransition transicion;

    public JavaBridge(AppContext contexto, ViewNotifier notificador) {
        this(contexto, notificador, null, Runnable::run);
    }

    /**
     * @param contexto    servicios de la aplicación
     * @param notificador avisos de Java hacia la vista
     * @param transicion  transiciones visuales de la ventana (puede ser {@code null})
     * @param hiloDeDatos ejecuta el acceso a los datos desde otros hilos (el del asistente)
     *                    en el mismo hilo o con el mismo candado que la vista
     */
    public JavaBridge(AppContext contexto, ViewNotifier notificador, ScreenTransition transicion, Executor hiloDeDatos) {
        this.notificador = notificador;
        this.transicion = transicion;
        this.perfiles = new ProfileController(contexto, notificador);
        this.inventario = new InventoryController(contexto);
        this.catalogo = new CatalogController(contexto);
        this.transacciones = new TransactionController(contexto);
        this.respaldos = new BackupController(contexto, notificador);
        this.almacenamiento = new StorageController(contexto, notificador);
        this.costos = new CostController(contexto);
        this.fiados = new CreditController(contexto);
        this.sesion = new SessionController(contexto);
        this.etiquetas = new LabelController(contexto, new DesktopFileOpener());
        this.informes = new ReportController(contexto, new DesktopFileOpener());
        this.asistente = new AssistantController(contexto, notificador, hiloDeDatos);
        PhoneLink celular = new PhoneLink(contexto.getCarpetaCelular());
        celular.prepararCertificado();
        this.escaneo = new ScanController(contexto, new CameraProvider() {
            @Override
            public List<String> listar() {
                return WebcamCamera.disponibles();
            }

            @Override
            public Camera crear(String nombre) {
                return PhoneCamera.NOMBRE.equals(nombre) ? new PhoneCamera(celular) : new WebcamCamera(nombre);
            }
        }, notificador, celular);
    }

    // ── Transiciones de la ventana ──────────────────────────────────────

    /**
     * Pide a la ventana que desvanezca una imagen de la zona indicada por encima
     * de la página (ver {@link ScreenTransition}).
     *
     * @return {@code "OK"} si la transición se hizo; {@code "NO"} si la vista no
     *         la ofrece o falló, y entonces la página cambia sin animación
     */
    public String transicionPantalla(double x, double y, double ancho, double alto, int milisegundos) {
        if (transicion == null) {
            return "NO";
        }
        try {
            transicion.desvanecerZona(x, y, ancho, alto, milisegundos);
            return "OK";
        } catch (RuntimeException e) {
            System.err.println("[JavaBridge] No se pudo hacer la transición: " + e.getMessage());
            return "NO";
        }
    }

    // ── Informes ────────────────────────────────────────────────────────

    public String obtenerInforme(String desde, String hasta, String anteriorDesde, String anteriorHasta) {
        return informes.obtenerInforme(desde, hasta, anteriorDesde, anteriorHasta);
    }

    public String exportarInformeExcel(String desde, String hasta, String anteriorDesde, String anteriorHasta) {
        return informes.exportarInformeExcel(desde, hasta, anteriorDesde, anteriorHasta);
    }

    // ── Asistente ───────────────────────────────────────────────────────

    public String asistenteEstado() {
        return asistente.asistenteEstado();
    }

    public String asistenteConfigurar(String clave, String modelo) {
        return asistente.asistenteConfigurar(clave, modelo);
    }

    public String asistenteEnviar(String mensaje) {
        return asistente.asistenteEnviar(mensaje);
    }

    public String asistenteProbar() {
        return asistente.asistenteProbar();
    }

    /**
     * @param clave  clave escrita en la pantalla (no se guarda)
     * @param modelo modelo elegido
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String asistenteProbarClave(String clave, String modelo) {
        return asistente.asistenteProbarClave(clave, modelo);
    }

    public String asistenteConfirmar(String propuestaId) {
        return asistente.asistenteConfirmar(propuestaId);
    }

    public String asistenteDescartar(String propuestaId) {
        return asistente.asistenteDescartar(propuestaId);
    }

    public String asistenteReiniciar() {
        return asistente.asistenteReiniciar();
    }

    // ── Perfiles ────────────────────────────────────────────────────────

    // ---- Sesión y usuarios -------------------------------------------------

    /** @return JSON con {@code primeraVez} y la sesión abierta (o {@code null}) */
    public String estadoSesion() {
        return sesion.estadoSesion();
    }

    /**
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return estado de la sesión, o {@code "ERROR: ..."}
     */
    public String iniciarSesion(String usuario, String clave) {
        return sesion.iniciarSesion(usuario, clave);
    }

    /** @param filtrosJson filtros del historial @return historial de acciones visible para quien consulta */
    public String listarHistorial(String filtrosJson) {
        return sesion.listarHistorial(filtrosJson);
    }

    /**
     * @param nombre  nombre del dueño
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return estado de la sesión, o {@code "ERROR: ..."}
     */
    public String crearPrimerDueno(String nombre, String usuario, String clave) {
        return sesion.crearPrimerDueno(nombre, usuario, clave);
    }

    /** @return {@code "OK"} */
    public String cerrarSesion() {
        return sesion.cerrarSesion();
    }

    /**
     * @param actual contraseña actual
     * @param nueva  contraseña nueva
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarMiClave(String actual, String nueva) {
        return sesion.cambiarMiClave(actual, nueva);
    }

    /** @return personas del negocio abierto */
    public String listarUsuarios() {
        return sesion.listarUsuarios();
    }

    /**
     * @param json {@code {id?, nombre, usuario, clave?, rol}}
     * @return la persona, o {@code "ERROR: ..."}
     */
    public String guardarUsuario(String json) {
        return sesion.guardarUsuario(json);
    }

    /**
     * @param id     persona
     * @param activo {@code "false"} para deshabilitar
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoUsuario(String id, String activo) {
        return sesion.cambiarEstadoUsuario(id, activo);
    }

    /**
     * @param id    persona
     * @param nueva contraseña nueva
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String restablecerClaveUsuario(String id, String nueva) {
        return sesion.restablecerClaveUsuario(id, nueva);
    }

    public String listarPerfiles() {
        return perfiles.listarPerfiles();
    }

    public String obtenerPerfilActivo() {
        return perfiles.obtenerPerfilActivo();
    }

    public String obtenerSugerencias() {
        return perfiles.obtenerSugerencias();
    }

    public boolean requiereControlVencimiento() {
        return perfiles.requiereControlVencimiento();
    }

    public String obtenerTiposNegocio() {
        return perfiles.obtenerTiposNegocio();
    }

    public String crearPerfil(String jsonPerfil) {
        return perfiles.crearPerfil(jsonPerfil);
    }

    public String editarPerfil(String jsonPerfil) {
        return perfiles.editarPerfil(jsonPerfil);
    }

    /**
     * @param id negocio a deshabilitar
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String deshabilitarPerfil(String id) {
        return perfiles.deshabilitarPerfil(id);
    }

    /**
     * @param id negocio a habilitar
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String habilitarPerfil(String id) {
        return perfiles.habilitarPerfil(id);
    }

    public String seleccionarPerfil(String id) {
        return perfiles.seleccionarPerfil(id);
    }

    // ── Dashboard ───────────────────────────────────────────────────────

    public String obtenerEstadisticas() {
        return inventario.obtenerEstadisticas(perfiles.requiereControlVencimiento());
    }

    public String obtenerProductosVencidos() {
        return inventario.obtenerProductosVencidos();
    }

    public String obtenerProductosPorVencer(int dias) {
        return inventario.obtenerProductosPorVencer(dias);
    }

    // ── Productos ───────────────────────────────────────────────────────

    public String obtenerProductos() {
        return inventario.obtenerProductos();
    }

    public String buscarProductos(String texto) {
        return inventario.buscarProductos(texto);
    }

    public String filtrarProductosPorCategoria(String categoria) {
        return inventario.filtrarProductosPorCategoria(categoria);
    }

    public String guardarProducto(String jsonProducto) {
        return inventario.guardarProducto(jsonProducto);
    }

    /** @return productos deshabilitados */
    public String obtenerProductosDeshabilitados() {
        return inventario.obtenerProductosDeshabilitados();
    }

    /**
     * @param id producto
     * @param activo "true" o "false"
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoProducto(String id, String activo) {
        return inventario.cambiarEstadoProducto(id, activo);
    }

    // ── Categorías y proveedores ────────────────────────────────────────

    public String obtenerCategorias() {
        return catalogo.obtenerCategorias();
    }

    public String guardarCategoria(String jsonCategoria) {
        return catalogo.guardarCategoria(jsonCategoria);
    }

    /** @return categorías deshabilitadas */
    public String obtenerCategoriasDeshabilitadas() {
        return catalogo.obtenerCategoriasDeshabilitadas();
    }

    /**
     * @param id categoría
     * @param activa "true" o "false"
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoCategoria(String id, String activa) {
        return catalogo.cambiarEstadoCategoria(id, activa);
    }

    public String obtenerProveedores() {
        return catalogo.obtenerProveedores();
    }

    public String guardarProveedor(String jsonProveedor) {
        return catalogo.guardarProveedor(jsonProveedor);
    }

    /** @return proveedores deshabilitados */
    public String obtenerProveedoresDeshabilitados() {
        return catalogo.obtenerProveedoresDeshabilitados();
    }

    /**
     * @param id proveedor
     * @param activo "true" o "false"
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoProveedor(String id, String activo) {
        return catalogo.cambiarEstadoProveedor(id, activo);
    }

    // ── Entradas, salidas y ventas ──────────────────────────────────────

    public String obtenerEntradas() {
        return transacciones.obtenerEntradas();
    }

    public String registrarEntrada(String jsonEntrada) {
        return transacciones.registrarEntrada(jsonEntrada);
    }

    public String obtenerSalidas() {
        return transacciones.obtenerSalidas();
    }

    public String registrarSalida(String jsonSalida) {
        return transacciones.registrarSalida(jsonSalida);
    }

    public String obtenerVentas() {
        return transacciones.obtenerVentas();
    }

    public String crearVenta(String jsonVenta) {
        return transacciones.crearVenta(jsonVenta);
    }

    /** @return ventas anuladas */
    public String obtenerVentasAnuladas() {
        return transacciones.obtenerVentasAnuladas();
    }

    /** @return entradas anuladas */
    public String obtenerEntradasAnuladas() {
        return transacciones.obtenerEntradasAnuladas();
    }

    /** @return salidas anuladas */
    public String obtenerSalidasAnuladas() {
        return transacciones.obtenerSalidasAnuladas();
    }

    /**
     * @param id venta
     * @param motivo motivo
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String anularVenta(String id, String motivo) {
        return transacciones.anularVenta(id, motivo);
    }

    /**
     * @param id entrada
     * @param motivo motivo
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String anularEntrada(String id, String motivo) {
        return transacciones.anularEntrada(id, motivo);
    }

    /**
     * @param id salida
     * @param motivo motivo
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String anularSalida(String id, String motivo) {
        return transacciones.anularSalida(id, motivo);
    }

    // ── Copias de seguridad ─────────────────────────────────────────────

    public String listarRespaldos() {
        return respaldos.listarRespaldos();
    }

    public String crearRespaldo() {
        return respaldos.crearRespaldo();
    }

    public String restaurarRespaldo(String nombre) {
        return respaldos.restaurarRespaldo(nombre);
    }

    public String eliminarRespaldo(String nombre) {
        return respaldos.eliminarRespaldo(nombre);
    }

    public String obtenerCarpetaRespaldos() {
        return respaldos.obtenerCarpetaRespaldos();
    }

    // ---- Costos ---------------------------------------------------------------

    /**
     * @param desde primer día de las compras ("yyyy-MM-dd"), vacío sin límite
     * @param hasta último día de las compras ("yyyy-MM-dd"), vacío sin límite
     * @return costos del inventario y compras del periodo
     */
    public String obtenerCostos(String desde, String hasta) {
        return costos.obtenerCostos(desde, hasta);
    }

    // ---- Varios equipos ------------------------------------------------------

    /**
     * @param datosJson motor, URL, usuario y clave de la base que ya usa el negocio
     * @return mensaje o "ERROR: ..." (solo funciona antes de que existan usuarios en este computador)
     */
    public String conectarBaseExistente(String datosJson) {
        return almacenamiento.conectarBaseExistente(datosJson);
    }

    /** @return número que cambia cuando otro equipo guardó algo en la misma base */
    public String revisarCambios() {
        return almacenamiento.revisarCambios();
    }

    // ---- QR de cobro ---------------------------------------------------------

    /**
     * @param perfilId negocio
     * @param dataUrl  imagen del QR de Bre-B (vacío para quitarla)
     * @return OK o el error
     */
    public String guardarQrCobro(String perfilId, String dataUrl) {
        return perfiles.guardarQrCobro(perfilId, dataUrl);
    }

    /**
     * @param perfilId negocio (vacío para el abierto)
     * @return la imagen del QR de cobro o vacío
     */
    public String obtenerQrCobro(String perfilId) {
        return perfiles.obtenerQrCobro(perfilId);
    }

    // ---- Fiados -------------------------------------------------------------

    /** @return quién debe y cuánto, sin el detalle (para la pantalla Hoy) */
    public String resumenFiados() {
        return fiados.resumenFiados();
    }

    /** @return quién debe, cuánto y lo abonado */
    public String listarFiados() {
        return fiados.listarFiados();
    }

    /**
     * @param clave cédula del cliente
     * @return su cuenta con las ventas fiadas y los abonos
     */
    public String obtenerFiado(String clave) {
        return fiados.obtenerFiado(clave);
    }

    /**
     * @param jsonAbono datos del abono
     * @return el abono registrado
     */
    public String registrarAbono(String jsonAbono) {
        return fiados.registrarAbono(jsonAbono);
    }

    /**
     * @param id     ID del abono
     * @param motivo por qué se anula
     * @return OK o el error
     */
    public String anularAbono(String id, String motivo) {
        return fiados.anularAbono(id, motivo);
    }

    // ---- Base de datos ------------------------------------------------------

    /** @return configuración del almacenamiento (sin la clave) */
    public String obtenerAlmacenamiento() {
        return almacenamiento.obtenerAlmacenamiento();
    }

    /**
     * @param datosJson motor, URL, usuario y clave
     * @return resultado de la prueba de conexión
     */
    public String probarAlmacenamiento(String datosJson) {
        return almacenamiento.probarAlmacenamiento(datosJson);
    }

    /**
     * @param datosJson motor, URL, usuario y clave
     * @return mensaje del cambio de almacenamiento
     */
    public String guardarAlmacenamiento(String datosJson) {
        return almacenamiento.guardarAlmacenamiento(datosJson);
    }

    // ── Etiquetas con código QR ─────────────────────────────────────────

    public String obtenerEtiquetaQR(String productoId) {
        return etiquetas.obtenerEtiquetaQR(productoId);
    }

    public String guardarEtiquetaQR(String productoId) {
        return etiquetas.guardarEtiquetaQR(productoId);
    }

    public String generarHojaEtiquetas() {
        return etiquetas.generarHojaEtiquetas();
    }

    // ── Venta por QR ────────────────────────────────────────────────────

    public String iniciarEscaneoQR() {
        return escaneo.iniciarEscaneoQR();
    }

    /**
     * @param nombre cámara elegida (vacío: la predeterminada)
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String iniciarEscaneoQRCon(String nombre) {
        return escaneo.iniciarEscaneoQRCon(nombre);
    }

    /** @return {@code "OK"}; la lista llega con {@code tbCamarasDisponibles(...)} */
    public String listarCamaras() {
        return escaneo.listarCamaras();
    }

    /** @return JSON con la dirección y el QR para conectar el celular, o {@code "ERROR: ..."} */
    public String prepararCelular() {
        return escaneo.prepararCelular();
    }

    /** @return {@code true} si el celular está enviando imágenes */
    public boolean celularConectado() {
        return escaneo.celularConectado();
    }

    public String detenerEscaneoQR() {
        return escaneo.detenerEscaneoQR();
    }

    // ── Utilidades de la vista ──────────────────────────────────────────

    public void recargarEstado() {
        notificador.ejecutarScript("cargarEstadoDesdeJava()");
    }

    public void mostrarToast(String mensaje, String tipo) {
        notificador.ejecutarScript("showToast(" + BridgeResponse.literalJs(mensaje)
                + ", " + BridgeResponse.literalJs(tipo) + ")");
    }
}
