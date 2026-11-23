package sistemainventario.repository;

import sistemainventario.model.StockExit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Salidas de inventario de un negocio en la tabla SALIDA. */
public class StockExitJdbcRepository extends JdbcRepository<StockExit> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public StockExitJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "SALIDA";
    }

    @Override
    protected List<String> columnas() {
        return List.of("PRODUCTO_ID", "CANTIDAD", "FECHA", "MOTIVO", "TIPO", "ESTADO", "FECHA_REGISTRO", "FECHA_ANULACION", "MOTIVO_ANULACION", "ANULADA_POR");
    }

    @Override
    protected Object[] valores(StockExit s) {
        return new Object[]{s.getProductoId(), s.getCantidad(), momento(s.getFecha()), texto(s.getMotivo()),
                texto(s.getTipo()), texto(s.getEstado()), momento(s.getFechaRegistro()),
                momento(s.getFechaAnulacion()), texto(s.getMotivoAnulacion()), texto(s.getAnuladaPor())};
    }

    @Override
    protected StockExit leer(ResultSet fila) throws SQLException {
        StockExit s = new StockExit();
        s.setId(fila.getString("ID"));
        s.setProductoId(fila.getString("PRODUCTO_ID"));
        s.setCantidad(fila.getInt("CANTIDAD"));
        s.setFecha(leerMomento(fila, "FECHA"));
        s.setMotivo(fila.getString("MOTIVO"));
        s.setTipo(fila.getString("TIPO"));
        s.setEstado(fila.getString("ESTADO"));
        s.setFechaRegistro(leerMomento(fila, "FECHA_REGISTRO"));
        s.setAnulacion(leerMomento(fila, "FECHA_ANULACION"), fila.getString("MOTIVO_ANULACION"));
        s.setAnuladaPor(fila.getString("ANULADA_POR"));
        return s;
    }
}
