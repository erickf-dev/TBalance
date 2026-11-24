package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.DatabaseSettings;
import sistemainventario.repository.BackupStore;
import sistemainventario.repository.Database;
import sistemainventario.repository.DatabaseBackupRepository;
import sistemainventario.repository.DatabaseSettingsRepository;
import sistemainventario.repository.DatabaseStorageProvider;
import sistemainventario.repository.SinConexionStorageProvider;
import sistemainventario.repository.StorageProvider;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Conecta TBalance con Oracle y prepara la base.
 *
 * <p>La primera vez que se conecta a una base vacía crea las tablas, los
 * disparadores, las funciones y los procedimientos. Si Oracle no responde al
 * abrir, TBalance no guarda nada: muestra el error y pide los datos de
 * conexión.</p>
 */
public class StorageService {

    private final DatabaseSettingsRepository ajustes;
    private final DataTransferService copiador;

    /**
     * Base de datos lista para usar (o sin conexión).
     *
     * @param almacen   fábrica de repositorios
     * @param copias    cómo se sacan y reponen los datos en las copias de seguridad
     * @param respaldos dónde se guardan las copias ({@code null} si no hay conexión)
     * @param aviso     mensaje para el usuario (error de conexión), o {@code null}
     * @param conectado {@code true} si hay conexión con Oracle
     */
    public record Apertura(StorageProvider almacen, DataSnapshot copias, BackupStore respaldos, String aviso,
                           boolean conectado) {
    }

    /**
     * @param ajustes  archivo {@code basedatos.json}
     * @param copiador servicio que copia los datos para las copias de seguridad
     */
    public StorageService(DatabaseSettingsRepository ajustes, DataTransferService copiador) {
        this.ajustes = Objects.requireNonNull(ajustes);
        this.copiador = Objects.requireNonNull(copiador);
    }

    /** @return la conexión guardada en este computador */
    public DatabaseSettings leer() {
        return ajustes.leer();
    }

    /**
     * Abre Oracle con la conexión guardada. Crea o actualiza las tablas,
     * funciones y procedimientos si hace falta.
     *
     * @return la base abierta, o el estado "sin conexión" con el motivo
     */
    public Apertura abrir() {
        DatabaseSettings config = ajustes.leer();
        if (!config.estaConfigurada()) {
            return sinConexion("Conecte TBalance con Oracle: escriba la URL, el usuario y la contraseña de la base.");
        }
        Database bd = new Database(config);
        try {
            bd.actualizarEsquema();
            DatabaseStorageProvider base = new DatabaseStorageProvider(bd);
            return new Apertura(base, new DatabaseSnapshot(base, copiador), new DatabaseBackupRepository(bd), null, true);
        } catch (RuntimeException e) {
            bd.close();
            return sinConexion(e.getMessage());
        }
    }

    /**
     * Prueba una conexión sin guardarla.
     *
     * @param config datos de conexión
     * @return mensaje para el usuario
     */
    public String probar(DatabaseSettings config) {
        validar(config);
        try (Database bd = new Database(config)) {
            bd.probar();
            if (!bd.tieneTablas()) {
                return "Conexión correcta con Oracle. Al guardar se crean las tablas, las funciones y los procedimientos.";
            }
            int negocios = bd.contarNegocios();
            if (negocios < 0) {
                return "Conexión correcta con Oracle. Al guardar se actualizan las funciones y los procedimientos.";
            }
            return "Conexión correcta con Oracle. La base tiene " + negocios + (negocios == 1 ? " negocio." : " negocios.");
        }
    }

    /**
     * Prueba la conexión y, si funciona, la guarda en este computador.
     *
     * @param config datos de conexión
     */
    public void guardar(DatabaseSettings config) {
        probar(config);
        ajustes.guardar(config);
    }

    private Apertura sinConexion(String motivo) {
        return new Apertura(new SinConexionStorageProvider(), SIN_DATOS, null, motivo, false);
    }

    private static void validar(DatabaseSettings config) {
        if (config.url().isBlank()) {
            throw new ValidationException("Escriba la URL de conexión");
        }
        if (config.usuario().isBlank()) {
            throw new ValidationException("Escriba el usuario de Oracle");
        }
        if (config.clave().isEmpty()) {
            throw new ValidationException("Escriba la contraseña del usuario de Oracle");
        }
    }

    /** Sin conexión no hay datos para copiar ni dónde restaurar. */
    public static final DataSnapshot SIN_DATOS = new DataSnapshot() {
        @Override
        public Path prepararCarpeta() {
            throw new ValidationException(SinConexionStorageProvider.MENSAJE);
        }

        @Override
        public void liberarCarpeta(Path carpeta) {
            // no hay nada que liberar
        }

        @Override
        public void restaurarDesde(Path carpeta) {
            throw new ValidationException(SinConexionStorageProvider.MENSAJE);
        }
    };
}
