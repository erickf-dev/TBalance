package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Administra las carpetas físicas de cada perfil:
 * {@code ~/.inventariopro/data/perfiles/<ID_PERFIL>/}.
 *
 * <p><b>SRP:</b> en P2 estas operaciones de sistema de archivos estaban
 * mezcladas dentro de {@code ProfileService}. Separarlas deja al servicio
 * con una sola razón de cambio: las reglas de negocio de los perfiles.</p>
 */
public class ProfileDirectory {

    private static final String CARPETA_PERFILES = "perfiles";

    private final Path directorioBase;

    /**
     * @param directorioBase carpeta raíz de datos
     */
    public ProfileDirectory(Path directorioBase) {
        this.directorioBase = directorioBase;
    }

    /**
     * @param perfilId ID del perfil
     * @return ruta de la carpeta del perfil (no se crea)
     */
    public Path resolver(String perfilId) {
        return directorioBase.resolve(CARPETA_PERFILES).resolve(perfilId);
    }

    /**
     * Crea la carpeta del perfil si no existe.
     *
     * @param perfilId ID del perfil
     * @return ruta de la carpeta creada
     */
    public Path crear(String perfilId) {
        Path carpeta = resolver(perfilId);
        JsonFileStore.crearDirectorio(carpeta);
        return carpeta;
    }

    /**
     * Elimina la carpeta del perfil y todos sus archivos.
     *
     * @param perfilId ID del perfil
     * @throws PersistenceException si algún archivo no se puede borrar
     */
    public void eliminar(String perfilId) {
        Path carpeta = resolver(perfilId);
        if (!Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> rutas = Files.walk(carpeta)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(ProfileDirectory::borrar);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo eliminar la carpeta del perfil " + perfilId, e);
        }
    }

    private static void borrar(Path ruta) {
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo eliminar " + ruta.getFileName(), e);
        }
    }

    public Path getDirectorioBase() {
        return directorioBase;
    }
}
