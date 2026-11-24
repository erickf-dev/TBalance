package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.CallableStatement;
import java.sql.Types;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Copias de seguridad dentro de Oracle: cada copia es una fila de la tabla
 * RESPALDO con el .zip en una columna BLOB. Se leen con FN_LISTAR_COPIAS y
 * FN_LEER_COPIA y se guardan con SP_GUARDAR_COPIA y SP_BORRAR_COPIA. Así,
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
        return bd.llamar("FN_LISTAR_COPIAS",
                fila -> new Backup(fila.getString("NOMBRE"),
                        fila.getTimestamp("FECHA").toLocalDateTime().format(FORMATO_FECHA), fila.getLong("TAMANO")));
    }

    @Override
    public boolean existe(String nombre) {
        validarNombre(nombre);
        return bd.llamarNumero("FN_EXISTE_COPIA", nombre) > 0;
    }

    @Override
    public Backup guardar(String nombre, Path zip) {
        validarNombre(nombre);
        try {
            byte[] contenido = Files.readAllBytes(zip);
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            bd.transaccion(c -> {
                Database.procedimiento(c, "SP_GUARDAR_COPIA",
                        nombre, Timestamp.valueOf(ahora), (long) contenido.length, new Database.Binario(contenido));
                return null;
            });
            Files.deleteIfExists(zip);
            return new Backup(nombre, ahora.format(FORMATO_FECHA), contenido.length);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer la copia " + nombre, e);
        }
    }

    @Override
    public void leer(String nombre, Path destino) {
        validarNombre(nombre);
        boolean encontrada = bd.transaccion(c -> !bd.llamar(c, "FN_LEER_COPIA", fila -> {
            try (InputStream entrada = fila.getBinaryStream("CONTENIDO")) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new SQLException("No se pudo escribir la copia en " + destino, e);
            }
            return true;
        }, nombre).isEmpty());
        if (!encontrada) {
            throw new ValidationException("La copia de seguridad " + nombre + " no existe");
        }
    }

    @Override
    public void eliminar(String nombre) {
        validarNombre(nombre);
        int borradas = bd.transaccion(c -> {
            try (CallableStatement llamada = c.prepareCall("{call SP_BORRAR_COPIA(?, ?)}")) {
                llamada.setString(1, nombre);
                llamada.registerOutParameter(2, Types.NUMERIC);
                llamada.execute();
                return llamada.getInt(2);
            }
        });
        if (borradas == 0) {
            throw new ValidationException("La copia de seguridad " + nombre + " no existe");
        }
    }

    @Override
    public String ubicacion() {
        return "la base de datos Oracle (tabla RESPALDO)";
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
