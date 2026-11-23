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

/**
 * Dialecto de PostgreSQL (11 o superior).
 *
 * <p>Los nombres sin comillas quedan en minúscula; las funciones PL/pgSQL van
 * entre "$$" y pueden tener ";" adentro. Los disparadores de TBalance lanzan
 * sus errores con el código {@code P0001} ({@code RAISE EXCEPTION}).</p>
 */
public class PostgresDialect implements SqlDialect {

    private static final String ERROR_DE_DISPARADOR = "P0001";

    @Override
    public DatabaseEngine motor() {
        return DatabaseEngine.POSTGRESQL;
    }

    @Override
    public String carpetaScripts() {
        return "sql/postgresql";
    }

    @Override
    public String nombreDriver() {
        return "postgresql (org.postgresql)";
    }

    @Override
    public boolean tablaExiste(Connection conexion, String tabla) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?")) {
            consulta.setString(1, tabla.toLowerCase(Locale.ROOT));
            try (ResultSet fila = consulta.executeQuery()) {
                return fila.next() && fila.getInt(1) > 0;
            }
        }
    }

    @Override
    public List<String> separarSentencias(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enFuncion = false;
        for (String linea : script.split("\\R")) {
            String limpia = linea.trim();
            if (!enFuncion && (limpia.isEmpty() || limpia.startsWith("--")) && actual.length() == 0) {
                continue;
            }
            if (!enFuncion && limpia.startsWith("--")) {
                continue;
            }
            actual.append(linea).append('\n');
            if (cantidadDeDolares(linea) % 2 == 1) {
                enFuncion = !enFuncion;
            }
            if (!enFuncion && limpia.endsWith(";")) {
                String texto = actual.toString().trim();
                sentencias.add(texto.substring(0, texto.length() - 1).trim());
                actual.setLength(0);
            }
        }
        if (!actual.toString().isBlank()) {
            sentencias.add(actual.toString().trim());
        }
        return sentencias;
    }

    @Override
    public Optional<String> mensajeDeDisparador(SQLException error) {
        if (!ERROR_DE_DISPARADOR.equals(error.getSQLState())) {
            return Optional.empty();
        }
        String mensaje = error.getMessage() == null ? "" : error.getMessage().split("\\R")[0];
        return Optional.of(mensaje.replaceFirst("^ERROR:\\s*", "").trim());
    }

    private static int cantidadDeDolares(String linea) {
        int cantidad = 0;
        int desde = 0;
        while ((desde = linea.indexOf("$$", desde)) >= 0) {
            cantidad++;
            desde += 2;
        }
        return cantidad;
    }
}
