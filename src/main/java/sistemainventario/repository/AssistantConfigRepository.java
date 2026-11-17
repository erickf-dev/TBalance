package sistemainventario.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sistemainventario.exception.PersistenceException;
import sistemainventario.model.AssistantSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Guarda la clave y el modelo del asistente en {@code ~/.inventariopro/asistente.json}.
 *
 * <p>Está fuera de la carpeta {@code data}, así que no entra en los respaldos
 * ni en el repositorio de GitHub: cada integrante usa su propia clave.</p>
 */
public class AssistantConfigRepository {

    private final Path archivo;

    /** @param archivo ruta del archivo de configuración */
    public AssistantConfigRepository(Path archivo) {
        this.archivo = archivo;
    }

    /** @return la configuración guardada, si existe */
    public Optional<AssistantSettings> leer() {
        if (!Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(archivo, StandardCharsets.UTF_8)).getAsJsonObject();
            String clave = json.has("clave") ? json.get("clave").getAsString() : "";
            String modelo = json.has("modelo") ? json.get("modelo").getAsString() : "";
            return clave.isBlank() ? Optional.empty() : Optional.of(new AssistantSettings(clave, modelo));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * @param config configuración a guardar
     * @throws PersistenceException si no se pudo escribir
     */
    public void guardar(AssistantSettings config) {
        JsonObject json = new JsonObject();
        json.addProperty("clave", config.clave());
        json.addProperty("modelo", config.modelo());
        try {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            Files.writeString(archivo, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar la configuración del asistente", e);
        }
    }

    /** Borra la clave guardada. */
    public void borrar() {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo borrar la configuración del asistente", e);
        }
    }
}
