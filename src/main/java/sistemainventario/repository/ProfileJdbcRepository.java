package sistemainventario.repository;

import sistemainventario.model.Profile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Perfiles (negocios) en la tabla PERFIL y su configuración en
 * PERFIL_CONFIGURACION. Al borrar un perfil, la base borra en cascada todos
 * sus datos (ON DELETE CASCADE).
 */
public class ProfileJdbcRepository extends JdbcRepository<Profile> {

    /** @param bd conexión a la base */
    public ProfileJdbcRepository(Database bd) {
        super(bd, null);
    }

    @Override
    protected String tabla() {
        return "PERFIL";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "DESCRIPCION", "COLOR", "TIPO_NEGOCIO", "FECHA_CREACION", "ACTIVO");
    }

    @Override
    protected Object[] valores(Profile p) {
        return new Object[]{p.getNombre(), texto(p.getDescripcion()), texto(p.getColor()), texto(p.getTipoNegocio()),
                dia(p.getFechaCreacion()), p.isActivo()};
    }

    @Override
    protected Profile leer(ResultSet fila) throws SQLException {
        Profile p = new Profile();
        p.setId(fila.getString("ID"));
        p.setNombre(fila.getString("NOMBRE"));
        p.setDescripcion(fila.getString("DESCRIPCION"));
        p.setColor(fila.getString("COLOR"));
        p.setTipoNegocio(fila.getString("TIPO_NEGOCIO"));
        p.setFechaCreacion(leerDia(fila, "FECHA_CREACION"));
        p.setActivo(fila.getInt("ACTIVO") == 1);
        return p;
    }

    @Override
    protected void completar(List<Profile> perfiles) {
        Map<String, Map<String, String>> porPerfil = new LinkedHashMap<>();
        bd.consultar("SELECT PERFIL_ID, CLAVE, VALOR FROM PERFIL_CONFIGURACION ORDER BY PERFIL_ID, CLAVE", fila -> {
            porPerfil.computeIfAbsent(fila.getString("PERFIL_ID"), k -> new LinkedHashMap<>())
                    .put(fila.getString("CLAVE"), fila.getString("VALOR"));
            return null;
        });
        for (Profile perfil : perfiles) {
            perfil.setConfiguracion(porPerfil.getOrDefault(perfil.getId(), new LinkedHashMap<>()));
        }
    }

    @Override
    protected void guardarHijos(Connection conexion, Profile perfil, boolean nuevo) throws SQLException {
        if (!nuevo) {
            Database.actualizar(conexion, "DELETE FROM PERFIL_CONFIGURACION WHERE PERFIL_ID = ?", perfil.getId());
        }
        for (Map.Entry<String, String> dato : perfil.getConfiguracion().entrySet()) {
            Database.actualizar(conexion, "INSERT INTO PERFIL_CONFIGURACION (PERFIL_ID, CLAVE, VALOR) VALUES (?, ?, ?)",
                    perfil.getId(), dato.getKey(), texto(dato.getValue()));
        }
    }
}
