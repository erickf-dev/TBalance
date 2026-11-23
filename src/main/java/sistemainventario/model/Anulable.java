package sistemainventario.model;

/**
 * Operación que no se borra sino que se anula: ventas, entradas y salidas.
 *
 * <p>Una venta anulada sigue en el historial con su fecha y su motivo, pero no
 * cuenta en los totales, y el stock que movió se devuelve.</p>
 */
public interface Anulable {

    /** Estado de una operación vigente. */
    String VIGENTE = "COMPLETADA";
    /** Estado de una operación anulada. */
    String ANULADA = "ANULADA";

    /** @return {@code true} si la operación fue anulada */
    boolean isAnulada();

    /** @return fecha y hora de la anulación, o {@code null} */
    String getFechaAnulacion();

    /** @return motivo de la anulación, o {@code null} */
    String getMotivoAnulacion();

    /**
     * Marca la operación como anulada.
     *
     * @param fecha  fecha y hora de la anulación
     * @param motivo por qué se anula (obligatorio)
     * @throws sistemainventario.exception.ValidationException si ya estaba anulada o falta el motivo
     */
    void anular(String fecha, String motivo);

    /** @return nombre de quien anuló, o {@code null} (anulaciones antes de existir el login) */
    String getAnuladaPor();

    /** @param anuladaPor nombre de quien anuló */
    void setAnuladaPor(String anuladaPor);
}
