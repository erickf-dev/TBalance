package sistemainventario.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuenta de un cliente que compró fiado: cuánto se le fió, cuánto ha abonado
 * y cuánto debe todavía. Se calcula con las ventas fiadas y los abonos; no se
 * guarda.
 */
public class CustomerDebt {

    private final String clave;
    private String cliente;
    private String documento;
    private double totalFiado;
    private double totalAbonado;
    private String ultimaFecha;
    private double saldo;
    private final List<Sale> ventas = new ArrayList<>();
    private final List<CreditPayment> abonos = new ArrayList<>();

    /**
     * @param clave     cédula del cliente (o su nombre, en fiados viejos sin cédula)
     * @param cliente   nombre del cliente
     * @param documento cédula, si la tiene
     */
    public CustomerDebt(String clave, String cliente, String documento) {
        this.clave = clave;
        this.cliente = cliente;
        this.documento = documento;
    }

    /** @param venta venta fiada vigente de este cliente */
    public void agregarVenta(Sale venta) {
        ventas.add(venta);
        totalFiado += venta.getTotal();
        calcularSaldo();
        recordarFecha(venta.getFechaRegistro() != null ? venta.getFechaRegistro() : venta.getFecha());
        if (venta.getCliente() != null && !venta.getCliente().isBlank()) {
            cliente = venta.getCliente();
        }
    }

    /** @param abono abono vigente de este cliente */
    public void agregarAbono(CreditPayment abono) {
        abonos.add(abono);
        totalAbonado += abono.getMonto();
        calcularSaldo();
        recordarFecha(abono.getFecha());
    }

    private void recordarFecha(String fecha) {
        if (fecha != null && (ultimaFecha == null || fecha.compareTo(ultimaFecha) > 0)) {
            ultimaFecha = fecha;
        }
    }

    private void calcularSaldo() {
        saldo = Math.max(0, Math.round((totalFiado - totalAbonado) * 100.0) / 100.0);
    }

    /** @return lo que debe todavía (0 si ya pagó todo) */
    public double getSaldo() {
        return saldo;
    }

    /** @return {@code true} si ya no debe nada */
    public boolean estaAlDia() {
        return getSaldo() < 0.5;
    }

    public String getClave() {
        return clave;
    }

    public String getCliente() {
        return cliente;
    }

    public String getDocumento() {
        return documento;
    }

    public double getTotalFiado() {
        return totalFiado;
    }

    public double getTotalAbonado() {
        return totalAbonado;
    }

    public String getUltimaFecha() {
        return ultimaFecha;
    }

    public List<Sale> getVentas() {
        return ventas;
    }

    public List<CreditPayment> getAbonos() {
        return abonos;
    }
}
