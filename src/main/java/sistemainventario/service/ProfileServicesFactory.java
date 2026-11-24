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
        return armar(perfilId);
    }

    /**
     * Deja todos los productos del negocio con una categoría válida (ver
     * {@link InventoryService#normalizarCategorias()}), sin cargar sus ventas.
     *
     * @param perfilId ID del perfil
     * @return cuántos productos se corrigieron
     */
    public int normalizarCategorias(String perfilId) {
        InventoryService inventario = new InventoryService(almacen.productos(perfilId));
        inventario.usarCategorias(new CategoryService(almacen.categorias(perfilId)));
        return inventario.normalizarCategorias();
    }

    private ProfileServices armar(String perfilId) {
        InventoryService inventario = new InventoryService(almacen.productos(perfilId));
        CategoryService categorias = new CategoryService(almacen.categorias(perfilId));
        inventario.usarCategorias(categorias);
        inventario.normalizarCategorias();
        SaleService ventas = new SaleService(inventario, almacen.ventas(perfilId));
        almacen.rutinas(perfilId).ifPresent(ventas::usarRutinas);
        ventas.usarMediosDePago(metodo -> almacen.perfiles().buscarPorId(perfilId)
                .map(perfil -> perfil.aceptaMedioPago(metodo)).orElse(true));
        return new ProfileServices(
                inventario,
                categorias,
                new SupplierService(almacen.proveedores(perfilId)),
                new MovementService(inventario, almacen.entradas(perfilId), almacen.salidas(perfilId)),
                ventas,
                new CreditService(ventas, almacen.abonos(perfilId)));
    }
}
