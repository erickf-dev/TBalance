package sistemainventario.repository;

import sistemainventario.exception.PersistenceException;
import sistemainventario.model.SalesReport;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Guarda un {@link SalesReport} como libro de Excel (.xlsx) con formato de
 * informe: encabezado con el negocio y el período, tablas con estilos, filas
 * alternadas, totales con fórmulas, filtros, barras de datos, encabezados fijos
 * y pie de página con el número de página para imprimir.
 *
 * <p>Hojas: Resumen, Ventas por período, Productos y Detalle de ventas.</p>
 *
 * <p>Un archivo .xlsx es un .zip con archivos XML (formato Office Open XML).
 * Se escribe directamente con {@link ZipOutputStream} para no agregar
 * dependencias. <b>SRP:</b> solo da formato y guarda; los cálculos están en
 * {@code ReportService}.</p>
 */
public class ReportExcelWriter {

    // Índices de los estilos definidos en ESTILOS (cellXfs)
    private static final int TITULO = 1;
    private static final int SUBTITULO = 2;
    private static final int ENCABEZADO = 3;
    private static final int TEXTO = 4;
    private static final int DINERO = 5;
    private static final int ENTERO = 6;
    private static final int PORCENTAJE = 7;
    private static final int VARIACION = 8;
    private static final int FECHA = 9;
    /** Sumar a TEXTO..FECHA para la fila alterna (sombreada). */
    private static final int ALTERNA = 6;
    private static final int TOTAL_TEXTO = 16;
    private static final int TOTAL_DINERO = 17;
    private static final int TOTAL_ENTERO = 18;
    private static final int TOTAL_PORCENTAJE = 19;
    private static final int NOTA = 20;
    private static final int INDICADOR = 21;

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLUMNAS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    /**
     * @param informe informe a guardar
     * @param negocio nombre del negocio (perfil activo)
     * @param destino archivo .xlsx de salida (se reemplaza si existe)
     * @throws PersistenceException si no se pudo escribir el archivo
     */
    public void escribir(SalesReport informe, String negocio, Path destino) {
        String periodo = "Período: " + fecha(informe.desde()) + " a " + fecha(informe.hasta())
                + "   ·   Comparado con: " + fecha(informe.anteriorDesde()) + " a " + fecha(informe.anteriorHasta());
        String generado = "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"))
                + " con TBalance   ·   Cifras en pesos colombianos (COP)";
        List<Hoja> hojas = List.of(
                hojaResumen(informe, negocio, periodo, generado),
                hojaSerie(informe, negocio, periodo),
                hojaProductos(informe, negocio, periodo),
                hojaDetalle(informe, negocio, periodo));
        try {
            Files.createDirectories(destino.toAbsolutePath().getParent());
            try (OutputStream archivo = Files.newOutputStream(destino);
                 ZipOutputStream zip = new ZipOutputStream(archivo, StandardCharsets.UTF_8)) {
                agregar(zip, "[Content_Types].xml", tiposDeContenido(hojas.size()));
                agregar(zip, "_rels/.rels", RELACIONES_RAIZ);
                agregar(zip, "docProps/core.xml", propiedades(negocio));
                agregar(zip, "xl/workbook.xml", libro(hojas));
                agregar(zip, "xl/_rels/workbook.xml.rels", relacionesLibro(hojas.size()));
                agregar(zip, "xl/styles.xml", ESTILOS);
                for (int i = 0; i < hojas.size(); i++) {
                    agregar(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", hojas.get(i).xml());
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar el informe en Excel: " + e.getMessage(), e);
        }
    }

    // ── Hojas ───────────────────────────────────────────────────────────

    private Hoja hojaResumen(SalesReport informe, String negocio, String periodo, String generado) {
        Hoja h = new Hoja("Resumen", new double[] {30, 20, 20, 16, 4});
        h.fila(1, 30, texto("A1", "Informe de ventas", TITULO));
        h.fila(2, 20, texto("A2", negocio, SUBTITULO));
        h.fila(3, 16, texto("A3", periodo, NOTA));
        h.fila(4, 16, texto("A4", generado, NOTA));
        h.unir("A1:D1");
        h.unir("A2:D2");
        h.unir("A3:D3");
        h.unir("A4:D4");
        h.fila(6, 22, texto("A6", "Indicador", ENCABEZADO), texto("B6", "Período", ENCABEZADO),
                texto("C6", "Período anterior", ENCABEZADO), texto("D6", "Variación", ENCABEZADO));
        SalesReport.Totals a = informe.actual();
        SalesReport.Totals b = informe.anterior();
        Object[][] datos = {
                {"Ventas", a.ventas(), b.ventas(), DINERO},
                {"Número de ventas", (double) a.numeroVentas(), (double) b.numeroVentas(), ENTERO},
                {"Unidades vendidas", (double) a.unidades(), (double) b.unidades(), ENTERO},
                {"Ticket promedio", a.ticketPromedio(), b.ticketPromedio(), DINERO},
                {"Utilidad", a.utilidad(), b.utilidad(), DINERO},
                {"Margen de utilidad", a.margen() / 100, b.margen() / 100, PORCENTAJE},
        };
        int r = 7;
        for (Object[] d : datos) {
            int estilo = (int) d[3];
            int alterna = (r % 2 == 0) ? ALTERNA : 0;
            String variacion = (double) d[2] != 0
                    ? formula("D" + r, "IF(C" + r + "=0,\"\",(B" + r + "-C" + r + ")/ABS(C" + r + "))", VARIACION)
                    : texto("D" + r, "—", TEXTO + alterna);
            h.fila(r, 20, texto("A" + r, (String) d[0], INDICADOR), numero("B" + r, (double) d[1], estilo + alterna),
                    numero("C" + r, (double) d[2], estilo + alterna), variacion);
            r++;
        }
        if (a.unidadesSinCosto() > 0) {
            r++;
            h.fila(r, 16, texto("A" + r, a.unidadesSinCosto() == 1
                    ? "Nota: 1 unidad vendida no tiene precio de compra y no suma a la utilidad."
                    : "Nota: " + a.unidadesSinCosto() + " unidades vendidas no tienen precio de compra y no suman a la utilidad.", NOTA));
            h.unir("A" + r + ":D" + r);
        }
        r += 2;
        int inicio = r + 1;
        h.fila(r, 22, texto("A" + r, "Método de pago", ENCABEZADO), texto("B" + r, "Ventas", ENCABEZADO),
                texto("C" + r, "N.º de ventas", ENCABEZADO), texto("D" + r, "Participación", ENCABEZADO));
        List<SalesReport.PaymentShare> metodos = informe.metodosPago();
        int fin = inicio + Math.max(metodos.size(), 1) - 1;
        for (SalesReport.PaymentShare m : metodos) {
            r++;
            int alterna = (r % 2 == 0) ? ALTERNA : 0;
            h.fila(r, 18, texto("A" + r, m.metodo(), TEXTO + alterna), numero("B" + r, m.ventas(), DINERO + alterna),
                    numero("C" + r, m.numeroVentas(), ENTERO + alterna),
                    formula("D" + r, "IF(SUM(B$" + inicio + ":B$" + fin + ")=0,0,B" + r + "/SUM(B$" + inicio + ":B$" + fin + "))",
                            PORCENTAJE + alterna));
        }
        if (metodos.isEmpty()) {
            r++;
            h.fila(r, 18, texto("A" + r, "Sin ventas en el período", TEXTO));
        }
        r++;
        h.fila(r, 20, texto("A" + r, "Total", TOTAL_TEXTO), formula("B" + r, "SUM(B" + inicio + ":B" + fin + ")", TOTAL_DINERO),
                formula("C" + r, "SUM(C" + inicio + ":C" + fin + ")", TOTAL_ENTERO), numero("D" + r, metodos.isEmpty() ? 0 : 1, TOTAL_PORCENTAJE));
        h.barras("B" + inicio + ":B" + fin);
        return h;
    }

    private Hoja hojaSerie(SalesReport informe, String negocio, String periodo) {
        String grupo = switch (informe.agrupacion()) {
            case "semana" -> "semana";
            case "mes" -> "mes";
            default -> "día";
        };
        Hoja h = new Hoja("Ventas por período", new double[] {16, 16, 14, 18, 18, 12});
        encabezadoDeHoja(h, "Ventas por " + grupo, negocio, periodo, 6);
        h.fila(5, 22, texto("A5", "Desde", ENCABEZADO), texto("B5", "Grupo", ENCABEZADO), texto("C5", "N.º de ventas", ENCABEZADO),
                texto("D5", "Ventas", ENCABEZADO), texto("E5", "Utilidad", ENCABEZADO), texto("F5", "Margen", ENCABEZADO));
        int r = 6;
        for (SalesReport.Point p : informe.serie()) {
            int alterna = (r % 2 == 1) ? ALTERNA : 0;
            h.fila(r, 18, fechaCelda("A" + r, p.desde(), FECHA + alterna), texto("B" + r, p.etiqueta(), TEXTO + alterna),
                    numero("C" + r, p.numeroVentas(), ENTERO + alterna), numero("D" + r, p.ventas(), DINERO + alterna),
                    numero("E" + r, p.utilidad(), DINERO + alterna),
                    formula("F" + r, "IF(D" + r + "=0,0,E" + r + "/D" + r + ")", PORCENTAJE + alterna));
            r++;
        }
        int ultimo = r - 1;
        h.fila(r, 20, texto("A" + r, "Total", TOTAL_TEXTO), texto("B" + r, "", TOTAL_TEXTO),
                formula("C" + r, "SUM(C6:C" + ultimo + ")", TOTAL_ENTERO), formula("D" + r, "SUM(D6:D" + ultimo + ")", TOTAL_DINERO),
                formula("E" + r, "SUM(E6:E" + ultimo + ")", TOTAL_DINERO),
                formula("F" + r, "IF(D" + r + "=0,0,E" + r + "/D" + r + ")", TOTAL_PORCENTAJE));
        h.filtro("A5:F" + ultimo, 5);
        h.barras("D6:D" + ultimo);
        return h;
    }

    private Hoja hojaProductos(SalesReport informe, String negocio, String periodo) {
        Hoja h = new Hoja("Productos", new double[] {6, 40, 12, 18, 18, 11, 13});
        encabezadoDeHoja(h, "Productos más vendidos", negocio, periodo, 7);
        h.fila(5, 22, texto("A5", "#", ENCABEZADO), texto("B5", "Producto", ENCABEZADO), texto("C5", "Unidades", ENCABEZADO),
                texto("D5", "Ventas", ENCABEZADO), texto("E5", "Utilidad", ENCABEZADO), texto("F5", "Margen", ENCABEZADO),
                texto("G5", "Participación", ENCABEZADO));
        int r = 6;
        int ultimo = 5 + Math.max(informe.productos().size(), 1);
        for (SalesReport.ProductRanking p : informe.productos()) {
            int alterna = (r % 2 == 1) ? ALTERNA : 0;
            String utilidad = p.conCosto() ? numero("E" + r, p.utilidad(), DINERO + alterna)
                    : texto("E" + r, "Sin precio de compra", TEXTO + alterna);
            String margen = p.conCosto() ? formula("F" + r, "IF(D" + r + "=0,0,E" + r + "/D" + r + ")", PORCENTAJE + alterna)
                    : texto("F" + r, "—", TEXTO + alterna);
            h.fila(r, 18, numero("A" + r, r - 5, ENTERO + alterna), texto("B" + r, p.nombre(), TEXTO + alterna),
                    numero("C" + r, p.unidades(), ENTERO + alterna), numero("D" + r, p.ventas(), DINERO + alterna), utilidad, margen,
                    formula("G" + r, "IF(SUM(D$6:D$" + ultimo + ")=0,0,D" + r + "/SUM(D$6:D$" + ultimo + "))", PORCENTAJE + alterna));
            r++;
        }
        if (informe.productos().isEmpty()) {
            h.fila(r, 18, texto("A" + r, "", TEXTO), texto("B" + r, "Sin productos vendidos en el período", TEXTO));
            r++;
        }
        h.fila(r, 20, texto("A" + r, "", TOTAL_TEXTO), texto("B" + r, "Total", TOTAL_TEXTO),
                formula("C" + r, "SUM(C6:C" + ultimo + ")", TOTAL_ENTERO), formula("D" + r, "SUM(D6:D" + ultimo + ")", TOTAL_DINERO),
                formula("E" + r, "SUM(E6:E" + ultimo + ")", TOTAL_DINERO),
                formula("F" + r, "IF(D" + r + "=0,0,E" + r + "/D" + r + ")", TOTAL_PORCENTAJE), numero("G" + r, 1, TOTAL_PORCENTAJE));
        h.filtro("A5:G" + ultimo, 5);
        h.barras("C6:C" + ultimo);
        return h;
    }

    private Hoja hojaDetalle(SalesReport informe, String negocio, String periodo) {
        Hoja h = new Hoja("Detalle de ventas", new double[] {13, 22, 22, 22, 16, 50, 11, 16, 16});
        encabezadoDeHoja(h, "Detalle de ventas", negocio, periodo, 9);
        String[] titulos = {"Fecha", "N.º de venta", "Cliente", "Método de pago", "Comprobante", "Productos", "Unidades", "Total", "Utilidad"};
        List<String> celdas = new ArrayList<>();
        for (int i = 0; i < titulos.length; i++) {
            celdas.add(texto(COLUMNAS[i] + "5", titulos[i], ENCABEZADO));
        }
        h.fila(5, 22, celdas.toArray(new String[0]));
        int r = 6;
        for (SalesReport.SaleLine v : informe.ventas()) {
            int alterna = (r % 2 == 1) ? ALTERNA : 0;
            // sin alto fijo: la fila crece si la lista de productos ocupa varias líneas
            h.fila(r, 0, fechaCelda("A" + r, v.fecha(), FECHA + alterna), texto("B" + r, v.id(), TEXTO + alterna),
                    texto("C" + r, v.cliente(), TEXTO + alterna), texto("D" + r, v.metodoPago(), TEXTO + alterna),
                    texto("E" + r, v.referencia() == null ? "" : v.referencia(), TEXTO + alterna),
                    texto("F" + r, v.productos(), TEXTO + alterna), numero("G" + r, v.unidades(), ENTERO + alterna),
                    numero("H" + r, v.total(), DINERO + alterna), numero("I" + r, v.utilidad(), DINERO + alterna));
            r++;
        }
        int ultimo = 5 + Math.max(informe.ventas().size(), 1);
        if (informe.ventas().isEmpty()) {
            h.fila(r, 18, texto("A" + r, "Sin ventas en el período", TEXTO));
            r++;
        }
        h.fila(r, 20, texto("A" + r, "Total", TOTAL_TEXTO), texto("B" + r, informe.ventas().size() + " ventas", TOTAL_TEXTO),
                texto("C" + r, "", TOTAL_TEXTO), texto("D" + r, "", TOTAL_TEXTO), texto("E" + r, "", TOTAL_TEXTO), texto("F" + r, "", TOTAL_TEXTO),
                formula("G" + r, "SUM(G6:G" + ultimo + ")", TOTAL_ENTERO), formula("H" + r, "SUM(H6:H" + ultimo + ")", TOTAL_DINERO),
                formula("I" + r, "SUM(I6:I" + ultimo + ")", TOTAL_DINERO));
        h.filtro("A5:I" + ultimo, 5);
        return h;
    }

    private static void encabezadoDeHoja(Hoja h, String titulo, String negocio, String periodo, int columnas) {
        String hasta = COLUMNAS[columnas - 1];
        h.fila(1, 28, texto("A1", titulo, TITULO));
        h.fila(2, 18, texto("A2", negocio, SUBTITULO));
        h.fila(3, 16, texto("A3", periodo, NOTA));
        h.unir("A1:" + hasta + "1");
        h.unir("A2:" + hasta + "2");
        h.unir("A3:" + hasta + "3");
    }

    // ── Celdas ──────────────────────────────────────────────────────────

    private static String texto(String ref, String texto, int estilo) {
        return "<c r=\"" + ref + "\" s=\"" + estilo + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                + escapar(texto) + "</t></is></c>";
    }

    private static String numero(String ref, double valor, int estilo) {
        double seguro = Double.isFinite(valor) ? valor : 0;
        return "<c r=\"" + ref + "\" s=\"" + estilo + "\"><v>" + seguro + "</v></c>";
    }

    private static String formula(String ref, String formula, int estilo) {
        return "<c r=\"" + ref + "\" s=\"" + estilo + "\"><f>" + escapar(formula) + "</f></c>";
    }

    /** Fecha como número de serie de Excel (días desde el 30/12/1899), con formato dd/mm/aaaa. */
    private static String fechaCelda(String ref, String iso, int estilo) {
        try {
            long serie = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), LocalDate.parse(iso));
            return "<c r=\"" + ref + "\" s=\"" + estilo + "\"><v>" + serie + "</v></c>";
        } catch (RuntimeException e) {
            return texto(ref, iso, TEXTO);
        }
    }

    private static String fecha(String iso) {
        try {
            return LocalDate.parse(iso).format(DIA);
        } catch (RuntimeException e) {
            return iso;
        }
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Una hoja en construcción: filas, celdas unidas, filtro, barras de datos y encabezado fijo. */
    private static final class Hoja {
        private final String nombre;
        private final double[] anchos;
        private final List<String> filas = new ArrayList<>();
        private final List<String> uniones = new ArrayList<>();
        private final List<String> barras = new ArrayList<>();
        private String filtro;
        private int filaFija;

        private Hoja(String nombre, double[] anchos) {
            this.nombre = nombre;
            this.anchos = anchos;
        }

        private void fila(int numero, double alto, String... celdas) {
            String altura = alto > 0 ? " ht=\"" + alto + "\" customHeight=\"1\"" : "";
            filas.add("<row r=\"" + numero + "\"" + altura + ">" + String.join("", celdas) + "</row>");
        }

        private void unir(String rango) {
            uniones.add(rango);
        }

        private void filtro(String rango, int filaEncabezado) {
            filtro = rango;
            filaFija = filaEncabezado;
        }

        private void barras(String rango) {
            barras.add(rango);
        }

        private String rangoFiltroAbsoluto() {
            String[] partes = filtro.split(":");
            return "'" + nombre + "'!" + absoluto(partes[0]) + ":" + absoluto(partes[1]);
        }

        private static String absoluto(String ref) {
            return "$" + ref.replaceAll("([A-Z]+)(\\d+)", "$1\\$$2");
        }

        private String xml() {
            StringBuilder x = new StringBuilder(CABECERA)
                    .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                    .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                    .append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
                    .append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\" zoomScale=\"100\">");
            if (filaFija > 0) {
                x.append("<pane ySplit=\"").append(filaFija).append("\" topLeftCell=\"A").append(filaFija + 1)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/><selection pane=\"bottomLeft\" activeCell=\"A")
                        .append(filaFija + 1).append("\" sqref=\"A").append(filaFija + 1).append("\"/>");
            }
            x.append("</sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"16\"/><cols>");
            for (int i = 0; i < anchos.length; i++) {
                x.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                        .append("\" width=\"").append(anchos[i]).append("\" customWidth=\"1\"/>");
            }
            x.append("</cols><sheetData>");
            filas.forEach(x::append);
            x.append("</sheetData>");
            if (filtro != null) {
                x.append("<autoFilter ref=\"").append(filtro).append("\"/>");
            }
            if (!uniones.isEmpty()) {
                x.append("<mergeCells count=\"").append(uniones.size()).append("\">");
                uniones.forEach(u -> x.append("<mergeCell ref=\"").append(u).append("\"/>"));
                x.append("</mergeCells>");
            }
            int prioridad = 1;
            for (String rango : barras) {
                x.append("<conditionalFormatting sqref=\"").append(rango).append("\"><cfRule type=\"dataBar\" priority=\"")
                        .append(prioridad++).append("\"><dataBar><cfvo type=\"min\"/><cfvo type=\"max\"/>")
                        .append("<color rgb=\"FF9CC7B8\"/></dataBar></cfRule></conditionalFormatting>");
            }
            x.append("<printOptions horizontalCentered=\"1\"/>")
                    .append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.7\" header=\"0.3\" footer=\"0.3\"/>")
                    .append("<pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
                    .append("<headerFooter><oddFooter>&amp;L&amp;8TBalance · ").append(escapar(nombre))
                    .append("&amp;R&amp;8Página &amp;P de &amp;N</oddFooter></headerFooter>")
                    .append("</worksheet>");
            return x.toString();
        }
    }

    // ── Partes fijas del libro ──────────────────────────────────────────

    private static void agregar(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String libro(List<Hoja> hojas) {
        StringBuilder x = new StringBuilder(CABECERA)
                .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < hojas.size(); i++) {
            x.append("<sheet name=\"").append(escapar(hojas.get(i).nombre)).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        x.append("</sheets><definedNames>");
        for (int i = 0; i < hojas.size(); i++) {
            if (hojas.get(i).filtro != null) {
                x.append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"").append(i).append("\" hidden=\"1\">")
                        .append(escapar(hojas.get(i).rangoFiltroAbsoluto())).append("</definedName>");
            }
        }
        return x.append("</definedNames><calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/></workbook>").toString();
    }

    private static String relacionesLibro(int cantidad) {
        StringBuilder x = new StringBuilder(CABECERA)
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= cantidad; i++) {
            x.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        x.append("<Relationship Id=\"rId").append(cantidad + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return x.append("</Relationships>").toString();
    }

    private static String tiposDeContenido(int cantidad) {
        StringBuilder xml = new StringBuilder(CABECERA)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                .append("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>");
        for (int i = 1; i <= cantidad; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private static String propiedades(String negocio) {
        String ahora = LocalDateTime.now().withNano(0) + "Z";
        return CABECERA + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:title>Informe de ventas</dc:title><dc:creator>TBalance</dc:creator>"
                + "<dc:subject>" + escapar(negocio) + "</dc:subject>"
                + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + ahora + "</dcterms:created>"
                + "</cp:coreProperties>";
    }

    private static final String CABECERA = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    private static final String RELACIONES_RAIZ = CABECERA
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
            + "</Relationships>";

    /** Bordes finos gris verdoso, encabezados verdes (#315D53) con letra blanca, filas alternas y totales. */
    private static final String ESTILOS = CABECERA
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"5\">"
            + "<numFmt numFmtId=\"164\" formatCode=\"&quot;$&quot;\\ #,##0\"/>"
            + "<numFmt numFmtId=\"165\" formatCode=\"0.0%\"/>"
            + "<numFmt numFmtId=\"166\" formatCode=\"[Color10]&quot;▲ &quot;0.0%;[Red]&quot;▼ &quot;0.0%;&quot;= &quot;0.0%\"/>"
            + "<numFmt numFmtId=\"167\" formatCode=\"dd/mm/yyyy\"/>"
            + "<numFmt numFmtId=\"168\" formatCode=\"#,##0\"/>"
            + "</numFmts>"
            + "<fonts count=\"7\">"
            + "<font><sz val=\"10.5\"/><color rgb=\"FF1F2A26\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"20\"/><color rgb=\"FF315D53\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"12\"/><color rgb=\"FF1F2A26\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"10.5\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"10.5\"/><color rgb=\"FF1D3A33\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"9.5\"/><color rgb=\"FF5E6964\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><sz val=\"10.5\"/><color rgb=\"FF1F2A26\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"5\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF315D53\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF2F7F5\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDCEBE5\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
            + "<borders count=\"3\"><border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFD9E3DF\"/></left><right style=\"thin\"><color rgb=\"FFD9E3DF\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFD9E3DF\"/></top><bottom style=\"thin\"><color rgb=\"FFD9E3DF\"/></bottom><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFB9CEC6\"/></left><right style=\"thin\"><color rgb=\"FFB9CEC6\"/></right>"
            + "<top style=\"medium\"><color rgb=\"FF315D53\"/></top><bottom style=\"thin\"><color rgb=\"FFB9CEC6\"/></bottom><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"22\">"
            // 0 normal, 1 título, 2 subtítulo, 3 encabezado
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf>"
            // 4-9 celdas: texto, dinero, entero, porcentaje, variación, fecha
            + celdas(0)
            // 10-15 las mismas, en fila alterna
            + celdas(3)
            // 16-19 totales: texto, dinero, entero, porcentaje
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"4\" borderId=\"2\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"164\" fontId=\"4\" fillId=\"4\" borderId=\"2\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"168\" fontId=\"4\" fillId=\"4\" borderId=\"2\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"165\" fontId=\"4\" fillId=\"4\" borderId=\"2\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
            // 20 nota, 21 indicador (negrita con borde)
            + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"><alignment vertical=\"center\"/></xf>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";

    /** Seis estilos de celda con borde (texto, dinero, entero, porcentaje, variación, fecha) con el relleno indicado. */
    private static String celdas(int relleno) {
        int[] formatos = {0, 164, 168, 165, 166, 167};
        StringBuilder x = new StringBuilder();
        for (int formato : formatos) {
            x.append("<xf numFmtId=\"").append(formato).append("\" fontId=\"6\" fillId=\"").append(relleno)
                    .append("\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\">")
                    .append("<alignment vertical=\"center\"").append(formato == 0 ? " wrapText=\"1\"" : "").append("/></xf>");
        }
        return x.toString();
    }
}
