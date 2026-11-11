package sistemainventario.service;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;
import sistemainventario.repository.ZipArchiver;
import sistemainventario.repository.GlobalConfigRepository;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.repository.ProfileRepository;
import sistemainventario.repository.BackupRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Casos de uso de copias de seguridad: crear, listar, restaurar y eliminar.
 *
 * <p><b>SRP:</b> solo coordina el respaldo de la carpeta de datos; la
 * compresión la hace {@link ZipArchiver} y el manejo de archivos .zip
 * {@link BackupRepository}. <b>Bajo acoplamiento:</b> recibe sus
 * dependencias por constructor.</p>
 *
 * <p>Antes de restaurar se crea automáticamente una copia del estado actual,
 * para que una restauración equivocada siempre se pueda deshacer.</p>
 */
public class BackupService {

    public static final String PREFIJO = "TBalance";
    public static final String PREFIJO_SEGURIDAD = "TBalance_antes-de-restaurar";
    private static final DateTimeFormatter FORMATO_NOMBRE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path directorioDatos;
    private final BackupRepository respaldos;
    private final ZipArchiver archivador;

    public BackupService(Path directorioDatos, BackupRepository respaldos, ZipArchiver archivador) {
        this.directorioDatos = Objects.requireNonNull(directorioDatos);
        this.respaldos = Objects.requireNonNull(respaldos);
        this.archivador = Objects.requireNonNull(archivador);
    }

    /** @return respaldos guardados, del más reciente al más antiguo */
    public List<Backup> listar() {
        return respaldos.listar();
    }

    /**
     * Comprime toda la carpeta de datos (perfiles, productos, ventas...).
     *
     * @return el respaldo creado
     */
    public Backup crear() {
        return crearConPrefijo(PREFIJO);
    }

    /**
     * Reemplaza los datos actuales por los del respaldo indicado.
     * Antes guarda una copia automática del estado actual.
     *
     * @param nombre archivo .zip a restaurar
     * @return la copia automática creada antes de restaurar
     * @throws ValidationException   si el archivo no existe o no es un respaldo de TBalance
     * @throws PersistenceException si falla la lectura o escritura
     */
    public Backup restaurar(String nombre) {
        Path archivo = respaldos.rutaExistente(nombre);
        Path temporal = crearCarpetaTemporal();
        try {
            archivador.extraer(archivo, temporal);
            validarContenido(temporal);
            Backup copiaPrevia = crearConPrefijo(PREFIJO_SEGURIDAD);
            reemplazarDatos(temporal);
            return copiaPrevia;
        } finally {
            borrarCarpeta(temporal);
        }
    }

    /**
     * @param nombre archivo .zip a eliminar
     */
    public void eliminar(String nombre) {
        respaldos.eliminar(nombre);
    }

    /** @return ruta de la carpeta donde se guardan los respaldos */
    public Path getCarpetaRespaldos() {
        return respaldos.getCarpeta();
    }

    private Backup crearConPrefijo(String prefijo) {
        Path destino = nombreDisponible(prefijo);
        archivador.comprimir(directorioDatos, destino);
        return respaldos.describir(destino);
    }

    /** Evita sobrescribir un respaldo creado en el mismo segundo. */
    private Path nombreDisponible(String prefijo) {
        String base = prefijo + "_" + LocalDateTime.now().format(FORMATO_NOMBRE);
        Path destino = respaldos.rutaNueva(base + ".zip");
        for (int n = 2; Files.exists(destino); n++) {
            destino = respaldos.rutaNueva(base + "-" + n + ".zip");
        }
        return destino;
    }

    private void validarContenido(Path carpeta) {
        boolean tienePerfiles = Files.isRegularFile(carpeta.resolve(ProfileRepository.ARCHIVO));
        boolean tieneConfiguracion = Files.isRegularFile(carpeta.resolve(GlobalConfigRepository.ARCHIVO));
        if (!tienePerfiles && !tieneConfiguracion) {
            throw new ValidationException("El archivo no es una copia de seguridad de TBalance");
        }
    }

    private void reemplazarDatos(Path origen) {
        borrarCarpeta(directorioDatos);
        JsonFileStore.crearDirectorio(directorioDatos);
        try (Stream<Path> rutas = Files.walk(origen)) {
            for (Path ruta : rutas.toList()) {
                Path destino = directorioDatos.resolve(origen.relativize(ruta).toString());
                if (Files.isDirectory(ruta)) {
                    JsonFileStore.crearDirectorio(destino);
                } else {
                    Files.copy(ruta, destino);
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudieron restaurar los datos", e);
        }
    }

    private Path crearCarpetaTemporal() {
        try {
            return Files.createTempDirectory(respaldos.getCarpeta(), "restaurando-");
        } catch (IOException e) {
            throw new PersistenceException("No se pudo preparar la restauracion", e);
        }
    }

    private void borrarCarpeta(Path carpeta) {
        if (!Files.exists(carpeta)) {
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
}
