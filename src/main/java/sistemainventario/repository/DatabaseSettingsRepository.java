package sistemainventario.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sistemainventario.exception.PersistenceException;
import sistemainventario.model.DatabaseSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guarda en {@code ~/.inventariopro/basedatos.json} cómo se conecta este
 * computador a Oracle (URL, usuario y clave).
 *
 * <p>Igual que la clave del asistente, está fuera de la carpeta {@code data}:
 * no entra en los respaldos ni en GitHub.</p>
 */
public class DatabaseSettingsRepository {

    private final Path archivo;

    /** @param archivo ruta del archivo de configuración */
    public DatabaseSettingsRepository(Path archivo) {
        this.archivo = archivo;
    }

    /** @return la configuración guardada o, si no hay, la de Oracle local sin clave */
    public DatabaseSettings leer() {
        if (!Files.isRegularFile(archivo)) {
            return DatabaseSettings.POR_DEFECTO;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(archivo, StandardCharsets.UTF_8)).getAsJsonObject();
            return new DatabaseSettings(texto(json, "url"), texto(json, "usuario"), texto(json, "clave"));
        } catch (IOException | RuntimeException e) {
            return DatabaseSettings.POR_DEFECTO;
        }
    }

    /**
     * @param config configuración a guardar
     * @throws PersistenceException si no se pudo escribir
     */
    public void guardar(DatabaseSettings config) {
        JsonObject json = new JsonObject();
        json.addProperty("motor", config.motor().name());
        json.addProperty("url", config.url());
        json.addProperty("usuario", config.usuario());
        json.addProperty("clave", config.clave());
        try {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            Files.writeString(archivo, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar la configuración de la base de datos", e);
        }
    }

    private static String texto(JsonObject json, String campo) {
        return json.has(campo) && !json.get(campo).isJsonNull() ? json.get(campo).getAsString() : "";
    }
}
