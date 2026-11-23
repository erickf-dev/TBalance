package sistemainventario.model;

/**
 * Registro que no se borra sino que se deshabilita (borrado lógico).
 *
 * <p>Un negocio, un producto, una categoría o un proveedor tienen historial
 * (ventas, entradas, salidas). Si se borraran, ese historial quedaría huérfano
 * o se perdería; por eso se marcan como inactivos y se pueden volver a
 * habilitar.</p>
 */
public interface Deshabilitable {

    /** @return {@code true} si está habilitado (los datos antiguos sin el campo cuentan como habilitados) */
    boolean isActivo();

    /** @param activo {@code false} para deshabilitar, {@code true} para habilitar */
    void setActivo(boolean activo);
}
