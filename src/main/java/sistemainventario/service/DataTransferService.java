package sistemainventario.service;

import sistemainventario.model.IdentifiedEntity;
import sistemainventario.model.Profile;
import sistemainventario.model.User;
import sistemainventario.repository.Repository;
import sistemainventario.repository.StorageProvider;

import java.util.List;

/**
 * Copia todos los negocios de un almacenamiento a otro, para las copias de
 * seguridad: de Oracle a los archivos del .zip al crearla, y de esos archivos a
 * Oracle al restaurarla.
 *
 * <p>Solo usa la interfaz {@link Repository}, así que sirve en cualquier
 * dirección. Copia primero categorías y proveedores, porque los productos los
 * referencian.</p>
 */
public class DataTransferService {

    /**
     * Cantidades copiadas.
     *
     * @param perfiles  negocios
     * @param productos productos de todos los negocios
     * @param ventas    ventas de todos los negocios
     */
    public record Resumen(int perfiles, int productos, int ventas) {
    }

    /**
     * @param origen  de dónde se leen los datos
     * @param destino a dónde se copian (debe estar vacío)
     * @return cuánto se copió
     */
    public Resumen copiar(StorageProvider origen, StorageProvider destino) {
        Repository<Profile> perfilesDestino = destino.perfiles();
        int productos = 0;
        int ventas = 0;
        List<Profile> perfiles = origen.perfiles().listarTodos();
        for (Profile perfil : perfiles) {
            String id = perfil.getId();
            perfilesDestino.agregar(perfil);
            copiarTodo(origen.categorias(id).listarTodos(), destino.categorias(id));
            copiarTodo(origen.proveedores(id).listarTodos(), destino.proveedores(id));
            productos += copiarTodo(origen.productos(id).listarTodos(), destino.productos(id));
            copiarTodo(origen.entradas(id).listarTodos(), destino.entradas(id));
            copiarTodo(origen.salidas(id).listarTodos(), destino.salidas(id));
            copiarTodo(origen.abonos(id).listarTodos(), destino.abonos(id));
            ventas += copiarTodo(origen.ventas(id).listarTodos(), destino.ventas(id));
        }
        // las personas van después de los negocios: sus accesos apuntan a ellos
        copiarUsuarios(origen.usuarios().listarTodos(), destino);
        // el historial va completo (sin repetir lo que ya estuviera en el destino)
        var historialDestino = destino.auditoria();
        for (sistemainventario.model.AuditEntry entrada : origen.auditoria().listarTodos()) {
            if (!historialDestino.existe(entrada.getId())) {
                historialDestino.agregar(entrada);
            }
        }
        return new Resumen(perfiles.size(), productos, ventas);
    }

    /**
     * Copia personas al almacenamiento, dejando solo los accesos a negocios que existen ahí.
     *
     * @param usuarios personas a copiar
     * @param destino  almacenamiento que ya tiene los negocios
     */
    public void copiarUsuarios(List<User> usuarios, StorageProvider destino) {
        java.util.Set<String> negocios = new java.util.HashSet<>();
        destino.perfiles().listarTodos().forEach(p -> negocios.add(p.getId()));
        Repository<User> repositorio = destino.usuarios();
        for (User persona : usuarios) {
            persona.setAccesos(persona.getAccesos().stream().filter(a -> negocios.contains(a.getPerfilId())).toList());
            repositorio.agregar(persona);
        }
    }

    /**
     * Borra todos los negocios del almacenamiento. En la base, cada negocio se
     * lleva sus datos por ON DELETE CASCADE.
     *
     * @param almacen almacenamiento a vaciar
     */
    public void vaciar(StorageProvider almacen) {
        Repository<User> usuarios = almacen.usuarios();
        for (User persona : usuarios.listarTodos()) {
            usuarios.eliminar(persona.getId());
        }
        Repository<Profile> perfiles = almacen.perfiles();
        for (Profile perfil : perfiles.listarTodos()) {
            perfiles.eliminar(perfil.getId());
        }
    }

    private static <T extends IdentifiedEntity> int copiarTodo(List<T> datos, Repository<T> destino) {
        for (T dato : datos) {
            destino.agregar(dato);
        }
        return datos.size();
    }

}
