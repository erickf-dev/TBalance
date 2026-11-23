package sistemainventario.repository;

import sistemainventario.model.AuditEntry;

import java.nio.file.Path;

/** Historial de acciones en {@code auditoria.json} (junto a {@code usuarios.json}). */
public class AuditRepository extends JsonRepository<AuditEntry> {

    public static final String ARCHIVO = "auditoria.json";

    /** @param directorio carpeta {@code data} */
    public AuditRepository(Path directorio) {
        super(directorio, ARCHIVO, AuditEntry.class);
    }
}
