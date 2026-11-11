package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.TextUtil;

/**
 * Movimiento que modifica el stock de un producto (entrada o salida).
 *
 * <p><b>Herencia:</b> agrupa los atributos que P2 tenía duplicados en
 * {@link StockEntry} y {@link StockExit} (producto, cantidad y fecha).</p>
 *
 * <p><b>Polimorfismo / OCP:</b> cada subclase define cómo se aplica al
 * producto mediante {@link #aplicarA(Product)}. El gestor de movimientos
 * trabaja con el tipo abstracto, por lo que un nuevo tipo de movimiento
 * (por ejemplo un ajuste o una devolución) se agrega creando una subclase,
 * sin modificar el servicio.</p>
 */
public abstract class InventoryMovement extends IdentifiedEntity {

    private String productoId;
    private int cantidad;
    private String fecha;

    protected InventoryMovement() {
        super();
    }

    protected InventoryMovement(String id, String productoId, int cantidad, String fecha) {
        super(id);
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.fecha = fecha;
    }

    /**
     * Aplica el efecto del movimiento sobre el stock del producto.
     *
     * @param producto producto afectado
     * @throws ValidationException si el movimiento no puede aplicarse
     */
    public abstract void aplicarA(Product producto);

    /** @return prefijo usado para generar el ID ({@code ENT}, {@code SAL}, ...) */
    public abstract String getPrefijoId();

    /**
     * Comprueba los datos mínimos del movimiento.
     *
     * @throws ValidationException si falta el producto o la cantidad no es positiva
     */
    public void validar() {
        if (TextUtil.esVacio(productoId)) {
            throw new ValidationException("Debe seleccionar un producto");
        }
        if (cantidad <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
    }

    /**
     * Marca el movimiento como registrado asignándole ID y fecha.
     *
     * @param nuevoId ID generado por el servicio
     * @param fechaRegistro fecha usada si el movimiento no trae una
     */
    public void registrar(String nuevoId, String fechaRegistro) {
        setId(nuevoId);
        if (TextUtil.esVacio(fecha)) {
            fecha = fechaRegistro;
        }
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
