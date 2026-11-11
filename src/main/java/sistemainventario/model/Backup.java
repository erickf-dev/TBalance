package sistemainventario.model;

/**
 * Información de un archivo de copia de seguridad (.zip) del sistema.
 *
 * <p>Es un objeto de solo lectura: se construye a partir del archivo en disco
 * y se envía a la vista para listarlo.</p>
 */
public class Backup {

    private final String nombre;
    private final String fecha;
    private final long tamanoBytes;

    /**
     * @param nombre      nombre del archivo, por ejemplo {@code TBalance_2026-10-27_21-30-05.zip}
     * @param fecha       fecha y hora de creación en formato legible
     * @param tamanoBytes tamaño del archivo en bytes
     */
    public Backup(String nombre, String fecha, long tamanoBytes) {
        this.nombre = nombre;
        this.fecha = fecha;
        this.tamanoBytes = tamanoBytes;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFecha() {
        return fecha;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }
}
