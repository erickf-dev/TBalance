package sistemainventario.bridge;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Permiso;
import sistemainventario.model.Rol;
import sistemainventario.model.User;
import sistemainventario.util.DateUtil;
import java.time.LocalDateTime;
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
        return BridgeResponse.ejecutar(() -> {
            Sale venta = JsonUtil.fromJson(jsonVenta, Sale.class);
            // quién vendió lo pone Java con la sesión, nunca la página
            User vendedor = contexto.getSesion().requerir();
            if (venta != null) {
                venta.setUsuarioId(vendedor.getId());
                venta.setVendedor(vendedor.getNombre());
                venta.setFechaRegistro(DateUtil.ahora());
            }
            return BridgeResponse.json(ventas().registrar(venta));
        });
    }

    /** @return ventas anuladas del perfil activo */
    public String obtenerVentasAnuladas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.ventas().listarAnuladas()))
                .orElse(LISTA_VACIA);
    }

    /** @return entradas anuladas del perfil activo */
    public String obtenerEntradasAnuladas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.movimientos().listarEntradasAnuladas()))
                .orElse(LISTA_VACIA);
    }

    /** @return salidas anuladas del perfil activo */
    public String obtenerSalidasAnuladas() {
        return contexto.getServiciosActivos()
                .map(servicios -> BridgeResponse.json(servicios.movimientos().listarSalidasAnuladas()))
                .orElse(LISTA_VACIA);
    }

    /**
     * @param id     ID de la venta
     * @param motivo por qué se anula
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String anularVenta(String id, String motivo) {
        return BridgeResponse.ejecutarOk(() -> {
            User quien = contexto.getSesion().requerir();
            Rol rol = contexto.rolActual().orElseThrow(() -> new ValidationException("No tiene acceso a este negocio"));
            if (!rol.puede(Permiso.ANULAR) && !puedeAnularPropia(quien, ventas().obtener(id))) {
                throw new ValidationException("Solo puede anular sus propias ventas en los primeros "
                        + Rol.MINUTOS_ANULAR_PROPIA + " minutos. Pídaselo al administrador o al dueño.");
            }
            ventas().anular(id, motivo, quien.getNombre());
        });
    }

    /**
     * El cajero se puede equivocar al cobrar: anula su propia venta si no han
     * pasado {@value Rol#MINUTOS_ANULAR_PROPIA} minutos.
     */
    static boolean puedeAnularPropia(User quien, Sale venta) {
        if (venta.getUsuarioId() == null || !venta.getUsuarioId().equals(quien.getId())) {
            return false;
        }
        return DateUtil.parsearMomento(venta.getFechaRegistro())
                .map(registro -> !registro.plusMinutes(Rol.MINUTOS_ANULAR_PROPIA).isBefore(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * @param id     ID de la entrada
     * @param motivo por qué se anula
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String anularEntrada(String id, String motivo) {
        return BridgeResponse.ejecutarOk(() -> movimientos().anularEntrada(id, motivo, nombreSesion()));
    }

    /**
     * @param id     ID de la salida
     * @param motivo por qué se anula
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String anularSalida(String id, String motivo) {
        return BridgeResponse.ejecutarOk(() -> movimientos().anularSalida(id, motivo, nombreSesion()));
    }

    private String nombreSesion() {
        return contexto.getSesion().actual().map(User::getNombre).orElse(null);
    }

    private MovementService movimientos() {
        return contexto.requerirServiciosActivos().movimientos();
    }

    private SaleService ventas() {
        return contexto.requerirServiciosActivos().ventas();
    }
}
