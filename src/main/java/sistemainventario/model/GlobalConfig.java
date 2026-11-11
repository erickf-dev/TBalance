package sistemainventario.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración global de la aplicación: registro de perfiles y puntero al
 * perfil activo. No contiene datos de negocio.
 *
 * <p>Se persiste en {@code configuracion-global.json} con la misma estructura de P2.</p>
 */
public class GlobalConfig {

    public static final String VERSION_ACTUAL = "2.1.0";

    private List<Profile> perfiles = new ArrayList<>();
    private String perfilActivoId;
    private String version = VERSION_ACTUAL;

    /**
     * <b>GRASP Experto:</b> la configuración conoce la lista y el ID activo.
     *
     * @return el perfil activo o {@code null} si no hay ninguno
     */
    public Profile getPerfilActivo() {
        if (perfilActivoId == null || perfiles == null) {
            return null;
        }
        return perfiles.stream()
                .filter(perfil -> perfilActivoId.equals(perfil.getId()))
                .findFirst()
                .orElse(null);
    }

    /** @return {@code true} si existe al menos un perfil registrado */
    public boolean tienePerfiles() {
        return perfiles != null && !perfiles.isEmpty();
    }

    /** @return {@code true} si hay un perfil marcado como activo */
    public boolean tienePerfilActivo() {
        return perfilActivoId != null && !perfilActivoId.isBlank();
    }

    public List<Profile> getPerfiles() {
        return perfiles;
    }

    public void setPerfiles(List<Profile> perfiles) {
        this.perfiles = perfiles == null ? new ArrayList<>() : new ArrayList<>(perfiles);
    }

    public String getPerfilActivoId() {
        return perfilActivoId;
    }

    public void setPerfilActivoId(String perfilActivoId) {
        this.perfilActivoId = perfilActivoId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
