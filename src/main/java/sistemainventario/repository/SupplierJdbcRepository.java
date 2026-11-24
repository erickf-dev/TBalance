package sistemainventario.repository;

import sistemainventario.model.Supplier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Proveedores de un negocio en la tabla PROVEEDOR. */
public class SupplierJdbcRepository extends JdbcRepository<Supplier> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public SupplierJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "PROVEEDOR";
    }

    @Override
    protected String funcionListar() {
        return "FN_LISTAR_PROVEEDORES";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "CONTACTO", "TELEFONO", "EMAIL", "DIRECCION", "CATEGORIA", "ACTIVO");
    }

    @Override
    protected Object[] valores(Supplier p) {
        return new Object[]{p.getNombre(), texto(p.getContacto()), texto(p.getTelefono()), texto(p.getEmail()),
                texto(p.getDireccion()), texto(p.getCategoria()), p.isActivo()};
    }

    @Override
    protected Supplier leer(ResultSet fila) throws SQLException {
        Supplier p = new Supplier();
        p.setId(fila.getString("ID"));
        p.setNombre(fila.getString("NOMBRE"));
        p.setContacto(fila.getString("CONTACTO"));
        p.setTelefono(fila.getString("TELEFONO"));
        p.setEmail(fila.getString("EMAIL"));
        p.setDireccion(fila.getString("DIRECCION"));
        p.setCategoria(fila.getString("CATEGORIA"));
        p.setActivo(fila.getInt("ACTIVO") == 1);
        return p;
    }

    /** Lee de PROVEEDOR_CATEGORIA los tipos de producto de todos los proveedores del negocio. */
    @Override
    protected void completar(List<Supplier> proveedores) {
        Map<String, List<String>> porProveedor = new LinkedHashMap<>();
        bd.llamar("FN_LISTAR_PROVEEDOR_CATEGORIAS",
                fila -> porProveedor.computeIfAbsent(fila.getString("PROVEEDOR_ID"), k -> new ArrayList<>())
                        .add(fila.getString("CATEGORIA_ID")), perfilId);
        for (Supplier proveedor : proveedores) {
            proveedor.setCategoriaIds(porProveedor.getOrDefault(proveedor.getId(), List.of()));
        }
    }

    /** Reescribe los tipos de producto del proveedor (solo categorías que existen en el negocio). */
    @Override
    protected void guardarHijos(Connection conexion, Supplier proveedor, boolean nuevo) throws SQLException {
        if (!nuevo) {
            Database.procedimiento(conexion, "SP_BORRAR_TIPOS_PROVEEDOR", perfilId, proveedor.getId());
        }
        for (String categoriaId : proveedor.getCategoriaIds()) {
            if (referenciaValida("FN_EXISTE_CATEGORIA", categoriaId) instanceof String) {
                Database.procedimiento(conexion, "SP_AGREGAR_TIPO_PROVEEDOR", perfilId, proveedor.getId(), categoriaId);
            }
        }
    }
}
