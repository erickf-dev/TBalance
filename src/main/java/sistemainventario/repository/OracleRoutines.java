package sistemainventario.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import sistemainventario.model.AuditEntry;
import sistemainventario.model.CreditPayment;
import sistemainventario.model.Sale;
import sistemainventario.model.SaleItem;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Llama a los procedimientos y funciones de Oracle ({@code 04_funciones.sql}
 * y {@code 05_procedimientos.sql}) con {@link CallableStatement}.
 *
 * <p>Cada llamada va en una transacción de {@link Database#llamar}: si el
 * procedimiento lanza un error (por ejemplo, "Stock insuficiente"), se hace
 * ROLLBACK y el mensaje le llega al usuario tal cual.</p>
 */
public class OracleRoutines implements DatabaseRoutines {

    private final Database bd;
    private final String perfilId;

    /**
     * @param bd       conexión a Oracle
     * @param perfilId negocio al que pertenecen los datos
     */
    public OracleRoutines(Database bd, String perfilId) {
        this.bd = Objects.requireNonNull(bd);
        this.perfilId = Objects.requireNonNull(perfilId);
    }

    @Override
    public double registrarVenta(Sale venta) {
        BigDecimal total = bd.llamar(c -> {
            try (CallableStatement sp = c.prepareCall(
                    "{call SP_REGISTRAR_VENTA(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                Object[] valores = {perfilId, venta.getId(), texto(venta.getCliente()),
                        texto(venta.getClienteDocumento()), venta.getMetodoPago(), texto(venta.getObservacion()),
                        texto(venta.getReferenciaPago()), JdbcRepository.momento(venta.getFecha()),
                        texto(venta.getUsuarioId()), texto(venta.getVendedor()),
                        JdbcRepository.momento(venta.getFechaRegistro())};
                for (int i = 0; i < valores.length; i++) {
                    Database.asignarUno(sp, i + 1, valores[i]);
                }
                sp.setString(12, itemsJson(venta));
                sp.registerOutParameter(13, Types.NUMERIC);
                sp.execute();
                return sp.getBigDecimal(13);
            }
        });
        return total == null ? 0 : total.doubleValue();
    }

    @Override
    public void anularVenta(String ventaId, String motivo, AuditEntry h) {
        bd.llamar(c -> {
            try (CallableStatement sp = c.prepareCall("{call SP_ANULAR_VENTA(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                Object[] valores = {perfilId, ventaId, texto(motivo), JdbcRepository.momento(h.getFecha()),
                        texto(h.getId()), texto(h.getUsuarioId()), texto(h.getUsuario()), texto(h.getRol()),
                        texto(h.getPerfil())};
                for (int i = 0; i < valores.length; i++) {
                    Database.asignarUno(sp, i + 1, valores[i]);
                }
                sp.execute();
                return null;
            }
        });
    }

    @Override
    public String registrarAbono(CreditPayment a) {
        return bd.llamar(c -> {
            try (CallableStatement sp = c.prepareCall("{call SP_REGISTRAR_ABONO(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                Object[] valores = {perfilId, a.getId(), a.getClienteDocumento(), a.getMonto(), a.getMetodoPago(),
                        texto(a.getReferenciaPago()), texto(a.getNota()), JdbcRepository.momento(a.getFecha()),
                        texto(a.getUsuarioId()), texto(a.getRegistradoPor())};
                for (int i = 0; i < valores.length; i++) {
                    Database.asignarUno(sp, i + 1, valores[i]);
                }
                sp.registerOutParameter(11, Types.VARCHAR);
                sp.execute();
                return sp.getString(11);
            }
        });
    }

    @Override
    public double saldoCliente(String claveCliente) {
        return bd.llamarDecimal("FN_SALDO_CLIENTE", perfilId, claveCliente);
    }

    @Override
    public double valorInventario() {
        return bd.llamarDecimal("FN_VALOR_INVENTARIO", perfilId);
    }

    @Override
    public double utilidadPeriodo(LocalDate desde, LocalDate hasta) {
        return bd.llamarDecimal("FN_UTILIDAD_PERIODO", perfilId,
                Date.valueOf(desde), Date.valueOf(hasta));
    }

    /** @return los productos de la venta como texto JSON, para leerlos con JSON_TABLE */
    static String itemsJson(Sale venta) {
        JsonArray lista = new JsonArray();
        for (SaleItem item : venta.getItems()) {
            JsonObject fila = new JsonObject();
            fila.addProperty("productoId", item.getProductoId());
            fila.addProperty("nombreProducto", item.getNombreProducto());
            fila.addProperty("cantidad", item.getCantidad());
            fila.addProperty("precioUnitario", BigDecimal.valueOf(item.getPrecioUnitario()));
            lista.add(fila);
        }
        return lista.toString();
    }

    private static Object texto(String valor) {
        return valor == null ? new Database.Nulo(Types.VARCHAR) : valor;
    }
}
