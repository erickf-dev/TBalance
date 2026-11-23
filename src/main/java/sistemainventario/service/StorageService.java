package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.DatabaseSettings;
import sistemainventario.repository.BackupStore;
import sistemainventario.repository.Database;
import sistemainventario.repository.DatabaseBackupRepository;
import sistemainventario.repository.DatabaseSettingsRepository;
import sistemainventario.repository.DatabaseStorageProvider;
import sistemainventario.repository.JsonStorageProvider;
import sistemainventario.repository.StorageProvider;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Decide dónde se guardan los datos (archivos JSON, PostgreSQL u Oracle) y
 * prepara ese almacenamiento.
 *
 * <p>La primera vez que se conecta a una base vacía crea las tablas y los
 * disparadores y copia ahí los negocios de los archivos JSON. Si la base no
 * responde al iniciar, TBalance sigue con los archivos y muestra un aviso, en
 * vez de no abrir.</p>
 */
public class StorageService {

    private final DatabaseSettingsRepository ajustes;
    private final Path directorioDatos;
    private final DataTransferService copiador;

    /**
     * Almacenamiento listo para usar.
     *
     * @param almacen fábrica de repositorios
     * @param copias    cómo se sacan y reponen los datos en las copias de seguridad
     * @param respaldos dónde se guardan las copias si es una base de datos ({@code null}: en la carpeta)
     * @param aviso     mensaje para el usuario (migración hecha o error de conexión), o {@code null}
     */
    public record Apertura(StorageProvider almacen, DataSnapshot copias, BackupStore respaldos, String aviso) {
    }

    /**
     * @param ajustes         archivo {@code basedatos.json}
     * @param directorioDatos carpeta {@code data}
     * @param copiador        servicio que copia entre almacenamientos
     */
    public StorageService(DatabaseSettingsRepository ajustes, Path directorioDatos, DataTransferService copiador) {
        this.ajustes = Objects.requireNonNull(ajustes);
        this.directorioDatos = Objects.requireNonNull(directorioDatos);
        this.copiador = Objects.requireNonNull(copiador);
    }

    /** @return la configuración guardada en este computador */
    public DatabaseSettings leer() {
        return ajustes.leer();
    }

    /**
     * Abre el almacenamiento configurado.
     *
     * @return el almacenamiento y, si hace falta, un aviso para el usuario
     */
    public Apertura abrir() {
        DatabaseSettings config = ajustes.leer();
        JsonStorageProvider archivos = new JsonStorageProvider(directorioDatos);
        if (!config.usaBaseDeDatos()) {
            return new Apertura(archivos, new FileSnapshot(directorioDatos), null, null);
        }
        Database bd = new Database(config);
        try {
            DatabaseStorageProvider base = new DatabaseStorageProvider(bd);
            String aviso = preparar(bd, base, archivos);
            return new Apertura(base, new DatabaseSnapshot(base, copiador), new DatabaseBackupRepository(bd), aviso);
        } catch (RuntimeException e) {
            bd.close();
            return new Apertura(archivos, new FileSnapshot(directorioDatos), null,
                    e.getMessage() + " Mientras tanto se usan los archivos de este computador.");
        }
    }

    /**
     * Prueba una configuración sin guardarla.
     *
     * @param config motor, URL, usuario y clave
     * @return descripción del resultado
     * @throws ValidationException si faltan datos
     * @throws sistemainventario.exception.PersistenceException si no se pudo conectar
     */
    public String probar(DatabaseSettings config) {
        if (!config.usaBaseDeDatos()) {
            return "Los datos se guardan en archivos de este computador.";
        }
        validar(config);
        try (Database bd = new Database(config)) {
            bd.probar();
            if (!bd.tieneTablas()) {
                return "Conexión correcta con " + bd.nombreMotor()
                        + ". Al guardar se crean las tablas y se copian sus negocios.";
            }
            int negocios = bd.consultar("SELECT COUNT(*) FROM PERFIL", fila -> fila.getInt(1)).get(0);
            return "Conexión correcta con " + bd.nombreMotor() + ". La base ya tiene " + negocios
                    + (negocios == 1 ? " negocio." : " negocios.")
                    + (negocios == 0 ? " Al guardar se copian sus negocios." : " Al guardar se usan esos.");
        }
    }

    /**
     * Prueba (si es una base) y guarda la configuración.
     *
     * @param config motor, URL, usuario y clave
     */
    public void guardar(DatabaseSettings config) {
        probar(config);
        ajustes.guardar(config);
    }

    /**
     * Copia los datos de la base a los archivos JSON de este computador. Se usa
     * al volver de una base de datos a archivos, para no perder lo hecho en la base.
     *
     * @param base almacenamiento en la base de datos
     */
    public void exportarAArchivos(StorageProvider base) {
        JsonStorageProvider archivos = new JsonStorageProvider(directorioDatos);
        archivos.vaciarArchivos();
        copiador.copiar(base, archivos);
    }

    private String preparar(Database bd, DatabaseStorageProvider base, JsonStorageProvider archivos) {
        bd.actualizarEsquema();
        if (!bd.estaVacia() || archivos.perfiles().listarTodos().isEmpty()) {
            return null;
        }
        try {
            DataTransferService.Resumen copiado = copiador.copiar(archivos, base);
            return "Se copiaron a " + bd.nombreMotor() + " " + copiado.perfiles() + " negocios, "
                    + copiado.productos() + " productos y " + copiado.ventas() + " ventas.";
        } catch (RuntimeException e) {
            copiador.vaciar(new DatabaseStorageProvider(bd));
            throw e;
        }
    }

    private static void validar(DatabaseSettings config) {
        if (config.url().isBlank()) {
            throw new ValidationException("Escriba la URL de conexión");
        }
        if (config.usuario().isBlank()) {
            throw new ValidationException("Escriba el usuario de la base de datos");
        }
    }
}
