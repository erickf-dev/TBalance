package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.CreditPayment;
import sistemainventario.model.User;
import sistemainventario.service.CreditService;
import sistemainventario.util.JsonUtil;

/**
 * Sección Fiados: quién le debe al negocio y los abonos con que va pagando.
 * Quién registra o anula un abono lo pone Java con la sesión, nunca la página.
 */
public class CreditController {

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public CreditController(AppContext contexto) {
        this.contexto = contexto;
    }

    /** @return cuentas de los clientes con fiados (lo que deben y lo abonado) */
    public String listarFiados() {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(fiados().listarCuentas()));
    }

    /** @return solo lo que la pantalla Hoy necesita: cliente, lo que debe y la fecha de la primera compra fiada */
    public String resumenFiados() {
        return BridgeResponse.ejecutar(() -> {
            java.util.List<java.util.Map<String, Object>> filas = new java.util.ArrayList<>();
            for (sistemainventario.model.CustomerDebt c : fiados().listarCuentas()) {
                if (c.estaAlDia()) {
                    continue;
                }
                java.util.Map<String, Object> f = new java.util.LinkedHashMap<>();
                f.put("clave", c.getClave());
                f.put("cliente", c.getCliente());
                f.put("saldo", c.getSaldo());
                f.put("desde", c.getVentas().stream().map(v -> v.getFecha()).filter(java.util.Objects::nonNull)
                        .min(String::compareTo).orElse(c.getUltimaFecha()));
                filas.add(f);
            }
            return BridgeResponse.json(filas);
        });
    }

    /**
     * @param clave cédula del cliente
     * @return la cuenta del cliente con sus ventas fiadas y sus abonos
     */
    public String obtenerFiado(String clave) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(fiados().obtenerCuenta(clave)));
    }

    /**
     * @param jsonAbono cliente (cédula), monto, metodoPago, referenciaPago y nota
     * @return el abono registrado
     */
    public String registrarAbono(String jsonAbono) {
        return BridgeResponse.ejecutar(() -> {
            CreditPayment abono = JsonUtil.fromJson(jsonAbono, CreditPayment.class);
            User quien = contexto.getSesion().requerir();
            if (abono != null) {
                abono.setUsuarioId(quien.getId());
                abono.setRegistradoPor(quien.getNombre());
            }
            return BridgeResponse.json(fiados().registrarAbono(abono));
        });
    }

    /**
     * @param id     ID del abono
     * @param motivo por qué se anula
     * @return OK o el error
     */
    public String anularAbono(String id, String motivo) {
        return BridgeResponse.ejecutarOk(() ->
                fiados().anularAbono(id, motivo, contexto.getSesion().requerir().getNombre()));
    }

    private CreditService fiados() {
        return contexto.requerirServiciosActivos().fiados();
    }
}
