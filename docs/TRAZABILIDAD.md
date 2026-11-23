# Tabla de trazabilidad — GRASP, SOLID y código limpio

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

Cada fila dice dónde está aplicado el patrón o principio en el código de la versión v3.5.0 y por qué.

## Patrones GRASP

| Patrón | Dónde | Por qué |
|---|---|---|
| Creador | [`Sale.agregarItem`](../src/main/java/sistemainventario/model/Sale.java) | La venta contiene sus renglones, por eso los crea. |
| Creador | [`ProfileServicesFactory`](../src/main/java/sistemainventario/service/ProfileServicesFactory.java) | Tiene el negocio y el almacenamiento, por eso crea los servicios de cada negocio. |
| Creador | [`CategoryService.crearPredefinidas`](../src/main/java/sistemainventario/service/CategoryService.java) | Administra las categorías y crea las sugeridas según el tipo de negocio. |
| Creador | [`CreditService.listarCuentas`](../src/main/java/sistemainventario/service/CreditService.java) | Tiene las ventas fiadas y los abonos, por eso arma la cuenta de cada cliente. |
| Controlador | [`JavaBridge`](../src/main/java/sistemainventario/bridge/JavaBridge.java) | Es el único punto que recibe lo que hace el usuario en la página. |
| Controlador | [`TransactionController`](../src/main/java/sistemainventario/bridge/TransactionController.java), [`CreditController`](../src/main/java/sistemainventario/bridge/CreditController.java), [`SessionController`](../src/main/java/sistemainventario/bridge/SessionController.java) y los demás de `bridge` | Cada uno atiende un grupo de acciones y llama a los servicios, sin reglas del negocio. |
| Experto en información | [`Product`](../src/main/java/sistemainventario/model/Product.java) | Tiene el stock, el mínimo y el vencimiento: decide si está agotado, por vencer o si alcanza para vender. |
| Experto en información | [`Sale`](../src/main/java/sistemainventario/model/Sale.java) | Calcula su total y revisa su medio de pago (un fiado pide nombre y cédula). |
| Experto en información | [`CustomerDebt`](../src/main/java/sistemainventario/model/CustomerDebt.java) | Calcula cuánto debe un cliente con lo fiado y lo abonado. |
| Experto en información | [`Rol`](../src/main/java/sistemainventario/model/Rol.java) | Sabe qué permisos tiene cada rol. |
| Experto en información | [`Profile.aceptaMedioPago`](../src/main/java/sistemainventario/model/Profile.java) | El negocio sabe qué medios de pago recibe. |
| Bajo acoplamiento | [`Repository<T>`](../src/main/java/sistemainventario/repository/Repository.java) | Los servicios no saben si los datos van a archivos, a PostgreSQL o a Oracle. |
| Bajo acoplamiento | [`StorageProvider`](../src/main/java/sistemainventario/repository/StorageProvider.java) | Solo `AppContext` elige el almacenamiento; el resto pide los repositorios. |
| Bajo acoplamiento | [`Camera`](../src/main/java/sistemainventario/dispositivo/Camera.java), [`LanguageModel`](../src/main/java/sistemainventario/ia/LanguageModel.java) | La venta por QR y el asistente no dependen de una cámara ni de un proveedor de IA en particular. |
| Alta cohesión | Controladores del paquete [`bridge`](../src/main/java/sistemainventario/bridge) | Cada uno se ocupa de un solo tema. |
| Alta cohesión | [`AccessPolicy`](../src/main/java/sistemainventario/bridge/AccessPolicy.java), [`AuditTrail`](../src/main/java/sistemainventario/bridge/AuditTrail.java) | Uno solo revisa permisos; el otro solo anota el historial. |
| Alta cohesión | [`CreditService`](../src/main/java/sistemainventario/service/CreditService.java) | Todo lo de fiados y abonos está en un solo servicio. |

## Principios SOLID

| Principio | Dónde | Argumento |
|---|---|---|
| SRP | [`Product`](../src/main/java/sistemainventario/model/Product.java) / [`InventoryService`](../src/main/java/sistemainventario/service/InventoryService.java) / [`ProductRepository`](../src/main/java/sistemainventario/repository/ProductRepository.java) | Reglas del producto, caso de uso y guardado: cada clase cambia por una sola razón. |
| SRP | [`UserService`](../src/main/java/sistemainventario/service/UserService.java) / [`SessionService`](../src/main/java/sistemainventario/service/SessionService.java) / [`PasswordHasher`](../src/main/java/sistemainventario/util/PasswordHasher.java) | Usuarios, sesión y cifrado de contraseñas separados. |
| SRP | [`ReportService`](../src/main/java/sistemainventario/service/ReportService.java) / [`ReportExportService`](../src/main/java/sistemainventario/service/ReportExportService.java) / [`ReportExcelWriter`](../src/main/java/sistemainventario/repository/ReportExcelWriter.java) | Calcular el informe, decidir dónde se guarda y escribir el Excel están separados. |
| SRP | [`BackupService`](../src/main/java/sistemainventario/service/BackupService.java) / [`ZipArchiver`](../src/main/java/sistemainventario/repository/ZipArchiver.java) / [`BackupStore`](../src/main/java/sistemainventario/repository/BackupStore.java) | Coordinar la copia, comprimir y guardar el archivo están separados. |
| OCP | [`JsonRepository<T>`](../src/main/java/sistemainventario/repository/JsonRepository.java) y [`JdbcRepository<T>`](../src/main/java/sistemainventario/repository/JdbcRepository.java) | Guardar en base de datos se agregó con clases nuevas, sin cambiar los servicios. |
| OCP | [`SqlDialect`](../src/main/java/sistemainventario/repository/SqlDialect.java) | Otro motor de base de datos sería un dialecto nuevo. |
| OCP | [`InventoryMovement.aplicarA`](../src/main/java/sistemainventario/model/InventoryMovement.java) | Un tipo de movimiento nuevo sería otra subclase; `MovementService` no cambia. |
| OCP | [`Camera`](../src/main/java/sistemainventario/dispositivo/Camera.java) | El celular como lector (`PhoneCamera`) se agregó sin cambiar la venta por QR. |

## Código limpio

| Práctica | Dónde | Cómo |
|---|---|---|
| Nombres que dicen lo que hacen | [`Product.disminuirStock`](../src/main/java/sistemainventario/model/Product.java), [`CreditService.registrarAbono`](../src/main/java/sistemainventario/service/CreditService.java) | El nombre explica la acción. |
| Métodos cortos | [`SaleService.registrar`](../src/main/java/sistemainventario/service/SaleService.java) | Valida, revisa stock, guarda costos, descuenta y guarda, cada paso en su propio método. |
| Un solo lugar para los errores | [`BridgeResponse`](../src/main/java/sistemainventario/bridge/BridgeResponse.java) | Convierte cualquier error en `ERROR: mensaje` para la pantalla. |
| Constantes con nombre | [`Sale.PREFIJO_ID`](../src/main/java/sistemainventario/model/Sale.java), [`Rol.MINUTOS_ANULAR_PROPIA`](../src/main/java/sistemainventario/model/Rol.java) | No hay números ni textos sueltos repetidos. |
| Validación en el modelo | `validar()` en [`Product`](../src/main/java/sistemainventario/model/Product.java), [`Sale`](../src/main/java/sistemainventario/model/Sale.java), [`CreditPayment`](../src/main/java/sistemainventario/model/CreditPayment.java) | Cada clase revisa sus propios datos. |
| Javadoc | Todas las clases | Cada clase y método público tiene su comentario. |
| Sin datos privados en el código | [`DatabaseSettingsRepository`](../src/main/java/sistemainventario/repository/DatabaseSettingsRepository.java), [`AssistantConfigRepository`](../src/main/java/sistemainventario/repository/AssistantConfigRepository.java) | Las claves se guardan en el computador del usuario, no en el repositorio. |
