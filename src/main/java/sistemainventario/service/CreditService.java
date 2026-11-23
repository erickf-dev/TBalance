package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.CreditPayment;
import sistemainventario.model.CustomerDebt;
import sistemainventario.model.PaymentMethod;
import sistemainventario.model.Sale;
import sistemainventario.repository.Repository;
import sistemainventario.util.DateUtil;
import sistemainventario.util.IdGenerator;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Caso de uso "fiados": quién le debe al negocio, cuánto, y los abonos con
 * que va pagando.
 *
 * <p>La deuda no se guarda aparte: se calcula con las ventas fiadas vigentes
 * y los abonos vigentes. Así, si se anula una venta fiada o un abono, el saldo
 * del cliente se corrige solo.</p>
 */
public class CreditService {

    /** Margen para no dejar saldos de centavos por redondeo. */
    private static final double TOLERANCIA = 0.5;

    private final SaleService ventas;
    private final Repository<CreditPayment> abonos;

    /**
     * @param ventas ventas del negocio (de ahí salen los fiados)
     * @param abonos abonos guardados del negocio
     */
    public CreditService(SaleService ventas, Repository<CreditPayment> abonos) {
        this.ventas = Objects.requireNonNull(ventas);
        this.abonos = Objects.requireNonNull(abonos);
    }

    /**
     * @return las cuentas de todos los clientes con fiados: primero los que
     * deben más; al final los que ya están al día
     */
    public List<CustomerDebt> listarCuentas() {
        List<CustomerDebt> cuentas = new ArrayList<>(armarCuentas().values());
        cuentas.sort(Comparator.comparing(CustomerDebt::estaAlDia)
                .thenComparing(Comparator.comparingDouble(CustomerDebt::getSaldo).reversed())
                .thenComparing(c -> c.getCliente() == null ? "" : c.getCliente()));
        return cuentas;
    }

    /**
     * @param clave cédula del cliente (o la clave de un fiado sin cédula)
     * @return la cuenta con sus ventas fiadas y sus abonos
     * @throws ValidationException si el cliente no tiene fiados
     */
    public CustomerDebt obtenerCuenta(String clave) {
        CustomerDebt cuenta = armarCuentas().get(clave);
        if (cuenta == null) {
            throw new ValidationException("Ese cliente no tiene fiados en este negocio");
        }
        return cuenta;
    }

    /** @return lo que deben entre todos los clientes */
    public double totalPorCobrar() {
        return armarCuentas().values().stream().mapToDouble(CustomerDebt::getSaldo).sum();
    }

    /**
     * Registra un abono. No se puede abonar más de lo que el cliente debe ni
     * con un medio de pago que el negocio no recibe.
     *
     * @param abono datos del abono (cliente, valor, medio de pago, nota)
     * @return el abono guardado con su ID y fecha
     * @throws ValidationException si falta un dato o el valor pasa del saldo
     */
    public CreditPayment registrarAbono(CreditPayment abono) {
        if (abono == null) {
            throw new ValidationException("No se recibieron los datos del abono");
        }
        abono.validar();
        abonos.refrescar();
        if (!ventas.aceptaMedioPago(abono.getMetodoPago())) {
            throw new ValidationException("Este negocio no tiene activo el pago con " + abono.getMetodoPago());
        }
        CustomerDebt cuenta = obtenerCuenta(abono.getClienteDocumento());
        if (cuenta.estaAlDia()) {
            throw new ValidationException(cuenta.getCliente() + " no debe nada");
        }
        if (abono.getMonto() > cuenta.getSaldo() + TOLERANCIA) {
            throw new ValidationException("El abono pasa de lo que debe " + cuenta.getCliente()
                    + ". Debe $ " + String.format(Locale.ROOT, "%,.0f", cuenta.getSaldo()).replace(',', '.'));
        }
        abono.setCliente(cuenta.getCliente());
        abono.setId(IdGenerator.conPrefijo(CreditPayment.PREFIJO_ID));
        abono.setFecha(DateUtil.ahora());
        abono.setEstado(CreditPayment.VIGENTE);
        abonos.agregar(abono);
        return abono;
    }

    /**
     * Anula un abono registrado por error; el saldo del cliente vuelve a subir.
     *
     * @param id     ID del abono
     * @param motivo por qué se anula
     * @param quien  nombre de quien anula
     * @return el abono anulado
     */
    public CreditPayment anularAbono(String id, String motivo, String quien) {
        CreditPayment abono = abonos.buscarPorId(id)
                .orElseThrow(() -> new ValidationException("Abono no encontrado"));
        abono.anular(DateUtil.ahora(), motivo);
        abono.setAnuladaPor(quien);
        abonos.actualizar(abono);
        return abono;
    }

    /** @return todos los abonos del negocio, también los anulados */
    public List<CreditPayment> listarAbonos() {
        return abonos.listarTodos();
    }

    /**
     * Clave con la que se agrupan los fiados de un cliente: su cédula, o su
     * nombre (sin tildes ni mayúsculas) en ventas fiadas viejas sin cédula.
     *
     * @param venta venta fiada
     * @return la clave del cliente
     */
    public static String claveCliente(Sale venta) {
        String documento = venta.getClienteDocumento();
        if (documento != null && !documento.isBlank()) {
            return documento;
        }
        String nombre = venta.getCliente() == null ? "" : venta.getCliente();
        return "N:" + Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private Map<String, CustomerDebt> armarCuentas() {
        Map<String, CustomerDebt> cuentas = new LinkedHashMap<>();
        String fiado = PaymentMethod.FIADO.getEtiqueta();
        for (Sale venta : ventas.listarTodas()) {
            if (!fiado.equals(venta.getMetodoPago()) || venta.isAnulada()) {
                continue;
            }
            String clave = claveCliente(venta);
            cuentas.computeIfAbsent(clave, c -> new CustomerDebt(c, venta.getCliente(), venta.getClienteDocumento()))
                    .agregarVenta(venta);
        }
        for (CreditPayment abono : abonos.listarTodos()) {
            CustomerDebt cuenta = cuentas.get(abono.getClienteDocumento());
            if (cuenta != null && !abono.isAnulada()) {
                cuenta.agregarAbono(abono);
            }
        }
        return cuentas;
    }
}
