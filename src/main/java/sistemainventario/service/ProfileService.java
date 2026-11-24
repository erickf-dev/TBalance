package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Profile;
import sistemainventario.model.BusinessType;
import sistemainventario.repository.Repository;
import sistemainventario.util.TextUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ciclo de vida de los perfiles: crear, editar, eliminar y seleccionar.
 *
 * <p><b>GRASP Creator:</b> registra los perfiles, por eso crea las instancias
 * de {@link Profile}. <b>SRP:</b> delega el guardado en el repositorio
 * (Oracle), el negocio abierto en este computador en
 * {@link GlobalConfigService} y la construcción de servicios en
 * {@link ProfileServicesFactory}.</p>
 */
public class ProfileService {

    private final Repository<Profile> perfiles;
    private final GlobalConfigService configuracion;
    private final ProfileServicesFactory fabricaServicios;

    public ProfileService(Repository<Profile> perfiles,
                          GlobalConfigService configuracion,
                          ProfileServicesFactory fabricaServicios) {
        this.perfiles = Objects.requireNonNull(perfiles);
        this.configuracion = Objects.requireNonNull(configuracion);
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
    /**
     * Determina qué perfil cargar al iniciar: el activo guardado o, si no hay,
     * el primero de la lista (comportamiento de P2).
     *
     * @return ID del perfil a cargar, o vacío si no hay perfiles
     */
    public Optional<String> resolverPerfilInicial() {
        Optional<String> activo = configuracion.getPerfilActivoId()
                .filter(id -> perfiles.buscarPorId(id).filter(Profile::isActivo).isPresent());
        if (activo.isPresent()) {
            return activo;
        }
        Optional<String> primero = perfiles.listarTodos().stream().filter(Profile::isActivo)
                .map(Profile::getId).findFirst();
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
        perfil.copiarAjustesDePago(datos);
        perfil.validar();
        perfiles.agregar(perfil);
        crearCategoriasIniciales(perfil);
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
        perfil.copiarAjustesDePago(cambios);
        perfiles.actualizar(perfil);
        return perfil;
    }

    /**
     * Deshabilita un negocio (borrado lógico): sus productos, ventas y demás
     * datos se conservan y se puede volver a habilitar. Si era el activo, se
     * activa otro negocio habilitado.
     *
     * @param id ID del perfil
     * @throws ValidationException si no existe o es el único habilitado
     */
    public void deshabilitar(String id) {
        Profile perfil = obtener(id);
        Optional<String> otro = perfiles.listarTodos().stream()
                .filter(p -> p.isActivo() && !p.getId().equals(id))
                .map(Profile::getId).findFirst();
        if (otro.isEmpty()) {
            throw new ValidationException("Debe quedar al menos un negocio habilitado");
        }
        perfil.setActivo(false);
        perfiles.actualizar(perfil);
        if (configuracion.getPerfilActivoId().filter(id::equals).isPresent()) {
            configuracion.establecerPerfilActivo(otro.get());
        }
    }

    /**
     * Vuelve a habilitar un negocio deshabilitado.
     *
     * @param id ID del perfil
     * @throws ValidationException si el perfil no existe
     */
    public void habilitar(String id) {
        Profile perfil = obtener(id);
        perfil.setActivo(true);
        perfiles.actualizar(perfil);
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
        if (!obtener(id).isActivo()) {
            throw new ValidationException("Ese negocio está deshabilitado. Habilítelo en Mis negocios.");
        }
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

    /**
     * Revisa la categoría de los productos de todos los negocios (los datos de
     * versiones en que era opcional quedan en "Sin categoría").
     *
     * @return cuántos productos se corrigieron en total
     */
    public int normalizarCategorias() {
        return perfiles.listarTodos().stream()
                .mapToInt(perfil -> fabricaServicios.normalizarCategorias(perfil.getId())).sum();
    }

    private Profile obtener(String id) {
        return perfiles.buscarPorId(id)
                .orElseThrow(() -> new ValidationException("Perfil no encontrado"));
    }

}
