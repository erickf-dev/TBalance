package sistemainventario.service;

import sistemainventario.model.GlobalConfig;
import sistemainventario.model.Profile;
import sistemainventario.repository.GlobalConfigRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lectura y actualización de la {@link GlobalConfig}.
 *
 * <p><b>SRP:</b> solo se ocupa del puntero al perfil activo y de mantener
 * sincronizada la lista de perfiles en el archivo de configuración. En P2,
 * {@code ProfileService} también escribía directamente este archivo.</p>
 */
public class GlobalConfigService {

    private final GlobalConfigRepository repositorio;

    public GlobalConfigService(GlobalConfigRepository repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    /** @return {@code true} si aún no se ha creado ningún perfil */
    public boolean esPrimerArranque() {
        return !repositorio.existe() || !repositorio.cargar().tienePerfiles();
    }

    /** @return ID del perfil activo, si hay uno */
    public Optional<String> getPerfilActivoId() {
        GlobalConfig configuracion = repositorio.cargar();
        return configuracion.tienePerfilActivo()
                ? Optional.of(configuracion.getPerfilActivoId())
                : Optional.empty();
    }

    /**
     * @param perfilId ID del nuevo perfil activo, o {@code null} para ninguno
     */
    public void establecerPerfilActivo(String perfilId) {
        GlobalConfig configuracion = repositorio.cargar();
        configuracion.setPerfilActivoId(perfilId);
        repositorio.guardar(configuracion);
    }

    /**
     * Copia la lista de perfiles al archivo de configuración (estructura de P2).
     *
     * @param perfiles lista actual de perfiles
     */
    public void sincronizarPerfiles(List<Profile> perfiles) {
        GlobalConfig configuracion = repositorio.cargar();
        configuracion.setPerfiles(perfiles);
        repositorio.guardar(configuracion);
    }
}
