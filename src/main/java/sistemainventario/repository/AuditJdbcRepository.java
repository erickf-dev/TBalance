package sistemainventario.repository;

import sistemainventario.model.AuditEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Historial de acciones en la tabla AUDITORIA. No pertenece a un solo negocio:
 * PERFIL_ID es un dato más (texto, sin llave foránea, para que el historial se
 * conserve aunque el negocio cambie).
 */
public class AuditJdbcRepository extends JdbcRepository<AuditEntry> {

    /** @param bd conexión a la base */
    public AuditJdbcRepository(Database bd) {
        super(bd, null);
    }

    @Override
    protected String tabla() {
        return "AUDITORIA";
    }

    @Override
    protected List<String> columnas() {
        return List.of("FECHA", "USUARIO_ID", "USUARIO", "ROL", "PERFIL_ID", "PERFIL", "ACCION", "DETALLE");
    }

    @Override
    protected Object[] valores(AuditEntry e) {
        return new Object[]{momento(e.getFecha()), texto(e.getUsuarioId()), texto(e.getUsuario()), texto(e.getRol()),
                texto(e.getPerfilId()), texto(e.getPerfil()), texto(e.getAccion()), texto(e.getDetalle())};
    }

    @Override
    protected AuditEntry leer(ResultSet fila) throws SQLException {
        AuditEntry e = new AuditEntry(leerMomento(fila, "FECHA"), fila.getString("USUARIO_ID"), fila.getString("USUARIO"),
                fila.getString("ROL"), fila.getString("PERFIL_ID"), fila.getString("PERFIL"), fila.getString("ACCION"),
                fila.getString("DETALLE"));
        e.setId(fila.getString("ID"));
        return e;
    }
}
