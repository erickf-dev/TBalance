package sistemainventario.repository;

import sistemainventario.model.Product;

import java.nio.file.Path;

/**
 * Persistencia de los productos de un perfil en {@code productos.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class ProductRepository extends JsonRepository<Product> {

    public static final String ARCHIVO = "productos.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public ProductRepository(Path directorio) {
        super(directorio, ARCHIVO, Product.class);
    }
}
