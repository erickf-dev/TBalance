package sistemainventario.model;

import sistemainventario.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transacción comercial compuesta por uno o varios {@link SaleItem}.
 *
 * <p><b>GRASP Creator:</b> la venta contiene y registra sus ítems, por eso es
 * ella quien los crea en {@link #agregarItem(Product, int, double)}.</p>
 *
 * <p><b>GRASP Experto:</b> conoce todos sus ítems, por lo que calcula el
 * total, las unidades y las cantidades agrupadas por producto.</p>
 */
public class Sale extends IdentifiedEntity implements Anulable {

    public static final String PREFIJO_ID = "VTA";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";

    private String cliente;
    /** Cédula u otro documento del cliente; obligatoria cuando la venta es fiada. */
    private String clienteDocumento;
    private String metodoPago;
    private String observacion;
    /** Número de comprobante de un pago digital (Nequi, Daviplata, tarjeta...); opcional. */
    private String referenciaPago;
    private String fecha;
    private String estado;
    private String fechaAnulacion;
    private String motivoAnulacion;
    private String anuladaPor;
    /** Quién registró la venta (para que el cajero solo anule las suyas). */
    private String usuarioId;
    private String vendedor;
    /** Fecha y hora exactas en que se registró (la fecha de la venta es solo el día). */
    private String fechaRegistro;
    private List<SaleItem> items = new ArrayList<>();
    private double total;

    /** Constructor vacío requerido por Gson. */
    public Sale() {
        super();
    }

    public Sale(String id, String cliente, String metodoPago, String observacion, String fecha) {
        super(id);
        this.cliente = cliente;
        this.metodoPago = metodoPago;
        this.observacion = observacion;
        this.fecha = fecha;
        this.estado = ESTADO_COMPLETADA;
    }

    /**
     * <b>Creator:</b> crea y agrega un ítem a partir de un producto.
     *
     * @param producto       producto vendido
     * @param cantidad       unidades vendidas
     * @param precioUnitario precio aplicado en la venta
     * @return el ítem creado
     */
    public SaleItem agregarItem(Product producto, int cantidad, double precioUnitario) {
        SaleItem item = new SaleItem(producto.getId(), producto.getNombre(), cantidad, precioUnitario);
        item.setCostoUnitario(producto.getPrecioCompra());
        item.validar();
        itemsSeguros().add(item);
        recalcularTotal();
        return item;
    }

    /**
     * @throws ValidationException si la venta no tiene ítems o alguno es inválido
     */
    public void validar() {
        if (itemsSeguros().isEmpty()) {
            throw new ValidationException("La venta debe tener al menos un producto");
        }
        itemsSeguros().forEach(SaleItem::validar);
    }

    /**
     * Asigna ID, fecha y estado COMPLETADA, y sincroniza el total persistido.
     *
     * @param nuevoId        ID generado por el servicio
     * @param fechaRegistro  fecha de la venta
     */
    public void completar(String nuevoId, String fechaRegistro) {
        setId(nuevoId);
        this.fecha = fechaRegistro;
        this.estado = ESTADO_COMPLETADA;
        recalcularTotal();
    }

    /** @return suma de los subtotales de los ítems */
    public double calcularTotal() {
        return itemsSeguros().stream().mapToDouble(SaleItem::getSubtotal).sum();
    }

    /** Sincroniza los atributos {@code total} y {@code subtotal} antes de serializar. */
    public void recalcularTotal() {
        itemsSeguros().forEach(SaleItem::sincronizarSubtotal);
        this.total = calcularTotal();
    }

    /** @return total de la venta, siempre consistente con sus ítems */
    public double getTotal() {
        return calcularTotal();
    }

    /** @return número total de unidades vendidas */
    public int getTotalUnidades() {
        return itemsSeguros().stream().mapToInt(SaleItem::getCantidad).sum();
    }

    /**
     * Agrupa las cantidades por producto; útil cuando el mismo producto
     * aparece en más de un ítem.
     *
     * @return mapa productoId → unidades totales
     */
    public Map<String, Integer> cantidadesPorProducto() {
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (SaleItem item : itemsSeguros()) {
            cantidades.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    /** @return {@code true} si la venta está en estado COMPLETADA */
    public boolean estaCompletada() {
        return ESTADO_COMPLETADA.equals(estado);
    }

    private List<SaleItem> itemsSeguros() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    /**
     * Deja el método de pago con su nombre oficial y revisa sus reglas: un
     * fiado necesita el nombre y la cédula del cliente (para saber quién debe),
     * una venta de contado sin cliente queda como "Consumidor final" y solo los
     * pagos digitales llevan comprobante.
     *
     * @throws ValidationException si el método no se reconoce o falta el cliente del fiado
     */
    public void normalizarPago() {
        PaymentMethod metodo = PaymentMethod.desdeTexto(metodoPago)
                .orElseThrow(() -> new ValidationException("Elija un método de pago válido"));
        metodoPago = metodo.getEtiqueta();
        if (metodo == PaymentMethod.FIADO && (cliente == null || cliente.isBlank()
                || cliente.trim().toLowerCase().startsWith("cliente"))) {
            throw new ValidationException("Para fiar escriba el nombre del cliente");
        }
        String documento = clienteDocumento == null ? "" : clienteDocumento.replaceAll("[\\s.]", "");
        if (metodo == PaymentMethod.FIADO && !documento.matches("[0-9A-Za-z-]{5,15}")) {
            throw new ValidationException("Para fiar escriba la cédula del cliente");
        }
        clienteDocumento = documento.isEmpty() ? null : documento;
        if (metodo != PaymentMethod.FIADO && (cliente == null || cliente.isBlank()
                || cliente.trim().equalsIgnoreCase("cliente"))) {
            cliente = "Consumidor final";
        }
        if (!metodo.esDigital() || referenciaPago == null || referenciaPago.isBlank()) {
            referenciaPago = null;
        } else {
            referenciaPago = referenciaPago.trim();
        }
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /** @return vista de solo lectura de los ítems */
    public List<SaleItem> getItems() {
        return Collections.unmodifiableList(itemsSeguros());
    }

    public void setItems(List<SaleItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    @Override
    public boolean isAnulada() {
        return ANULADA.equals(estado);
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

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** @return nombre de quien vendió, o {@code null} en ventas de antes del login */
    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
