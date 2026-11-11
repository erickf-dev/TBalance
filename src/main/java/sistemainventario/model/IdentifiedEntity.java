package sistemainventario.model;

import sistemainventario.util.TextUtil;

import java.util.Objects;

/**
 * Superclase abstracta de todas las entidades persistibles del sistema.
 *
 * <p><b>Abstracción y herencia:</b> garantiza que Producto, Categoria,
 * Proveedor, Perfil, Venta y los movimientos de inventario tengan un
 * identificador y un comportamiento común.</p>
 *
 * <p><b>OCP:</b> el repositorio genérico
 * {@link sistemainventario.repository.JsonRepository} trabaja con cualquier
 * subclase; añadir una entidad nueva no obliga a modificarlo.</p>
 */
public abstract class IdentifiedEntity {

    private String id;

    protected IdentifiedEntity() {
    }

    protected IdentifiedEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * <b>GRASP Experto:</b> la entidad sabe si ya fue identificada.
     *
     * @return {@code true} si el ID no es nulo ni vacío
     */
    public boolean tieneId() {
        return !TextUtil.esVacio(id);
    }

    /**
     * Etiqueta legible para selectores y tablas. Las subclases con nombre
     * la sobrescriben (polimorfismo); por defecto devuelve el ID.
     *
     * @return texto descriptivo de la entidad
     */
    public String getEtiqueta() {
        return tieneId() ? id : "";
    }

    /**
     * Dos entidades son iguales si son del mismo tipo concreto y comparten ID.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        IdentifiedEntity entidad = (IdentifiedEntity) otro;
        return id != null && id.equals(entidad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
