package sistemainventario.model;

import java.util.Locale;

/**
 * Dónde guarda TBalance los datos de los negocios.
 *
 * <p>{@link #JSON} es el modo de siempre (archivos en el computador).
 * {@link #POSTGRESQL} y {@link #ORACLE} guardan en una base de datos
 * relacional a través de JDBC.</p>
 */
public enum DatabaseEngine {

    /** Archivos JSON en {@code ~/.inventariopro/data}. */
    JSON("Archivos de este computador", ""),
    /** PostgreSQL 11 o superior. */
    POSTGRESQL("PostgreSQL", "jdbc:postgresql://localhost:5432/tbalance"),
    /** Oracle 18c Express Edition o superior (base conectable XEPDB1). */
    ORACLE("Oracle", "jdbc:oracle:thin:@localhost:1521/XEPDB1");

    private final String etiqueta;
    private final String urlPorDefecto;

    DatabaseEngine(String etiqueta, String urlPorDefecto) {
        this.etiqueta = etiqueta;
        this.urlPorDefecto = urlPorDefecto;
    }

    /** @return nombre para mostrar en la interfaz */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** @return URL JDBC sugerida para una instalación local */
    public String getUrlPorDefecto() {
        return urlPorDefecto;
    }

    /** @return {@code true} si el motor es una base de datos relacional */
    public boolean esRelacional() {
        return this != JSON;
    }

    /**
     * @param nombre nombre del motor (sin importar mayúsculas); vacío o desconocido es JSON
     * @return el motor correspondiente
     */
    public static DatabaseEngine desde(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return JSON;
        }
        try {
            return valueOf(nombre.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return JSON;
        }
    }
}
