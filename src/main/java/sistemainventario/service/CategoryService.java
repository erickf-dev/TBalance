package sistemainventario.service;

import sistemainventario.model.Category;
import sistemainventario.model.BusinessType;
import sistemainventario.repository.Repository;
import sistemainventario.util.IdGenerator;

import java.util.List;

/**
 * Casos de uso de las categorías de un perfil.
 *
 * <p><b>GRASP Creator:</b> es quien agrega y registra las categorías, por eso
 * crea las categorías predefinidas de un tipo de negocio
 * ({@link #crearPredefinidas(BusinessType)}).</p>
 */
public class CategoryService extends EntityService<Category> {

    /** Prefijo que P2 ya usaba en {@code categorias.json}. */
    public static final String PREFIJO_ID = "CAT";

    public CategoryService(Repository<Category> repositorio) {
        super(repositorio);
        asignarIdsFaltantes();
    }

    /**
     * Migración de datos de P2: las categorías predefinidas se guardaban con
     * {@code id = null}, lo que impedía editarlas o eliminarlas. Al cargar el
     * perfil se les asigna un ID único.
     */
    private void asignarIdsFaltantes() {
        List<Category> sinId = repositorio.filtrar(categoria -> !categoria.tieneId());
        sinId.stream().map(Category::getId).distinct().forEach(repositorio::eliminar);
        sinId.forEach(categoria -> {
            categoria.setId(generarId());
            repositorio.agregar(categoria);
        });
    }

    @Override
    protected void validar(Category categoria) {
        categoria.validar();
    }

    @Override
    protected String generarId() {
        return IdGenerator.conPrefijo(PREFIJO_ID);
    }

    @Override
    protected String nombreEntidad() {
        return "Categoria";
    }

    /**
     * <b>Creator:</b> crea las categorías iniciales de un tipo de negocio.
     * En P2 se guardaban sin ID; ahora cada una recibe un ID único.
     *
     * @param tipoNegocio tipo elegido al crear el perfil
     */
    public void crearPredefinidas(BusinessType tipoNegocio) {
        String descripcion = "Categoria predefinida para " + tipoNegocio.getEtiqueta();
        for (String nombre : tipoNegocio.getCategoriasPredefinidas()) {
            guardar(new Category(null, nombre, descripcion));
        }
    }
}
