package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.ia.GeminiClient;
import sistemainventario.ia.LanguageModel;
import sistemainventario.model.AssistantSettings;
import sistemainventario.repository.AssistantConfigRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Configuración del asistente: guarda la clave y el modelo, crea el cliente del
 * modelo de lenguaje y prueba la conexión.
 *
 * <p>Separa al controlador de la persistencia ({@link AssistantConfigRepository})
 * y del servicio externo ({@link GeminiClient}), igual que los demás servicios:
 * la capa de presentación solo habla con la lógica de negocio.</p>
 */
public class AssistantSettingsService {

    private final AssistantConfigRepository repositorio;

    /** @param repositorio archivo donde se guarda la configuración */
    public AssistantSettingsService(AssistantConfigRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** @return la configuración guardada, si hay una clave */
    public Optional<AssistantSettings> leer() {
        return repositorio.leer();
    }

    /** @return el modelo guardado o, si no hay, el modelo por defecto */
    public String modeloActual() {
        return leer().map(AssistantSettings::modelo).filter(m -> !m.isBlank())
                .orElse(GeminiClient.MODELO_POR_DEFECTO);
    }

    /**
     * Guarda la clave y el modelo; con la clave vacía borra la configuración.
     *
     * @param clave  clave de API
     * @param modelo modelo (vacío para el de por defecto)
     */
    public void configurar(String clave, String modelo) {
        if (clave == null || clave.isBlank()) {
            repositorio.borrar();
            return;
        }
        repositorio.guardar(new AssistantSettings(clave.trim(),
                modelo == null || modelo.isBlank() ? GeminiClient.MODELO_POR_DEFECTO : modelo.trim()));
    }

    /**
     * @return la configuración guardada
     * @throws ValidationException si todavía no hay clave
     */
    public AssistantSettings requerir() {
        return leer().orElseThrow(() -> new ValidationException("Primero configure la clave del asistente"));
    }

    /**
     * Crea el cliente del modelo con la configuración guardada. Si Google retiró
     * el modelo y el cliente pasa a otro, el nuevo queda guardado.
     *
     * @param alCambiarModelo aviso con el nombre del modelo nuevo (para la vista)
     * @return el modelo de lenguaje listo para usar
     */
    public LanguageModel crearModelo(Consumer<String> alCambiarModelo) {
        return crearCliente(requerir(), alCambiarModelo);
    }

    /**
     * Envía un mensaje corto para comprobar la clave y la conexión.
     *
     * @param alCambiarModelo aviso si se cambió de modelo
     * @return el resultado, con el modelo que respondió
     * @throws sistemainventario.exception.AssistantException si no hubo conexión o Google rechazó la clave
     */
    public ConnectionTest probarConexion(Consumer<String> alCambiarModelo) {
        AssistantSettings configuracion = requerir();
        GeminiClient cliente = crearCliente(configuracion, alCambiarModelo);
        String texto = cliente.generar("Responde solo con la palabra: listo",
                List.of(GeminiClient.textoComoContenido("user", "Prueba de conexión")), null).texto();
        boolean cambio = !cliente.modelo().equals(configuracion.modelo());
        return new ConnectionTest(cliente.modelo(), "Conexión correcta con " + cliente.modelo()
                + (cambio ? " (Google ya no ofrecía " + configuracion.modelo() + "; se cambió solo)" : "")
                + ". Respuesta: " + texto);
    }

    /**
     * Resultado de la prueba de conexión.
     *
     * @param modelo  modelo que respondió
     * @param mensaje texto para el usuario
     */
    public record ConnectionTest(String modelo, String mensaje) {
    }

    private GeminiClient crearCliente(AssistantSettings configuracion, Consumer<String> alCambiarModelo) {
        GeminiClient cliente = new GeminiClient(configuracion.clave(), configuracion.modelo());
        cliente.alCambiarModelo(nuevo -> {
            repositorio.guardar(new AssistantSettings(configuracion.clave(), nuevo));
            alCambiarModelo.accept(nuevo);
        });
        return cliente;
    }
}
