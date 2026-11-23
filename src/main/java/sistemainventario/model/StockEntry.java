package sistemainventario.model;

/**
 * Ingreso de mercancía al inventario: incrementa el stock del producto.
 */
public class StockEntry extends InventoryMovement {

    public static final String PREFIJO_ID = "ENT";

    private String proveedorId;
    private double precioUnitario;
    private String observacion;

    /** Constructor vacío requerido por Gson. */
    public StockEntry() {
        super();
    }

    public StockEntry(String id, String productoId, String proveedorId,
                   int cantidad, double precioUnitario,
                   String fecha, String observacion) {
        super(id, productoId, cantidad, fecha);
        this.proveedorId = proveedorId;
        this.precioUnitario = precioUnitario;
        this.observacion = observacion;
    }

    /**
     * Polimorfismo: una entrada suma unidades al producto. Si trae costo por
     * unidad y es distinto, el producto queda con ese costo (último costo de compra).
     */
    @Override
    public void aplicarA(Product producto) {
        producto.aumentarStock(getCantidad());
        if (precioUnitario > 0 && precioUnitario != producto.getPrecioCompra()) {
            producto.setPrecioCompra(precioUnitario);
        }
    }

    @Override
    public String getPrefijoId() {
        return PREFIJO_ID;
    }

    public String getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(String proveedorId) {
        this.proveedorId = proveedorId;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
