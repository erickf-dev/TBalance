package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.model.Sale;
import sistemainventario.model.SaleItem;
import sistemainventario.model.SalesReport;
import sistemainventario.util.DateUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcula los informes de ventas de un período.
 *
 * <p><b>GRASP Experto:</b> reúne las ventas ({@link SaleService}) y los costos
 * de los productos ({@link InventoryService}) para responder preguntas como
 * "¿cuánto vendí esta semana?", "¿qué se vende más?" o "¿cuánto gané?".
 * <b>SRP:</b> solo calcula; mostrar o exportar el informe lo hacen otras clases.</p>
 *
 * <p>La utilidad usa el costo guardado en cada venta; si la venta es anterior a
 * v3.4.0 y no lo tiene, usa el precio de compra actual del producto.</p>
 */
public class ReportService {

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"};
    private static final int DIAS_MAX_POR_DIA = 31;
    private static final int DIAS_MAX_POR_SEMANA = 120;
    private static final int TOPE_PRODUCTOS = 50;

    private final SaleService ventas;
    private final InventoryService inventario;

    public ReportService(SaleService ventas, InventoryService inventario) {
        this.ventas = Objects.requireNonNull(ventas);
        this.inventario = Objects.requireNonNull(inventario);
    }

    /**
     * @param desdeIso primer día (yyyy-MM-dd)
     * @param hastaIso último día (yyyy-MM-dd), incluido
     * @return el informe del período
     * @throws ValidationException si las fechas no son válidas
     */
    public SalesReport generar(String desdeIso, String hastaIso) {
        return generar(desdeIso, hastaIso, null, null);
    }

    /**
     * @param desdeIso         primer día (yyyy-MM-dd)
     * @param hastaIso         último día (yyyy-MM-dd), incluido
     * @param anteriorDesdeIso inicio del período con el que se compara; si viene vacío se usa
     *                         el período inmediatamente anterior de la misma duración
     * @param anteriorHastaIso fin del período con el que se compara
     * @return el informe del período
     */
    public SalesReport generar(String desdeIso, String hastaIso, String anteriorDesdeIso, String anteriorHastaIso) {
        LocalDate desde = DateUtil.parsear(desdeIso)
                .orElseThrow(() -> new ValidationException("La fecha inicial no es válida"));
        LocalDate hasta = DateUtil.parsear(hastaIso)
                .orElseThrow(() -> new ValidationException("La fecha final no es válida"));
        if (hasta.isBefore(desde)) {
            throw new ValidationException("La fecha final no puede ser anterior a la inicial");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate desdeAnterior = DateUtil.parsear(anteriorDesdeIso).orElse(desde.minusDays(dias));
        LocalDate hastaAnterior = DateUtil.parsear(anteriorHastaIso).orElse(desde.minusDays(1));

        List<Sale> delPeriodo = ventasEntre(desde, hasta);
        String agrupacion = dias <= DIAS_MAX_POR_DIA ? "dia" : (dias <= DIAS_MAX_POR_SEMANA ? "semana" : "mes");

        return new SalesReport(desde.toString(), hasta.toString(),
                desdeAnterior.toString(), hastaAnterior.toString(), agrupacion,
                conUtilidadDeLaBase(totales(delPeriodo), desde, hasta),
                conUtilidadDeLaBase(totales(ventasEntre(desdeAnterior, hastaAnterior)), desdeAnterior, hastaAnterior),
                serie(delPeriodo, desde, hasta, agrupacion),
                ranking(delPeriodo),
                metodosPago(delPeriodo),
                detalle(delPeriodo));
    }

    /** @return cada venta del período, de la más reciente a la más antigua */
    private List<SalesReport.SaleLine> detalle(List<Sale> lista) {
        List<SalesReport.SaleLine> lineas = new ArrayList<>();
        for (Sale venta : lista) {
            Acumulado suma = new Acumulado();
            StringBuilder productos = new StringBuilder();
            for (SaleItem item : venta.getItems()) {
                suma.sumar(item, costoDe(item));
                productos.append(productos.length() > 0 ? ", " : "")
                        .append(item.getCantidad()).append(" × ").append(nombreDe(item));
            }
            lineas.add(new SalesReport.SaleLine(fechaDe(venta).map(LocalDate::toString).orElse(""), venta.getId(),
                    venta.getCliente(), venta.getMetodoPago(), venta.getReferenciaPago(), productos.toString(),
                    suma.unidades, venta.getTotal(), suma.utilidad));
        }
        lineas.sort(Comparator.comparing(SalesReport.SaleLine::fecha).reversed());
        return lineas;
    }

    /** @return ventas completadas cuya fecha está entre las dos fechas (incluidas) */
    private List<Sale> ventasEntre(LocalDate desde, LocalDate hasta) {
        List<Sale> resultado = new ArrayList<>();
        for (Sale venta : ventas.listarTodas()) {
            Optional<LocalDate> fecha = fechaDe(venta);
            if (venta.estaCompletada() && fecha.isPresent()
                    && !fecha.get().isBefore(desde) && !fecha.get().isAfter(hasta)) {
                resultado.add(venta);
            }
        }
        return resultado;
    }

    private static Optional<LocalDate> fechaDe(Sale venta) {
        String fecha = venta.getFecha();
        return fecha == null || fecha.length() < 10 ? Optional.empty() : DateUtil.parsear(fecha.substring(0, 10));
    }

    /** @return el costo del ítem: el guardado en la venta o, si no hay, el actual del producto */
    private double costoDe(SaleItem item) {
        if (item.tieneCosto()) {
            return item.getCostoUnitario();
        }
        return inventario.buscarPorId(item.getProductoId())
                .filter(Product::tienePrecioCompra)
                .map(Product::getPrecioCompra)
                .orElse(0.0);
    }

    /** Oracle: la utilidad del periodo la calcula FN_UTILIDAD_PERIODO. */
    private SalesReport.Totals conUtilidadDeLaBase(SalesReport.Totals t, LocalDate desde, LocalDate hasta) {
        return ventas.rutinas()
                .map(r -> new SalesReport.Totals(t.ventas(), t.numeroVentas(), t.unidades(), t.ticketPromedio(),
                        r.utilidadPeriodo(desde, hasta), t.unidadesSinCosto()))
                .orElse(t);
    }

    private SalesReport.Totals totales(List<Sale> lista) {
        double total = 0;
        double utilidad = 0;
        int unidades = 0;
        int sinCosto = 0;
        for (Sale venta : lista) {
            total += venta.getTotal();
            for (SaleItem item : venta.getItems()) {
                unidades += item.getCantidad();
                double costo = costoDe(item);
                if (costo > 0) {
                    utilidad += item.utilidadCon(costo);
                } else {
                    sinCosto += item.getCantidad();
                }
            }
        }
        double ticket = lista.isEmpty() ? 0 : total / lista.size();
        return new SalesReport.Totals(total, lista.size(), unidades, ticket, utilidad, sinCosto);
    }

    /** Agrupa las ventas por día, semana (desde el lunes) o mes, incluyendo los grupos sin ventas. */
    private List<SalesReport.Point> serie(List<Sale> lista, LocalDate desde, LocalDate hasta, String agrupacion) {
        Map<LocalDate, Acumulado> grupos = new LinkedHashMap<>();
        for (LocalDate g = inicioDeGrupo(desde, agrupacion); !g.isAfter(hasta); g = siguienteGrupo(g, agrupacion)) {
            grupos.put(g, new Acumulado());
        }
        for (Sale venta : lista) {
            LocalDate clave = inicioDeGrupo(fechaDe(venta).orElse(desde), agrupacion);
            Acumulado grupo = grupos.computeIfAbsent(clave, k -> new Acumulado());
            grupo.numero++;
            for (SaleItem item : venta.getItems()) {
                grupo.sumar(item, costoDe(item));
            }
        }
        List<SalesReport.Point> puntos = new ArrayList<>();
        grupos.forEach((inicio, g) -> puntos.add(new SalesReport.Point(
                etiqueta(inicio, agrupacion), inicio.toString(), g.ventas, g.utilidad, g.numero)));
        return puntos;
    }

    private static LocalDate inicioDeGrupo(LocalDate fecha, String agrupacion) {
        switch (agrupacion) {
            case "semana":
                return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes":
                return fecha.withDayOfMonth(1);
            default:
                return fecha;
        }
    }

    private static LocalDate siguienteGrupo(LocalDate inicio, String agrupacion) {
        switch (agrupacion) {
            case "semana":
                return inicio.plusWeeks(1);
            case "mes":
                return inicio.plusMonths(1);
            default:
                return inicio.plusDays(1);
        }
    }

    private static String etiqueta(LocalDate inicio, String agrupacion) {
        String mes = MESES[inicio.getMonthValue() - 1];
        if ("mes".equals(agrupacion)) {
            return mes + " " + inicio.getYear();
        }
        return ("semana".equals(agrupacion) ? "sem. " : "") + inicio.getDayOfMonth() + " " + mes;
    }

    /** @return productos vendidos, de más a menos unidades (máximo 50) */
    private List<SalesReport.ProductRanking> ranking(List<Sale> lista) {
        Map<String, Acumulado> porProducto = new HashMap<>();
        for (Sale venta : lista) {
            for (SaleItem item : venta.getItems()) {
                Acumulado producto = porProducto.computeIfAbsent(item.getProductoId(), id -> new Acumulado());
                producto.nombre = nombreDe(item);
                producto.sumar(item, costoDe(item));
            }
        }
        List<SalesReport.ProductRanking> resultado = new ArrayList<>();
        porProducto.forEach((id, p) -> resultado.add(new SalesReport.ProductRanking(
                id, p.nombre, p.unidades, p.ventas, p.utilidad, p.conCosto)));
        resultado.sort(Comparator.comparingInt(SalesReport.ProductRanking::unidades).reversed()
                .thenComparing(Comparator.comparingDouble(SalesReport.ProductRanking::ventas).reversed()));
        return resultado.size() > TOPE_PRODUCTOS ? resultado.subList(0, TOPE_PRODUCTOS) : resultado;
    }

    private String nombreDe(SaleItem item) {
        return inventario.buscarPorId(item.getProductoId()).map(Product::getNombre)
                .orElse(item.getNombreProducto() == null ? "Producto eliminado" : item.getNombreProducto());
    }

    private List<SalesReport.PaymentShare> metodosPago(List<Sale> lista) {
        Map<String, double[]> porMetodo = new LinkedHashMap<>();
        for (Sale venta : lista) {
            String metodo = venta.getMetodoPago() == null || venta.getMetodoPago().isBlank() ? "Sin especificar" : venta.getMetodoPago();
            double[] v = porMetodo.computeIfAbsent(metodo, k -> new double[2]);
            v[0] += venta.getTotal();
            v[1]++;
        }
        List<SalesReport.PaymentShare> resultado = new ArrayList<>();
        porMetodo.forEach((m, v) -> resultado.add(new SalesReport.PaymentShare(m, v[0], (int) v[1])));
        resultado.sort(Comparator.comparingDouble(SalesReport.PaymentShare::ventas).reversed());
        return resultado;
    }

    /** Suma de ventas, unidades y utilidad de un grupo (un día, una semana, un producto...). */
    private static final class Acumulado {
        private String nombre;
        private double ventas;
        private double utilidad;
        private int unidades;
        private int numero;
        private boolean conCosto = true;

        private void sumar(SaleItem item, double costo) {
            ventas += item.getSubtotal();
            unidades += item.getCantidad();
            if (costo > 0) {
                utilidad += item.utilidadCon(costo);
            } else {
                conCosto = false;
            }
        }
    }
}
