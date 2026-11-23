package sistemainventario.model;

import sistemainventario.exception.ValidationException;

/**
 * Abono: plata que un cliente entrega para pagar lo que quedó debiendo de
 * sus ventas fiadas. Como las ventas, no se borra: si se registró por error
 * se anula con un motivo.
 */
public class CreditPayment extends IdentifiedEntity implements Anulable {

    /** Prefijo de los IDs de los abonos. */
    public static final String PREFIJO_ID = "ABO";

    private String cliente;
    private String clienteDocumento;
    private double monto;
    private String metodoPago;
    private String referenciaPago;
    private String nota;
    private String fecha;
    private String usuarioId;
    private String registradoPor;
    private String estado;
    private String fechaAnulacion;
    private String motivoAnulacion;
    private String anuladaPor;

    public CreditPayment() {
    }

    /**
     * @param clienteDocumento cédula de quien abona (o la clave del cliente sin cédula)
     * @param monto            valor del abono
     * @param metodoPago       cómo pagó
     */
    public CreditPayment(String clienteDocumento, double monto, String metodoPago) {
        this.clienteDocumento = clienteDocumento;
        this.monto = monto;
        this.metodoPago = metodoPago;
    }

    /**
     * Revisa que el abono tenga cliente, un valor positivo y un medio de pago
     * válido (no se puede abonar "fiando" otra vez).
     *
     * @throws ValidationException si falta algún dato
     */
    public void validar() {
        if (clienteDocumento == null || clienteDocumento.isBlank()) {
            throw new ValidationException("Elija el cliente que abona");
        }
        if (!(monto > 0)) {
            throw new ValidationException("Escriba cuánto abona el cliente");
        }
        PaymentMethod metodo = PaymentMethod.desdeTexto(metodoPago)
                .orElseThrow(() -> new ValidationException("Elija cómo pagó el abono"));
        if (metodo == PaymentMethod.FIADO) {
            throw new ValidationException("Un abono no se puede fiar: elija cómo pagó");
        }
        metodoPago = metodo.getEtiqueta();
        if (!metodo.esDigital() || referenciaPago == null || referenciaPago.isBlank()) {
            referenciaPago = null;
        } else {
            referenciaPago = referenciaPago.trim();
        }
        nota = nota == null || nota.isBlank() ? null : nota.trim();
    }

    @Override
    public boolean isAnulada() {
        return ANULADA.equals(estado);
    }

    /** @return {@code COMPLETADA} o {@code ANULADA} */
    public String getEstado() {
        return estado == null ? VIGENTE : estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public void anular(String fecha, String motivo) {
        if (isAnulada()) {
            throw new ValidationException("Ese abono ya estaba anulado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ValidationException("Escriba el motivo de la anulación");
        }
        this.estado = ANULADA;
        this.fechaAnulacion = fecha;
        this.motivoAnulacion = motivo.trim();
    }

    /**
     * @param fechaAnulacion  fecha de la anulación (al leer de la base)
     * @param motivoAnulacion motivo (al leer de la base)
     */
    public void setAnulacion(String fechaAnulacion, String motivoAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
        this.motivoAnulacion = motivoAnulacion;
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
    public String getAnuladaPor() {
        return anuladaPor;
    }

    @Override
    public void setAnuladaPor(String anuladaPor) {
        this.anuladaPor = anuladaPor;
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

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }
}
