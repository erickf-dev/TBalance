package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.service.CostService;
import sistemainventario.service.ProfileServices;
import sistemainventario.util.TextUtil;

import java.time.LocalDate;

/** Casos de uso de la sección Costos. */
public class CostController {

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public CostController(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * @param desde primer día de las compras ("yyyy-MM-dd"), vacío sin límite
     * @param hasta último día de las compras ("yyyy-MM-dd"), vacío sin límite
     * @return JSON con el costo del inventario por producto y las compras del periodo
     */
    public String obtenerCostos(String desde, String hasta) {
        return BridgeResponse.ejecutar(() -> {
            ProfileServices s = contexto.requerirServiciosActivos();
            CostService costos = new CostService(s.inventario(), s.movimientos(), s.proveedores());
            s.ventas().rutinas().ifPresent(costos::usarRutinas);
            return BridgeResponse.json(costos.resumen(fecha(desde), fecha(hasta)));
        });
    }

    private static LocalDate fecha(String texto) {
        return TextUtil.esVacio(texto) || "null".equals(texto) ? null : LocalDate.parse(texto);
    }
}
