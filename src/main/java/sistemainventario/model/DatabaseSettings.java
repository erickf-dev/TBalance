package sistemainventario.model;

import java.util.Objects;

/**
 * Conexión a la base de datos elegida en este computador.
 *
 * @param motor   JSON, PostgreSQL u Oracle
 * @param url     URL JDBC (vacía en modo JSON)
 * @param usuario usuario de la base de datos
 * @param clave   clave del usuario (solo se guarda en este computador)
 */
public record DatabaseSettings(DatabaseEngine motor, String url, String usuario, String clave) {

    /** Configuración por defecto: archivos JSON. */
    public static final DatabaseSettings ARCHIVOS = new DatabaseSettings(DatabaseEngine.JSON, "", "", "");

    /** Normaliza los textos nulos a vacíos. */
    public DatabaseSettings {
        motor = Objects.requireNonNullElse(motor, DatabaseEngine.JSON);
        url = url == null ? "" : url.trim();
        usuario = usuario == null ? "" : usuario.trim();
        clave = clave == null ? "" : clave;
    }

    /** @return {@code true} si los datos van a PostgreSQL u Oracle */
    public boolean usaBaseDeDatos() {
        return motor.esRelacional();
    }
}
