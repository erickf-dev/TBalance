package sistemainventario.repository;

import sistemainventario.model.IdentifiedEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Contrato de acceso a datos para cualquier entidad del sistema.
 *
 * <p><b>Bajo acoplamiento / DIP:</b> los servicios dependen de esta interfaz y
 * no de la implementación JSON. Si en una fase posterior se migra a una base
 * de datos relacional (DAO + JDBC), basta con crear otra implementación sin
 * tocar la capa de servicios (<b>OCP</b>).</p>
 *
 * @param <T> tipo de entidad administrada
 */
public interface Repository<T extends IdentifiedEntity> {

    /** @return copia de todas las entidades almacenadas */
    List<T> listarTodos();

    /**
     * @param id identificador buscado
     * @return la entidad, o vacío si no existe
     */
    Optional<T> buscarPorId(String id);

    /**
     * @param id identificador buscado
     * @return {@code true} si existe una entidad con ese ID
     */
    boolean existe(String id);

    /**
     * @param condicion criterio de filtrado
     * @return entidades que cumplen la condición
     */
    List<T> filtrar(Predicate<T> condicion);

    /**
     * Agrega una entidad nueva y persiste el cambio.
     *
     * @param entidad entidad con ID asignado
     */
    void agregar(T entidad);

    /**
     * Reemplaza la entidad que tenga el mismo ID y persiste el cambio.
     *
     * @param entidad entidad modificada
     * @return {@code true} si existía y fue actualizada
     */
    boolean actualizar(T entidad);

    /**
     * @param id identificador de la entidad a eliminar
     * @return {@code true} si existía y fue eliminada
     */
    boolean eliminar(String id);

    /** @return el siguiente ID numérico disponible (mayor ID + 1) */
    String siguienteIdNumerico();
}
