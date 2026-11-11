package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Product;
import sistemainventario.service.InventoryService;
import sistemainventario.service.ProfileServices;
import sistemainventario.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <b>GRASP Controller</b> de los casos de uso de productos y del dashboard.
 */
public class InventoryController {

    private static final String LISTA_VACIA = "[]";

    private final AppContext contexto;

    public InventoryController(AppContext contexto) {
        this.contexto = contexto;
    }

    public String obtenerProductos() {
        return consultar(InventoryService::listarTodos);
    }

    public String buscarProductos(String texto) {
        return consultar(inventario -> inventario.buscarPorTexto(texto));
    }

    public String filtrarProductosPorCategoria(String categoria) {
        return consultar(inventario -> inventario.filtrarPorCategoria(categoria));
    }

    public String obtenerProductosVencidos() {
        return consultar(InventoryService::getVencidos);
    }

    public String obtenerProductosPorVencer(int dias) {
        return consultar(inventario -> inventario.getPorVencer(dias));
    }

    public String guardarProducto(String jsonProducto) {
        return BridgeResponse.ejecutarOk(() ->
                inventario().guardar(JsonUtil.fromJson(jsonProducto, Product.class)));
    }

    public String eliminarProducto(String id) {
        return BridgeResponse.ejecutar(() ->
                inventario().eliminar(id) ? BridgeResponse.OK : BridgeResponse.error("Producto no encontrado"));
    }

    /**
     * @param incluirVencimientos si el perfil activo controla vencimientos
     * @return indicadores clave del perfil activo en JSON
     */
    public String obtenerEstadisticas(boolean incluirVencimientos) {
        return BridgeResponse.ejecutar(() -> {
            ProfileServices servicios = contexto.requerirServiciosActivos();
            InventoryService inventario = servicios.inventario();
            Map<String, Object> estadisticas = new LinkedHashMap<>();
            estadisticas.put("totalProductos", inventario.listarTodos().size());
            estadisticas.put("bajoStock", inventario.getBajoStock().size());
            estadisticas.put("agotados", inventario.getAgotados().size());
            estadisticas.put("totalCategorias", servicios.categorias().listarTodos().size());
            estadisticas.put("totalProveedores", servicios.proveedores().listarTodos().size());
            estadisticas.put("totalVentas", servicios.ventas().getTotalVentas());
            estadisticas.put("totalTransacciones", servicios.ventas().getTotalTransacciones());
            if (incluirVencimientos) {
                estadisticas.put("vencidos", inventario.getVencidos().size());
                estadisticas.put("porVencer7", inventario.getPorVencer(7).size());
                estadisticas.put("porVencer30", inventario.getPorVencer(30).size());
            }
            return BridgeResponse.json(estadisticas);
        });
    }

    private String consultar(Function<InventoryService, List<Product>> consulta) {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(consulta.apply(servicios.inventario())))
                .orElse(LISTA_VACIA);
    }

    private InventoryService inventario() {
        return contexto.requerirServiciosActivos().inventario();
    }
}
