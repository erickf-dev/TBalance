package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Sale;
import sistemainventario.service.MovementService;
import sistemainventario.service.SaleService;
import sistemainventario.util.JsonUtil;

/**
 * <b>GRASP Controller</b> de los casos de uso que mueven stock:
 * entradas, salidas y ventas.
 */
public class TransactionController {

    private static final String LISTA_VACIA = "[]";

    private final AppContext contexto;

    public TransactionController(AppContext contexto) {
        this.contexto = contexto;
    }

    public String obtenerEntradas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.movimientos().listarEntradas()))
                .orElse(LISTA_VACIA);
    }

    public String registrarEntrada(String jsonEntrada) {
        return BridgeResponse.ejecutarOk(() ->
                movimientos().registrarEntrada(JsonUtil.fromJson(jsonEntrada, StockEntry.class)));
    }

    public String obtenerSalidas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.movimientos().listarSalidas()))
                .orElse(LISTA_VACIA);
    }

    public String registrarSalida(String jsonSalida) {
        return BridgeResponse.ejecutarOk(() ->
                movimientos().registrarSalida(JsonUtil.fromJson(jsonSalida, StockExit.class)));
    }

    public String obtenerVentas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.ventas().listarTodas()))
                .orElse(LISTA_VACIA);
    }

    public String crearVenta(String jsonVenta) {
        return BridgeResponse.ejecutar(() ->
                BridgeResponse.json(ventas().registrar(JsonUtil.fromJson(jsonVenta, Sale.class))));
    }

    private MovementService movimientos() {
        return contexto.requerirServiciosActivos().movimientos();
    }

    private SaleService ventas() {
        return contexto.requerirServiciosActivos().ventas();
    }
}
