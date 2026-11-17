package sistemainventario.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro de errores del asistente en {@code ~/.inventariopro/asistente.log},
 * para poder ver qué pasó si no responde. Nunca guarda la clave.
 */
public final class AssistantLog {

    private static final Path ARCHIVO = Path.of(System.getProperty("user.home"), ".inventariopro", "asistente.log");
    private static final long TAMANO_MAXIMO = 200_000;

    private AssistantLog() {
    }

    /** @param mensaje línea a registrar */
    public static synchronized void registrar(String mensaje) {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            if (Files.exists(ARCHIVO) && Files.size(ARCHIVO) > TAMANO_MAXIMO) {
                Files.delete(ARCHIVO);
            }
            String linea = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + "  " + mensaje + System.lineSeparator();
            Files.writeString(ARCHIVO, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignorado) {
            // el registro es solo de ayuda; si falla no debe afectar al asistente
        }
    }

    /** @return ruta del archivo de registro */
    public static Path archivo() {
        return ARCHIVO;
    }
}
