package sistemainventario.service;

/**
 * Conjunto de servicios que operan sobre los datos de un perfil concreto.
 *
 * <p>Al cambiar de perfil se reemplaza la instancia completa, lo que garantiza
 * el aislamiento de datos entre perfiles.</p>
 *
 * @param inventario  productos
 * @param categorias  categorías
 * @param proveedores proveedores
 * @param movimientos entradas y salidas
 * @param ventas      ventas
 */
public record ProfileServices(InventoryService inventario,
                              CategoryService categorias,
                              SupplierService proveedores,
                              MovementService movimientos,
                              SaleService ventas) {
}
