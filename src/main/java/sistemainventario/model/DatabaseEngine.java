package sistemainventario.model;

/**
 * Base de datos que usa TBalance: Oracle.
 *
 * <p>TBalance guarda todo en Oracle (18c Express Edition o superior). Las
 * consultas y los cambios se hacen con funciones y procedimientos almacenados
 * en PL/SQL; Java solo los llama.</p>
 */
public enum DatabaseEngine {

    /** Oracle 18c Express Edition o superior (base conectable XEPDB1). */
    ORACLE("Oracle", "jdbc:oracle:thin:@localhost:1521/XEPDB1");

    private final String etiqueta;
    private final String urlPorDefecto;

    DatabaseEngine(String etiqueta, String urlPorDefecto) {
        this.etiqueta = etiqueta;
        this.urlPorDefecto = urlPorDefecto;
    }

    /** @return nombre para mostrar */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** @return URL JDBC sugerida */
    public String getUrlPorDefecto() {
        return urlPorDefecto;
    }
}
