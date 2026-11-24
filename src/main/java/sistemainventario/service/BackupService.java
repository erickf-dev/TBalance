package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;
import sistemainventario.repository.BackupStore;
import sistemainventario.repository.GlobalConfigRepository;
import sistemainventario.repository.ProfileRepository;
import sistemainventario.repository.ZipArchiver;
import sistemainventario.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Casos de uso de copias de seguridad: crear, listar, restaurar y eliminar.
 *
 * <p><b>SRP:</b> solo coordina el respaldo; la compresión la hace
 * {@link ZipArchiver}, sacar y reponer los datos {@link DataSnapshot} y guardar
 * las copias {@link BackupStore} (tabla RESPALDO dentro de Oracle, o la
 * carpeta {@code respaldos} mientras no hay conexión). <b>OCP:</b> un nuevo lugar para las copias es
 * otra implementación de {@link BackupStore}, sin tocar este servicio.</p>
 *
 * <p>Antes de restaurar se crea automáticamente una copia del estado actual,
 * para que una restauración equivocada siempre se pueda deshacer.</p>
 */
public class BackupService {

    public static final String PREFIJO = "TBalance";
    public static final String PREFIJO_SEGURIDAD = "TBalance_antes-de-restaurar";
    private static final DateTimeFormatter FORMATO_NOMBRE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ZipArchiver archivador;
    private BackupStore almacen;
    private DataSnapshot datos;

    /**
     * @param almacen    dónde se guardan las copias (carpeta o base de datos)
     * @param datos      cómo se sacan y reponen los datos
     * @param archivador compresor .zip
     */
    public BackupService(BackupStore almacen, DataSnapshot datos, ZipArchiver archivador) {
        this.almacen = Objects.requireNonNull(almacen);
        this.datos = Objects.requireNonNull(datos);
        this.archivador = Objects.requireNonNull(archivador);
    }

    /**
     * Cambia el destino de las copias al cambiar de almacenamiento: con base de
     * datos las copias quedan en la misma base; con archivos, en la carpeta.
     *
     * @param almacen dónde se guardan las copias
     * @param datos   cómo se sacan y reponen los datos
     */
    public void usar(BackupStore almacen, DataSnapshot datos) {
        this.almacen = Objects.requireNonNull(almacen);
        this.datos = Objects.requireNonNull(datos);
    }

    /** @return copias guardadas, de la más reciente a la más antigua */
    public List<Backup> listar() {
        return almacen.listar();
    }

    /** @return la copia creada */
    public Backup crear() {
        return crearConPrefijo(PREFIJO);
    }

    /**
     * Restaura una copia. Antes guarda una copia de seguridad del estado actual.
     *
     * @param nombre copia a restaurar
     * @return la copia de seguridad que se hizo antes de restaurar
     */
    public Backup restaurar(String nombre) {
        Path temporal = FileUtil.carpetaTemporal("tbalance-restaurar-");
        try {
            Path zip = temporal.resolve("copia.zip");
            almacen.leer(nombre, zip);
            Path contenido = temporal.resolve("datos");
            archivador.extraer(zip, contenido);
            validarContenido(contenido);
            Backup copiaPrevia = crearConPrefijo(PREFIJO_SEGURIDAD);
            datos.restaurarDesde(contenido);
            return copiaPrevia;
        } finally {
            FileUtil.borrarCarpeta(temporal);
        }
    }

    /** @param nombre copia a borrar */
    public void eliminar(String nombre) {
        almacen.eliminar(nombre);
    }

    /** @return la carpeta de las copias, o {@code null} si están en la base de datos */
    public Path getCarpetaRespaldos() {
        return almacen.carpeta();
    }

    /** @return dónde están las copias, para mostrarlo */
    public String getUbicacion() {
        return almacen.ubicacion();
    }

    private Backup crearConPrefijo(String prefijo) {
        String nombre = nombreDisponible(prefijo);
        Path carpeta = datos.prepararCarpeta();
        Path temporal = FileUtil.carpetaTemporal("tbalance-zip-");
        try {
            Path zip = temporal.resolve(nombre);
            archivador.comprimir(carpeta, zip);
            return almacen.guardar(nombre, zip);
        } finally {
            datos.liberarCarpeta(carpeta);
            FileUtil.borrarCarpeta(temporal);
        }
    }

    private String nombreDisponible(String prefijo) {
        String base = prefijo + "_" + LocalDateTime.now().format(FORMATO_NOMBRE);
        String nombre = base + ".zip";
        for (int n = 2; almacen.existe(nombre); n++) {
            nombre = base + "-" + n + ".zip";
        }
        return nombre;
    }

    private void validarContenido(Path carpeta) {
        boolean tienePerfiles = Files.isRegularFile(carpeta.resolve(ProfileRepository.ARCHIVO));
        boolean tieneConfiguracion = Files.isRegularFile(carpeta.resolve(GlobalConfigRepository.ARCHIVO));
        if (!tienePerfiles && !tieneConfiguracion) {
            throw new ValidationException("El archivo no es una copia de seguridad de TBalance");
        }
    }
}
