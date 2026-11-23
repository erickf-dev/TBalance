package sistemainventario.service;

import sistemainventario.model.Permiso;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Category;
import sistemainventario.model.PaymentMethod;
import sistemainventario.model.Product;
import sistemainventario.model.Sale;
import sistemainventario.model.SalesReport;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Supplier;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Funciones del inventario que el asistente puede usar.
 *
 * <p>Las consultas (stock, qué reponer, ventas) responden de inmediato. Lo que
 * cambia el inventario (ventas, entradas, salidas, precios, productos nuevos)
 * nunca se hace directamente: se crea una {@link AssistantProposal} que el
 * usuario debe confirmar en la ventana del asistente.</p>
 *
 * <p><b>GRASP Experto / Fabricación pura:</b> traduce lo que pide el modelo a
 * llamadas a los servicios que ya existen ({@link InventoryService},
 * {@link SaleService}, {@link MovementService}, {@link ReportService}); no
 * duplica reglas de negocio.</p>
 */
public class AssistantTools {

    private static final String OBSERVACION = "Registrado con el asistente";

    private final ProfileServices servicios;
    private final java.util.Set<Permiso> permisos;
    private final Consumer<AssistantProposal> alProponer;
    private int contador;

    /**
     * @param servicios  servicios del perfil activo
     * @param alProponer recibe cada cambio propuesto para mostrarlo al usuario
     */
    public AssistantTools(ProfileServices servicios, Consumer<AssistantProposal> alProponer) {
        this(servicios, alProponer, java.util.EnumSet.allOf(Permiso.class));
    }

    /**
     * @param servicios  servicios del negocio abierto
     * @param alProponer recibe cada cambio propuesto para que el usuario lo confirme
     * @param permisos   lo que puede hacer el rol de quien pregunta (el cajero no ve
     *                   costos ni propone entradas o cambios de precio)
     */
    public AssistantTools(ProfileServices servicios, Consumer<AssistantProposal> alProponer, java.util.Set<Permiso> permisos) {
        this.servicios = servicios;
        this.alProponer = alProponer;
        this.permisos = java.util.EnumSet.copyOf(permisos.isEmpty() ? java.util.EnumSet.noneOf(Permiso.class) : permisos);
    }

    /**
     * Permiso que pide cada función. La lista de proveedores (con teléfonos) es de quien maneja el catálogo;
     * "qué falta de un proveedor" queda para todos, porque el cajero atiende al proveedor cuando llega.
     */
    private static final java.util.Map<String, Permiso> PERMISO_FUNCION = java.util.Map.of(
            "listar_proveedores", Permiso.EDITAR_CATALOGO,
            "consultar_fiados", Permiso.VENDER,
            "proponer_venta", Permiso.VENDER,
            "proponer_entrada", Permiso.MOVIMIENTOS,
            "proponer_salida", Permiso.MOVIMIENTOS,
            "proponer_cambio_precio", Permiso.EDITAR_CATALOGO,
            "proponer_producto_nuevo", Permiso.EDITAR_CATALOGO);

    /**
     * @param tipo tipo de una propuesta ({@code venta}, {@code entrada}...)
     * @return permiso que se necesita para confirmarla
     */
    public static Permiso permisoParaConfirmar(String tipo) {
        return switch (String.valueOf(tipo)) {
            case "venta" -> Permiso.VENDER;
            case "entrada", "salida" -> Permiso.MOVIMIENTOS;
            default -> Permiso.EDITAR_CATALOGO;
        };
    }

    // ── Declaraciones para el modelo ────────────────────────────────────

    /** @return todas las funciones, en el formato de declaraciones de Gemini */
    public static JsonArray declaraciones() {
        return declaraciones(java.util.EnumSet.allOf(Permiso.class));
    }

    /**
     * El modelo solo conoce las funciones que el rol puede usar: así no ofrece, por ejemplo,
     * registrar mercancía a un cajero (y Java igual lo rechazaría al confirmar).
     *
     * @param permisos lo que puede hacer quien conversa
     * @return las funciones permitidas, en el formato de declaraciones de Gemini
     */
    public static JsonArray declaraciones(java.util.Set<Permiso> permisos) {
        JsonArray permitidas = new JsonArray();
        for (com.google.gson.JsonElement f : todasLasDeclaraciones()) {
            Permiso pedido = PERMISO_FUNCION.get(f.getAsJsonObject().get("name").getAsString());
            if (pedido == null || permisos.contains(pedido)) {
                permitidas.add(f);
            }
        }
        return permitidas;
    }

    private static JsonArray todasLasDeclaraciones() {
        JsonArray lista = new JsonArray();
        lista.add(funcion("buscar_productos",
                "Busca productos por nombre, marca o código y devuelve stock, mínimo, precios, categoría y proveedor. "
                        + "Úsala antes de responder sobre un producto. Con texto vacío lista todo el inventario.",
                objeto().prop("texto", texto("Nombre, marca o código; vacío para todos"))));
        lista.add(funcion("productos_por_reponer",
                "Productos agotados o en su stock mínimo, con la cantidad sugerida para pedir. Sirve cuando llega "
                        + "un proveedor: se puede filtrar por el nombre del proveedor o de la marca.",
                objeto().prop("proveedor", texto("Proveedor o marca, por ejemplo Postobón; vacío para todos"))
                        .prop("todos", booleano("true para listar todos los productos del proveedor aunque no falten"))));
        lista.add(funcion("listar_proveedores", "Lista los proveedores registrados y cuántos productos tiene cada uno.",
                objeto()));
        lista.add(funcion("resumen_ventas",
                "Ventas, número de ventas, ticket promedio, utilidad, comparación con el período anterior, "
                        + "TODOS los productos vendidos (productos_vendidos) y, si el período es de uno o dos días, "
                        + "cada venta con su hora y sus productos (detalle_ventas). Para preguntas como 'qué vendí "
                        + "hoy en la noche' use detalle_ventas y filtre por la hora. Al listar lo vendido, nombre "
                        + "todos los productos de productos_vendidos, no solo los más vendidos.",
                objeto().prop("periodo", enumeracion("Período", "hoy", "ayer", "semana", "mes", "anio"))
                        .prop("desde", texto("Fecha inicial yyyy-MM-dd (solo si no usa periodo)"))
                        .prop("hasta", texto("Fecha final yyyy-MM-dd (solo si no usa periodo)"))));
        lista.add(funcion("consultar_fiados",
                "Quién le debe al negocio por ventas fiadas: lo que se le fió, lo que ha abonado y lo que debe "
                        + "cada cliente, y el total por cobrar. Se puede buscar un cliente por nombre o cédula. "
                        + "Los abonos se registran en la sección Fiados.",
                objeto().prop("cliente", texto("Nombre o cédula del cliente; vacío para todos"))));
        lista.add(funcion("productos_por_vencer", "Productos vencidos o que vencen dentro de los próximos días.",
                objeto().prop("dias", entero("Días hacia adelante; por defecto 15"))));
        lista.add(funcion("proponer_venta",
                "Prepara una venta para que el usuario la confirme. No la registra todavía.",
                objeto().prop("items", items(true)).prop("metodo_pago", texto("Efectivo, Nequi, Daviplata, Bre-B, Transferencia Bancolombia, Tarjeta débito, Tarjeta crédito o Fiado (fiado exige nombre y cédula del cliente)"))
                        .prop("cliente", texto("Nombre del cliente; obligatorio si es fiado"))
                        .prop("cedula", texto("Cédula del cliente; obligatoria si es fiado")).requerido("items")));
        lista.add(funcion("proponer_entrada",
                "Prepara la entrada de mercancía (llegó un pedido) para que el usuario la confirme. Si se indica "
                        + "costo, también actualiza el precio de compra del producto.",
                objeto().prop("items", items(false)).prop("proveedor", texto("Proveedor que entregó, opcional"))
                        .requerido("items")));
        lista.add(funcion("proponer_salida",
                "Prepara una salida de inventario que no es venta (daño, vencido, consumo, pérdida) para confirmar.",
                objeto().prop("producto", texto("Producto")).prop("cantidad", entero("Unidades"))
                        .prop("motivo", texto("Motivo de la salida")).requerido("producto", "cantidad", "motivo")));
        lista.add(funcion("proponer_cambio_precio",
                "Prepara el cambio del precio de venta y/o de compra de un producto para confirmar.",
                objeto().prop("producto", texto("Producto")).prop("precio_venta", numero("Nuevo precio de venta"))
                        .prop("precio_compra", numero("Nuevo precio de compra (costo)")).requerido("producto")));
        lista.add(funcion("proponer_producto_nuevo",
                "Prepara la creación de un producto nuevo para confirmar.",
                objeto().prop("nombre", texto("Nombre")).prop("precio_venta", numero("Precio de venta"))
                        .prop("precio_compra", numero("Precio de compra")).prop("stock", entero("Stock inicial"))
                        .prop("stock_minimo", entero("Stock mínimo")).prop("categoria", texto("Categoría"))
                        .prop("proveedor", texto("Proveedor")).requerido("nombre", "precio_venta")));
        return lista;
    }

    // ── Ejecución ───────────────────────────────────────────────────────

    /**
     * @param nombre     función pedida por el modelo
     * @param argumentos argumentos en JSON
     * @return resultado en JSON para devolverle al modelo
     */
    public JsonObject ejecutar(String nombre, JsonObject argumentos) {
        Permiso pedido = PERMISO_FUNCION.get(nombre);
        if (pedido != null && !permisos.contains(pedido)) {
            return error("El rol de esta persona no permite esto. Explíquele que debe pedírselo al administrador o al dueño.");
        }
        JsonObject resultado = ejecutarSinFiltro(nombre, argumentos);
        if (!permisos.contains(Permiso.VER_GANANCIA)) {
            quitarCostos(resultado);
        }
        return resultado;
    }

    /** El cajero no ve cuánto costó la mercancía ni la ganancia. */
    private static void quitarCostos(com.google.gson.JsonElement elemento) {
        if (elemento instanceof JsonObject objeto) {
            objeto.remove("precio_compra");
            objeto.remove("utilidad");
            objeto.entrySet().forEach(e -> quitarCostos(e.getValue()));
        } else if (elemento instanceof JsonArray lista) {
            lista.forEach(AssistantTools::quitarCostos);
        }
    }

    private JsonObject ejecutarSinFiltro(String nombre, JsonObject argumentos) {
        try {
            switch (nombre) {
                case "buscar_productos": return buscarProductos(cadena(argumentos, "texto"));
                case "productos_por_reponer": return porReponer(cadena(argumentos, "proveedor"), booleano(argumentos, "todos"));
                case "listar_proveedores": return listarProveedores();
                case "resumen_ventas": return resumenVentas(argumentos);
                case "consultar_fiados": return consultarFiados(cadena(argumentos, "cliente"));
                case "productos_por_vencer": return porVencer(argumentos.has("dias") ? argumentos.get("dias").getAsInt() : 15);
                case "proponer_venta": return proponerVenta(argumentos);
                case "proponer_entrada": return proponerEntrada(argumentos);
                case "proponer_salida": return proponerSalida(argumentos);
                case "proponer_cambio_precio": return proponerCambioPrecio(argumentos);
                case "proponer_producto_nuevo": return proponerProductoNuevo(argumentos);
                default: return error("La función " + nombre + " no existe.");
            }
        } catch (ValidationException e) {
            return error(e.getMessage());
        } catch (RuntimeException e) {
            return error("No se pudo completar: " + e.getMessage());
        }
    }

    private JsonObject buscarProductos(String texto) {
        List<Product> lista = texto.isBlank() ? servicios.inventario().listarTodos() : coincidencias(texto, 12);
        JsonArray productos = new JsonArray();
        lista.stream().limit(40).forEach(p -> productos.add(resumen(p)));
        JsonObject r = new JsonObject();
        r.add("productos", productos);
        r.addProperty("total_encontrados", lista.size());
        if (lista.isEmpty()) {
            r.addProperty("nota", "No hay productos que coincidan con \"" + texto + "\".");
        }
        return r;
    }

    private JsonObject porReponer(String proveedorTexto, boolean todos) {
        List<Product> base = servicios.inventario().listarTodos();
        String filtro = "";
        if (!proveedorTexto.isBlank()) {
            Optional<Supplier> proveedor = resolverProveedor(proveedorTexto);
            String marca = normalizar(proveedorTexto);
            filtro = proveedor.map(Supplier::getNombre).orElse(proveedorTexto);
            base = base.stream().filter(p -> proveedor.map(s -> s.getId().equals(p.getProveedorId())).orElse(false)
                    || normalizar(p.getNombre()).contains(marca)).toList();
        }
        JsonArray productos = new JsonArray();
        for (Product p : base) {
            boolean falta = p.getStock() <= p.getStockMin();
            if (!falta && !todos) {
                continue;
            }
            JsonObject fila = resumen(p);
            fila.addProperty("sugerido_pedir", falta ? Math.max(1, p.getStockMin() * 2 - p.getStock()) : 0);
            fila.addProperty("estado", p.getStock() <= 0 ? "agotado" : (falta ? "bajo el mínimo" : "suficiente"));
            productos.add(fila);
        }
        JsonObject r = new JsonObject();
        if (!filtro.isEmpty()) {
            r.addProperty("proveedor", filtro);
        }
        r.add("productos", productos);
        if (productos.isEmpty()) {
            r.addProperty("nota", base.isEmpty() ? "No hay productos de " + filtro + " en el inventario."
                    : "No falta nada: todo está por encima del mínimo.");
        }
        return r;
    }

    private JsonObject listarProveedores() {
        JsonArray lista = new JsonArray();
        for (Supplier s : servicios.proveedores().listarTodos()) {
            JsonObject fila = new JsonObject();
            fila.addProperty("nombre", s.getNombre());
            fila.addProperty("telefono", s.getTelefono());
            fila.addProperty("productos", servicios.inventario().listarTodos().stream()
                    .filter(p -> s.getId().equals(p.getProveedorId())).count());
            lista.add(fila);
        }
        JsonObject r = new JsonObject();
        r.add("proveedores", lista);
        return r;
    }

    private JsonObject consultarFiados(String cliente) {
        String buscado = cliente == null ? "" : cliente.trim().toLowerCase();
        JsonArray lista = new JsonArray();
        double total = 0;
        for (sistemainventario.model.CustomerDebt c : servicios.fiados().listarCuentas()) {
            String nombre = c.getCliente() == null ? "" : c.getCliente();
            boolean coincide = buscado.isEmpty() || nombre.toLowerCase().contains(buscado)
                    || (c.getDocumento() != null && c.getDocumento().contains(buscado));
            if (!coincide || (buscado.isEmpty() && c.estaAlDia())) {
                continue;
            }
            JsonObject fila = new JsonObject();
            fila.addProperty("cliente", nombre);
            fila.addProperty("cedula", c.getDocumento());
            fila.addProperty("fiado", c.getTotalFiado());
            fila.addProperty("abonado", c.getTotalAbonado());
            fila.addProperty("debe", c.getSaldo());
            fila.addProperty("ultimo_movimiento", c.getUltimaFecha());
            lista.add(fila);
            total += c.getSaldo();
        }
        JsonObject r = new JsonObject();
        r.add("clientes", lista);
        r.addProperty("total_por_cobrar", total);
        return r;
    }

    private static final int MAXIMO_PRODUCTOS_VENDIDOS = 80;
    private static final int MAXIMO_VENTAS_DETALLE = 40;

    private JsonObject resumenVentas(JsonObject a) {
        LocalDate hoy = LocalDate.now();
        String periodo = cadena(a, "periodo");
        LocalDate desde;
        LocalDate hasta = hoy;
        switch (periodo) {
            case "hoy": desde = hoy; break;
            case "ayer": desde = hoy.minusDays(1); hasta = desde; break;
            case "mes": desde = hoy.withDayOfMonth(1); break;
            case "anio": desde = hoy.withDayOfYear(1); break;
            case "semana": desde = hoy.minusDays((hoy.getDayOfWeek().getValue() + 6) % 7); break;
            default:
                desde = LocalDate.parse(cadena(a, "desde").isBlank() ? hoy.minusDays(6).toString() : cadena(a, "desde"));
                hasta = LocalDate.parse(cadena(a, "hasta").isBlank() ? hoy.toString() : cadena(a, "hasta"));
        }
        SalesReport informe = new ReportService(servicios.ventas(), servicios.inventario())
                .generar(desde.toString(), hasta.toString());
        JsonObject r = new JsonObject();
        r.addProperty("desde", informe.desde());
        r.addProperty("hasta", informe.hasta());
        r.add("periodo", totales(informe.actual()));
        r.add("periodo_anterior_igual_duracion", totales(informe.anterior()));
        JsonArray top = new JsonArray();
        informe.productos().stream().limit(5).forEach(p -> {
            JsonObject f = new JsonObject();
            f.addProperty("producto", p.nombre());
            f.addProperty("unidades", p.unidades());
            f.addProperty("ventas", Math.round(p.ventas()));
            top.add(f);
        });
        r.add("mas_vendidos", top);
        // Todo lo vendido, no solo los cinco primeros: si no, el asistente dice que se vendieron
        // cinco productos cuando fueron más.
        JsonArray vendidos = new JsonArray();
        informe.productos().stream().limit(MAXIMO_PRODUCTOS_VENDIDOS).forEach(p -> {
            JsonObject f = new JsonObject();
            f.addProperty("producto", p.nombre());
            f.addProperty("unidades", p.unidades());
            f.addProperty("ventas", Math.round(p.ventas()));
            vendidos.add(f);
        });
        r.addProperty("total_productos_distintos", informe.productos().size());
        r.add("productos_vendidos", vendidos);
        if (!hasta.isAfter(desde.plusDays(1))) {
            r.add("detalle_ventas", detalleVentas(desde, hasta));
        }
        return r;
    }

    /**
     * La venta guarda solo el día; la hora sale de su ID ({@code VTA-<milisegundos>}),
     * igual que en el cuaderno de ventas de la pantalla.
     */
    private static Optional<String> horaDeVenta(Sale venta) {
        if (venta.getFecha() != null && venta.getFecha().length() >= 16) {
            return Optional.of(venta.getFecha().substring(11, 16));
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{13})").matcher(String.valueOf(venta.getId()));
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(java.time.Instant.ofEpochMilli(Long.parseLong(m.group(1)))
                .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
    }

    /** Cada venta del período con su hora, total, método de pago y productos (las más recientes primero). */
    private JsonArray detalleVentas(LocalDate desde, LocalDate hasta) {
        JsonArray lista = new JsonArray();
        servicios.ventas().listarTodas().stream()
                .filter(v -> v.getFecha() != null && v.getFecha().length() >= 10)
                .filter(v -> {
                    LocalDate dia = LocalDate.parse(v.getFecha().substring(0, 10));
                    return !dia.isBefore(desde) && !dia.isAfter(hasta);
                })
                .limit(MAXIMO_VENTAS_DETALLE)
                .forEach(v -> {
                    JsonObject f = new JsonObject();
                    f.addProperty("fecha", v.getFecha().substring(0, 10));
                    horaDeVenta(v).ifPresent(hora -> f.addProperty("hora", hora));
                    f.addProperty("total", Math.round(v.getTotal()));
                    f.addProperty("metodo_pago", v.getMetodoPago());
                    if (v.getCliente() != null && !v.getCliente().isBlank()) {
                        f.addProperty("cliente", v.getCliente());
                    }
                    JsonArray items = new JsonArray();
                    v.getItems().forEach(i -> items.add(i.getCantidad() + " " + i.getNombreProducto()));
                    f.add("productos", items);
                    lista.add(f);
                });
        return lista;
    }

    private JsonObject porVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        JsonArray lista = new JsonArray();
        for (Product p : servicios.inventario().listarTodos()) {
            p.getFechaVencimientoComoFecha().ifPresent(fecha -> {
                long faltan = java.time.temporal.ChronoUnit.DAYS.between(hoy, fecha);
                if (faltan <= dias) {
                    JsonObject f = resumen(p);
                    f.addProperty("vence", fecha.toString());
                    f.addProperty("dias_para_vencer", faltan);
                    lista.add(f);
                }
            });
        }
        JsonObject r = new JsonObject();
        r.add("productos", lista);
        return r;
    }

    // ── Propuestas (requieren confirmación) ─────────────────────────────

    private JsonObject proponerVenta(JsonObject a) {
        List<String> errores = new ArrayList<>();
        List<LineaVenta> lineas = new ArrayList<>();
        for (JsonElement e : arreglo(a, "items")) {
            JsonObject item = e.getAsJsonObject();
            int cantidad = entero(item, "cantidad", 1);
            Optional<Product> p = unico(cadena(item, "producto"), errores);
            if (p.isEmpty()) {
                continue;
            }
            if (p.get().getStock() < cantidad) {
                errores.add("Solo hay " + p.get().getStock() + " de " + p.get().getNombre() + ".");
                continue;
            }
            double precio = item.has("precio") ? item.get("precio").getAsDouble() : p.get().getPrecioVenta();
            lineas.add(new LineaVenta(p.get().getId(), p.get().getNombre(), cantidad, precio));
        }
        if (!errores.isEmpty() || lineas.isEmpty()) {
            return errorConDetalle("No se preparó la venta.", errores);
        }
        String metodo = PaymentMethod.desdeTexto(cadena(a, "metodo_pago")).orElse(PaymentMethod.EFECTIVO).getEtiqueta();
        String cliente = cadena(a, "cliente").isBlank() ? "Consumidor final" : cadena(a, "cliente");
        String cedula = cadena(a, "cedula").replaceAll("[\\s.]", "");
        if (!servicios.ventas().aceptaMedioPago(metodo)) {
            return error("Este negocio no recibe pagos con " + metodo + ". Pregunte al cliente con qué otro medio paga.");
        }
        if ("Fiado".equals(metodo) && (cadena(a, "cliente").isBlank() || cedula.isBlank())) {
            return error("Para fiar se necesitan el nombre y la cédula del cliente. Pregúntelos.");
        }
        double total = lineas.stream().mapToDouble(LineaVenta::subtotal).sum();
        List<String> detalle = new ArrayList<>();
        lineas.forEach(l -> detalle.add(l.cantidad() + " × " + l.nombre() + " — " + dinero(l.subtotal())));
        detalle.add("Pago: " + metodo + " · Cliente: " + cliente + (cedula.isBlank() ? "" : " (C.C. " + cedula + ")"));
        return proponer("venta", "Registrar venta", detalle, total, () -> {
            Sale venta = new Sale(null, cliente, metodo, OBSERVACION, null);
            venta.setClienteDocumento(cedula.isBlank() ? null : cedula);
            for (LineaVenta l : lineas) {
                venta.agregarItem(servicios.inventario().obtener(l.productoId()), l.cantidad(), l.precio());
            }
            servicios.ventas().registrar(venta);
        });
    }

    private JsonObject proponerEntrada(JsonObject a) {
        List<String> errores = new ArrayList<>();
        Optional<Supplier> proveedor = cadena(a, "proveedor").isBlank() ? Optional.empty() : resolverProveedor(cadena(a, "proveedor"));
        List<LineaEntrada> lineas = new ArrayList<>();
        for (JsonElement e : arreglo(a, "items")) {
            JsonObject item = e.getAsJsonObject();
            int cantidad = entero(item, "cantidad", 0);
            Optional<Product> p = unico(cadena(item, "producto"), errores);
            if (p.isEmpty()) {
                continue;
            }
            if (cantidad <= 0) {
                errores.add("Falta la cantidad de " + p.get().getNombre() + ".");
                continue;
            }
            Double costo = item.has("costo") ? item.get("costo").getAsDouble() : null;
            lineas.add(new LineaEntrada(p.get().getId(), p.get().getNombre(), cantidad, costo, p.get().getStock()));
        }
        if (!errores.isEmpty() || lineas.isEmpty()) {
            return errorConDetalle("No se preparó la entrada.", errores);
        }
        List<String> detalle = new ArrayList<>();
        double total = 0;
        for (LineaEntrada l : lineas) {
            detalle.add("+" + l.cantidad() + " " + l.nombre() + " (queda en " + (l.stockActual() + l.cantidad()) + ")"
                    + (l.costo() != null ? " · costo " + dinero(l.costo()) : ""));
            total += l.costo() != null ? l.costo() * l.cantidad() : 0;
        }
        proveedor.ifPresent(s -> detalle.add("Proveedor: " + s.getNombre()));
        String proveedorId = proveedor.map(Supplier::getId).orElse(null);
        return proponer("entrada", "Registrar entrada de mercancía", detalle, total > 0 ? total : null, () -> {
            for (LineaEntrada l : lineas) {
                Product producto = servicios.inventario().obtener(l.productoId());
                double costo = l.costo() != null ? l.costo() : producto.getPrecioCompra();
                if (l.costo() != null && costo > 0) {
                    producto.setPrecioCompra(costo);
                    servicios.inventario().guardar(producto);
                }
                servicios.movimientos().registrarEntrada(new StockEntry(null, producto.getId(),
                        proveedorId != null ? proveedorId : producto.getProveedorId(), l.cantidad(), costo, null, OBSERVACION));
            }
        });
    }

    private JsonObject proponerSalida(JsonObject a) {
        List<String> errores = new ArrayList<>();
        Optional<Product> p = unico(cadena(a, "producto"), errores);
        int cantidad = entero(a, "cantidad", 0);
        if (p.isEmpty() || cantidad <= 0) {
            return errorConDetalle("No se preparó la salida.", errores.isEmpty() ? List.of("Falta la cantidad.") : errores);
        }
        if (p.get().getStock() < cantidad) {
            return error("Solo hay " + p.get().getStock() + " de " + p.get().getNombre() + ".");
        }
        String motivo = cadena(a, "motivo");
        String id = p.get().getId();
        return proponer("salida", "Registrar salida de inventario",
                List.of("-" + cantidad + " " + p.get().getNombre() + " (queda en " + (p.get().getStock() - cantidad) + ")",
                        "Motivo: " + motivo), null,
                () -> servicios.movimientos().registrarSalida(new StockExit(null, id, cantidad, motivo, null, "AJUSTE")));
    }

    private JsonObject proponerCambioPrecio(JsonObject a) {
        List<String> errores = new ArrayList<>();
        Optional<Product> p = unico(cadena(a, "producto"), errores);
        if (p.isEmpty()) {
            return errorConDetalle("No se preparó el cambio.", errores);
        }
        Double venta = a.has("precio_venta") ? a.get("precio_venta").getAsDouble() : null;
        Double compra = a.has("precio_compra") ? a.get("precio_compra").getAsDouble() : null;
        if (venta == null && compra == null) {
            return error("Indique el nuevo precio de venta o de compra.");
        }
        List<String> detalle = new ArrayList<>();
        if (venta != null) {
            detalle.add("Precio de venta: " + dinero(p.get().getPrecioVenta()) + " → " + dinero(venta));
        }
        if (compra != null) {
            detalle.add("Precio de compra: " + dinero(p.get().getPrecioCompra()) + " → " + dinero(compra));
        }
        String id = p.get().getId();
        return proponer("precio", "Cambiar precio de " + p.get().getNombre(), detalle, null, () -> {
            Product producto = servicios.inventario().obtener(id);
            if (venta != null) {
                producto.setPrecioVenta(venta);
            }
            if (compra != null) {
                producto.setPrecioCompra(compra);
            }
            servicios.inventario().guardar(producto);
        });
    }

    private JsonObject proponerProductoNuevo(JsonObject a) {
        String nombre = cadena(a, "nombre");
        if (nombre.isBlank() || !a.has("precio_venta")) {
            return error("Para crear el producto se necesita al menos el nombre y el precio de venta.");
        }
        List<Product> parecidos = coincidencias(nombre, 3);
        if (!parecidos.isEmpty() && normalizar(parecidos.get(0).getNombre()).equals(normalizar(nombre))) {
            return error("Ya existe un producto llamado " + parecidos.get(0).getNombre() + ".");
        }
        double venta = a.get("precio_venta").getAsDouble();
        double compra = a.has("precio_compra") ? a.get("precio_compra").getAsDouble() : 0;
        int stock = entero(a, "stock", 0);
        int minimo = entero(a, "stock_minimo", 5);
        String categoria = cadena(a, "categoria");
        Category cat = servicios.categorias().listarTodos().stream()
                .filter(c -> normalizar(c.getNombre()).equals(normalizar(categoria))).findFirst()
                .orElseGet(() -> servicios.categorias().sinCategoria());
        Optional<Supplier> proveedor = cadena(a, "proveedor").isBlank() ? Optional.empty() : resolverProveedor(cadena(a, "proveedor"));
        List<String> detalle = new ArrayList<>(List.of(
                "Precio de venta: " + dinero(venta) + (compra > 0 ? " · costo " + dinero(compra) : " · sin costo"),
                "Stock inicial: " + stock + " · mínimo: " + minimo));
        detalle.add("Categoría: " + cat.getNombre()
                + (categoria.isBlank() || cat.getNombre().equalsIgnoreCase(categoria) ? "" : " (no existe " + categoria + ")"));
        proveedor.ifPresent(s -> detalle.add("Proveedor: " + s.getNombre()));
        return proponer("producto", "Crear producto " + nombre, detalle, null, () -> servicios.inventario().guardar(
                new Product(null, nombre, "", cat.getNombre(), cat.getId(), proveedor.map(Supplier::getId).orElse(null),
                        compra, venta, stock, minimo, null)));
    }

    private JsonObject proponer(String tipo, String titulo, List<String> detalle, Double total, Runnable accion) {
        AssistantProposal propuesta = new AssistantProposal("P" + (++contador) + "-" + System.nanoTime() % 100000,
                tipo, titulo, detalle, total, accion);
        alProponer.accept(propuesta);
        JsonObject r = new JsonObject();
        r.addProperty("estado", "pendiente_de_confirmacion");
        r.addProperty("mensaje", "Se mostró una tarjeta al usuario para que confirme. Todavía NO está registrado.");
        r.addProperty("resumen", titulo + ": " + String.join("; ", detalle));
        return r;
    }

    // ── Búsqueda aproximada de productos y proveedores ──────────────────

    private Optional<Product> unico(String texto, List<String> errores) {
        if (texto.isBlank()) {
            errores.add("Falta el nombre de un producto.");
            return Optional.empty();
        }
        List<Product> candidatos = coincidencias(texto, 5);
        if (candidatos.isEmpty()) {
            errores.add("No encontré \"" + texto + "\" en el inventario.");
            return Optional.empty();
        }
        double primero = puntaje(candidatos.get(0), texto);
        double segundo = candidatos.size() > 1 ? puntaje(candidatos.get(1), texto) : 0;
        if (primero >= 90 || primero - segundo >= 15) {
            return Optional.of(candidatos.get(0));
        }
        StringBuilder opciones = new StringBuilder();
        candidatos.forEach(p -> opciones.append(opciones.length() > 0 ? ", " : "").append(p.getNombre()));
        errores.add("\"" + texto + "\" puede ser: " + opciones + ". Pregunte cuál.");
        return Optional.empty();
    }

    private List<Product> coincidencias(String texto, int maximo) {
        return servicios.inventario().listarTodos().stream()
                .filter(p -> puntaje(p, texto) >= 35)
                .sorted(Comparator.comparingDouble((Product p) -> puntaje(p, texto)).reversed())
                .limit(maximo).toList();
    }

    private static double puntaje(Product p, String consulta) {
        String q = normalizar(consulta);
        String n = normalizar(p.getNombre());
        if (q.isEmpty()) {
            return 0;
        }
        if (n.equals(q) || (p.getCodigo() != null && normalizar(p.getCodigo()).equals(q))) {
            return 100;
        }
        if (n.contains(q)) {
            return 90;
        }
        String[] palabras = q.split(" ");
        int encontradas = 0;
        int validas = 0;
        for (String palabra : palabras) {
            if (palabra.length() < 2) {
                continue;
            }
            validas++;
            String raiz = palabra.replaceAll("(es|s)$", "");
            if (n.contains(palabra) || (raiz.length() >= 3 && n.contains(raiz))) {
                encontradas++;
            }
        }
        return validas == 0 ? 0 : 80.0 * encontradas / validas;
    }

    private Optional<Supplier> resolverProveedor(String texto) {
        String q = normalizar(texto);
        return servicios.proveedores().listarTodos().stream()
                .filter(s -> normalizar(s.getNombre()).contains(q) || q.contains(normalizar(s.getNombre())))
                .findFirst();
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    // ── Ayudas de JSON ──────────────────────────────────────────────────

    private JsonObject resumen(Product p) {
        JsonObject f = new JsonObject();
        f.addProperty("nombre", p.getNombre());
        f.addProperty("stock", p.getStock());
        f.addProperty("stock_minimo", p.getStockMin());
        f.addProperty("precio_venta", p.getPrecioVenta());
        if (p.tienePrecioCompra()) {
            f.addProperty("precio_compra", p.getPrecioCompra());
        }
        if (p.getCategoria() != null) {
            f.addProperty("categoria", p.getCategoria());
        }
        if (p.getProveedorId() != null) {
            servicios.proveedores().buscarPorId(p.getProveedorId()).ifPresent(s -> f.addProperty("proveedor", s.getNombre()));
        }
        return f;
    }

    private static JsonObject totales(SalesReport.Totals t) {
        JsonObject f = new JsonObject();
        f.addProperty("ventas", Math.round(t.ventas()));
        f.addProperty("numero_ventas", t.numeroVentas());
        f.addProperty("unidades", t.unidades());
        f.addProperty("ticket_promedio", Math.round(t.ticketPromedio()));
        f.addProperty("utilidad", Math.round(t.utilidad()));
        return f;
    }

    private static JsonObject error(String mensaje) {
        JsonObject r = new JsonObject();
        r.addProperty("error", mensaje);
        return r;
    }

    private static JsonObject errorConDetalle(String mensaje, List<String> detalle) {
        return error(mensaje + " " + String.join(" ", detalle));
    }

    static String dinero(double valor) {
        return "$ " + String.format(new Locale("es", "CO"), "%,.0f", valor);
    }

    private static String cadena(JsonObject o, String clave) {
        return o != null && o.has(clave) && !o.get(clave).isJsonNull() ? o.get(clave).getAsString().trim() : "";
    }

    private static boolean booleano(JsonObject o, String clave) {
        return o != null && o.has(clave) && !o.get(clave).isJsonNull() && o.get(clave).getAsBoolean();
    }

    private static int entero(JsonObject o, String clave, int defecto) {
        return o != null && o.has(clave) && !o.get(clave).isJsonNull() ? (int) Math.round(o.get(clave).getAsDouble()) : defecto;
    }

    private static JsonArray arreglo(JsonObject o, String clave) {
        return o != null && o.has(clave) && o.get(clave).isJsonArray() ? o.getAsJsonArray(clave) : new JsonArray();
    }

    // Esquemas de parámetros (formato OpenAPI que usa Gemini)

    private static JsonObject funcion(String nombre, String descripcion, Esquema parametros) {
        JsonObject f = new JsonObject();
        f.addProperty("name", nombre);
        f.addProperty("description", descripcion);
        if (parametros.propiedades.size() > 0) {
            f.add("parameters", parametros.json);
        }
        return f;
    }

    private static Esquema objeto() {
        return new Esquema();
    }

    private static JsonObject tipo(String tipo, String descripcion) {
        JsonObject t = new JsonObject();
        t.addProperty("type", tipo);
        t.addProperty("description", descripcion);
        return t;
    }

    private static JsonObject texto(String d) {
        return tipo("string", d);
    }

    private static JsonObject numero(String d) {
        return tipo("number", d);
    }

    private static JsonObject entero(String d) {
        return tipo("integer", d);
    }

    private static JsonObject booleano(String d) {
        return tipo("boolean", d);
    }

    private static JsonObject enumeracion(String d, String... valores) {
        JsonObject t = texto(d);
        t.addProperty("format", "enum");
        JsonArray lista = new JsonArray();
        for (String v : valores) {
            lista.add(v);
        }
        t.add("enum", lista);
        return t;
    }

    private static JsonObject items(boolean conPrecio) {
        Esquema item = objeto().prop("producto", texto("Nombre del producto como lo dijo el usuario"))
                .prop("cantidad", entero("Unidades"));
        if (conPrecio) {
            item.prop("precio", numero("Precio unitario si es distinto al registrado"));
        } else {
            item.prop("costo", numero("Costo unitario de compra, si el usuario lo dijo"));
        }
        item.requerido("producto", "cantidad");
        JsonObject arreglo = new JsonObject();
        arreglo.addProperty("type", "array");
        arreglo.addProperty("description", "Productos y cantidades");
        arreglo.add("items", item.json);
        return arreglo;
    }

    /** Pequeño constructor de esquemas de objeto. */
    private static final class Esquema {
        private final JsonObject json = new JsonObject();
        private final JsonObject propiedades = new JsonObject();

        private Esquema() {
            json.addProperty("type", "object");
            json.add("properties", propiedades);
        }

        private Esquema prop(String nombre, JsonObject tipo) {
            propiedades.add(nombre, tipo);
            return this;
        }

        private Esquema requerido(String... nombres) {
            JsonArray lista = new JsonArray();
            for (String n : nombres) {
                lista.add(n);
            }
            json.add("required", lista);
            return this;
        }
    }

    /** Producto, cantidad y precio de una venta propuesta. */
    private record LineaVenta(String productoId, String nombre, int cantidad, double precio) {
        double subtotal() {
            return cantidad * precio;
        }
    }

    /** Producto, cantidad y costo (si se dijo) de una entrada propuesta. */
    private record LineaEntrada(String productoId, String nombre, int cantidad, Double costo, int stockActual) {
    }
}
