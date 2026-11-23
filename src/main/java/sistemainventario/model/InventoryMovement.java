package sistemainventario.model;

import sistemainventario.exception.ValidationException;
import sistemainventario.util.TextUtil;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
public abstract class InventoryMovement extends IdentifiedEntity implements Anulable {

    private String productoId;
    private int cantidad;
    private String fecha;
    private String fechaRegistro;
    /** {@code null} en los datos anteriores, que cuentan como vigentes. */
    private String estado;
    private String fechaAnulacion;
    private String motivoAnulacion;
    private String anuladaPor;

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
        validarFecha(LocalDate.now());
    }

    /**
     * La fecha es la del movimiento real (por ejemplo, el día en que llegó la
     * mercancía), que puede ser anterior al registro pero nunca futura.
     *
     * @param hoy fecha de referencia
     * @throws ValidationException si la fecha no es válida o es futura
     */
    void validarFecha(LocalDate hoy) {
        if (TextUtil.esVacio(fecha)) {
            return;
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha.length() > 10 ? fecha.substring(0, 10) : fecha);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Fecha inválida: " + fecha);
        }
        if (dia.isAfter(hoy)) {
            throw new ValidationException("La fecha no puede ser futura");
        }
    }

    /**
     * Marca el movimiento como registrado asignándole ID y fecha.
     *
     * @param nuevoId ID generado por el servicio
     * @param fechaRegistro fecha y hora del registro; también es la fecha del
     *                      movimiento si no trae una
     */
    public void registrar(String nuevoId, String fechaRegistro) {
        setId(nuevoId);
        this.fechaRegistro = fechaRegistro;
        if (TextUtil.esVacio(fecha)) {
            fecha = fechaRegistro.length() > 10 ? fechaRegistro.substring(0, 10) : fechaRegistro;
        }
    }

    /** @return fecha y hora en que se registró en TBalance (puede ser posterior a {@link #getFecha()}) */
    public String getFechaRegistro() {
        return fechaRegistro;
    }

    /** @param fechaRegistro fecha y hora del registro (al leer de la base) */
    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
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

    @Override
    public boolean isAnulada() {
        return ANULADA.equals(estado);
    }

    /** @return estado guardado ({@code COMPLETADA} o {@code ANULADA}) */
    public String getEstado() {
        return estado == null ? VIGENTE : estado;
    }

    /** @param estado estado leído de la base */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String getFechaAnulacion() {
        return fechaAnulacion;
    }

    @Override
    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    @Override
    public void anular(String fecha, String motivo) {
        if (isAnulada()) {
            throw new ValidationException("Ya estaba anulada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ValidationException("Escriba el motivo de la anulación");
        }
        this.estado = ANULADA;
        this.fechaAnulacion = fecha;
        this.motivoAnulacion = motivo.trim();
    }

    /**
     * @param fechaAnulacion fecha de la anulación (al leer de la base)
     * @param motivoAnulacion motivo de la anulación (al leer de la base)
     */
    public void setAnulacion(String fechaAnulacion, String motivoAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
        this.motivoAnulacion = motivoAnulacion;
    }

    @Override
    public String getAnuladaPor() {
        return anuladaPor;
    }

    @Override
    public void setAnuladaPor(String anuladaPor) {
        this.anuladaPor = anuladaPor;
    }
}
