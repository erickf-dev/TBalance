package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Administra los archivos de copia de seguridad guardados en
 * {@code ~/.inventariopro/respaldos/}.
 *
 * <p>Valida los nombres recibidos desde la vista para que solo se pueda
 * acceder a archivos .zip de esa carpeta.</p>
 */
public class BackupRepository {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_\\-]+\\.zip");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path carpeta;

    /**
     * @param carpeta carpeta donde se guardan los respaldos (se crea si no existe)
     */
    public BackupRepository(Path carpeta) {
        this.carpeta = carpeta;
        JsonFileStore.crearDirectorio(carpeta);
    }

    /** @return respaldos existentes, del más reciente al más antiguo */
    public List<Backup> listar() {
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos
                    .filter(archivo -> NOMBRE_VALIDO.matcher(archivo.getFileName().toString()).matches())
                    .map(this::describir)
                    .sorted(Comparator.comparing(Backup::getFecha).reversed())
                    .toList();
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer la carpeta de respaldos", e);
        }
    }

    /**
     * @param nombre nombre del archivo
     * @return ruta del respaldo existente
     * @throws ValidationException si el nombre no es válido o el archivo no existe
     */
    public Path rutaExistente(String nombre) {
        Path ruta = rutaNueva(nombre);
        if (!Files.isRegularFile(ruta)) {
            throw new ValidationException("La copia de seguridad " + nombre + " no existe");
        }
        return ruta;
    }

    /**
     * @param nombre nombre del archivo a crear
     * @return ruta dentro de la carpeta de respaldos
     * @throws ValidationException si el nombre no es válido
     */
    public Path rutaNueva(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new ValidationException("Nombre de copia de seguridad no valido");
        }
        return carpeta.resolve(nombre);
    }

    /**
     * @param ruta archivo .zip
     * @return la información del respaldo
     */
    public Backup describir(Path ruta) {
        try {
            FileTime modificado = Files.getLastModifiedTime(ruta);
            String fecha = LocalDateTime.ofInstant(modificado.toInstant(), ZoneId.systemDefault()).format(FORMATO_FECHA);
            return new Backup(ruta.getFileName().toString(), fecha, Files.size(ruta));
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer " + ruta.getFileName(), e);
        }
    }

    /**
     * @param nombre archivo a eliminar
     * @throws ValidationException si no existe
     */
    public void eliminar(String nombre) {
        try {
            Files.delete(rutaExistente(nombre));
        } catch (IOException e) {
            throw new PersistenceException("No se pudo eliminar la copia " + nombre, e);
        }
    }

    public Path getCarpeta() {
        return carpeta;
    }
}
