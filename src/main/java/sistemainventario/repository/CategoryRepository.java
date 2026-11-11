package sistemainventario.repository;

import sistemainventario.model.Category;

import java.nio.file.Path;

/**
 * Persistencia de las categorías de un perfil en {@code categorias.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class CategoryRepository extends JsonRepository<Category> {

    public static final String ARCHIVO = "categorias.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public CategoryRepository(Path directorio) {
        super(directorio, ARCHIVO, Category.class);
    }
}
