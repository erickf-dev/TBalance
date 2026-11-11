package sistemainventario.service;

import sistemainventario.repository.CategoryRepository;
import sistemainventario.repository.ProfileDirectory;
import sistemainventario.repository.StockEntryRepository;
import sistemainventario.repository.ProductRepository;
import sistemainventario.repository.SupplierRepository;
import sistemainventario.repository.StockExitRepository;
import sistemainventario.repository.SaleRepository;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Construye los repositorios y servicios de un perfil.
 *
 * <p><b>GRASP Creator:</b> es la clase que posee los datos de inicialización
 * (la carpeta del perfil) y por eso es responsable de crear los
 * repositorios JSON y de inyectarlos en los gestores. Así ningún gestor hace
 * {@code new XxxRepository(...)}, lo que reduce el acoplamiento.</p>
 */
public class ProfileServicesFactory {

    private final ProfileDirectory directorios;

    public ProfileServicesFactory(ProfileDirectory directorios) {
        this.directorios = Objects.requireNonNull(directorios);
    }

    /**
     * @param perfilId ID del perfil
     * @return servicios listos para operar sobre los datos de ese perfil
     */
    public ProfileServices crear(String perfilId) {
        Path carpeta = directorios.crear(perfilId);
        InventoryService inventario = new InventoryService(new ProductRepository(carpeta));
        return new ProfileServices(
                inventario,
                new CategoryService(new CategoryRepository(carpeta)),
                new SupplierService(new SupplierRepository(carpeta)),
                new MovementService(inventario, new StockEntryRepository(carpeta), new StockExitRepository(carpeta)),
                new SaleService(inventario, new SaleRepository(carpeta)));
    }
}
