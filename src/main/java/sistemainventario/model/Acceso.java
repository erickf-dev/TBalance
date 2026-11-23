package sistemainventario.model;

import java.util.Objects;

/**
 * Permiso de una persona sobre un negocio: en qué negocio entra y con qué rol
 * (tabla NEGOCIO_USUARIO).
 */
public class Acceso {

    private String perfilId;
    private String rol;

    /** Constructor vacío para Gson. */
    public Acceso() {
    }

    /**
     * @param perfilId negocio
     * @param rol      rol dentro de ese negocio
     */
    public Acceso(String perfilId, Rol rol) {
        this.perfilId = Objects.requireNonNull(perfilId);
        this.rol = rol.name();
    }

    public String getPerfilId() {
        return perfilId;
    }

    /** @return el rol (cajero si el dato guardado no se reconoce, por seguridad) */
    public Rol getRol() {
        try {
            return Rol.valueOf(rol);
        } catch (RuntimeException e) {
            return Rol.CAJERO;
        }
    }

    public void setRol(Rol rol) {
        this.rol = rol.name();
    }
}
