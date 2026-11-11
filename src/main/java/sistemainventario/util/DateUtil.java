package sistemainventario.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Utilidades de fecha usadas por todo el sistema.
 *
 * <p>Todas las fechas se persisten en JSON con formato ISO {@code yyyy-MM-dd},
 * por eso este es el único lugar donde se conoce ese formato.</p>
 */
public final class DateUtil {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter FORMATO_VISUAL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateUtil() {
    }

    /** @return la fecha actual en formato {@code yyyy-MM-dd} */
    public static String hoy() {
        return LocalDate.now().format(FORMATO_FECHA);
    }

    /** @return la fecha y hora actual en formato ISO */
    public static String ahora() {
        return LocalDateTime.now().format(FORMATO_FECHA_HORA);
    }

    /**
     * Convierte un texto ISO en {@link LocalDate} sin lanzar excepciones.
     *
     * @param fechaIso fecha en formato {@code yyyy-MM-dd}
     * @return la fecha, o {@link Optional#empty()} si es nula o inválida
     */
    public static Optional<LocalDate> parsear(String fechaIso) {
        if (TextUtil.esVacio(fechaIso)) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(fechaIso.trim(), FORMATO_FECHA));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * @param fechaIso fecha en formato {@code yyyy-MM-dd}
     * @return la fecha en formato {@code dd/MM/yyyy}, o el texto original si no es válida
     */
    public static String formatearFecha(String fechaIso) {
        return parsear(fechaIso).map(f -> f.format(FORMATO_VISUAL)).orElse(fechaIso);
    }
}
