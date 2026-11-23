# Fase 1 — Especificación y planificación

Universidad Popular del Cesar · Programación de Computadores III (SS462) · Docente: Ing. Esp. Alfredo Bautista<br>
Integrantes: Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero

[README](../README.md) · [Fase 1](FASE1_Especificacion.md) · [Fase 2](FASE2_Arquitectura_y_Diseno.md) · [Fase 3](FASE3_Desarrollo.md) · [Fase 4](FASE4_Documentacion_Tecnica.md) · [Trazabilidad](TRAZABILIDAD.md)

---

## Portada

| | |
|---|---|
| **Proyecto** | TBalance — Sistema de Inventario |
| **Universidad** | Universidad Popular del Cesar |
| **Asignatura** | Programación de Computadores III (SS462) — grupo 3 |
| **Docente** | Ing. Esp. Alfredo Bautista |
| **Integrantes** | Kevin Jhosep Mercado · Reinaldo Padilla Pedroza · Dair José Quintero |
| **Repositorio** | https://github.com/kjhosepmercadom/TBalance |
| **Versión documentada** | v3.5.0 |
| **Lugar y año** | Valledupar, 2026 |

## Resumen

TBalance es un programa de escritorio para llevar el inventario y las ventas de negocios pequeños, como tiendas de barrio, droguerías y ferreterías. Una misma persona puede manejar varios negocios, y cada uno guarda por separado sus productos, categorías, proveedores, entradas, salidas, ventas y fiados. Cada persona entra con su usuario y ve solo lo que su rol le permite: dueño, administrador o cajero. El programa avisa qué productos se están agotando o están por vencer, muestra cuánto se vende y se gana, lleva la cuenta de lo que deben los clientes que compran fiado y deja registrado quién hizo cada cosa. Además permite vender leyendo etiquetas QR con la cámara del computador o con un celular, hacer copias de seguridad y consultar a un asistente con inteligencia artificial. Está hecho en Java y guarda los datos en PostgreSQL, en Oracle o en archivos del computador. El proyecto parte del sistema construido en Programación II; en Programación III se organizó en capas, se aplicaron los patrones GRASP y los principios SOLID vistos en clase y se agregaron las funciones nuevas.

## 1. Introducción

### 1.1 Contextualización

Muchos negocios pequeños controlan su mercancía en un cuaderno o en una hoja de cálculo. Así es difícil saber en el momento qué productos se están agotando, cuáles están por vencer, cuánto se vendió en el día o cuánto deben los clientes a los que se les fía. Cuando el negocio tiene empleados, tampoco queda claro quién vendió o quién anuló una venta.

### 1.2 Importancia

Un control de inventario confiable evita perder ventas por falta de producto, reduce las pérdidas por mercancía vencida y deja un registro de cada movimiento. Para un negocio pequeño es importante que la herramienta sea gratuita, fácil de usar y que funcione sin internet.

### 1.3 Breve descripción

TBalance funciona en el computador del negocio y no necesita internet (solo el asistente lo usa). El dueño crea su cuenta y sus negocios, y luego crea las cuentas de su administrador y de sus cajeros. Cada entrada, salida o venta actualiza el stock de inmediato. La pantalla Hoy resume lo vendido y lo que hay que resolver; Informes y Costos muestran las ventas, la ganancia y lo que costó la mercancía; Fiados muestra quién debe y registra sus abonos; Historial muestra quién hizo cada acción. Cada producto puede tener una etiqueta QR para vender más rápido con la cámara o con un celular.

## 2. Planteamiento del problema

### 2.1 Identificación del problema

Los negocios pequeños no tienen una forma sencilla y económica de saber en todo momento cuánto stock tienen, qué productos están por agotarse o por vencer, cuánto han vendido y cuánto les deben. El registro manual es lento, se presta a errores y, si se pierde el cuaderno o el archivo, se pierde la información.

### 2.2 Justificación

TBalance responde a esa necesidad con una herramienta gratuita y de escritorio que actualiza el stock con cada movimiento, avisa de los productos en riesgo, lleva las cuentas de los fiados, separa lo que puede hacer cada empleado y protege la información con copias de seguridad. Desde lo académico, el proyecto permite aplicar los temas de la asignatura: programación orientada a objetos, herencia y polimorfismo, interfaces, colecciones, arquitectura en capas, persistencia con base de datos, patrones GRASP y principios SOLID.

### 2.3 Estado del arte / vigilancia tecnológica

| Solución | Tipo | Características | Diferencia con TBalance |
|---|---|---|---|
| Cuaderno u hoja de cálculo (Excel) | Manual | Sin costo adicional, pero cada cálculo y alerta se hace a mano. | TBalance actualiza el stock y genera las alertas solo. |
| Loyverse POS | Punto de venta en la nube | Gratuito, funciona en celulares y tabletas, inventario y reportes. | Depende de la nube; TBalance funciona en el computador sin internet. |
| Alegra | Software contable y punto de venta en la nube | Facturación, inventario y lector de código de barras; pago por suscripción. | TBalance es gratuito y no pide suscripción. |
| Odoo Inventory | Sistema empresarial (módulo de inventario) | Muy completo, con escáner de códigos; gratis para una sola aplicación. | Necesita instalación y configuración de un sistema grande; TBalance está pensado para un negocio pequeño. |
| **TBalance** | Programa de escritorio | Gratuito, sin internet, varios negocios, usuarios con roles, alertas de stock y vencimiento, informes con ganancia real, fiados con abonos, historial, venta por QR con cámara o celular, copias de seguridad y asistente con IA. | — |

## 3. Objetivos

### 3.1 Objetivo general

Desarrollar un programa de escritorio en Java para gestionar el inventario y las ventas de negocios pequeños, organizado en capas y aplicando los patrones GRASP, los principios SOLID y buenas prácticas de programación.

### 3.2 Objetivos específicos

1. Organizar el sistema de Programación II en tres capas (presentación, lógica de negocio y acceso a datos) sin perder los datos que ya tenían los usuarios.
2. Modelar el negocio con clases que usen encapsulamiento, herencia, polimorfismo, clases abstractas e interfaces.
3. Implementar la gestión de negocios, productos, categorías, proveedores, entradas, salidas, ventas y fiados, validando los datos.
4. Guardar la información en una base de datos relacional (PostgreSQL u Oracle) y mantener la opción de archivos.
5. Controlar el acceso con usuarios y roles, y registrar quién hace cada acción.
6. Implementar copias de seguridad, etiquetas QR, venta por QR, informes de ventas y un asistente con inteligencia artificial.
7. Trabajar en equipo con Git y GitHub, con una rama por integrante y Pull Requests aprobados por otro compañero.

## 4. Descripción de la solución

### 4.1 Descripción general

TBalance tiene tres capas. La **presentación** es una página web (HTML, CSS y JavaScript) que se abre en una ventana de Microsoft Edge; si el computador no tiene Edge, se abre en una ventana de JavaFX. La página le pide todo a Java por medio de un objeto llamado `javaBridge`. La **lógica de negocio** está en los servicios y en las clases del modelo, que aplican las reglas (por ejemplo, no vender más de lo que hay en stock). El **acceso a datos** está en los repositorios, que guardan en PostgreSQL, en Oracle o en archivos JSON. La arquitectura se explica en la [Fase 2](FASE2_Arquitectura_y_Diseno.md).

Secciones del programa: Inicio de sesión, Hoy, Productos, Categorías, Proveedores, Entradas, Salidas, Ventas (con venta por QR), Fiados, Informes, Costos, Mis negocios, Usuarios, Historial y Copias de seguridad, más el asistente "Pregúntale a TBalance" en la barra superior.

### 4.2 Requerimientos funcionales

| Código | Requerimiento | Descripción |
|---|---|---|
| RF01 | Gestionar negocios | Crear, editar, deshabilitar, volver a habilitar y abrir negocios. Cada negocio tiene un tipo (tienda de barrio, droguería, ferretería, supermercado, boutique, tecnología u otro) que trae categorías sugeridas y su propio color. Un negocio deshabilitado conserva sus datos. |
| RF02 | Gestionar productos | Registrar, editar, deshabilitar y volver a habilitar productos con nombre, código, descripción, categoría, proveedor, precio de compra y de venta, stock, stock mínimo y fecha de vencimiento. |
| RF03 | Buscar y filtrar productos | Buscar por nombre, descripción o código y filtrar por categoría. |
| RF04 | Gestionar categorías | Crear, editar, deshabilitar y volver a habilitar categorías. No se deshabilita una categoría que tenga productos habilitados. |
| RF05 | Gestionar proveedores | Crear, editar, deshabilitar y volver a habilitar proveedores con empresa, contacto, teléfono, correo, dirección y los tipos de producto que surten. |
| RF06 | Registrar entradas | Registrar la mercancía que llega: producto, cantidad, fecha de llegada (no futura), costo por unidad y proveedor. El stock sube y, si el costo cambió, el producto queda con el costo nuevo. |
| RF07 | Registrar salidas | Registrar mercancía que sale sin venderse (dañada, vencida, consumo interno, devolución o pérdida) con su motivo. El stock baja y no se puede sacar más de lo que hay. |
| RF08 | Registrar ventas | Registrar una venta con uno o varios productos, el cliente y el medio de pago. Se revisa el stock de todos los productos antes de descontarlo y se guarda el costo de cada producto en ese momento. |
| RF09 | Ver el resumen del día (Hoy) | Ver lo vendido hoy comparado con el mismo día de la semana anterior, lo que hay que resolver (productos vencidos, por vencer, agotados o con poco stock, clientes que deben y productos sin precio de compra), las ventas del día y los pedidos sugeridos por proveedor. |
| RF10 | Copias de seguridad | Crear una copia de todos los datos, listarlas, restaurar una (antes se guarda una copia del estado actual) y eliminarlas. Con base de datos las copias se guardan dentro de la base. |
| RF11 | Etiquetas QR | Ver y guardar la etiqueta QR de un producto e imprimir hojas tamaño carta con 18 etiquetas. |
| RF12 | Venta por QR | Leer las etiquetas QR con la cámara del computador o con un celular conectado al Wi-Fi del local (sin instalar aplicaciones ni usar internet) y agregar cada producto leído a la venta. Se rechazan etiquetas de otro negocio o de productos agotados o deshabilitados. |
| RF13 | Ver la ganancia por producto | Al registrar un producto se muestra cuánto se gana por unidad y el margen; la tabla de productos muestra la ganancia de cada uno. |
| RF14 | Medios de pago | Registrar cómo pagó el cliente: efectivo, Nequi, Daviplata, Bre-B, transferencia Bancolombia, tarjeta débito, tarjeta crédito o fiado. En pagos digitales se puede anotar el número de comprobante y, en tarjeta, el número del voucher del datáfono. TBalance solo registra el medio de pago: no cobra. |
| RF15 | Informes de ventas | Ver las ventas de hoy, la semana, el mes, el año o un rango de fechas, comparadas con el período anterior: número de ventas, ticket promedio, ganancia real, gráfica por día, semana o mes, productos más vendidos y ventas por medio de pago. |
| RF16 | Exportar informes | Guardar el informe en Excel (cuatro hojas) en la carpeta Documentos/TBalance o imprimirlo en PDF. |
| RF17 | Asistente con inteligencia artificial | Hacerle preguntas al asistente (por ejemplo, "¿qué le falta de Postobón?" o "¿quién me debe?"). El asistente también prepara ventas, entradas, salidas, cambios de precio y productos nuevos, que solo se guardan cuando el usuario pulsa Confirmar. Usa Google Gemini en su versión gratuita y es lo único que necesita internet. |
| RF18 | Elegir dónde se guardan los datos | Elegir entre archivos del computador, PostgreSQL u Oracle (en el computador o en la nube); probar la conexión y empezar a usarla. La primera vez TBalance crea las tablas y copia los negocios que ya existían. Un computador nuevo puede conectarse, desde la pantalla de inicio, a la base que ya usa el negocio. |
| RF27 | Trabajar desde varios computadores | Con la base en la nube, el dueño, el administrador y el cajero trabajan cada uno en su computador con los mismos datos. Cada pantalla se actualiza sola, en pocos segundos, cuando otro computador guarda algo, y antes de vender se revisa el stock real para que dos cajas no vendan la misma unidad. |
| RF19 | Anular movimientos | Las ventas, entradas y salidas no se borran: se anulan con un motivo, el stock se corrige y quedan en una lista de anuladas. |
| RF20 | Consultar costos | Ver cuánto costó el inventario actual, cuánto se compró en un período por producto y por proveedor, y qué productos no tienen costo registrado. |
| RF21 | Iniciar sesión con roles | Cada persona entra con su usuario y contraseña y solo ve los negocios a los que tiene acceso. El **dueño** puede hacer todo; el **administrador** maneja productos, mercancía, anulaciones, informes, costos y cajeros; el **cajero** vende, consulta precios y recibe abonos, sin ver costos ni ganancias, y puede anular sus propias ventas en los primeros 10 minutos. |
| RF22 | Ver el historial de acciones | Cada venta, anulación, entrada, salida, cambio, copia de seguridad, abono e inicio de sesión queda anotado con la persona, su rol, el negocio, la fecha y la hora. Lo ven el dueño y el administrador; nadie lo puede editar ni borrar. |
| RF23 | Identificar a quien queda debiendo | Una venta fiada exige el nombre y la cédula del cliente. En las demás ventas el cliente es opcional y, si no se escribe, queda como "Consumidor final". |
| RF24 | Buscar el producto escribiendo | En la venta, la entrada y la salida se escribe parte del nombre o del código y la lista se reduce; Enter elige el primero. En entradas y salidas también se filtra por categoría. |
| RF25 | Medios de pago y cuentas por negocio | El dueño elige qué medios de pago recibe cada negocio y escribe sus cuentas (Nequi, Daviplata, Bancolombia y llave Bre-B); también puede subir la imagen del QR de Bre-B que le da su banco. En la venta los medios se agrupan en Efectivo, Transferencia o QR, Tarjeta y Fiado; en una transferencia se elige a qué cuenta llegó, se muestra el QR o la llave, y el cajero debe confirmar que el dinero llegó antes de registrar la venta. El efectivo siempre está activo. |
| RF26 | Fiados y abonos | Ver quién le debe al negocio y cuánto; abrir un cliente para ver sus compras fiadas y sus abonos; registrar un abono con su medio de pago (no puede pasar de lo que el cliente debe). El dueño y el administrador pueden anular un abono con un motivo. |

### 4.3 Requerimientos no funcionales

| Código | Requerimiento | Descripción |
|---|---|---|
| RNF01 | Plataforma | Programa de escritorio en Java 17 o superior (se ejecuta con JDK 21) con JavaFX 21; se compila y se ejecuta con Maven. La interfaz se abre en Microsoft Edge, que viene con Windows 10 y 11. |
| RNF02 | Funcionamiento sin internet | Todo funciona sin conexión excepto el asistente. Las fuentes, los estilos y las librerías vienen dentro del proyecto. |
| RNF03 | Base de datos | Los datos se guardan en PostgreSQL (11 o superior) u Oracle (18c XE o superior), en el computador o en la nube (por ejemplo, Neon u Oracle Autonomous Database), o en archivos JSON. Cuando hay una versión nueva, TBalance agrega solo las tablas o columnas que faltan, sin borrar datos. Si la conexión se corta, TBalance se vuelve a conectar solo. |
| RNF04 | Integridad de los datos | Nada que tenga historial se borra: se deshabilita o se anula. Los archivos se escriben primero en un temporal para no dejarlos a medias. |
| RNF05 | Validación y mensajes | Las reglas se revisan en Java y los errores se le muestran al usuario como un aviso claro (por ejemplo, "Para fiar escriba la cédula del cliente"). |
| RNF06 | Seguridad de las cuentas | Las contraseñas se guardan cifradas. Después de 5 intentos fallidos el usuario se bloquea 5 minutos y la sesión se cierra tras 30 minutos sin uso. Los permisos se revisan en Java antes de cada acción, no solo ocultando botones. |
| RNF07 | Datos privados fuera del código | La clave del asistente y la contraseña de la base de datos se guardan solo en el computador (carpeta `.inventariopro` del usuario), nunca en el código ni en GitHub. |
| RNF08 | Usabilidad | Diseño limpio con modo claro y oscuro, color según el tipo de negocio, dinero en pesos colombianos ($ 1.250.000) y un aviso después de cada operación. |
| RNF09 | Fluidez | La ventana de Edge usa la tarjeta gráfica para las animaciones. Después de cada operación, la pantalla le pide a Java todos los datos en un solo viaje y solo dibuja lo que se ve. En computadores lentos el programa apaga solo los efectos de desenfoque. |
| RNF10 | Mantenibilidad | Código en capas, con Javadoc, nombres claros y los patrones GRASP y principios SOLID aplicados. |

## 5. Gestión del proyecto con Git

### 5.1 Repositorio

| | |
|---|---|
| **Nombre** | TBalance |
| **URL** | https://github.com/kjhosepmercadom/TBalance |
| **Descripción** | Sistema de inventario de escritorio en Java para negocios pequeños, con varios negocios, usuarios con roles, ventas, fiados, informes, copias de seguridad, venta por QR y asistente con IA. Proyecto de aula de Programación III. |

### 5.2 Roles del equipo

| Integrante | Usuario GitHub | Rol | Responsabilidades |
|---|---|---|---|
| Kevin Jhosep Mercado | @kjhosepmercadom | Líder e integrador | Dueño del repositorio; servicios y controladores; base de datos (PostgreSQL y Oracle), borrado lógico, copias en la base, historial y fiados; Pull Requests de `dev` a `main` y etiquetas de versión. |
| Reinaldo Padilla Pedroza | @rjpadillap | Persistencia, seguridad y documentación | Repositorios con archivos, copias de seguridad, lector QR y cámara; usuarios y roles; asistente por rol; documentación por fases y diagramas. |
| Dair José Quintero | @djosequintero | Modelo e interfaz | Modelo del negocio, interfaz web, ventana de Edge, celular como lector QR y capturas de pantalla. |

Cada Pull Request lo revisa y lo aprueba otro integrante antes de fusionarse; nadie aprueba el suyo.

### 5.3 Estrategia de ramas

| Rama | Uso |
|---|---|
| `main` | Versiones entregadas. Solo recibe fusiones desde `dev` y cada una se marca con una etiqueta (`v3.0.0-avance1`, `v3.1.0`, `v3.2.0`, `v3.3.0`, `v3.4.0`, `v3.5.0`). |
| `dev` | Integración del equipo; recibe los Pull Requests de las ramas de cada integrante. |
| `feature/<integrante>-<tema>` | Una rama por integrante y tema (por ejemplo `feature/kevin-database-history-credit`). Se borra después de fusionarse. |

Los mensajes de commit siguen la forma `tipo: descripción`, con los tipos `feat` (función nueva), `fix` (corrección), `refactor` (reorganización), `docs` (documentación), `build` y `chore`.

### 5.4 Tareas por integrante

| Integrante | Tarea | Rama | Versión |
|---|---|---|---|
| Kevin Mercado | Importar el código de P2 y configurar el repositorio | `main` | v3.0.0-avance1 |
| Dair Quintero | Modelo de dominio: `Product` y `Sale` como expertos, jerarquía `InventoryMovement`, `IdentifiedEntity` encapsulada | `feature/dair-modelo-dominio` | v3.0.0-avance1 |
| Reinaldo Padilla | Persistencia: `Repository<T>`, `JsonRepository<T>`, `JsonFileStore`, `ProfileDirectory`; quitar código muerto | `feature/reinaldo-persistencia-json` | v3.0.0-avance1 |
| Kevin Mercado | Servicios con `EntityService<T>`, inyección de dependencias, `AppContext` y `JavaBridge` con controladores | `feature/kevin-servicios-bridge` | v3.0.0-avance1 |
| Reinaldo Padilla | Respaldos: `ZipArchiver`, `Backup`, `BackupRepository` | `feature/reinaldo-backup-storage` | v3.1.0 |
| Kevin Mercado | `BackupService`, recarga del `AppContext` y `BackupController` | `feature/kevin-backup-service` | v3.1.0 |
| Dair Quintero | Sección Respaldos en la interfaz y documentación | `feature/dair-backup-ui` | v3.1.0 |
| Reinaldo Padilla | Documentación de arquitectura de respaldos | `feature/reinaldo-architecture-docs` | v3.1.0 |
| Reinaldo Padilla | Dependencias ZXing y webcam-capture, `QrGenerator`, `QrReader`, `LabelRepository`, `Camera` | `feature/reinaldo-qr-reader-camera` | v3.2.0 |
| Kevin Mercado | QR en `Product`, `LabelService`, `QrSaleScanner`, `LabelController`, `ScanController` | `feature/kevin-qr-sale-service` | v3.2.0 |
| Dair Quintero | Etiquetas QR en Productos y pantalla Venta por QR | `feature/dair-qr-sale-ui` | v3.2.0 |
| Kevin Mercado | Nombres de las clases en inglés y utilidad por producto | `feature/kevin-english-names-profit` | v3.3.0 |
| Dair Quintero | Precio de compra y venta, columna de utilidad e interfaz rediseñada | `feature/dair-profit-ui-redesign` | v3.3.0 |
| Reinaldo Padilla | Documentación por fases (Fase 1 a 4) | `feature/reinaldo-docs-v3.3.0` | v3.3.0 |
| Kevin Mercado | Métodos de pago (`PaymentMethod`), informes (`ReportService`, Excel) y asistente (`ia`, `AssistantService`) | `feature/kevin-payments-reports-assistant` | v3.4.0 |
| Dair Quintero | Ventana de Edge, interfaz plana con color por tipo de negocio, pantalla Hoy, cuaderno de caja, Informes, asistente y métodos de pago, capturas | `feature/dair-edge-window-new-ui` | v3.4.0 |
| Reinaldo Padilla | Documentación v3.4.0: diagramas, requerimientos, clases nuevas y trazabilidad | `feature/reinaldo-docs-v3.4.0` | v3.4.0 |
| Kevin Mercado | Guardar los datos en PostgreSQL u Oracle (repositorios JDBC, scripts SQL y cambios automáticos del esquema) | `feature/kevin-database-history-credit` | v3.5.0 |
| Kevin Mercado | Borrado lógico (deshabilitar y anular con motivo) y sección Costos | `feature/kevin-database-history-credit` | v3.5.0 |
| Kevin Mercado | Copias de seguridad dentro de la base de datos | `feature/kevin-database-history-credit` | v3.5.0 |
| Kevin Mercado | Historial de acciones (quién hizo qué y cuándo) | `feature/kevin-database-history-credit` | v3.5.0 |
| Kevin Mercado | Fiados: quién debe, abonos y anulación de abonos | `feature/kevin-database-history-credit` | v3.5.0 |
| Kevin Mercado | Base en la nube desde varios computadores: recarga por cambios, stock real al vender y reconexión | `feature/kevin-database-history-credit` | v3.5.0 |
| Dair Quintero | Celular como lector de etiquetas QR por el Wi-Fi del local | `feature/dair-glass-ui-phone-reader` | v3.5.0 |
| Dair Quintero | Interfaz: inicio de sesión y ventanas en vidrio, Fiados, Historial, Usuarios, buscador, cobro por cuenta y QR de Bre-B, y carga más rápida | `feature/dair-glass-ui-phone-reader` | v3.5.0 |
| Dair Quintero | Capturas de la interfaz v3.5.0 | `feature/dair-glass-ui-phone-reader` | v3.5.0 |
| Reinaldo Padilla | Inicio de sesión con roles (dueño, administrador y cajero) | `feature/reinaldo-roles-docs-v3.5.0` | v3.5.0 |
| Reinaldo Padilla | El asistente respeta el rol y responde quién debe | `feature/reinaldo-roles-docs-v3.5.0` | v3.5.0 |
| Reinaldo Padilla | Documentación v3.5.0: fases 1 a 4, diagramas, trazabilidad y README | `feature/reinaldo-roles-docs-v3.5.0` | v3.5.0 |

El estado de cada tarea y los commits están en la [Fase 3](FASE3_Desarrollo.md#7-gestión-del-proyecto-con-git).

## 6. Conclusiones

1. Separar el programa en capas permitió cambiar de archivos a base de datos creando repositorios nuevos, sin tocar las reglas del negocio ni la interfaz.
2. Dejar cada regla en la clase que tiene los datos (el producto controla su stock, la venta calcula su total) evitó repetir código y errores.
3. Revisar los permisos en Java, y no solo en la pantalla, hizo que el cajero no pueda ver costos ni hacer lo que no le corresponde, aunque intente llamar la función directamente.
4. No borrar nada con historial (deshabilitar y anular) y anotar quién hace cada acción le da al dueño control sobre su negocio y sobre sus empleados.
5. Trabajar con ramas por integrante y Pull Requests aprobados por otro compañero dejó un registro claro de quién hizo cada parte.

## 7. Bibliografía

- Larman, C. (2004). *UML y patrones: una introducción al análisis y diseño orientado a objetos y al proceso unificado* (2.ª ed.). Pearson.
- Martin, R. C. (2009). *Código limpio: manual de estilo para el desarrollo ágil de software*. Anaya Multimedia.
- Gamma, E., Helm, R., Johnson, R. y Vlissides, J. (2003). *Patrones de diseño*. Addison-Wesley.
- Chacon, S. y Straub, B. (2014). *Pro Git* (2.ª ed.). Apress. https://git-scm.com/book/es/v2
- Oracle. *Java SE 17 Documentation*. https://docs.oracle.com/en/java/javase/17/
- OpenJFX. *JavaFX 21 Documentation*. https://openjfx.io/
- The PostgreSQL Global Development Group. *PostgreSQL Documentation*. https://www.postgresql.org/docs/
- Oracle. *Oracle Database 18c Express Edition*. https://docs.oracle.com/en/database/oracle/oracle-database/18/xeinl/
- Google. *Gson*. https://github.com/google/gson
- ZXing Project. *ZXing ("Zebra Crossing")*. https://github.com/zxing/zxing
- Firyn, B. *Webcam Capture*. https://github.com/sarxos/webcam-capture
- Google. *Gemini API: llamadas a funciones*. https://ai.google.dev/gemini-api/docs/function-calling
- Loyverse POS en Capterra. https://www.capterra.es/software/150632/loyverse-pos
- Alegra. *Cómo aprovechar al máximo Alegra si tienes un negocio de retail*. https://blog.alegra.com/colombia/como-aprovechar-al-maximo-alegra-si-tienes-un-negocio-de-retail/
- Odoo. *Inventory*. https://www.odoo.com/app/inventory
