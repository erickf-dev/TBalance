package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.TextUtil;

/**
 * Línea de detalle de una {@link Sale}: producto, cantidad y precio.
 *
 * <p><b>GRASP Experto:</b> conoce cantidad y precio unitario, por lo tanto
 * es quien calcula su subtotal. El atributo {@code subtotal} se conserva
 * solo para que el JSON de P2 siga siendo compatible.</p>
 */
public class SaleItem {

    private String productoId;
    private String nombreProducto;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;

    /** Constructor vacío requerido por Gson. */
    public SaleItem() {
    }

    /**
     * Visibilidad de paquete: según GRASP Creator, solo {@link Sale}
     * (que agrega sus ítems) crea instancias con datos.
     */
    SaleItem(String productoId, String nombreProducto, int cantidad, double precioUnitario) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        sincronizarSubtotal();
    }

    /**
     * @throws ValidationException si falta el producto, la cantidad no es
     *                             positiva o el precio es negativo
     */
    public void validar() {
        if (TextUtil.esVacio(productoId)) {
            throw new ValidationException("Cada item de la venta debe tener un producto");
        }
        if (cantidad <= 0) {
            throw new ValidationException("La cantidad vendida debe ser mayor que cero");
        }
        if (precioUnitario < 0) {
            throw new ValidationException("El precio unitario no puede ser negativo");
        }
    }

    /** @return cantidad × precio unitario, siempre calculado */
    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    /** Actualiza el atributo persistido con el subtotal calculado. */
    void sincronizarSubtotal() {
        this.subtotal = getSubtotal();
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        sincronizarSubtotal();
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
        sincronizarSubtotal();
    }
}
