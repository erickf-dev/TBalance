package sistemainventario.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persona que usa TBalance. Inicia sesión con su usuario y su contraseña y
 * entra solo a los negocios de su lista de {@link Acceso accesos}, con el rol
 * de cada uno.
 *
 * <p>La contraseña nunca se guarda: se guarda su resumen PBKDF2 con sal (ver
 * {@link sistemainventario.util.PasswordHasher}). Las personas no se borran,
 * se deshabilitan, porque sus ventas y anulaciones quedan en el historial.</p>
 */
public class User extends IdentifiedEntity implements Deshabilitable {

    private String nombre;
    private String usuario;
    private String clave;
    private Boolean activo;
    private int intentosFallidos;
    private String bloqueadoHasta;
    private String fechaCreacion;
    private String ultimoIngreso;
    private List<Acceso> accesos = new ArrayList<>();

    /** Constructor vacío para Gson. */
    public User() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return nombre de usuario para iniciar sesión, en minúsculas */
    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario == null ? null : usuario.trim().toLowerCase();
    }

    /** @return resumen de la contraseña ({@code pbkdf2_sha256$iteraciones$sal$resumen}) */
    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    @Override
    public boolean isActivo() {
        return !Boolean.FALSE.equals(activo);
    }

    @Override
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    /** @return fecha y hora hasta la que no puede entrar por intentos fallidos, o {@code null} */
    public String getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(String bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getUltimoIngreso() {
        return ultimoIngreso;
    }

    public void setUltimoIngreso(String ultimoIngreso) {
        this.ultimoIngreso = ultimoIngreso;
    }

    /** @return accesos a negocios (copia) */
    public List<Acceso> getAccesos() {
        return accesos == null ? new ArrayList<>() : new ArrayList<>(accesos);
    }

    public void setAccesos(List<Acceso> accesos) {
        this.accesos = accesos == null ? new ArrayList<>() : new ArrayList<>(accesos);
    }

    /**
     * <b>GRASP Experto:</b> la persona conoce sus accesos y responde qué rol
     * tiene en cada negocio.
     *
     * @param perfilId negocio
     * @return su rol en ese negocio, si tiene acceso
     */
    public Optional<Rol> rolEn(String perfilId) {
        if (perfilId == null) {
            return Optional.empty();
        }
        return getAccesos().stream().filter(a -> perfilId.equals(a.getPerfilId())).map(Acceso::getRol).findFirst();
    }

    /**
     * Da o cambia el acceso a un negocio.
     *
     * @param perfilId negocio
     * @param rol      rol en ese negocio
     */
    public void darAcceso(String perfilId, Rol rol) {
        if (accesos == null) {
            accesos = new ArrayList<>();
        }
        accesos.removeIf(a -> perfilId.equals(a.getPerfilId()));
        accesos.add(new Acceso(perfilId, rol));
    }

    /** @return {@code true} si es dueño de al menos un negocio */
    public boolean esDuenoDeAlguno() {
        return getAccesos().stream().anyMatch(a -> a.getRol() == Rol.DUENO);
    }
}
