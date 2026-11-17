package sistemainventario.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Abre la interfaz en una ventana de aplicación de Microsoft Edge (o de Google
 * Chrome si no hay Edge): una ventana propia, sin pestañas ni barra de
 * direcciones, que dibuja la página con la tarjeta gráfica.
 *
 * <p>Usa una carpeta de perfil propia del programa para no mezclarse con el
 * navegador personal del usuario y para que cerrar la ventana cierre ese
 * proceso.</p>
 */
public class BrowserWindow {

    private static final int ANCHO = 1300;
    private static final int ALTO = 800;
    /** Cambiar este valor cuando cambie el ícono, para que Edge olvide el anterior. */
    private static final String VERSION_ICONO = "3";

    private final Path carpetaPerfil;
    private Process proceso;

    /**
     * @param carpetaPerfil carpeta donde el navegador guarda su perfil para TBalance
     */
    public BrowserWindow(Path carpetaPerfil) {
        this.carpetaPerfil = carpetaPerfil;
    }

    /**
     * @return ruta del ejecutable de Edge o Chrome, si está instalado
     */
    public static Optional<String> buscarNavegador() {
        List<String> rutas = new ArrayList<>();
        String programas = System.getenv("ProgramFiles");
        String programas86 = System.getenv("ProgramFiles(x86)");
        String local = System.getenv("LOCALAPPDATA");
        for (String base : new String[] {programas86, programas, local}) {
            if (base != null) {
                rutas.add(base + "\\Microsoft\\Edge\\Application\\msedge.exe");
            }
        }
        for (String base : new String[] {programas, programas86, local}) {
            if (base != null) {
                rutas.add(base + "\\Google\\Chrome\\Application\\chrome.exe");
            }
        }
        // macOS y Linux, por si alguien del equipo no usa Windows
        rutas.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
        rutas.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        rutas.add("/usr/bin/microsoft-edge");
        rutas.add("/usr/bin/google-chrome");
        rutas.add("/usr/bin/chromium");
        return rutas.stream().filter(r -> new File(r).canExecute()).findFirst();
    }

    /**
     * Abre la ventana con la dirección indicada.
     *
     * @param navegador ruta del ejecutable
     * @param url       dirección de la interfaz
     * @throws IOException si no se pudo iniciar el navegador
     */
    public void abrir(String navegador, String url) throws IOException {
        limpiarIconoGuardado();
        List<String> comando = List.of(
                navegador,
                "--app=" + url,
                "--user-data-dir=" + carpetaPerfil.toAbsolutePath(),
                "--window-size=" + ANCHO + "," + ALTO,
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-background-mode",
                "--disable-features=Translate,msEdgeStartupBoost",
                "--disable-sync",
                "--disable-extensions");
        proceso = new ProcessBuilder(comando).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    }

    /**
     * Edge guarda el ícono de cada página. Cuando cambia el ícono de TBalance se
     * borra esa memoria (solo la de la carpeta propia de TBalance, nunca la del
     * Edge personal) para que la barra de tareas muestre el nuevo.
     */
    private void limpiarIconoGuardado() {
        Path marca = carpetaPerfil.resolve("icono-" + VERSION_ICONO);
        if (Files.exists(marca)) {
            return;
        }
        try {
            Path predeterminado = carpetaPerfil.resolve("Default");
            if (Files.isDirectory(predeterminado)) {
                try (var archivos = Files.list(predeterminado)) {
                    for (Path p : archivos.filter(f -> f.getFileName().toString().startsWith("Favicons")).toList()) {
                        Files.deleteIfExists(p);
                    }
                }
            }
            Files.createDirectories(carpetaPerfil);
            Files.writeString(marca, "ícono de la ventana actualizado");
        } catch (IOException | RuntimeException e) {
            System.err.println("[TBalance] No se pudo actualizar el ícono guardado: " + e.getMessage());
        }
    }

    /** @return el proceso del navegador, o {@code null} si no se ha abierto */
    public Process getProceso() {
        return proceso;
    }

    /** Cierra la ventana si sigue abierta. */
    public void cerrar() {
        if (proceso != null && proceso.isAlive()) {
            proceso.descendants().forEach(ProcessHandle::destroy);
            proceso.destroy();
        }
    }
}
