package sistemainventario.repository;

import sistemainventario.model.Acceso;
import sistemainventario.model.Rol;
import sistemainventario.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Personas en la tabla USUARIO y sus accesos en NEGOCIO_USUARIO (qué negocio y
 * con qué rol). La tabla USUARIO no pertenece a un negocio: una persona puede
 * entrar a varios.
 */
public class UserJdbcRepository extends JdbcRepository<User> {

    /** @param bd conexión a la base */
    public UserJdbcRepository(Database bd) {
        super(bd, null);
    }

    @Override
    protected String tabla() {
        return "USUARIO";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "USUARIO", "CLAVE", "ACTIVO", "INTENTOS_FALLIDOS", "BLOQUEADO_HASTA",
                "FECHA_CREACION", "ULTIMO_INGRESO");
    }

    @Override
    protected Object[] valores(User u) {
        return new Object[]{u.getNombre(), u.getUsuario(), u.getClave(), u.isActivo(), u.getIntentosFallidos(),
                momento(u.getBloqueadoHasta()), momento(u.getFechaCreacion()), momento(u.getUltimoIngreso())};
    }

    @Override
    protected User leer(ResultSet fila) throws SQLException {
        User u = new User();
        u.setId(fila.getString("ID"));
        u.setNombre(fila.getString("NOMBRE"));
        u.setUsuario(fila.getString("USUARIO"));
        u.setClave(fila.getString("CLAVE"));
        u.setActivo(fila.getInt("ACTIVO") == 1);
        u.setIntentosFallidos(fila.getInt("INTENTOS_FALLIDOS"));
        u.setBloqueadoHasta(leerMomento(fila, "BLOQUEADO_HASTA"));
        u.setFechaCreacion(leerMomento(fila, "FECHA_CREACION"));
        u.setUltimoIngreso(leerMomento(fila, "ULTIMO_INGRESO"));
        return u;
    }

    @Override
    protected void completar(List<User> usuarios) {
        Map<String, List<Acceso>> porUsuario = new LinkedHashMap<>();
        bd.consultar("SELECT USUARIO_ID, PERFIL_ID, ROL FROM NEGOCIO_USUARIO ORDER BY USUARIO_ID, PERFIL_ID", fila -> {
            porUsuario.computeIfAbsent(fila.getString("USUARIO_ID"), k -> new ArrayList<>())
                    .add(new Acceso(fila.getString("PERFIL_ID"), Rol.desde(fila.getString("ROL"))));
            return null;
        });
        for (User usuario : usuarios) {
            usuario.setAccesos(porUsuario.getOrDefault(usuario.getId(), new ArrayList<>()));
        }
    }

    @Override
    protected void guardarHijos(Connection conexion, User usuario, boolean nuevo) throws SQLException {
        if (!nuevo) {
            Database.actualizar(conexion, "DELETE FROM NEGOCIO_USUARIO WHERE USUARIO_ID = ?", usuario.getId());
        }
        for (Acceso acceso : usuario.getAccesos()) {
            Database.actualizar(conexion, "INSERT INTO NEGOCIO_USUARIO (USUARIO_ID, PERFIL_ID, ROL) VALUES (?, ?, ?)",
                    usuario.getId(), acceso.getPerfilId(), acceso.getRol().name());
        }
    }
}
