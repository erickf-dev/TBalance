package sistemainventario.repository;

import sistemainventario.model.DatabaseEngine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Lo que cambia entre PostgreSQL y Oracle.
 *
 * <p>Las consultas de los repositorios son SQL estándar y sirven para los dos
 * motores; aquí quedan solo las diferencias: dónde están los scripts, cómo se
 * pregunta si una tabla existe, cómo se separan las sentencias de un script
 * (los bloques PL/SQL de Oracle terminan en "/" y las funciones de PostgreSQL
 * van entre "$$") y cómo llega el mensaje de un disparador.</p>
 */
public interface SqlDialect {

    /** @return motor al que corresponde este dialecto */
    DatabaseEngine motor();

    /** @return carpeta de los scripts dentro de los recursos (por ejemplo {@code sql/oracle}) */
    String carpetaScripts();

    /** @return nombre del driver JDBC, para los mensajes de error */
    String nombreDriver();

    /**
     * @param conexion conexión abierta
     * @param tabla    nombre de la tabla
     * @return {@code true} si la tabla existe en el esquema del usuario
     * @throws SQLException si falla la consulta al diccionario de datos
     */
    boolean tablaExiste(Connection conexion, String tabla) throws SQLException;

    /**
     * @param script contenido de un archivo .sql
     * @return sentencias listas para enviar una por una por JDBC
     */
    List<String> separarSentencias(String script);

    /**
     * @param error error devuelto por la base
     * @return el mensaje para el usuario si el error lo lanzó un disparador de TBalance
     */
    Optional<String> mensajeDeDisparador(SQLException error);

    /**
     * @param motor PostgreSQL u Oracle
     * @return el dialecto de ese motor
     * @throws IllegalArgumentException si el motor no es relacional
     */
    static SqlDialect para(DatabaseEngine motor) {
        return switch (motor) {
            case POSTGRESQL -> new PostgresDialect();
            case ORACLE -> new OracleDialect();
            case JSON -> throw new IllegalArgumentException("El modo JSON no usa SQL");
        };
    }
}
