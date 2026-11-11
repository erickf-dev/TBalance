package sistemainventario.service;

import sistemainventario.model.Supplier;
import sistemainventario.repository.Repository;

import java.util.List;

/**
 * Casos de uso de los proveedores de un perfil.
 */
public class SupplierService extends EntityService<Supplier> {

    public SupplierService(Repository<Supplier> repositorio) {
        super(repositorio);
    }

    @Override
    protected void validar(Supplier proveedor) {
        proveedor.validar();
    }

    @Override
    protected String generarId() {
        return repositorio.siguienteIdNumerico();
    }

    @Override
    protected String nombreEntidad() {
        return "Proveedor";
    }

    /**
     * @param texto texto buscado en nombre o contacto
     * @return proveedores que coinciden
     */
    public List<Supplier> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        return repositorio.filtrar(proveedor -> proveedor.coincideCon(texto));
    }
}
