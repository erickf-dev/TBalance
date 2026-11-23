package sistemainventario.repository;

import sistemainventario.model.CreditPayment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Abonos de los clientes que deben, en la tabla ABONO. */
public class CreditPaymentJdbcRepository extends JdbcRepository<CreditPayment> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public CreditPaymentJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "ABONO";
    }

    @Override
    protected List<String> columnas() {
        return List.of("CLIENTE_DOCUMENTO", "CLIENTE", "MONTO", "METODO_PAGO", "REFERENCIA_PAGO", "NOTA", "FECHA",
                "USUARIO_ID", "REGISTRADO_POR", "ESTADO", "FECHA_ANULACION", "MOTIVO_ANULACION", "ANULADA_POR");
    }

    @Override
    protected Object[] valores(CreditPayment a) {
        return new Object[]{a.getClienteDocumento(), texto(a.getCliente()), a.getMonto(), a.getMetodoPago(),
                texto(a.getReferenciaPago()), texto(a.getNota()), momento(a.getFecha()), texto(a.getUsuarioId()),
                texto(a.getRegistradoPor()), a.getEstado(), momento(a.getFechaAnulacion()),
                texto(a.getMotivoAnulacion()), texto(a.getAnuladaPor())};
    }

    @Override
    protected CreditPayment leer(ResultSet fila) throws SQLException {
        CreditPayment a = new CreditPayment();
        a.setId(fila.getString("ID"));
        a.setClienteDocumento(fila.getString("CLIENTE_DOCUMENTO"));
        a.setCliente(fila.getString("CLIENTE"));
        a.setMonto(leerNumero(fila, "MONTO"));
        a.setMetodoPago(fila.getString("METODO_PAGO"));
        a.setReferenciaPago(fila.getString("REFERENCIA_PAGO"));
        a.setNota(fila.getString("NOTA"));
        a.setFecha(leerMomento(fila, "FECHA"));
        a.setUsuarioId(fila.getString("USUARIO_ID"));
        a.setRegistradoPor(fila.getString("REGISTRADO_POR"));
        a.setEstado(fila.getString("ESTADO"));
        a.setAnulacion(leerMomento(fila, "FECHA_ANULACION"), fila.getString("MOTIVO_ANULACION"));
        a.setAnuladaPor(fila.getString("ANULADA_POR"));
        return a;
    }
}
