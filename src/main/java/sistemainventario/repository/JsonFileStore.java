package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Lectura y escritura de un archivo de texto JSON en disco.
 *
 * <p><b>SRP:</b> en P2 la lógica de E/S estaba repetida en
 * {@code JsonRepository} y en {@code GlobalConfigRepository}. Ahora
 * ambos delegan en esta clase, que solo sabe manejar archivos.</p>
 *
 * <p>La escritura es segura: primero se escribe un archivo temporal y luego
 * se reemplaza el original, evitando archivos corruptos si la aplicación se
 * cierra a mitad de una escritura.</p>
 */
public class JsonFileStore {

    private final Path archivo;

    /**
     * @param archivo ruta completa del archivo; su carpeta se crea si no existe
     */
    public JsonFileStore(Path archivo) {
        this.archivo = archivo;
        crearDirectorio(archivo.getParent());
    }

    /** @return {@code true} si el archivo existe en disco */
    public boolean existe() {
        return Files.exists(archivo);
    }

    /**
     * @return el contenido del archivo, o vacío si todavía no existe
     * @throws PersistenceException si el archivo existe pero no se puede leer
     */
    public Optional<String> leer() {
        if (!existe()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(archivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer " + archivo.getFileName(), e);
        }
    }

    /**
     * Reemplaza el contenido del archivo de forma segura.
     *
     * @param contenido texto JSON a guardar
     * @throws PersistenceException si no se puede escribir
     */
    public void escribir(String contenido) {
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        try {
            Files.writeString(temporal, contenido, StandardCharsets.UTF_8);
            reemplazar(temporal);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar " + archivo.getFileName(), e);
        }
    }

    private void reemplazar(Path temporal) throws IOException {
        try {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path getArchivo() {
        return archivo;
    }

    /**
     * Crea una carpeta (y sus padres) si no existe.
     *
     * @param directorio carpeta a crear
     * @throws PersistenceException si el sistema operativo no lo permite
     */
    public static void crearDirectorio(Path directorio) {
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo crear la carpeta " + directorio, e);
        }
    }
}
