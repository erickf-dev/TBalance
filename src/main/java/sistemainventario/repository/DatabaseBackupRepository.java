package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Copias de seguridad dentro de PostgreSQL u Oracle: cada copia es una fila de
 * la tabla RESPALDO con el .zip en una columna binaria (BYTEA o BLOB). Así,
 * con base de datos, las copias no quedan en archivos del computador.
 */
public class DatabaseBackupRepository implements BackupStore {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_\\-]+\\.zip");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Database bd;

    /** @param bd conexión a la base */
    public DatabaseBackupRepository(Database bd) {
        this.bd = Objects.requireNonNull(bd);
    }

    @Override
    public List<Backup> listar() {
        return bd.consultar("SELECT NOMBRE, FECHA, TAMANO FROM RESPALDO ORDER BY FECHA DESC",
                fila -> new Backup(fila.getString("NOMBRE"),
                        fila.getTimestamp("FECHA").toLocalDateTime().format(FORMATO_FECHA), fila.getLong("TAMANO")));
    }

    @Override
    public boolean existe(String nombre) {
        validarNombre(nombre);
        return bd.consultar("SELECT COUNT(*) FROM RESPALDO WHERE NOMBRE = ?", fila -> fila.getInt(1), nombre).get(0) > 0;
    }

    @Override
    public Backup guardar(String nombre, Path zip) {
        validarNombre(nombre);
        try {
            byte[] contenido = Files.readAllBytes(zip);
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            bd.transaccion(c -> Database.actualizar(c,
                    "INSERT INTO RESPALDO (NOMBRE, FECHA, TAMANO, CONTENIDO) VALUES (?, ?, ?, ?)",
                    nombre, Timestamp.valueOf(ahora), (long) contenido.length, new Database.Binario(contenido)));
            Files.deleteIfExists(zip);
            return new Backup(nombre, ahora.format(FORMATO_FECHA), contenido.length);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer la copia " + nombre, e);
        }
    }

    @Override
    public void leer(String nombre, Path destino) {
        validarNombre(nombre);
        boolean encontrada = bd.transaccion(c -> {
            try (PreparedStatement consulta = c.prepareStatement("SELECT CONTENIDO FROM RESPALDO WHERE NOMBRE = ?")) {
                consulta.setString(1, nombre);
                try (ResultSet fila = consulta.executeQuery()) {
                    if (!fila.next()) {
                        return false;
                    }
                    try (InputStream entrada = fila.getBinaryStream(1)) {
                        Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new SQLException("No se pudo escribir la copia en " + destino, e);
                    }
                    return true;
                }
            }
        });
        if (!encontrada) {
            throw new ValidationException("La copia de seguridad " + nombre + " no existe");
        }
    }

    @Override
    public void eliminar(String nombre) {
        validarNombre(nombre);
        int borradas = bd.transaccion(c -> Database.actualizar(c, "DELETE FROM RESPALDO WHERE NOMBRE = ?", nombre));
        if (borradas == 0) {
            throw new ValidationException("La copia de seguridad " + nombre + " no existe");
        }
    }

    @Override
    public String ubicacion() {
        return "la base de datos " + bd.nombreMotor() + " (tabla RESPALDO)";
    }

    @Override
    public Path carpeta() {
        return null;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new ValidationException("Nombre de copia de seguridad no valido");
        }
    }
}
