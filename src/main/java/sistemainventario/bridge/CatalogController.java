package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Category;
import sistemainventario.model.Supplier;
import sistemainventario.service.CategoryService;
import sistemainventario.service.SupplierService;
import sistemainventario.util.JsonUtil;

/**
 * <b>GRASP Controller</b> de los casos de uso de categorías y proveedores.
 */
public class CatalogController {

    private static final String LISTA_VACIA = "[]";

    private final AppContext contexto;

    public CatalogController(AppContext contexto) {
        this.contexto = contexto;
    }

    // ── Categorías ──────────────────────────────────────────────────────

    public String obtenerCategorias() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.categorias().listarTodos()))
                .orElse(LISTA_VACIA);
    }

    public String guardarCategoria(String jsonCategoria) {
        return BridgeResponse.ejecutarOk(() ->
                categorias().guardar(JsonUtil.fromJson(jsonCategoria, Category.class)));
    }

    /** @return categorías deshabilitadas del perfil activo */
    public String obtenerCategoriasDeshabilitadas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.categorias().listarDeshabilitados()))
                .orElse(LISTA_VACIA);
    }

    /**
     * @param id     ID de la categoría
     * @param activa {@code "true"} para habilitar, {@code "false"} para deshabilitar
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoCategoria(String id, String activa) {
        return BridgeResponse.ejecutarOk(() -> categorias().cambiarEstado(id, Boolean.parseBoolean(activa)));
    }

    // ── Proveedores ─────────────────────────────────────────────────────

    public String obtenerProveedores() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.proveedores().listarTodos()))
                .orElse(LISTA_VACIA);
    }

    public String guardarProveedor(String jsonProveedor) {
        return BridgeResponse.ejecutarOk(() ->
                proveedores().guardar(JsonUtil.fromJson(jsonProveedor, Supplier.class)));
    }

    /** @return proveedores deshabilitados del perfil activo */
    public String obtenerProveedoresDeshabilitados() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.proveedores().listarDeshabilitados()))
                .orElse(LISTA_VACIA);
    }

    /**
     * @param id     ID del proveedor
     * @param activo {@code "true"} para habilitar, {@code "false"} para deshabilitar
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoProveedor(String id, String activo) {
        return BridgeResponse.ejecutarOk(() -> proveedores().cambiarEstado(id, Boolean.parseBoolean(activo)));
    }

    private CategoryService categorias() {
        return contexto.requerirServiciosActivos().categorias();
    }

    private SupplierService proveedores() {
        return contexto.requerirServiciosActivos().proveedores();
    }
}
