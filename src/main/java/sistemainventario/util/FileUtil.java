package sistemainventario.util;

import sistemainventario.exception.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Operaciones con carpetas que usan las copias de seguridad. */
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * @param prefijo inicio del nombre de la carpeta
     * @return una carpeta temporal nueva
     */
    public static Path carpetaTemporal(String prefijo) {
        try {
            return Files.createTempDirectory(prefijo);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo crear una carpeta temporal", e);
        }
    }

    /** @param carpeta carpeta a borrar con todo su contenido (si no existe, no hace nada) */
    public static void borrarCarpeta(Path carpeta) {
        if (carpeta == null || !Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> rutas = Files.walk(carpeta)) {
            for (Path ruta : rutas.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo limpiar la carpeta " + carpeta.getFileName(), e);
        }
    }

    /**
     * @param origen  carpeta a copiar
     * @param destino carpeta donde quedan los archivos (debe existir)
     */
    public static void copiarCarpeta(Path origen, Path destino) {
        try (Stream<Path> rutas = Files.walk(origen)) {
            for (Path ruta : rutas.toList()) {
                Path copia = destino.resolve(origen.relativize(ruta).toString());
                if (Files.isDirectory(ruta)) {
                    Files.createDirectories(copia);
                } else {
                    Files.copy(ruta, copia);
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudieron copiar los datos", e);
        }
    }
}
