package sistemainventario.repository;

import sistemainventario.model.Sale;
import sistemainventario.model.SaleItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventas de un negocio: la venta en VENTA y sus productos en DETALLE_VENTA.
 * La venta y su detalle se guardan en la misma transacción.
 */
public class SaleJdbcRepository extends JdbcRepository<Sale> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public SaleJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "VENTA";
    }

    @Override
    protected String funcionListar() {
        return "FN_LISTAR_VENTAS";
    }

    @Override
    protected List<String> columnas() {
        return List.of("CLIENTE", "METODO_PAGO", "OBSERVACION", "REFERENCIA_PAGO", "FECHA", "ESTADO", "TOTAL",
                "FECHA_ANULACION", "MOTIVO_ANULACION", "ANULADA_POR", "USUARIO_ID", "VENDEDOR", "FECHA_REGISTRO", "CLIENTE_DOCUMENTO");
    }

    @Override
    protected Object[] valores(Sale v) {
        v.recalcularTotal();
        return new Object[]{texto(v.getCliente()), texto(v.getMetodoPago()), texto(v.getObservacion()),
                texto(v.getReferenciaPago()), momento(v.getFecha()), texto(v.getEstado()), v.getTotal(),
                momento(v.getFechaAnulacion()), texto(v.getMotivoAnulacion()), texto(v.getAnuladaPor()),
                texto(v.getUsuarioId()), texto(v.getVendedor()), momento(v.getFechaRegistro()),
                texto(v.getClienteDocumento())};
    }

    @Override
    protected Sale leer(ResultSet fila) throws SQLException {
        Sale v = new Sale();
        v.setId(fila.getString("ID"));
        v.setCliente(fila.getString("CLIENTE"));
        v.setMetodoPago(fila.getString("METODO_PAGO"));
        v.setObservacion(fila.getString("OBSERVACION"));
        v.setReferenciaPago(fila.getString("REFERENCIA_PAGO"));
        v.setFecha(leerMomento(fila, "FECHA"));
        v.setEstado(fila.getString("ESTADO"));
        v.setAnulacion(leerMomento(fila, "FECHA_ANULACION"), fila.getString("MOTIVO_ANULACION"));
        v.setAnuladaPor(fila.getString("ANULADA_POR"));
        v.setUsuarioId(fila.getString("USUARIO_ID"));
        v.setVendedor(fila.getString("VENDEDOR"));
        v.setFechaRegistro(leerMomento(fila, "FECHA_REGISTRO"));
        v.setClienteDocumento(fila.getString("CLIENTE_DOCUMENTO"));
        return v;
    }

    /** Lee todo el detalle del negocio en una sola consulta y lo reparte entre las ventas. */
    @Override
    protected void completar(List<Sale> ventas) {
        Map<String, List<SaleItem>> porVenta = new LinkedHashMap<>();
        bd.llamar("FN_LISTAR_DETALLE_VENTAS",
                fila -> {
                    SaleItem item = new SaleItem();
                    item.setProductoId(fila.getString("PRODUCTO_ID"));
                    item.setNombreProducto(fila.getString("NOMBRE_PRODUCTO"));
                    item.setCantidad(fila.getInt("CANTIDAD"));
                    item.setPrecioUnitario(leerNumero(fila, "PRECIO_UNITARIO"));
                    item.setCostoUnitario(leerNumero(fila, "COSTO_UNITARIO"));
                    porVenta.computeIfAbsent(fila.getString("VENTA_ID"), k -> new ArrayList<>()).add(item);
                    return item;
                }, perfilId);
        for (Sale venta : ventas) {
            venta.setItems(porVenta.getOrDefault(venta.getId(), List.of()));
            venta.recalcularTotal();
        }
    }

    /** Reescribe el detalle de la venta (al editar se borra el anterior). */
    @Override
    protected void guardarHijos(Connection conexion, Sale venta, boolean nueva) throws SQLException {
        if (!nueva) {
            Database.procedimiento(conexion, "SP_BORRAR_DETALLE_VENTA", perfilId, venta.getId());
        }
        int linea = 1;
        for (SaleItem item : venta.getItems()) {
            Database.procedimiento(conexion, "SP_AGREGAR_DETALLE_VENTA",
                    perfilId, venta.getId(), linea++, item.getProductoId(), texto(item.getNombreProducto()),
                    item.getCantidad(), item.getPrecioUnitario(), item.getSubtotal(), item.getCostoUnitario());
        }
    }
}
