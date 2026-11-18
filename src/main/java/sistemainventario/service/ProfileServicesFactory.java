package sistemainventario.service;

import sistemainventario.repository.StorageProvider;

import java.util.Objects;

/**
 * Construye los repositorios y servicios de un perfil.
 *
 * <p><b>GRASP Creator:</b> es la clase que posee los datos de inicialización
 * (el ID del perfil) y por eso es responsable de pedir los repositorios al
 * almacenamiento ({@link StorageProvider}: archivos JSON, PostgreSQL u Oracle)
 * e inyectarlos en los gestores. Así ningún gestor sabe dónde se guardan los
 * datos, lo que reduce el acoplamiento.</p>
 */
public class ProfileServicesFactory {

    private final StorageProvider almacen;

    /** @param almacen almacenamiento de donde salen los repositorios */
    public ProfileServicesFactory(StorageProvider almacen) {
        this.almacen = Objects.requireNonNull(almacen);
    }

    /**
     * @param perfilId ID del perfil
     * @return servicios listos para operar sobre los datos de ese perfil
     */
    public ProfileServices crear(String perfilId) {
        InventoryService inventario = new InventoryService(almacen.productos(perfilId));
        return new ProfileServices(
                inventario,
                new CategoryService(almacen.categorias(perfilId)),
                new SupplierService(almacen.proveedores(perfilId)),
                new MovementService(inventario, almacen.entradas(perfilId), almacen.salidas(perfilId)),
                new SaleService(inventario, almacen.ventas(perfilId)));
    }
}
