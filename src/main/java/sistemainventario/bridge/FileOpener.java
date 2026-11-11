package sistemainventario.bridge;

import java.nio.file.Path;

/**
 * Abre un archivo o carpeta con el programa predeterminado del sistema
 * operativo (visor de imágenes, explorador de archivos...).
 *
 * <p><b>Bajo acoplamiento / DIP:</b> los controladores dependen de esta
 * interfaz y no de {@code java.awt.Desktop}, lo que permite probarlos sin
 * entorno gráfico.</p>
 */
@FunctionalInterface
public interface FileOpener {

    /**
     * @param ruta archivo o carpeta a abrir
     */
    void abrir(Path ruta);
}
