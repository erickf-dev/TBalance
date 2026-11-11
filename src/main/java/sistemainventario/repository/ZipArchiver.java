package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Comprime una carpeta en un archivo .zip y la extrae de nuevo.
 *
 * <p><b>SRP:</b> solo sabe manejar archivos .zip; no conoce perfiles ni reglas
 * de negocio. Lo usa {@link BackupRepository} a través del servicio de
 * respaldos.</p>
 *
 * <p>La extracción rechaza entradas que intenten escribir fuera de la carpeta
 * destino (ataque conocido como <i>zip slip</i>).</p>
 */
public class ZipArchiver {

    /**
     * Comprime todo el contenido de una carpeta.
     *
     * @param carpeta carpeta de origen
     * @param destino archivo .zip a crear
     * @throws PersistenceException si no se puede leer o escribir
     */
    public void comprimir(Path carpeta, Path destino) {
        try (OutputStream salida = Files.newOutputStream(destino);
             ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Path archivo : listarArchivos(carpeta)) {
                zip.putNextEntry(new ZipEntry(nombreEntrada(carpeta, archivo)));
                Files.copy(archivo, zip);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo crear la copia de seguridad", e);
        }
    }

    /**
     * Extrae un .zip dentro de una carpeta (que se crea si no existe).
     *
     * @param archivoZip archivo de origen
     * @param carpeta    carpeta destino
     * @return cantidad de archivos extraídos
     * @throws PersistenceException si el archivo está dañado o es inseguro
     */
    public int extraer(Path archivoZip, Path carpeta) {
        Path raiz = carpeta.toAbsolutePath().normalize();
        int extraidos = 0;
        try (InputStream entrada = Files.newInputStream(archivoZip);
             ZipInputStream zip = new ZipInputStream(entrada)) {
            JsonFileStore.crearDirectorio(raiz);
            ZipEntry entradaZip;
            while ((entradaZip = zip.getNextEntry()) != null) {
                Path destino = raiz.resolve(entradaZip.getName()).normalize();
                if (!destino.startsWith(raiz)) {
                    throw new PersistenceException("El archivo de respaldo contiene rutas no permitidas", null);
                }
                if (entradaZip.isDirectory()) {
                    JsonFileStore.crearDirectorio(destino);
                } else {
                    JsonFileStore.crearDirectorio(destino.getParent());
                    Files.copy(zip, destino);
                    extraidos++;
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new PersistenceException("El archivo de respaldo esta danado o no se puede leer", e);
        }
        return extraidos;
    }

    private List<Path> listarArchivos(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) {
            return new ArrayList<>();
        }
        try (Stream<Path> rutas = Files.walk(carpeta)) {
            return rutas.filter(Files::isRegularFile).sorted().toList();
        }
    }

    /** Los nombres dentro del zip siempre usan '/' para que funcionen en Windows, Mac y Linux. */
    private String nombreEntrada(Path carpeta, Path archivo) {
        return carpeta.relativize(archivo).toString().replace('\\', '/');
    }
}
