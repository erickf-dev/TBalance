package sistemainventario;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;
import sistemainventario.repository.ZipArchiver;
import sistemainventario.repository.GlobalConfigRepository;
import sistemainventario.repository.ProfileDirectory;
import sistemainventario.repository.LabelRepository;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.repository.ProfileRepository;
import sistemainventario.repository.BackupRepository;
import sistemainventario.service.ProfileServicesFactory;
import sistemainventario.service.GlobalConfigService;
import sistemainventario.service.LabelService;
import sistemainventario.service.ProfileService;
import sistemainventario.service.BackupService;
import sistemainventario.service.ProfileServices;
import sistemainventario.util.QrGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Contexto de la aplicación (Singleton) y raíz de composición.
 *
 * <p>Es el único lugar donde se conectan las implementaciones concretas
 * (repositorios JSON) con los servicios. Mantiene los servicios del perfil
 * activo y los reemplaza al cambiar de perfil.</p>
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

    private static AppContext instancia;

    private final Path directorioDatos;
    private final BackupService gestorRespaldo;
    private final LabelService gestorEtiquetas;
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
        recargar();
    }

    /**
     * Construye de nuevo los repositorios y servicios leyendo los archivos
     * JSON. Se usa al iniciar y después de restaurar una copia de seguridad.
     */
    public void recargar() {
        JsonFileStore.crearDirectorio(directorioDatos);
        ProfileDirectory directorios = new ProfileDirectory(directorioDatos);
        this.gestorPerfiles = new ProfileService(
                new ProfileRepository(directorioDatos),
                new GlobalConfigService(new GlobalConfigRepository(directorioDatos)),
                directorios,
                new ProfileServicesFactory(directorios));
        this.serviciosActivos = gestorPerfiles.resolverPerfilInicial()
                .map(gestorPerfiles::crearServicios)
                .orElse(null);
    }

    /**
     * Restaura una copia de seguridad y recarga todos los datos en memoria.
     *
     * @param nombre archivo .zip a restaurar
     * @return la copia automática del estado anterior
     */
    public Backup restaurarRespaldo(String nombre) {
        Backup copiaPrevia = gestorRespaldo.restaurar(nombre);
        recargar();
        return copiaPrevia;
    }

    public BackupService getGestorRespaldo() {
        return gestorRespaldo;
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
