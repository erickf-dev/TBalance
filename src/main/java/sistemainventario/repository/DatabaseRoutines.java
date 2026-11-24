package sistemainventario.repository;

import sistemainventario.model.AuditEntry;
import sistemainventario.model.CreditPayment;
import sistemainventario.model.Sale;

import java.time.LocalDate;

/**
 * Operaciones que hace la base de datos por su cuenta, con procedimientos y
 * funciones almacenados (PL/SQL en Oracle).
 *
 * <p>La implementa {@link OracleRoutines}. Mientras no hay conexión no hay
 * rutinas, pero entonces tampoco se puede entrar a TBalance.</p>
 */
public interface DatabaseRoutines {

    /**
     * SP_REGISTRAR_VENTA: revisa el stock, guarda la venta con su detalle y
     * descuenta las unidades, todo en una transacción.
     *
     * @param venta venta ya validada, con ID y fecha
     * @return el total que guardó la base
     */
    double registrarVenta(Sale venta);

    /**
     * SP_ANULAR_VENTA: anula la venta, devuelve el stock y la anota en el historial.
     *
     * @param ventaId   ID de la venta
     * @param motivo    por qué se anula
     * @param historial quién anula (fecha, usuario, rol y negocio) para AUDITORIA
     */
    void anularVenta(String ventaId, String motivo, AuditEntry historial);

    /**
     * SP_REGISTRAR_ABONO: revisa que no pase de la deuda y guarda el abono.
     *
     * @param abono abono ya validado, con ID y fecha
     * @return nombre del cliente que abonó
     */
    String registrarAbono(CreditPayment abono);

    /**
     * FN_SALDO_CLIENTE.
     *
     * @param claveCliente cédula del cliente (o "N:nombre")
     * @return lo que debe todavía
     */
    double saldoCliente(String claveCliente);

    /** @return FN_VALOR_INVENTARIO: cuánto costó lo que hay en inventario */
    double valorInventario();

    /**
     * FN_UTILIDAD_PERIODO.
     *
     * @param desde primer día (incluido)
     * @param hasta último día (incluido)
     * @return ganancia de las ventas completadas de esos días
     */
    double utilidadPeriodo(LocalDate desde, LocalDate hasta);
}
