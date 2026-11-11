package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.dispositivo.WebcamCamera;

/**
 * Objeto expuesto a JavaScript como {@code window.javaBridge}.
 *
 * <p><b>GRASP Controller (fachada):</b> es el único punto de entrada de los
 * eventos de la vista web. En P2 esta clase tenía 338 líneas y mezclaba
 * conversión JSON, reglas de negocio, generación de IDs y manejo de
 * errores. Ahora solo delega en controladores por caso de uso
 * ({@link ProfileController}, {@link InventoryController},
 * {@link CatalogController}, {@link TransactionController},
 * {@link BackupController}, {@link LabelController},
 * {@link ScanController}), cada uno con
 * <b>alta cohesión</b>.</p>
 *
 * <p>Los nombres y firmas de los métodos públicos se mantienen idénticos a
 * P2 porque el HTML los invoca por nombre.</p>
 */
public class JavaBridge {

    private final ProfileController perfiles;
    private final InventoryController inventario;
    private final CatalogController catalogo;
    private final TransactionController transacciones;
    private final BackupController respaldos;
    private final LabelController etiquetas;
    private final ScanController escaneo;
    private final ViewNotifier notificador;

    public JavaBridge(AppContext contexto, ViewNotifier notificador) {
        this.notificador = notificador;
        this.perfiles = new ProfileController(contexto, notificador);
        this.inventario = new InventoryController(contexto);
        this.catalogo = new CatalogController(contexto);
        this.transacciones = new TransactionController(contexto);
        this.respaldos = new BackupController(contexto, notificador);
        this.etiquetas = new LabelController(contexto, new DesktopFileOpener());
        this.escaneo = new ScanController(contexto, WebcamCamera::new, notificador);
    }

    // ── Perfiles ────────────────────────────────────────────────────────

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

    public String eliminarPerfil(String id) {
        return perfiles.eliminarPerfil(id);
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

    public String eliminarProducto(String id) {
        return inventario.eliminarProducto(id);
    }

    // ── Categorías y proveedores ────────────────────────────────────────

    public String obtenerCategorias() {
        return catalogo.obtenerCategorias();
    }

    public String guardarCategoria(String jsonCategoria) {
        return catalogo.guardarCategoria(jsonCategoria);
    }

    public String eliminarCategoria(String id) {
        return catalogo.eliminarCategoria(id);
    }

    public String obtenerProveedores() {
        return catalogo.obtenerProveedores();
    }

    public String guardarProveedor(String jsonProveedor) {
        return catalogo.guardarProveedor(jsonProveedor);
    }

    public String eliminarProveedor(String id) {
        return catalogo.eliminarProveedor(id);
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
