package sistemainventario.repository;

import sistemainventario.model.Sale;

import java.nio.file.Path;

/**
 * Persistencia de las ventas de un perfil en {@code ventas.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class SaleRepository extends JsonRepository<Sale> {

    public static final String ARCHIVO = "ventas.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public SaleRepository(Path directorio) {
        super(directorio, ARCHIVO, Sale.class);
    }
}
