package sistemainventario.repository;

import sistemainventario.model.DatabaseEngine;

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
public class OracleDialect implements SqlDialect {

    private static final Pattern INICIO_PLSQL = Pattern.compile(
            "^(CREATE\\s+(OR\\s+REPLACE\\s+)?(TRIGGER|PROCEDURE|FUNCTION|PACKAGE)|BEGIN|DECLARE)\\b.*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMANDO_SQLPLUS = Pattern.compile("^(SET|PROMPT|SPOOL)\\b.*", Pattern.CASE_INSENSITIVE);

    @Override
    public DatabaseEngine motor() {
        return DatabaseEngine.ORACLE;
    }

    @Override
    public String carpetaScripts() {
        return "sql/oracle";
    }

    @Override
    public String nombreDriver() {
        return "ojdbc11 (com.oracle.database.jdbc)";
    }

    @Override
    public boolean tablaExiste(Connection conexion, String tabla) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?")) {
            consulta.setString(1, tabla.toUpperCase(Locale.ROOT));
            try (ResultSet fila = consulta.executeQuery()) {
                return fila.next() && fila.getInt(1) > 0;
            }
        }
    }

    @Override
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

    @Override
    public Optional<String> mensajeDeDisparador(SQLException error) {
        int codigo = error.getErrorCode();
        if (codigo < 20000 || codigo > 20999) {
            return Optional.empty();
        }
        String mensaje = error.getMessage() == null ? "" : error.getMessage().split("\\R")[0];
        return Optional.of(mensaje.replaceFirst("^ORA-\\d{5}:\\s*", "").trim());
    }

    private static void agregar(List<String> sentencias, String texto) {
        if (!texto.isBlank()) {
            sentencias.add(texto.trim());
        }
    }
}
