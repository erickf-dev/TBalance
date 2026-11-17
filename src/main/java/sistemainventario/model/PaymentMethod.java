package sistemainventario.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Métodos de pago que se usan en las tiendas colombianas.
 *
 * <p>Por ahora solo se registra cómo pagó el cliente (no se cobra desde el
 * programa). <b>OCP:</b> una pasarela de pago futura puede usar el campo
 * {@link #esDigital()} sin cambiar las ventas ya guardadas, porque en la venta
 * se guarda la etiqueta.</p>
 */
public enum PaymentMethod {
    EFECTIVO("Efectivo", false),
    NEQUI("Nequi", true),
    DAVIPLATA("Daviplata", true),
    BRE_B("Bre-B", true),
    BANCOLOMBIA("Transferencia Bancolombia", true),
    TARJETA_DEBITO("Tarjeta débito", true),
    TARJETA_CREDITO("Tarjeta crédito", true),
    FIADO("Fiado", false);

    private final String etiqueta;
    private final boolean digital;

    PaymentMethod(String etiqueta, boolean digital) {
        this.etiqueta = etiqueta;
        this.digital = digital;
    }

    /** @return el texto que se guarda en la venta y se muestra en pantalla */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** @return {@code true} si el pago es electrónico y puede tener número de comprobante */
    public boolean esDigital() {
        return digital;
    }

    /**
     * Reconoce el método aunque venga escrito de otra forma ("nequi",
     * "transferencia", "tarjeta", "credito"...).
     *
     * @param texto método escrito por el usuario o guardado en la venta
     * @return el método, si se reconoce
     */
    public static Optional<PaymentMethod> desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
        for (PaymentMethod m : values()) {
            String e = Normalizer.normalize(m.etiqueta, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
            if (t.equals(e) || t.equals(m.name().toLowerCase(Locale.ROOT))) {
                return Optional.of(m);
            }
        }
        if (t.contains("nequi")) {
            return Optional.of(NEQUI);
        }
        if (t.contains("davi")) {
            return Optional.of(DAVIPLATA);
        }
        if (t.contains("bre")) {
            return Optional.of(BRE_B);
        }
        if (t.contains("bancolombia") || t.contains("transfer")) {
            return Optional.of(BANCOLOMBIA);
        }
        if (t.contains("credito")) {
            return Optional.of(t.contains("tarjeta") ? TARJETA_CREDITO : FIADO);
        }
        if (t.contains("tarjeta") || t.contains("debito") || t.contains("datafono")) {
            return Optional.of(TARJETA_DEBITO);
        }
        if (t.contains("fiad") || t.contains("cuaderno")) {
            return Optional.of(FIADO);
        }
        if (t.contains("efectivo") || t.contains("contado")) {
            return Optional.of(EFECTIVO);
        }
        return Optional.empty();
    }
}
