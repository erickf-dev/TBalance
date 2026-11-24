package sistemainventario;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Backup;
import sistemainventario.model.DatabaseSettings;
import sistemainventario.repository.ZipArchiver;
import sistemainventario.repository.GlobalConfigRepository;
import sistemainventario.repository.LabelRepository;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.repository.DatabaseSettingsRepository;
import sistemainventario.repository.StorageProvider;
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
import sistemainventario.service.SessionService;
import sistemainventario.service.UserService;
import sistemainventario.model.Profile;
import sistemainventario.model.Rol;
import sistemainventario.model.User;
import sistemainventario.util.QrGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Contexto de la aplicación (Singleton) y raíz de composición.
 *
 * <p>Es el único lugar donde se conectan las implementaciones concretas
 * (repositorios JDBC de Oracle, según {@link StorageService}) con los servicios.
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
    private boolean conectado;
    private ProfileService gestorPerfiles;
    private ProfileServices serviciosActivos;
    private UserService usuarios;
    private sistemainventario.service.AuditService auditoria;
    private final SessionService sesion = new SessionService();

    private AppContext(Path directorioDatos) {
        this.directorioDatos = directorioDatos;
        JsonFileStore.crearDirectorio(directorioDatos);
        this.gestorRespaldo = new BackupService(StorageService.SIN_COPIAS, StorageService.SIN_DATOS, new ZipArchiver());
        this.gestorEtiquetas = new LabelService(new QrGenerator(),
                new LabelRepository(directorioDatos.resolveSibling(CARPETA_ETIQUETAS)));
        this.ajustesAsistente = new AssistantSettingsService(
                new AssistantConfigRepository(directorioDatos.resolveSibling(ARCHIVO_ASISTENTE)));
        this.exportadorInformes = new ReportExportService(new ReportExcelWriter(),
                ReportExportService.carpetaPorDefecto(Path.of(System.getProperty("user.home"))));
        this.almacenamiento = new StorageService(
                new DatabaseSettingsRepository(directorioDatos.resolveSibling(ARCHIVO_BASE_DATOS)),
                new DataTransferService());
        recargar();
    }

    /**
     * Se conecta de nuevo con Oracle y vuelve a armar los repositorios y
     * servicios. Se usa al iniciar, al cambiar la conexión y después de
     * restaurar una copia de seguridad.
     */
    public void recargar() {
        JsonFileStore.crearDirectorio(directorioDatos);
        if (almacenActual != null) {
            almacenActual.close();
        }
        StorageService.Apertura apertura = almacenamiento.abrir();
        this.almacenActual = apertura.almacen();
        this.avisoAlmacenamiento = apertura.aviso();
        this.conectado = apertura.conectado();
        gestorRespaldo.usar(apertura.respaldos() != null ? apertura.respaldos() : StorageService.SIN_COPIAS, apertura.copias());
        this.gestorPerfiles = new ProfileService(
                almacenActual.perfiles(),
                new GlobalConfigService(new GlobalConfigRepository(directorioDatos)),
                new ProfileServicesFactory(almacenActual));
        gestorPerfiles.normalizarCategorias();
        this.usuarios = new UserService(almacenActual.usuarios());
        this.auditoria = new sistemainventario.service.AuditService(almacenActual.auditoria());
        this.serviciosActivos = gestorPerfiles.resolverPerfilInicial()
                .map(gestorPerfiles::crearServicios)
                .orElse(null);
        // Después de cambiar de almacenamiento o restaurar una copia, la sesión sigue
        // solo si la persona existe y está habilitada en los datos nuevos.
        sesion.actual().ifPresent(persona -> {
            Optional<User> nueva;
            try {
                nueva = Optional.of(usuarios.obtener(persona.getId())).filter(User::isActivo);
            } catch (ValidationException e) {
                nueva = Optional.empty();
            }
            if (nueva.isPresent()) {
                sesion.refrescar(nueva.get());
                asegurarPerfilPermitido();
            } else {
                sesion.cerrar();
            }
        });
    }

    // ---- sesión y permisos ---------------------------------------------------

    /** @return personas que usan TBalance */
    public UserService getUsuarios() {
        return usuarios;
    }

    /** @return la sesión de este computador */
    /** @return historial de acciones (quién hizo qué) */
    public sistemainventario.service.AuditService getAuditoria() {
        return auditoria;
    }

    public SessionService getSesion() {
        return sesion;
    }

    /**
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return la persona que entró
     */
    public User iniciarSesion(String usuario, String clave) {
        User persona = usuarios.iniciarSesion(usuario, clave);
        sesion.abrir(persona);
        asegurarPerfilPermitido();
        return persona;
    }

    /**
     * Primera vez: crea la cuenta del dueño con acceso a los negocios que ya hay
     * y abre su sesión.
     *
     * @param nombre  nombre de la persona
     * @param usuario nombre de usuario
     * @param clave   contraseña
     * @return la cuenta creada
     */
    public User crearPrimerDueno(String nombre, String usuario, String clave) {
        List<String> negocios = gestorPerfiles.listarTodos().stream().map(Profile::getId).toList();
        User dueno = usuarios.crearPrimerDueno(nombre, usuario, clave, negocios);
        sesion.abrir(dueno);
        asegurarPerfilPermitido();
        return dueno;
    }

    /** Cierra la sesión; los servicios del negocio quedan sin cargar hasta que alguien entre. */
    public void cerrarSesion() {
        sesion.cerrar();
    }

    /** @return negocios habilitados a los que entra la persona con sesión abierta */
    public List<Profile> perfilesPermitidos() {
        Optional<User> persona = sesion.actual();
        if (persona.isEmpty()) {
            return List.of();
        }
        return gestorPerfiles.listarTodos().stream().filter(p -> persona.get().rolEn(p.getId()).isPresent()).toList();
    }

    /** @return rol en el negocio abierto, si hay sesión y negocio */
    public Optional<Rol> rolActual() {
        return getPerfilActivoId() == null ? Optional.empty() : sesion.rolEn(getPerfilActivoId());
    }

    /**
     * @return {@code true} si la persona puede crear negocios: es dueño de alguno,
     *         o es la primera cuenta y todavía no hay negocios
     */
    public boolean puedeCrearNegocios() {
        return sesion.actual().map(u -> u.esDuenoDeAlguno() || u.getAccesos().isEmpty()).orElse(false);
    }

    /**
     * @param perfilId negocio
     * @return {@code true} si la persona con sesión es dueño de ese negocio
     */
    public boolean esDuenoDe(String perfilId) {
        return sesion.rolEn(perfilId).orElse(null) == Rol.DUENO;
    }

    /**
     * Da acceso de dueño a quien acaba de crear un negocio.
     *
     * @param perfilId negocio nuevo
     */
    public void registrarNegocioCreado(String perfilId) {
        sesion.actual().ifPresent(persona -> {
            usuarios.hacerDueno(persona, perfilId);
            sesion.refrescar(usuarios.obtener(persona.getId()));
        });
    }

    /**
     * Deja abierto un negocio al que la persona sí tiene acceso (o ninguno).
     */
    private void asegurarPerfilPermitido() {
        String actual = getPerfilActivoId();
        if (actual != null && sesion.rolEn(actual).isPresent()) {
            return;
        }
        String elegido = perfilesPermitidos().stream().filter(Profile::isActivo).map(Profile::getId).findFirst().orElse(null);
        activarPerfil(elegido);
    }

    /**
     * Cambia la conexión con Oracle de este computador (la prueba antes de guardarla).
     *
     * @param nueva datos de conexión
     * @return mensaje para el usuario
     */
    public String cambiarAlmacenamiento(DatabaseSettings nueva) {
        almacenamiento.guardar(nueva);
        recargar();
        if (!conectado) {
            throw new ValidationException(avisoAlmacenamiento);
        }
        return "TBalance quedó conectado con Oracle.";
    }

    /** @return {@code true} si hay conexión con Oracle */
    public boolean estaConectado() {
        return conectado;
    }

    /** @return configuración de la base de datos de este computador */
    public DatabaseSettings getAjustesBaseDeDatos() {
        return almacenamiento.leer();
    }

    /** @return servicio que prueba y guarda la configuración de la base */
    public StorageService getAlmacenamiento() {
        return almacenamiento;
    }

    /** @return "Oracle", o "Sin conexión" si Oracle no respondió */
    public String getAlmacenEnUso() {
        return almacenActual.nombre();
    }

    /** @return aviso pendiente sobre el almacenamiento (migración o error de conexión), o {@code null} */
    public String getAvisoAlmacenamiento() {
        return avisoAlmacenamiento;
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
        if (perfilId != null && sesion.actual().isPresent() && sesion.rolEn(perfilId).isEmpty()) {
            throw new ValidationException("No tiene acceso a ese negocio");
        }
        serviciosActivos = gestorPerfiles.seleccionar(perfilId).orElse(null);
    }

    /**
     * Deshabilita un negocio; si era el activo, quedan cargados los servicios del
     * negocio que lo reemplaza.
     *
     * @param perfilId negocio a deshabilitar
     */
    public void deshabilitarPerfil(String perfilId) {
        gestorPerfiles.deshabilitar(perfilId);
        this.serviciosActivos = gestorPerfiles.getPerfilActivo()
                .map(p -> gestorPerfiles.crearServicios(p.getId())).orElse(null);
        asegurarPerfilPermitido();
    }

    /**
     * @param perfilId negocio a volver a habilitar
     */
    public void habilitarPerfil(String perfilId) {
        gestorPerfiles.habilitar(perfilId);
    }

    /**
     * Con base de datos, pregunta si otro equipo guardó cambios.
     *
     * @return número que aumenta cada vez que otro equipo hizo cambios (0 sin conexión)
     */
    public long revisarCambiosDeOtrosEquipos() {
        return almacenActual instanceof sistemainventario.repository.DatabaseStorageProvider bd
                ? bd.getBaseDeDatos().revisarCambios(true) : 0;
    }

    /** @return imágenes del QR de cobro de cada negocio ({@code ~/.inventariopro/qr-cobro}) */
    public sistemainventario.repository.PaymentQrRepository getQrCobro() {
        return new sistemainventario.repository.PaymentQrRepository(directorioDatos.resolveSibling("qr-cobro"));
    }

    /** @return carpeta del certificado para conectar el celular ({@code ~/.inventariopro/celular}) */
    public Path getCarpetaCelular() {
        return directorioDatos.resolveSibling("celular");
    }

    /** @return ID del negocio abierto, o {@code null} si no hay ninguno */
    public String getPerfilActivoId() {
        return gestorPerfiles.getPerfilActivo().map(p -> p.getId()).orElse(null);
    }

    /** @return {@code true} si todavía no existe ningún perfil */
}
