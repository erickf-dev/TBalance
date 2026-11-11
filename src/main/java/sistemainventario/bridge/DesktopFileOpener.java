package sistemainventario.bridge;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Implementación de {@link FileOpener} con {@link Desktop}.
 * Abre el archivo en un hilo aparte para no bloquear la interfaz.
 */
public class DesktopFileOpener implements FileOpener {

    @Override
    public void abrir(Path ruta) {
        Thread hilo = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(ruta.toFile());
                }
            } catch (IOException | UnsupportedOperationException e) {
                System.err.println("[Etiquetas] No se pudo abrir " + ruta + ": " + e.getMessage());
            }
        }, "abrir-archivo");
        hilo.setDaemon(true);
        hilo.start();
    }
}
