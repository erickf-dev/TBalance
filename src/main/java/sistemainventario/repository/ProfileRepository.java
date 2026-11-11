package sistemainventario.repository;

import sistemainventario.model.Profile;

import java.nio.file.Path;

/**
 * Persistencia global de la lista de perfiles en {@code perfiles.json}.
 *
 * <p>Ejemplo de <b>OCP</b>: hereda todo el CRUD de {@link JsonRepository};
 * solo declara el archivo y el tipo que administra.</p>
 */
public class ProfileRepository extends JsonRepository<Profile> {

    public static final String ARCHIVO = "perfiles.json";

    /**
     * @param directorio carpeta donde se ubica {@value #ARCHIVO}
     */
    public ProfileRepository(Path directorio) {
        super(directorio, ARCHIVO, Profile.class);
    }
}
