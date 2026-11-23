package sistemainventario.repository;

import sistemainventario.model.CreditPayment;

import java.nio.file.Path;

/** Abonos de los clientes que deben, en {@code abonos.json} de cada negocio. */
public class CreditPaymentRepository extends JsonRepository<CreditPayment> {

    /** Nombre del archivo dentro de la carpeta del negocio. */
    public static final String ARCHIVO = "abonos.json";

    /** @param directorio carpeta del negocio */
    public CreditPaymentRepository(Path directorio) {
        super(directorio, ARCHIVO, CreditPayment.class);
    }
}
