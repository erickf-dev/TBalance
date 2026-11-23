package sistemainventario.service;

import sistemainventario.model.AuditEntry;
import sistemainventario.repository.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Historial de acciones: anota quién hizo qué y lo consulta. Solo agrega;
 * desde la aplicación no se puede editar ni borrar una entrada.
 */
public class AuditService {

    private final Repository<AuditEntry> historial;

    /** @param historial donde se guardan las entradas */
    public AuditService(Repository<AuditEntry> historial) {
        this.historial = historial;
    }

    /** @param entrada acción a anotar (se le pone un ID único) */
    public void registrar(AuditEntry entrada) {
        entrada.setId("H-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        historial.agregar(entrada);
    }

    /**
     * @param negocios        negocios cuyo historial puede ver quien consulta
     * @param incluirGenerales también lo que no fue en un negocio (ingresos, copias de seguridad...)
     * @param usuarioId       solo las de esta persona ({@code null} o vacío: todas)
     * @param desde           fecha inicial {@code yyyy-MM-dd} ({@code null}: sin límite)
     * @param hasta           fecha final {@code yyyy-MM-dd} ({@code null}: sin límite)
     * @param limite          cuántas devolver como máximo
     * @return entradas de la más reciente a la más antigua
     */
    public List<AuditEntry> listar(Collection<String> negocios, boolean incluirGenerales, String usuarioId,
                                   String desde, String hasta, int limite) {
        return historial.filtrar(e -> (e.getPerfilId() == null ? incluirGenerales : negocios.contains(e.getPerfilId()))
                        && (usuarioId == null || usuarioId.isBlank() || usuarioId.equals(e.getUsuarioId()))
                        && (desde == null || desde.isBlank() || String.valueOf(e.getFecha()).compareTo(desde) >= 0)
                        && (hasta == null || hasta.isBlank() || String.valueOf(e.getFecha()).compareTo(hasta + "T99") <= 0))
                .stream()
                .sorted(Comparator.comparing((AuditEntry e) -> String.valueOf(e.getFecha())).reversed())
                .limit(Math.max(1, limite))
                .toList();
    }
}
