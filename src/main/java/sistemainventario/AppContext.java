package sistemainventario;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;
import sistemainventario.model.DatabaseSettings;
import sistemainventario.repository.ZipArchiver;
import sistemainventario.repository.GlobalConfigRepository;
import sistemainventario.repository.ProfileDirectory;
import sistemainventario.repository.LabelRepository;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.repository.DatabaseSettingsRepository;
import sistemainventario.repository.StorageProvider;
import sistemainventario.repository.BackupRepository;
import sistemainventario.repository.AssistantConfigRepository;
import sistemainventario.repository.ReportExcelWriter;
import sistemainventario.service.ProfileServicesFactory;
import sistemainventario.service.GlobalConfigService;
import sistemainventario.service.LabelService;
import sistemainventario.service.ProfileService;
import sistemainventario.service.BackupService;
import sistemainventario.service.ProfileServices;
import sistemainventario.service.AssistantSettingsService;
import sistemainventario.service.ReportExportService;
import sistemainventario.service.DataTransferService;
import sistemainventario.service.StorageService;
import sistemainventario.util.QrGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Contexto de la aplicación (Singleton) y raíz de composición.
 *
 * <p>Es el único lugar donde se conectan las implementaciones concretas
 * (repositorios JSON o JDBC, según {@link StorageService}) con los servicios.
 * Mantiene los servicios del perfil activo y los reemplaza al cambiar de perfil
 * o de almacenamiento.</p>
 *
 * <p>Cambios respecto a P2: los campos dejaron de ser públicos y mutables
 * (encapsulamiento) y ya no se crean repositorios duplicados para el mismo
 * archivo. Después de restaurar una copia de seguridad, {@link #recargar()}
 * vuelve a leer todos los datos desde disco.</p>
 */
public final class AppContext {

    private static final Path DIRECTORIO_DATOS =
            Paths.get(System.getProperty("user.home"), ".inventariopro", "data");

    private static final String CARPETA_RESPALDOS = "respaldos";
    private static final String CARPETA_ETIQUETAS = "etiquetas";
    private static final String ARCHIVO_ASISTENTE = "asistente.json";
    private static final String ARCHIVO_BASE_DATOS = "basedatos.json";

    private static AppContext instancia;

    private final Path directorioDatos;
    private final BackupService gestorRespaldo;
    private final LabelService gestorEtiquetas;
    private final AssistantSettingsService ajustesAsistente;
    private final ReportExportService exportadorInformes;
    private final StorageService almacenamiento;
    private StorageProvider almacenActual;
    private String avisoAlmacenamiento;
    private ProfileService gestorPerfiles;
    private ProfileServices serviciosActivos;

    private AppContext(Path directorioDatos) {
        this.directorioDatos = directorioDatos;
        JsonFileStore.crearDirectorio(directorioDatos);
        Path carpetaRespaldos = directorioDatos.resolveSibling(CARPETA_RESPALDOS);
        this.gestorRespaldo = new BackupService(directorioDatos,
                new BackupRepository(carpetaRespaldos), new ZipArchiver());
        this.gestorEtiquetas = new LabelService(new QrGenerator(),
                new LabelRepository(directorioDatos.resolveSibling(CARPETA_ETIQUETAS)));
        this.ajustesAsistente = new AssistantSettingsService(
                new AssistantConfigRepository(directorioDatos.resolveSibling(ARCHIVO_ASISTENTE)));
        this.exportadorInformes = new ReportExportService(new ReportExcelWriter(),
                ReportExportService.carpetaPorDefecto(Path.of(System.getProperty("user.home"))));
        this.almacenamiento = new StorageService(
                new DatabaseSettingsRepository(directorioDatos.resolveSibling(ARCHIVO_BASE_DATOS)),
                directorioDatos, new DataTransferService());
        recargar();
    }

    /**
     * Construye de nuevo los repositorios y servicios leyendo los archivos
     * JSON. Se usa al iniciar y después de restaurar una copia de seguridad.
     */
    public void recargar() {
        JsonFileStore.crearDirectorio(directorioDatos);
        if (almacenActual != null) {
            almacenActual.close();
        }
        StorageService.Apertura apertura = almacenamiento.abrir();
        this.almacenActual = apertura.almacen();
        this.avisoAlmacenamiento = apertura.aviso();
        gestorRespaldo.usar(apertura.copias());
        this.gestorPerfiles = new ProfileService(
                almacenActual.perfiles(),
                new GlobalConfigService(new GlobalConfigRepository(directorioDatos)),
                new ProfileDirectory(directorioDatos),
                new ProfileServicesFactory(almacenActual));
        gestorPerfiles.sincronizarConfiguracion();
        this.serviciosActivos = gestorPerfiles.resolverPerfilInicial()
                .map(gestorPerfiles::crearServicios)
                .orElse(null);
    }

    /**
     * Cambia dónde se guardan los datos. Si se pasa de una base de datos a
     * archivos, primero se copian los datos de la base a los archivos.
     *
     * @param nueva motor, URL, usuario y clave
     * @return mensaje para el usuario
     */
    public String cambiarAlmacenamiento(DatabaseSettings nueva) {
        boolean veniaDeBase = almacenamiento.leer().usaBaseDeDatos() && avisoAlmacenamiento == null;
        if (veniaDeBase && !nueva.usaBaseDeDatos()) {
            crearRespaldoAntesDeCambiar();
        }
        almacenamiento.guardar(nueva);
        recargar();
        String mensaje = "Ahora los datos se guardan en " + almacenActual.nombre() + ".";
        return avisoAlmacenamiento == null ? mensaje : mensaje + " " + avisoAlmacenamiento;
    }

    /** @return configuración de la base de datos de este computador */
    public DatabaseSettings getAjustesBaseDeDatos() {
        return almacenamiento.leer();
    }

    /** @return servicio que prueba y guarda la configuración de la base */
    public StorageService getAlmacenamiento() {
        return almacenamiento;
    }

    /** @return nombre del almacenamiento en uso ("Archivos JSON", "PostgreSQL", "Oracle") */
    public String getAlmacenEnUso() {
        return almacenActual.nombre();
    }

    /** @return aviso pendiente sobre el almacenamiento (migración o error de conexión), o {@code null} */
    public String getAvisoAlmacenamiento() {
        return avisoAlmacenamiento;
    }

    private void crearRespaldoAntesDeCambiar() {
        gestorRespaldo.crear();
    }

    public Backup restaurarRespaldo(String nombre) {
        Backup copiaPrevia = gestorRespaldo.restaurar(nombre);
        recargar();
        return copiaPrevia;
    }

    public BackupService getGestorRespaldo() {
        return gestorRespaldo;
    }

    /** @return configuración del asistente (clave y modelo, fuera de los datos de los perfiles) */
    public AssistantSettingsService getAjustesAsistente() {
        return ajustesAsistente;
    }

    /** @return exportación de informes a Excel */
    public ReportExportService getExportadorInformes() {
        return exportadorInformes;
    }

    public LabelService getGestorEtiquetas() {
        return gestorEtiquetas;
    }

    /** @return la instancia única, creada la primera vez que se solicita */
    public static synchronized AppContext get() {
        if (instancia == null) {
            instancia = new AppContext(DIRECTORIO_DATOS);
        }
        return instancia;
    }

    public ProfileService getGestorPerfiles() {
        return gestorPerfiles;
    }

    /** @return servicios del perfil activo, si lo hay */
    public Optional<ProfileServices> getServiciosActivos() {
        return Optional.ofNullable(serviciosActivos);
    }

    /**
     * @return servicios del perfil activo
     * @throws ValidationException si no hay perfil activo
     */
    public ProfileServices requerirServiciosActivos() {
        return getServiciosActivos()
                .orElseThrow(() -> new ValidationException("No hay un perfil activo"));
    }

    /**
     * Activa un perfil y reconstruye sus servicios (aislamiento de datos).
     *
     * @param perfilId ID del perfil, o vacío para no dejar ninguno activo
     */
    public void activarPerfil(String perfilId) {
        serviciosActivos = gestorPerfiles.seleccionar(perfilId).orElse(null);
    }

    /**
     * Elimina un perfil y libera sus servicios si era el activo.
     *
     * @param perfilId ID del perfil a eliminar
     */
    public void eliminarPerfil(String perfilId) {
        gestorPerfiles.eliminar(perfilId);
        if (gestorPerfiles.getPerfilActivo().isEmpty()) {
            serviciosActivos = null;
        }
    }

    /** @return {@code true} si todavía no existe ningún perfil */
    public boolean esPrimerArranque() {
        return gestorPerfiles.esPrimerArranque();
    }
}
