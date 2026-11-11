package sistemainventario.repository;

import sistemainventario.model.StockEntry;

import java.nio.file.Path;

/**
 * Persistencia de las entradas de inventario de un perfil en {@code entradas.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class StockEntryRepository extends JsonRepository<StockEntry> {

    public static final String ARCHIVO = "entradas.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public StockEntryRepository(Path directorio) {
        super(directorio, ARCHIVO, StockEntry.class);
    }
}
