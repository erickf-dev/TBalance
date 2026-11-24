package sistemainventario.model;

/**
 * Lo que recuerda este computador: qué negocio quedó abierto. No contiene
 * datos de los negocios (esos están en Oracle).
 *
 * <p>Se guarda en {@code ~/.inventariopro/data/configuracion-global.json}.</p>
 */
public class GlobalConfig {

    /** Versión del formato del archivo. */
    public static final String VERSION_ACTUAL = "3.0.0";

    private String perfilActivoId;
    private String version = VERSION_ACTUAL;

    /** @return {@code true} si hay un negocio abierto guardado */
    public boolean tienePerfilActivo() {
        return perfilActivoId != null && !perfilActivoId.isBlank();
    }

    /** @return ID del negocio abierto, o {@code null} */
    public String getPerfilActivoId() {
        return perfilActivoId;
    }

    /** @param perfilActivoId ID del negocio abierto, o {@code null} */
    public void setPerfilActivoId(String perfilActivoId) {
        this.perfilActivoId = perfilActivoId;
    }

    /** @return versión del formato del archivo */
    public String getVersion() {
        return version;
    }

    /** @param version versión del formato del archivo */
    public void setVersion(String version) {
        this.version = version;
    }
}
