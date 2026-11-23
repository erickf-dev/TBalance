package sistemainventario.repository;

import sistemainventario.model.Product;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Productos de un negocio en la tabla PRODUCTO.
 *
 * <p>Al guardar, los disparadores TRG_PRODUCTO_VALIDAR y
 * TRG_PRODUCTO_HISTORIAL_PRECIO de la base revisan el producto y registran los
 * cambios de precio.</p>
 */
public class ProductJdbcRepository extends JdbcRepository<Product> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public ProductJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "PRODUCTO";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "CODIGO", "DESCRIPCION", "CATEGORIA", "CATEGORIA_ID", "PROVEEDOR_ID",
                "PRECIO_COMPRA", "PRECIO_VENTA", "STOCK", "STOCK_MIN", "TIPO", "REQUIERE_VENCIMIENTO",
                "FECHA_VENCIMIENTO", "ACTIVO");
    }

    @Override
    protected Object[] valores(Product p) {
        return new Object[]{p.getNombre(), texto(p.getCodigo()), texto(p.getDescripcion()), texto(p.getCategoria()),
                referenciaValida("CATEGORIA", p.getCategoriaId()), referenciaValida("PROVEEDOR", p.getProveedorId()),
                p.getPrecioCompra(), p.getPrecioVenta(), p.getStock(), p.getStockMin(), texto(p.getTipo()),
                p.isRequiereVencimiento(), dia(p.getFechaVencimiento()), p.isActivo()};
    }

    @Override
    protected Product leer(ResultSet fila) throws SQLException {
        Product p = new Product();
        p.setId(fila.getString("ID"));
        p.setNombre(fila.getString("NOMBRE"));
        p.setCodigo(fila.getString("CODIGO"));
        p.setDescripcion(fila.getString("DESCRIPCION"));
        p.setCategoria(fila.getString("CATEGORIA"));
        p.setCategoriaId(fila.getString("CATEGORIA_ID"));
        p.setProveedorId(fila.getString("PROVEEDOR_ID"));
        p.setPrecioCompra(leerNumero(fila, "PRECIO_COMPRA"));
        p.setPrecioVenta(leerNumero(fila, "PRECIO_VENTA"));
        p.setStock(fila.getInt("STOCK"));
        p.setStockMin(fila.getInt("STOCK_MIN"));
        p.setTipo(fila.getString("TIPO"));
        p.setRequiereVencimiento(fila.getInt("REQUIERE_VENCIMIENTO") == 1);
        p.setFechaVencimiento(leerDia(fila, "FECHA_VENCIMIENTO"));
        p.setActivo(fila.getInt("ACTIVO") == 1);
        return p;
    }
}
