package sistemainventario.repository;

import sistemainventario.model.StockEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Entradas de inventario de un negocio en la tabla ENTRADA. */
public class StockEntryJdbcRepository extends JdbcRepository<StockEntry> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public StockEntryJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "ENTRADA";
    }

    @Override
    protected String funcionListar() {
        return "FN_LISTAR_ENTRADAS";
    }

    @Override
    protected List<String> columnas() {
        return List.of("PRODUCTO_ID", "CANTIDAD", "FECHA", "PROVEEDOR_ID", "PRECIO_UNITARIO", "OBSERVACION", "ESTADO", "FECHA_REGISTRO", "FECHA_ANULACION", "MOTIVO_ANULACION", "ANULADA_POR");
    }

    @Override
    protected Object[] valores(StockEntry e) {
        return new Object[]{e.getProductoId(), e.getCantidad(), momento(e.getFecha()), texto(e.getProveedorId()),
                e.getPrecioUnitario(), texto(e.getObservacion()), texto(e.getEstado()), momento(e.getFechaRegistro()),
                momento(e.getFechaAnulacion()), texto(e.getMotivoAnulacion()), texto(e.getAnuladaPor())};
    }

    @Override
    protected StockEntry leer(ResultSet fila) throws SQLException {
        StockEntry e = new StockEntry();
        e.setId(fila.getString("ID"));
        e.setProductoId(fila.getString("PRODUCTO_ID"));
        e.setCantidad(fila.getInt("CANTIDAD"));
        e.setFecha(leerMomento(fila, "FECHA"));
        e.setProveedorId(fila.getString("PROVEEDOR_ID"));
        e.setPrecioUnitario(leerNumero(fila, "PRECIO_UNITARIO"));
        e.setObservacion(fila.getString("OBSERVACION"));
        e.setEstado(fila.getString("ESTADO"));
        e.setFechaRegistro(leerMomento(fila, "FECHA_REGISTRO"));
        e.setAnulacion(leerMomento(fila, "FECHA_ANULACION"), fila.getString("MOTIVO_ANULACION"));
        e.setAnuladaPor(fila.getString("ANULADA_POR"));
        return e;
    }
}
