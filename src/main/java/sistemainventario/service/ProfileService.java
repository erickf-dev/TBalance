package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Profile;
import sistemainventario.model.BusinessType;
import sistemainventario.repository.ProfileDirectory;
import sistemainventario.repository.Repository;
import sistemainventario.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ciclo de vida de los perfiles: crear, editar, eliminar y seleccionar.
 *
 * <p><b>GRASP Creator:</b> registra los perfiles, por eso crea las instancias
 * de {@link Profile}. <b>SRP:</b> delega el sistema de archivos en
 * {@link ProfileDirectory}, el archivo de configuración en
 * {@link GlobalConfigService} y la construcción de servicios en
 * {@link ProfileServicesFactory}.</p>
 */
public class ProfileService {

    private final Repository<Profile> perfiles;
    private final GlobalConfigService configuracion;
    private final ProfileDirectory directorios;
    private final ProfileServicesFactory fabricaServicios;

    public ProfileService(Repository<Profile> perfiles,
                          GlobalConfigService configuracion,
                          ProfileDirectory directorios,
                          ProfileServicesFactory fabricaServicios) {
        this.perfiles = Objects.requireNonNull(perfiles);
        this.configuracion = Objects.requireNonNull(configuracion);
        this.directorios = Objects.requireNonNull(directorios);
        this.fabricaServicios = Objects.requireNonNull(fabricaServicios);
    }

    public List<Profile> listarTodos() {
        return perfiles.listarTodos();
    }

    public Optional<Profile> buscarPorId(String id) {
        return perfiles.buscarPorId(id);
    }

    /** @return el perfil activo, si existe */
    public Optional<Profile> getPerfilActivo() {
        return configuracion.getPerfilActivoId().flatMap(perfiles::buscarPorId);
    }

    /** @return {@code true} si aún no se ha creado ningún perfil */
    public boolean esPrimerArranque() {
        return configuracion.esPrimerArranque();
    }

    /**
     * Determina qué perfil cargar al iniciar: el activo guardado o, si no hay,
     * el primero de la lista (comportamiento de P2).
     *
     * @return ID del perfil a cargar, o vacío si no hay perfiles
     */
    public Optional<String> resolverPerfilInicial() {
        Optional<String> activo = configuracion.getPerfilActivoId().filter(perfiles::existe);
        if (activo.isPresent()) {
            return activo;
        }
        Optional<String> primero = perfiles.listarTodos().stream().map(Profile::getId).findFirst();
        primero.ifPresent(configuracion::establecerPerfilActivo);
        return primero;
    }

    /**
     * <b>Creator:</b> crea el perfil, su carpeta y, si el tipo de negocio lo
     * indica, sus categorías predefinidas.
     *
     * @param datos datos enviados por la vista (nombre, descripción, color, tipo)
     * @return el perfil creado
     * @throws ValidationException si el nombre está vacío
     */
    public Profile crear(Profile datos) {
        if (datos == null) {
            throw new ValidationException("No se recibieron datos del perfil");
        }
        Profile perfil = new Profile(
                perfiles.siguienteIdNumerico(),
                TextUtil.recortar(datos.getNombre()),
                TextUtil.recortar(datos.getDescripcion()),
                datos.getColor(),
                datos.getTipoNegocio());
        perfil.validar();
        perfiles.agregar(perfil);
        directorios.crear(perfil.getId());
        crearCategoriasIniciales(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    private void crearCategoriasIniciales(Profile perfil) {
        BusinessType tipo = perfil.getPerfilNegocio();
        if (tipo.tieneCategorias()) {
            fabricaServicios.crear(perfil.getId()).categorias().crearPredefinidas(tipo);
        }
    }

    /**
     * Actualiza nombre, descripción y color; conserva el resto de datos.
     *
     * @param cambios datos enviados por la vista (debe incluir el ID)
     * @return el perfil actualizado
     * @throws ValidationException si el perfil no existe o el nombre está vacío
     */
    public Profile actualizar(Profile cambios) {
        Profile perfil = obtener(cambios == null ? null : cambios.getId());
        perfil.actualizarDatos(cambios.getNombre(), cambios.getDescripcion(), cambios.getColor());
        perfiles.actualizar(perfil);
        sincronizarConfiguracion();
        return perfil;
    }

    /**
     * Elimina el perfil y todos sus datos. Si era el activo, deja el sistema
     * sin perfil activo.
     *
     * @param id ID del perfil
     * @throws ValidationException si el perfil no existe
     */
    public void eliminar(String id) {
        obtener(id);
        perfiles.eliminar(id);
        directorios.eliminar(id);
        if (configuracion.getPerfilActivoId().filter(id::equals).isPresent()) {
            configuracion.establecerPerfilActivo(null);
        }
        sincronizarConfiguracion();
    }

    /**
     * @param id ID del perfil a activar, o vacío/{@code null} para ninguno
     * @return servicios del perfil activado, o vacío si se desactivó
     * @throws ValidationException si el perfil no existe
     */
    public Optional<ProfileServices> seleccionar(String id) {
        if (TextUtil.esVacio(id)) {
            configuracion.establecerPerfilActivo(null);
            return Optional.empty();
        }
        obtener(id);
        configuracion.establecerPerfilActivo(id);
        return Optional.of(fabricaServicios.crear(id));
    }

    /**
     * @param id ID del perfil
     * @return servicios para operar sobre los datos de ese perfil
     */
    public ProfileServices crearServicios(String id) {
        return fabricaServicios.crear(id);
    }

    private Profile obtener(String id) {
        return perfiles.buscarPorId(id)
                .orElseThrow(() -> new ValidationException("Perfil no encontrado"));
    }

    private void sincronizarConfiguracion() {
        configuracion.sincronizarPerfiles(perfiles.listarTodos());
    }
}
