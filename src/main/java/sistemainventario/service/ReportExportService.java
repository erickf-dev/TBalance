package sistemainventario.service;

import sistemainventario.model.SalesReport;
import sistemainventario.repository.ReportExcelWriter;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exporta un informe de ventas a un libro de Excel en la carpeta
 * {@code Documentos/TBalance}.
 *
 * <p><b>SRP:</b> {@link ReportService} calcula el informe, esta clase decide
 * dónde y con qué nombre se guarda, y {@link ReportExcelWriter} escribe el
 * archivo.</p>
 */
public class ReportExportService {

    private final ReportExcelWriter excel;
    private final Path carpeta;

    /**
     * @param excel   escritor del archivo .xlsx
     * @param carpeta carpeta donde se guardan los informes
     */
    public ReportExportService(ReportExcelWriter excel, Path carpeta) {
        this.excel = excel;
        this.carpeta = carpeta;
    }

    /**
     * Guarda el informe como .xlsx.
     *
     * @param informe informe calculado
     * @param negocio nombre del negocio (va en el archivo y en el encabezado)
     * @return ruta del archivo creado
     */
    public Path exportarExcel(SalesReport informe, String negocio) {
        String nombre = "Informe de ventas - " + negocio.replaceAll("[\\\\/:*?\"<>|]", "")
                + " - " + informe.desde() + " a " + informe.hasta() + ".xlsx";
        Path archivo = carpeta.resolve(nombre);
        excel.escribir(informe, negocio, archivo);
        return archivo;
    }

    /**
     * @param usuario carpeta del usuario
     * @return {@code Documentos/TBalance} si existe la carpeta Documentos; si no, {@code <usuario>/TBalance}
     */
    public static Path carpetaPorDefecto(Path usuario) {
        for (String nombre : new String[] {"Documents", "Documentos"}) {
            if (Files.isDirectory(usuario.resolve(nombre))) {
                return usuario.resolve(nombre).resolve("TBalance");
            }
        }
        return usuario.resolve("TBalance");
    }
}
