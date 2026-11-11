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

    public String eliminarCategoria(String id) {
        return BridgeResponse.ejecutar(() ->
                categorias().eliminar(id) ? BridgeResponse.OK : BridgeResponse.error("Categoria no encontrada"));
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

    public String eliminarProveedor(String id) {
        return BridgeResponse.ejecutar(() ->
                proveedores().eliminar(id) ? BridgeResponse.OK : BridgeResponse.error("Proveedor no encontrado"));
    }

    private CategoryService categorias() {
        return contexto.requerirServiciosActivos().categorias();
    }

    private SupplierService proveedores() {
        return contexto.requerirServiciosActivos().proveedores();
    }
}
