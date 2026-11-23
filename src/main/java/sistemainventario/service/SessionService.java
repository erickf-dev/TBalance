package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Permiso;
import sistemainventario.model.Rol;
import sistemainventario.model.User;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Sesión de la persona que está usando TBalance en este computador.
 *
 * <p>Se cierra sola después de {@value #MINUTOS_INACTIVIDAD} minutos sin uso:
 * si alguien deja la caja abierta, otra persona no puede seguir con su usuario.</p>
 */
public class SessionService {

    /** Minutos sin uso tras los cuales se cierra la sesión. */
    public static final int MINUTOS_INACTIVIDAD = 30;

    private final LongSupplier reloj;
    private User usuario;
    private long ultimaActividad;

    /** Sesión con el reloj del sistema. */
    public SessionService() {
        this(System::currentTimeMillis);
    }

    /** @param reloj milisegundos actuales (se cambia en las pruebas) */
    public SessionService(LongSupplier reloj) {
        this.reloj = reloj;
    }

    /** @param persona quien acaba de iniciar sesión */
    public synchronized void abrir(User persona) {
        this.usuario = persona;
        this.ultimaActividad = reloj.getAsLong();
    }

    /** Cierra la sesión. */
    public synchronized void cerrar() {
        this.usuario = null;
    }

    /** @param persona datos nuevos de la misma persona (por ejemplo, después de darle un negocio) */
    public synchronized void refrescar(User persona) {
        if (usuario != null && persona != null && usuario.getId().equals(persona.getId())) {
            usuario = persona;
        }
    }

    /**
     * Anota que la persona sigue usando TBalance.
     *
     * @return {@code true} si la sesión sigue abierta
     */
    public synchronized boolean registrarActividad() {
        if (actual().isEmpty()) {
            return false;
        }
        ultimaActividad = reloj.getAsLong();
        return true;
    }

    /** @return la persona con sesión abierta (vacío si nadie o si venció por inactividad) */
    public synchronized Optional<User> actual() {
        if (usuario != null && reloj.getAsLong() - ultimaActividad > MINUTOS_INACTIVIDAD * 60_000L) {
            usuario = null;
        }
        return Optional.ofNullable(usuario);
    }

    /**
     * @return la persona con sesión abierta
     * @throws ValidationException si no hay sesión
     */
    public User requerir() {
        return actual().orElseThrow(() -> new ValidationException("Inicie sesión para continuar"));
    }

    /**
     * @param perfilId negocio abierto
     * @return rol de la persona en ese negocio, si tiene acceso
     */
    public Optional<Rol> rolEn(String perfilId) {
        return actual().flatMap(u -> u.rolEn(perfilId));
    }

    /**
     * @param perfilId negocio abierto
     * @param permiso  acción
     * @return {@code true} si la persona puede hacerla en ese negocio
     */
    public boolean puede(String perfilId, Permiso permiso) {
        return rolEn(perfilId).map(r -> r.puede(permiso)).orElse(false);
    }
}
