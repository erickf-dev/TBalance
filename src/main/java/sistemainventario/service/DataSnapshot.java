package sistemainventario.service;

import java.nio.file.Path;

/**
 * Cómo se sacan y se reponen los datos para las copias de seguridad.
 *
 * <p>Una copia siempre es un .zip con los datos en JSON. Con archivos, se
 * comprime la carpeta {@code data} ({@link FileSnapshot}); con base de datos,
 * las tablas se vuelcan a una carpeta temporal y al restaurar se cargan de
 * nuevo en la base ({@link DatabaseSnapshot}).</p>
 */
public interface DataSnapshot {

    /** @return carpeta con los datos en JSON, lista para comprimir */
    Path prepararCarpeta();

    /** @param carpeta la carpeta que devolvió {@link #prepararCarpeta()} (se borra si era temporal) */
    void liberarCarpeta(Path carpeta);

    /**
     * Reemplaza los datos actuales por los de una copia.
     *
     * @param carpeta carpeta con el contenido extraído del .zip
     */
    void restaurarDesde(Path carpeta);
}
