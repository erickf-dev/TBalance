package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Deshabilitable;
import sistemainventario.model.IdentifiedEntity;
import sistemainventario.repository.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio base para las entidades con CRUD simple (productos, categorías y
 * proveedores).
 *
 * <p>En P2 cada gestor repetía la misma lógica de "si no tiene ID, generarlo;
 * si existe, actualizar; si no, agregar", y además esa lógica estaba
 * duplicada en el {@code JavaBridge}. Aquí se resuelve una sola vez con el
 * patrón <b>Template Method</b>: el algoritmo de {@link #guardar} es fijo y
 * las subclases solo completan los pasos variables ({@link #validar} y
 * {@link #generarId}).</p>
 *
 * <p><b>OCP:</b> un gestor para una entidad nueva se crea extendiendo esta
 * clase, sin modificarla. <b>Bajo acoplamiento:</b> depende de la interfaz
 * {@link Repository}, no de la implementación JSON.</p>
 *
 * @param <T> tipo de entidad administrada
 */
public abstract class EntityService<T extends IdentifiedEntity> {

    protected final Repository<T> repositorio;

    /**
     * Si otro equipo cambió los datos en la base, los vuelve a leer. Se usa
     * antes de vender o sacar mercancía, para revisar el stock real.
     */
    public void refrescar() {
        repositorio.refrescar();
    }

    protected EntityService(Repository<T> repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio es obligatorio");
    }

    /** @return todas las entidades del perfil activo */
    public List<T> listarTodos() {
        return repositorio.listarTodos().stream().filter(EntityService::habilitado).collect(Collectors.toList());
    }

    /** @return los registros deshabilitados (borrado lógico), para poder volver a habilitarlos */
    public List<T> listarDeshabilitados() {
        return repositorio.listarTodos().stream().filter(e -> !habilitado(e)).collect(Collectors.toList());
    }

    /**
     * @param id identificador
     * @return la entidad, o vacío si no existe
     */
    public Optional<T> buscarPorId(String id) {
        return repositorio.buscarPorId(id);
    }

    /**
     * @param id identificador
     * @return la entidad encontrada
     * @throws ValidationException si no existe
     */
    public T obtener(String id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new ValidationException(nombreEntidad() + " no encontrado"));
    }

    /**
     * Crea o actualiza una entidad (método plantilla).
     *
     * @param entidad datos recibidos desde la vista
     * @return la entidad guardada, con su ID asignado
     * @throws ValidationException si los datos no son válidos
     */
    public final T guardar(T entidad) {
        if (entidad == null) {
            throw new ValidationException("No se recibieron datos de " + nombreEntidad());
        }
        validar(entidad);
        if (!entidad.tieneId()) {
            entidad.setId(generarId());
        }
        Optional<T> guardado = repositorio.buscarPorId(entidad.getId());
        if (guardado.isPresent()) {
            conservarEstado(guardado.get(), entidad);
            repositorio.actualizar(entidad);
        } else {
            repositorio.agregar(entidad);
        }
        return entidad;
    }

    /**
     * @param id identificador
     * @return {@code true} si la entidad existía y fue eliminada
     */
    /**
     * Borrado lógico: deshabilita o vuelve a habilitar el registro. Nada se borra,
     * así se conserva el historial (ventas, entradas, salidas) que lo menciona.
     *
     * @param id     ID del registro
     * @param activo {@code false} para deshabilitar, {@code true} para habilitar
     * @return el registro con su nuevo estado
     * @throws ValidationException si no existe o no se puede deshabilitar
     */
    public T cambiarEstado(String id, boolean activo) {
        T entidad = obtener(id);
        if (!(entidad instanceof Deshabilitable registro)) {
            throw new ValidationException(nombreEntidad() + " no se puede deshabilitar");
        }
        if (!activo) {
            validarDeshabilitar(entidad);
        }
        registro.setActivo(activo);
        repositorio.actualizar(entidad);
        return entidad;
    }

    /**
     * Reglas extra antes de deshabilitar. Por defecto ninguna.
     *
     * @param entidad registro que se va a deshabilitar
     */
    protected void validarDeshabilitar(T entidad) {
    }

    /** Editar un registro no cambia si está habilitado: eso solo lo hace {@link #cambiarEstado}. */
    private static void conservarEstado(Object anterior, Object nuevo) {
        if (anterior instanceof Deshabilitable antes && nuevo instanceof Deshabilitable despues) {
            despues.setActivo(antes.isActivo());
        }
    }

    private static boolean habilitado(Object entidad) {
        return !(entidad instanceof Deshabilitable registro) || registro.isActivo();
    }

    /** Paso variable: reglas de validación propias de la entidad. */
    protected abstract void validar(T entidad);

    /** Paso variable: estrategia de generación de ID de la entidad. */
    protected abstract String generarId();

    /** @return nombre legible de la entidad para mensajes de error */
    protected abstract String nombreEntidad();
}
