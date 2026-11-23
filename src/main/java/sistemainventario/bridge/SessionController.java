package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Permiso;
import sistemainventario.model.Rol;
import sistemainventario.model.User;
import sistemainventario.service.SessionService;
import sistemainventario.service.UserService;
import sistemainventario.util.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>GRASP Controller</b> del inicio de sesión y de los usuarios de cada negocio.
 *
 * <p>Nunca devuelve el resumen de la contraseña: las personas se envían a la
 * vista solo con nombre, usuario, rol y estado.</p>
 */
public class SessionController {

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public SessionController(AppContext contexto) {
        this.contexto = contexto;
    }

    /**
     * @return JSON {@code {primeraVez, sesion}}: {@code sesion} es {@code null} si
     *         nadie ha entrado; si no, trae nombre, usuario, rol en el negocio abierto
     *         y la lista de permisos
     */
    public String estadoSesion() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("primeraVez", contexto.getUsuarios().esPrimeraVez());
        estado.put("sesion", contexto.getSesion().actual().map(this::describirSesion).orElse(null));
        estado.put("minutosInactividad", SessionService.MINUTOS_INACTIVIDAD);
        return BridgeResponse.json(estado);
    }

    /**
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return el estado de la sesión, o {@code "ERROR: ..."}
     */
    public String iniciarSesion(String usuario, String clave) {
        return BridgeResponse.ejecutar(() -> {
            contexto.iniciarSesion(usuario, clave);
            return estadoSesion();
        });
    }

    /**
     * Solo funciona la primera vez, cuando todavía no hay cuentas.
     *
     * @param nombre  nombre del dueño
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return el estado de la sesión, o {@code "ERROR: ..."}
     */
    public String crearPrimerDueno(String nombre, String usuario, String clave) {
        return BridgeResponse.ejecutar(() -> {
            contexto.crearPrimerDueno(nombre, usuario, clave);
            return estadoSesion();
        });
    }

    /** @return {@code "OK"} */
    public String cerrarSesion() {
        contexto.cerrarSesion();
        return BridgeResponse.OK;
    }

    /**
     * @param actual contraseña actual
     * @param nueva  contraseña nueva
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarMiClave(String actual, String nueva) {
        return BridgeResponse.ejecutarOk(() -> contexto.getUsuarios().cambiarMiClave(quien(), actual, nueva));
    }

    /** @return personas del negocio abierto (sin contraseñas) */
    public String listarUsuarios() {
        return BridgeResponse.ejecutar(() -> {
            String perfilId = negocio();
            List<Map<String, Object>> lista = new ArrayList<>();
            for (User persona : usuarios().usuariosDe(perfilId)) {
                lista.add(describir(persona, perfilId));
            }
            return BridgeResponse.json(lista);
        });
    }

    /**
     * Crea una persona (sin {@code id}) o cambia su nombre y rol (con {@code id}).
     *
     * @param json {@code {id?, nombre, usuario, clave?, rol}}
     * @return la persona, o {@code "ERROR: ..."}
     */
    public String guardarUsuario(String json) {
        return BridgeResponse.ejecutar(() -> {
            Datos datos = JsonUtil.fromJson(json, Datos.class);
            if (datos == null) {
                throw new ValidationException("No se recibieron los datos de la persona");
            }
            Rol rol = Rol.desde(datos.rol);
            String perfilId = negocio();
            User guardado = datos.id == null || datos.id.isBlank()
                    ? usuarios().crear(quien(), perfilId, datos.nombre, datos.usuario, datos.clave, rol)
                    : usuarios().editar(quien(), perfilId, datos.id, datos.nombre, rol);
            contexto.getSesion().refrescar(usuarios().obtener(quien().getId()));
            return BridgeResponse.json(describir(guardado, perfilId));
        });
    }

    /**
     * @param id     persona
     * @param activo {@code "false"} para deshabilitar
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String cambiarEstadoUsuario(String id, String activo) {
        return BridgeResponse.ejecutarOk(() -> usuarios().cambiarEstado(quien(), negocio(), id, Boolean.parseBoolean(activo)));
    }

    /**
     * @param id    persona
     * @param nueva contraseña nueva
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String restablecerClaveUsuario(String id, String nueva) {
        return BridgeResponse.ejecutarOk(() -> usuarios().restablecerClave(quien(), negocio(), id, nueva));
    }

    private Map<String, Object> describirSesion(User persona) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", persona.getId());
        s.put("nombre", persona.getNombre());
        s.put("usuario", persona.getUsuario());
        Optional<Rol> rol = contexto.rolActual();
        s.put("rol", rol.map(Rol::name).orElse(null));
        s.put("rolEtiqueta", rol.map(Rol::getEtiqueta).orElse(null));
        List<String> permisos = new ArrayList<>();
        rol.ifPresent(r -> r.getPermisos().forEach(p -> permisos.add(p.name())));
        s.put("permisos", permisos);
        s.put("puedeCrearNegocios", contexto.puedeCrearNegocios());
        s.put("minutosAnularPropia", Rol.MINUTOS_ANULAR_PROPIA);
        return s;
    }

    private Map<String, Object> describir(User persona, String perfilId) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", persona.getId());
        d.put("nombre", persona.getNombre());
        d.put("usuario", persona.getUsuario());
        Rol rol = persona.rolEn(perfilId).orElse(Rol.CAJERO);
        d.put("rol", rol.name());
        d.put("rolEtiqueta", rol.getEtiqueta());
        d.put("activo", persona.isActivo());
        d.put("ultimoIngreso", persona.getUltimoIngreso());
        d.put("esUsted", persona.getId().equals(quien().getId()));
        Rol suyo = contexto.rolActual().orElse(Rol.CAJERO);
        d.put("puedeAdministrar", suyo.puede(Permiso.USUARIOS) && suyo.puedeAsignar(rol) && !persona.getId().equals(quien().getId()));
        return d;
    }

    private User quien() {
        return contexto.getSesion().requerir();
    }

    /**
     * Historial de acciones que puede ver quien consulta: el dueño ve el de sus
     * negocios y las acciones generales (ingresos, copias de seguridad); el
     * administrador ve el de los negocios que administra.
     *
     * @param filtrosJson {@code {desde?, hasta?, usuarioId?, soloEsteNegocio?}}
     * @return lista JSON, de la más reciente a la más antigua (máximo 300)
     */
    public String listarHistorial(String filtrosJson) {
        return BridgeResponse.ejecutar(() -> {
            User yo = contexto.getSesion().requerir();
            Filtros f = filtrosJson == null || filtrosJson.isBlank() ? new Filtros() : JsonUtil.fromJson(filtrosJson, Filtros.class);
            if (f == null) {
                f = new Filtros();
            }
            java.util.Set<String> negocios = new java.util.HashSet<>();
            for (sistemainventario.model.Profile p : contexto.perfilesPermitidos()) {
                Optional<Rol> rol = yo.rolEn(p.getId());
                if (rol.isPresent() && rol.get().puede(Permiso.HISTORIAL)) {
                    negocios.add(p.getId());
                }
            }
            boolean generales = yo.esDuenoDeAlguno();
            if (f.soloEsteNegocio && contexto.getPerfilActivoId() != null) {
                negocios.retainAll(java.util.Set.of(contexto.getPerfilActivoId()));
                generales = false;
            }
            return BridgeResponse.json(contexto.getAuditoria().listar(negocios, generales, f.usuarioId, f.desde, f.hasta, 300));
        });
    }

    /** Filtros del historial que manda la página. */
    private static final class Filtros {
        String desde;
        String hasta;
        String usuarioId;
        boolean soloEsteNegocio;
    }

    private String negocio() {
        String perfilId = contexto.getPerfilActivoId();
        if (perfilId == null) {
            throw new ValidationException("Abra un negocio primero");
        }
        return perfilId;
    }

    private UserService usuarios() {
        return contexto.getUsuarios();
    }

    /** Datos que llegan de la ventana de usuario. */
    private static final class Datos {
        private String id;
        private String nombre;
        private String usuario;
        private String clave;
        private String rol;
    }
}
