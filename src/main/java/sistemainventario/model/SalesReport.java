package sistemainventario.model;

import java.util.List;

/**
 * Informe de ventas de un período: totales, comparación con el período
 * anterior, ventas agrupadas en el tiempo, productos más vendidos y métodos de
 * pago. Lo construye {@code ReportService} y la vista lo muestra o lo exporta.
 *
 * @param desde         primer día del período (yyyy-MM-dd)
 * @param hasta         último día del período (yyyy-MM-dd)
 * @param anteriorDesde primer día del período de comparación
 * @param anteriorHasta último día del período de comparación
 * @param agrupacion    cómo se agrupa la serie: {@code dia}, {@code semana} o {@code mes}
 * @param actual        totales del período
 * @param anterior      totales del período anterior de la misma duración
 * @param serie         ventas y utilidad por día, semana o mes
 * @param productos     productos vendidos, de más a menos unidades
 * @param metodosPago   ventas por método de pago
 * @param ventas        cada venta del período, para el detalle del informe
 */
public record SalesReport(String desde,
                          String hasta,
                          String anteriorDesde,
                          String anteriorHasta,
                          String agrupacion,
                          Totals actual,
                          Totals anterior,
                          List<Point> serie,
                          List<ProductRanking> productos,
                          List<PaymentShare> metodosPago,
                          List<SaleLine> ventas) {

    /**
     * @param ventas            valor vendido
     * @param numeroVentas      cantidad de ventas
     * @param unidades          unidades vendidas
     * @param ticketPromedio    valor promedio por venta
     * @param utilidad          ganancia de lo vendido (precio menos costo)
     * @param unidadesSinCosto  unidades vendidas cuyo producto no tiene precio de compra
     */
    public record Totals(double ventas, int numeroVentas, int unidades, double ticketPromedio,
                         double utilidad, int unidadesSinCosto) {

        /** @return margen de utilidad sobre lo vendido, en porcentaje */
        public double margen() {
            return ventas > 0 ? utilidad / ventas * 100 : 0;
        }
    }

    /**
     * @param etiqueta texto corto para la gráfica (por ejemplo "12 nov" o "nov 2026")
     * @param desde    primer día del grupo (yyyy-MM-dd)
     * @param ventas   valor vendido en el grupo
     * @param utilidad utilidad del grupo
     * @param numeroVentas cantidad de ventas del grupo
     */
    public record Point(String etiqueta, String desde, double ventas, double utilidad, int numeroVentas) {
    }

    /**
     * @param productoId id del producto
     * @param nombre     nombre con el que se vendió
     * @param unidades   unidades vendidas
     * @param ventas     valor vendido
     * @param utilidad   utilidad (0 si no tiene costo)
     * @param conCosto   si se pudo calcular la utilidad
     */
    public record ProductRanking(String productoId, String nombre, int unidades, double ventas,
                                 double utilidad, boolean conCosto) {
    }

    /**
     * @param metodo       método de pago (Efectivo, Nequi, Tarjeta...)
     * @param ventas       valor vendido con ese método
     * @param numeroVentas cantidad de ventas con ese método
     */
    public record PaymentShare(String metodo, double ventas, int numeroVentas) {
    }

    /**
     * Una venta del período, resumida para el detalle.
     *
     * @param fecha      fecha (yyyy-MM-dd)
     * @param id         número de la venta
     * @param cliente    cliente
     * @param metodoPago método de pago
     * @param referencia comprobante del pago digital, si lo hay
     * @param productos  productos vendidos, por ejemplo "2 × Arroz, 1 × Aceite"
     * @param unidades   unidades vendidas
     * @param total      valor de la venta
     * @param utilidad   utilidad de la venta (solo de los productos con costo)
     */
    public record SaleLine(String fecha, String id, String cliente, String metodoPago, String referencia,
                           String productos, int unidades, double total, double utilidad) {
    }
}
