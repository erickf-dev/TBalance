package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.TextUtil;

/**
 * Agrupación lógica de productos dentro de un perfil (por ejemplo
 * "Medicamentos" o "Herramientas").
 */
public class Category extends IdentifiedEntity implements Deshabilitable {

    private String nombre;
    private String descripcion;

    /** {@code null} en los datos de versiones anteriores, que cuentan como habilitados. */
    private Boolean activo;

    /** Constructor vacío requerido por Gson. */
    public Category() {
        super();
    }

    public Category(String id, String nombre, String descripcion) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    /**
     * <b>GRASP Experto:</b> la categoría valida sus propios datos.
     *
     * @throws ValidationException si el nombre está vacío
     */
    public void validar() {
        if (TextUtil.esVacio(nombre)) {
            throw new ValidationException("El nombre de la categoria es obligatorio");
        }
    }

    @Override
    public String getEtiqueta() {
        return nombre != null ? nombre : super.getEtiqueta();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public boolean isActivo() {
        return !Boolean.FALSE.equals(activo);
    }

    @Override
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
