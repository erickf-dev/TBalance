package sistemainventario.repository;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Dialecto de Oracle (18c Express Edition o superior).
 *
 * <p>Separa los scripts igual que SQL Developer con F5: las sentencias normales
 * terminan en ";" y los bloques PL/SQL (CREATE TRIGGER, BEGIN...) terminan en
 * una línea con "/". Los errores de los disparadores usan los códigos
 * ORA-20000 a ORA-20999 de {@code RAISE_APPLICATION_ERROR}.</p>
 */
public class OracleDialect {

    private static final Pattern INICIO_PLSQL = Pattern.compile(
            "^(CREATE\\s+(OR\\s+REPLACE\\s+)?(TRIGGER|PROCEDURE|FUNCTION|PACKAGE)|BEGIN|DECLARE)\\b.*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMANDO_SQLPLUS = Pattern.compile("^(SET|PROMPT|SPOOL)\\b.*", Pattern.CASE_INSENSITIVE);

    /**
     * @param conexion conexión abierta
     * @param tabla    nombre de la tabla
     * @return {@code true} si la tabla existe en el esquema del usuario (USER_TABLES)
     * @throws SQLException si falla la consulta al diccionario de datos
     */
    public boolean tablaExiste(Connection conexion, String tabla) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?")) {
            consulta.setString(1, tabla.toUpperCase(Locale.ROOT));
            try (ResultSet fila = consulta.executeQuery()) {
                return fila.next() && fila.getInt(1) > 0;
            }
        }
    }

    /**
     * @param script texto de un archivo .sql
     * @return sentencias listas para ejecutar, sin ";" ni "/" al final
     */
    public List<String> separarSentencias(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enBloque = false;
        for (String linea : script.split("\\R")) {
            String limpia = linea.trim();
            if (enBloque) {
                if (limpia.equals("/")) {
                    agregar(sentencias, actual.toString());
                    actual.setLength(0);
                    enBloque = false;
                } else {
                    actual.append(linea).append('\n');
                }
                continue;
            }
            if (actual.length() == 0) {
                if (limpia.isEmpty() || limpia.startsWith("--") || limpia.equals("/")
                        || COMANDO_SQLPLUS.matcher(limpia).matches()) {
                    continue;
                }
                if (INICIO_PLSQL.matcher(limpia).matches()) {
                    enBloque = true;
                    actual.append(linea).append('\n');
                    continue;
                }
            } else if (limpia.startsWith("--")) {
                continue;
            }
            actual.append(linea).append('\n');
            if (limpia.endsWith(";")) {
                String texto = actual.toString().trim();
                agregar(sentencias, texto.substring(0, texto.length() - 1));
                actual.setLength(0);
            }
        }
        agregar(sentencias, actual.toString());
        return sentencias;
    }

    /**
     * @param error error de JDBC
     * @return el mensaje de RAISE_APPLICATION_ERROR (códigos -20000 a -20999), si lo es
     */
    public Optional<String> mensajeDeDisparador(SQLException error) {
        int codigo = error.getErrorCode();
        if (codigo < 20000 || codigo > 20999) {
            return Optional.empty();
        }
        String mensaje = error.getMessage() == null ? "" : error.getMessage().split("\\R")[0];
        return Optional.of(mensaje.replaceFirst("^ORA-\\d{5}:\\s*", "").trim());
    }

    /**
     * Revisa si alguna función o procedimiento de TBalance quedó con errores (USER_ERRORS).
     *
     * @param conexion conexión abierta
     * @return el primer error, para mostrarlo; vacío si todo está bien
     * @throws SQLException si falla la consulta al diccionario de datos
     */
    public Optional<String> erroresDeRutinas(Connection conexion) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT NAME, LINE, TEXT FROM USER_ERRORS WHERE TYPE IN ('FUNCTION', 'PROCEDURE') "
                        + "AND (NAME LIKE 'FN\\_%' ESCAPE '\\' OR NAME LIKE 'SP\\_%' ESCAPE '\\') "
                        + "AND ATTRIBUTE = 'ERROR' ORDER BY NAME, SEQUENCE")) {
            try (ResultSet fila = consulta.executeQuery()) {
                if (fila.next()) {
                    return Optional.of("En Oracle, " + fila.getString("NAME") + " quedó con un error en la línea "
                            + fila.getInt("LINE") + ": " + fila.getString("TEXT").trim()
                            + " Envíe este mensaje a quien mantiene TBalance.");
                }
            }
        }
        return Optional.empty();
    }

    private static void agregar(List<String> sentencias, String texto) {
        if (!texto.isBlank()) {
            sentencias.add(texto.trim());
        }
    }
}
