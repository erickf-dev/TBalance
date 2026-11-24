package sistemainventario.repository;

import sistemainventario.model.Backup;

import java.nio.file.Path;
import java.util.List;

/**
 * Dónde se guardan las copias de seguridad (archivos .zip con los datos en JSON).
 *
 * <p>Normalmente quedan dentro de Oracle, en la tabla RESPALDO
 * ({@link DatabaseBackupRepository}); la carpeta {@code respaldos}
 * ({@link BackupRepository}) solo se usa mientras no hay conexión.</p>
 */
public interface BackupStore {

    /** @return las copias, de la más reciente a la más antigua */
    List<Backup> listar();

    /**
     * @param nombre nombre de la copia (por ejemplo {@code TBalance_2026-11-17_21-00-00.zip})
     * @return {@code true} si ya hay una copia con ese nombre
     */
    boolean existe(String nombre);

    /**
     * Guarda una copia.
     *
     * @param nombre nombre de la copia
     * @param zip    archivo .zip ya creado (temporal)
     * @return la copia guardada
     */
    Backup guardar(String nombre, Path zip);

    /**
     * Deja el contenido de una copia en un archivo .zip para restaurarla.
     *
     * @param nombre  nombre de la copia
     * @param destino archivo temporal donde escribirla
     * @throws sistemainventario.exception.ValidationException si la copia no existe
     */
    void leer(String nombre, Path destino);

    /** @param nombre copia a borrar */
    void eliminar(String nombre);

    /** @return dónde están las copias, para mostrarlo ("C:\\...\\respaldos" o "la base de datos Oracle") */
    String ubicacion();

    /** @return la carpeta de las copias, o {@code null} si están en una base de datos */
    Path carpeta();
}
