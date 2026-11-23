package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Permiso;
import sistemainventario.model.Rol;
import sistemainventario.model.User;
import sistemainventario.repository.Repository;
import sistemainventario.util.DateUtil;
import sistemainventario.util.IdGenerator;
import sistemainventario.util.PasswordHasher;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Casos de uso de las personas que usan TBalance: la primera cuenta (el dueño),
 * iniciar sesión, crear usuarios de un negocio, deshabilitarlos y cambiar contraseñas.
 *
 * <p>Las reglas de quién puede administrar a quién viven aquí y en {@link Rol}:
 * el dueño administra a todos los de sus negocios; el administrador solo a los
 * cajeros; nadie se deshabilita a sí mismo.</p>
 */
public class UserService {

    /** Intentos fallidos antes de bloquear el usuario un rato. */
    public static final int INTENTOS_PERMITIDOS = 5;
    /** Minutos de bloqueo después de {@value #INTENTOS_PERMITIDOS} intentos fallidos. */
    public static final int MINUTOS_BLOQUEO = 5;
    /** Largo mínimo de una contraseña. */
    public static final int LARGO_MINIMO_CLAVE = 6;

    private static final Pattern FORMATO_USUARIO = Pattern.compile("[a-z0-9._-]{3,30}");
    private static final String MENSAJE_INGRESO = "Usuario o contraseña incorrectos";

    private final Repository<User> repositorio;

    /** @param repositorio personas (JSON o base de datos) */
    public UserService(Repository<User> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    /** @return {@code true} si todavía no hay ninguna cuenta (primera vez que se abre TBalance) */
    public boolean esPrimeraVez() {
        return repositorio.listarTodos().isEmpty();
    }

    /**
     * Crea la primera cuenta, la del dueño, con acceso a todos los negocios que ya existan.
     *
     * @param nombre     nombre de la persona
     * @param usuario    nombre de usuario para entrar
     * @param clave      contraseña
     * @param negocios   IDs de los negocios que ya hay
     * @return la cuenta creada
     * @throws ValidationException si ya hay cuentas o los datos no sirven
     */
    public synchronized User crearPrimerDueno(String nombre, String usuario, String clave, Collection<String> negocios) {
        if (!esPrimeraVez()) {
            throw new ValidationException("La cuenta del dueño ya existe. Inicie sesión.");
        }
        User dueno = nuevo(nombre, usuario, clave);
        dueno.setUltimoIngreso(DateUtil.ahora());
        negocios.forEach(id -> dueno.darAcceso(id, Rol.DUENO));
        repositorio.agregar(dueno);
        return dueno;
    }

    /**
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return la persona, si la contraseña es correcta
     * @throws ValidationException con un mensaje que no dice si lo que falló fue el usuario o la contraseña
     */
    public synchronized User iniciarSesion(String usuario, String clave) {
        String nombreUsuario = usuario == null ? "" : usuario.trim().toLowerCase();
        Optional<User> encontrado = buscarPorUsuario(nombreUsuario);
        if (encontrado.isEmpty()) {
            // se hace el mismo cálculo para que no se note por el tiempo si el usuario existe
            PasswordHasher.coincide(String.valueOf(clave), resumenFicticio());
            throw new ValidationException(MENSAJE_INGRESO);
        }
        User persona = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime bloqueo = DateUtil.parsearMomento(persona.getBloqueadoHasta()).orElse(null);
        if (bloqueo != null && bloqueo.isAfter(ahora)) {
            long minutos = Math.max(1, java.time.Duration.between(ahora, bloqueo).toMinutes() + 1);
            throw new ValidationException("Demasiados intentos. Espere " + minutos + (minutos == 1 ? " minuto" : " minutos")
                    + " y vuelva a intentarlo.");
        }
        if (!PasswordHasher.coincide(String.valueOf(clave), persona.getClave())) {
            int fallidos = persona.getIntentosFallidos() + 1;
            persona.setIntentosFallidos(fallidos >= INTENTOS_PERMITIDOS ? 0 : fallidos);
            if (fallidos >= INTENTOS_PERMITIDOS) {
                persona.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO).format(DateUtil.FORMATO_FECHA_HORA));
            }
            repositorio.actualizar(persona);
            throw new ValidationException(fallidos >= INTENTOS_PERMITIDOS
                    ? "Demasiados intentos. Espere " + MINUTOS_BLOQUEO + " minutos y vuelva a intentarlo."
                    : MENSAJE_INGRESO);
        }
        if (!persona.isActivo()) {
            throw new ValidationException("Este usuario está deshabilitado. Hable con el dueño del negocio.");
        }
        persona.setIntentosFallidos(0);
        persona.setBloqueadoHasta(null);
        persona.setUltimoIngreso(DateUtil.ahora());
        repositorio.actualizar(persona);
        return persona;
    }

    /**
     * @param perfilId negocio
     * @return personas con acceso a ese negocio (habilitadas y deshabilitadas), por nombre
     */
    public List<User> usuariosDe(String perfilId) {
        return repositorio.filtrar(u -> u.rolEn(perfilId).isPresent()).stream()
                .sorted(Comparator.comparing(u -> String.valueOf(u.getNombre()).toLowerCase()))
                .toList();
    }

    /**
     * Crea una persona con acceso a un negocio, o le da acceso si ya existe ese usuario.
     *
     * @param quien    quien lo hace
     * @param perfilId negocio
     * @param nombre   nombre de la persona
     * @param usuario  nombre de usuario
     * @param clave    contraseña inicial
     * @param rol      rol en el negocio
     * @return la persona
     */
    public synchronized User crear(User quien, String perfilId, String nombre, String usuario, String clave, Rol rol) {
        exigirAdministrar(quien, perfilId, rol);
        String nombreUsuario = usuario == null ? "" : usuario.trim().toLowerCase();
        if (buscarPorUsuario(nombreUsuario).isPresent()) {
            throw new ValidationException("Ya existe el usuario " + nombreUsuario + ". Elija otro.");
        }
        User persona = nuevo(nombre, nombreUsuario, clave);
        persona.darAcceso(perfilId, rol);
        repositorio.agregar(persona);
        return persona;
    }

    /**
     * Cambia el nombre y el rol de una persona en un negocio.
     *
     * @param quien    quien lo hace
     * @param perfilId negocio
     * @param id       persona
     * @param nombre   nombre nuevo
     * @param rol      rol nuevo
     * @return la persona
     */
    public synchronized User editar(User quien, String perfilId, String id, String nombre, Rol rol) {
        User persona = obtener(id);
        Rol actual = persona.rolEn(perfilId).orElseThrow(() -> new ValidationException("Esa persona no es de este negocio"));
        exigirAdministrar(quien, perfilId, actual);
        exigirAdministrar(quien, perfilId, rol);
        if (persona.getId().equals(quien.getId()) && rol != actual) {
            throw new ValidationException("No puede cambiar su propio rol");
        }
        if (actual == Rol.DUENO && rol != Rol.DUENO && duenosActivos(perfilId) <= 1) {
            throw new ValidationException("El negocio necesita al menos un dueño");
        }
        persona.setNombre(validarNombre(nombre));
        persona.darAcceso(perfilId, rol);
        repositorio.actualizar(persona);
        return persona;
    }

    /**
     * Deshabilita o vuelve a habilitar una persona (no se borra: sus ventas quedan con su nombre).
     *
     * @param quien    quien lo hace
     * @param perfilId negocio desde el que se administra
     * @param id       persona
     * @param activo   {@code false} para deshabilitar
     */
    public synchronized void cambiarEstado(User quien, String perfilId, String id, boolean activo) {
        User persona = obtener(id);
        Rol rol = persona.rolEn(perfilId).orElseThrow(() -> new ValidationException("Esa persona no es de este negocio"));
        exigirAdministrar(quien, perfilId, rol);
        if (persona.getId().equals(quien.getId())) {
            throw new ValidationException("No puede deshabilitar su propio usuario");
        }
        if (!activo && rol == Rol.DUENO && duenosActivos(perfilId) <= 1) {
            throw new ValidationException("El negocio necesita al menos un dueño habilitado");
        }
        persona.setActivo(activo);
        if (activo) {
            persona.setIntentosFallidos(0);
            persona.setBloqueadoHasta(null);
        }
        repositorio.actualizar(persona);
    }

    /**
     * Pone una contraseña nueva a otra persona (por ejemplo, si la olvidó).
     *
     * @param quien    quien lo hace
     * @param perfilId negocio desde el que se administra
     * @param id       persona
     * @param nueva    contraseña nueva
     */
    public synchronized void restablecerClave(User quien, String perfilId, String id, String nueva) {
        User persona = obtener(id);
        Rol rol = persona.rolEn(perfilId).orElseThrow(() -> new ValidationException("Esa persona no es de este negocio"));
        exigirAdministrar(quien, perfilId, rol);
        persona.setClave(PasswordHasher.resumir(validarClave(nueva)));
        persona.setIntentosFallidos(0);
        persona.setBloqueadoHasta(null);
        repositorio.actualizar(persona);
    }

    /**
     * @param quien  persona con la sesión abierta
     * @param actual contraseña actual
     * @param nueva  contraseña nueva
     */
    public synchronized void cambiarMiClave(User quien, String actual, String nueva) {
        User persona = obtener(quien.getId());
        if (!PasswordHasher.coincide(String.valueOf(actual), persona.getClave())) {
            throw new ValidationException("La contraseña actual no es correcta");
        }
        persona.setClave(PasswordHasher.resumir(validarClave(nueva)));
        repositorio.actualizar(persona);
    }

    /**
     * Da acceso de dueño a quien crea un negocio nuevo.
     *
     * @param quien    persona que creó el negocio
     * @param perfilId negocio nuevo
     */
    public synchronized void hacerDueno(User quien, String perfilId) {
        User persona = obtener(quien.getId());
        persona.darAcceso(perfilId, Rol.DUENO);
        repositorio.actualizar(persona);
    }

    /**
     * @param id persona
     * @return la persona con los datos guardados más recientes
     */
    public User obtener(String id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new ValidationException("No existe esa persona"));
    }

    private static volatile String ficticio;

    private static String resumenFicticio() {
        if (ficticio == null) {
            ficticio = PasswordHasher.resumir("usuario-que-no-existe");
        }
        return ficticio;
    }

    private Optional<User> buscarPorUsuario(String usuario) {
        return repositorio.listarTodos().stream().filter(u -> usuario.equals(u.getUsuario())).findFirst();
    }

    private void exigirAdministrar(User quien, String perfilId, Rol rol) {
        Rol suyo = quien.rolEn(perfilId).orElseThrow(() -> new ValidationException("No tiene acceso a este negocio"));
        if (!suyo.puede(Permiso.USUARIOS) || !suyo.puedeAsignar(rol)) {
            throw new ValidationException(suyo == Rol.ADMINISTRADOR
                    ? "El administrador solo puede administrar cajeros"
                    : "Su rol no permite administrar usuarios");
        }
    }

    private long duenosActivos(String perfilId) {
        return repositorio.filtrar(u -> u.isActivo() && u.rolEn(perfilId).orElse(null) == Rol.DUENO).size();
    }

    private User nuevo(String nombre, String usuario, String clave) {
        String nombreUsuario = usuario == null ? "" : usuario.trim().toLowerCase();
        if (!FORMATO_USUARIO.matcher(nombreUsuario).matches()) {
            throw new ValidationException("El usuario debe tener de 3 a 30 letras sin tilde, números, punto, guion o guion bajo, sin espacios");
        }
        User persona = new User();
        persona.setId(IdGenerator.conPrefijo("USR"));
        persona.setNombre(validarNombre(nombre));
        persona.setUsuario(nombreUsuario);
        persona.setClave(PasswordHasher.resumir(validarClave(clave)));
        persona.setActivo(true);
        persona.setFechaCreacion(DateUtil.ahora());
        return persona;
    }

    private static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty() || limpio.length() > 100) {
            throw new ValidationException("Escriba el nombre de la persona (máximo 100 letras)");
        }
        return limpio;
    }

    private static String validarClave(String clave) {
        if (clave == null || clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ValidationException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
        }
        if (clave.length() > 128) {
            throw new ValidationException("La contraseña es demasiado larga");
        }
        return clave;
    }
}
