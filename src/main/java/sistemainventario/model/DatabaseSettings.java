package sistemainventario.model;

import java.util.Objects;

/**
 * Conexión a Oracle de este computador.
 *
 * @param motor   siempre {@link DatabaseEngine#ORACLE}
 * @param url     URL JDBC, por ejemplo {@code jdbc:oracle:thin:@localhost:1521/XEPDB1}
 * @param usuario usuario de Oracle (normalmente TBALANCE)
 * @param clave   clave del usuario (solo se guarda en este computador)
 */
public record DatabaseSettings(DatabaseEngine motor, String url, String usuario, String clave) {

    /** Lo que se propone antes de conectar por primera vez: Oracle local, usuario TBALANCE. */
    public static final DatabaseSettings POR_DEFECTO =
            new DatabaseSettings(DatabaseEngine.ORACLE, DatabaseEngine.ORACLE.getUrlPorDefecto(), "TBALANCE", "");

    /** Normaliza los textos; el motor es siempre Oracle. */
    public DatabaseSettings {
        motor = DatabaseEngine.ORACLE;
        url = url == null ? "" : url.trim();
        usuario = usuario == null ? "" : usuario.trim();
        clave = clave == null ? "" : clave;
    }

    /**
     * @param url     URL JDBC
     * @param usuario usuario de Oracle
     * @param clave   clave
     */
    public DatabaseSettings(String url, String usuario, String clave) {
        this(DatabaseEngine.ORACLE, url, usuario, clave);
    }

    /** @return {@code true} si ya se escribieron los datos de conexión en este computador */
    public boolean estaConfigurada() {
        return !url.isBlank() && !usuario.isBlank() && !Objects.equals(clave, "");
    }
}
