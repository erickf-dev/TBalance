package sistemainventario.repository;

import sistemainventario.model.Supplier;

import java.nio.file.Path;

/**
 * Persistencia de los proveedores de un perfil en {@code proveedores.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class SupplierRepository extends JsonRepository<Supplier> {

    public static final String ARCHIVO = "proveedores.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public SupplierRepository(Path directorio) {
        super(directorio, ARCHIVO, Supplier.class);
    }
}
