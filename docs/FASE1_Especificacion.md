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
| **Versión documentada** | v3.3.0 |
| **Lugar y año** | Valledupar, 2026 |

## Resumen

TBalance es una aplicación de escritorio para llevar el inventario de pequeños negocios. Permite tener varios perfiles de negocio independientes y, en cada uno, administrar productos, categorías y proveedores, registrar entradas, salidas y ventas, consultar un dashboard con alertas de stock y vencimiento, hacer copias de seguridad y vender leyendo etiquetas QR con la webcam del computador. Está desarrollada en Java 17 con JavaFX (WebView) y guarda los datos en archivos JSON locales. El proyecto parte del sistema construido en Programación II y en Programación III se reorganizó en capas aplicando patrones GRASP, principios SOLID y código limpio, y se le agregaron las copias de seguridad y la venta por QR.

## 1. Introducción

### 1.1 Contextualización

Muchas tiendas de barrio, farmacias, ferreterías y negocios pequeños controlan su mercancía en un cuaderno o en una hoja de cálculo. Así es difícil saber en el momento qué productos se están agotando, cuáles están por vencer o cuánto se ha vendido, y registrar cada venta a mano toma tiempo.

### 1.2 Importancia

Un control de inventario confiable evita perder ventas por falta de producto, reduce las pérdidas por mercancía vencida y deja un registro de cada movimiento. Para un negocio pequeño es importante que la herramienta sea gratuita, sencilla y que funcione sin internet.

### 1.3 Breve descripción

TBalance es un programa de escritorio que funciona sin conexión. El usuario crea un perfil por negocio (eligiendo el tipo: boutique, farmacia, ferretería, supermercado, tienda de barrio, tecnología u otro), registra sus productos y a partir de ahí cada entrada, salida o venta actualiza el stock automáticamente. El dashboard muestra las alertas, se pueden hacer copias de seguridad de todos los datos y cada producto puede tener una etiqueta QR que se lee con la webcam para vender más rápido.

## 2. Planteamiento del problema

### 2.1 Identificación del problema

Los negocios pequeños no tienen una forma sencilla y económica de saber en todo momento cuánto stock tienen, qué productos están en bajo stock, agotados o por vencer, y cuánto han vendido. El registro manual es lento, se presta a errores y, si se pierde el cuaderno o el archivo, se pierde la información.

### 2.2 Justificación

Se desarrolla TBalance porque responde a esa necesidad con una herramienta gratuita, de escritorio y sin internet, que actualiza el stock con cada movimiento, avisa de los productos en riesgo, protege la información con copias de seguridad y agiliza la venta con códigos QR. Desde lo académico, el proyecto permite aplicar los temas de la asignatura: programación orientada a objetos, herencia y polimorfismo, interfaces, colecciones, arquitectura en capas, patrones GRASP y principios SOLID.

### 2.3 Estado del arte / vigilancia tecnológica

| Solución | Tipo | Características | Diferencia con TBalance |
|---|---|---|---|
| Cuaderno u hoja de cálculo (Excel) | Manual | Sin costo adicional, pero cada cálculo y alerta se hace a mano. | TBalance actualiza el stock y genera las alertas automáticamente. |
| Loyverse POS | Punto de venta en la nube | Gratuito, funciona en celulares y tabletas, inventario y reportes. | Depende de la nube y de dispositivos móviles; TBalance es de escritorio y local. |
| Alegra | Software contable y POS en la nube | Facturación, inventario y lector de código de barras; pago por suscripción. | TBalance es gratuito y no requiere cuenta ni suscripción. |
| Odoo Inventory | ERP (módulo de inventario) | Muy completo, escáner de código de barras y QR; gratis para una sola aplicación. | Requiere instalación y configuración de un ERP; TBalance está pensado para un negocio pequeño. |
| **TBalance** | Aplicación de escritorio | Gratuita, sin internet, varios perfiles de negocio, alertas de stock y vencimiento, copias de seguridad y venta por QR con la webcam. | — |

## 3. Objetivos

### 3.1 Objetivo general

Desarrollar una aplicación de escritorio en Java para gestionar el inventario de pequeños negocios, organizada en capas y aplicando patrones GRASP, principios SOLID y buenas prácticas de programación.

### 3.2 Objetivos específicos

1. Reorganizar el sistema de Programación II en capas (presentación, lógica de negocio y acceso a datos) sin perder la compatibilidad con los datos existentes.
2. Modelar el dominio con clases que apliquen encapsulamiento, herencia, polimorfismo, clases abstractas e interfaces.
3. Implementar la gestión de perfiles, productos, categorías, proveedores, entradas, salidas y ventas con validación de los datos.
4. Implementar copias de seguridad para proteger la información del usuario.
5. Implementar etiquetas QR y la venta por QR con la webcam del computador.
6. Gestionar el desarrollo en equipo con Git y GitHub, usando ramas por integrante y funcionalidad y Pull Requests revisados.

## 4. Descripción de la solución

### 4.1 Descripción general

TBalance se compone de una interfaz web (HTML, CSS y JavaScript) que se muestra dentro de una ventana JavaFX con un `WebView`. La interfaz llama a los métodos de Java a través del objeto `window.javaBridge`; el bridge delega en los servicios, que aplican las reglas de negocio usando las clases del modelo, y los repositorios guardan los datos en archivos JSON. La arquitectura se describe en la [Fase 2](FASE2_Arquitectura_y_Diseno.md).

Secciones de la interfaz: Dashboard, Perfiles, Productos, Categorías, Proveedores, Entradas, Salidas, Ventas (con Venta por QR) y Respaldos.

### 4.2 Requerimientos funcionales

| Código | Requerimiento | Descripción |
|---|---|---|
| RF01 | Gestionar perfiles de negocio | Crear, editar, eliminar y seleccionar perfiles. Cada perfil tiene un tipo de negocio (`BusinessType`) que trae categorías predefinidas y textos de ejemplo, y guarda sus datos en una carpeta propia. |
| RF02 | Gestionar productos | Registrar, editar y eliminar productos con nombre, código, descripción, categoría, proveedor, precio de compra y de venta, stock, stock mínimo y fecha de vencimiento. |
| RF03 | Buscar y filtrar productos | Buscar por nombre, descripción, código o ID y filtrar por categoría. |
| RF04 | Gestionar categorías | Crear, editar y eliminar las categorías del perfil. |
| RF05 | Gestionar proveedores | Crear, editar y eliminar proveedores (nombre, contacto, teléfono, email, dirección, categoría y estado). |
| RF06 | Registrar entradas | Registrar el ingreso de mercancía de un producto; el stock aumenta automáticamente. |
| RF07 | Registrar salidas | Registrar la salida de unidades con su motivo; el stock disminuye y no se permite sacar más de lo disponible. |
| RF08 | Registrar ventas | Registrar una venta con uno o varios productos, cliente, método de pago y observación; se valida el stock de todos los ítems antes de descontarlo. |
| RF09 | Consultar el dashboard | Ver el total de productos, productos en bajo stock, número de ventas, total de proveedores, utilidad estimada del inventario, productos por reponer (con acceso directo a registrar su entrada), ventas de los últimos 14 días y, si el perfil controla vencimientos, productos vencidos y por vencer en 7 y 30 días. |
| RF10 | Copias de seguridad | Crear una copia `.zip` de todos los datos, listarlas, restaurar una (guardando antes una copia del estado actual) y eliminarlas. |
| RF11 | Etiquetas QR | Ver y guardar la etiqueta con código QR de un producto y generar hojas tamaño carta de 18 etiquetas (3 × 6) con todos los productos. |
| RF12 | Venta por QR | Encender la webcam del computador, leer las etiquetas QR y agregar cada producto leído a la venta; ajustar cantidades y registrar la venta. |
| RF13 | Estimar la utilidad | Al registrar o editar un producto se piden el precio de compra y el de venta y se muestra la utilidad por unidad y el margen; la tabla de productos muestra la utilidad de cada uno y el dashboard la utilidad estimada de todo el stock. |

### 4.3 Requerimientos no funcionales

| Código | Requerimiento | Descripción |
|---|---|---|
| RNF01 | Plataforma | Aplicación de escritorio en Java 17 o superior con JavaFX 21; se construye y ejecuta con Maven. |
| RNF02 | Funcionamiento sin internet | La interfaz, las fuentes, Tailwind CSS y las librerías se incluyen en el proyecto; no se necesita conexión. |
| RNF03 | Persistencia local compatible | Los datos se guardan en archivos JSON en `~/.inventariopro/data` con el mismo formato de la versión de Programación II. |
| RNF04 | Integridad de los datos | Cada archivo se escribe primero en un temporal y luego se reemplaza (`JsonFileStore.escribir`), para no dejar archivos a medias. |
| RNF05 | Validación y mensajes | Las reglas de negocio se validan en el modelo y los servicios; los errores llegan a la vista como `ERROR: mensaje` y se muestran como notificación. |
| RNF06 | Mantenibilidad | Arquitectura en capas, principios GRASP y SOLID, Javadoc y convenciones de nombres de Java. |
| RNF07 | Rendimiento de la cámara | La cámara funciona en un hilo propio (pausa de 90 ms entre cuadros), por lo que la ventana no se congela; una etiqueta quieta se cuenta una sola vez (2 s). |
| RNF08 | Seguridad de archivos | Solo se aceptan nombres de copia `.zip` y de etiqueta `.png` válidos, y al extraer un `.zip` se rechazan rutas fuera de la carpeta destino (*zip slip*). |
| RNF09 | Usabilidad | Interfaz en naranja y blanco con menú lateral cuya sección activa se ve como una pestaña unida a la página, tarjetas en forma de carpeta, modo claro y oscuro, íconos propios y notificaciones de cada operación. |

## 5. Gestión del proyecto con Git

### 5.1 Repositorio

| | |
|---|---|
| **Nombre** | TBalance |
| **URL** | https://github.com/kjhosepmercadom/TBalance |
| **Descripción** | Sistema de inventario de escritorio en Java 17 y JavaFX para pequeños negocios, con perfiles, ventas, copias de seguridad y venta por QR. Proyecto de aula de Programación III. |

### 5.2 Roles del equipo

| Integrante | Usuario GitHub | Rol | Responsabilidades |
|---|---|---|---|
| Kevin Jhosep Mercado | @kjhosepmercadom | Líder e integrador | Dueño del repositorio; capa de servicios, `AppContext` y controladores del bridge; Pull Requests de `dev` a `main` y etiquetas de versión. |
| Reinaldo Padilla Pedroza | @rjpadillap | Persistencia y dispositivos | Capa de acceso a datos (repositorios JSON, respaldos, etiquetas), librerías QR y cámara. |
| Dair José Quintero | @djosequintero | Modelo, interfaz y documentación | Modelo de dominio, interfaz web (HTML/JS) y documentación del proyecto. |

Cada Pull Request es revisado y aprobado por otro integrante antes de fusionarse.

### 5.3 Estrategia de ramas

| Rama | Uso |
|---|---|
| `main` | Versiones entregadas; solo recibe fusiones desde `dev` y cada fusión se marca con una etiqueta (`v3.0.0-avance1`, `v3.1.0`, `v3.2.0`). |
| `dev` | Integración del equipo; recibe los Pull Requests de las ramas de funcionalidad. |
| `feature/<integrante>-<funcionalidad>` | Una rama por integrante y funcionalidad (por ejemplo `feature/reinaldo-persistencia-json`). Se borra después de fusionarse. |

Convención de commits: `tipo(alcance): descripción`, con los tipos `feat`, `refactor`, `fix`, `docs`, `build` y `chore`.

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

El estado de cada tarea y la evidencia de los commits están en la [Fase 3](FASE3_Desarrollo.md#6-gestión-del-proyecto-con-git).

## 6. Conclusiones

1. Separar el sistema en capas permitió agregar las copias de seguridad y la venta por QR creando clases nuevas en cada capa, sin cambiar el formato de los datos ni romper lo que ya funcionaba.
2. Aplicar Experto en Información hizo que las reglas (stock, vencimiento, total de una venta) queden en la clase que tiene los datos, en lugar de repetirse en los servicios.
3. Depender de interfaces (`Repository<T>`, `Camera`, `ViewNotifier`, `FileOpener`) reduce el acoplamiento y deja preparado el cambio a una base de datos relacional.
4. Trabajar con ramas por integrante y Pull Requests aprobados por otro compañero dejó un historial claro de quién hizo cada parte.

## 7. Bibliografía

- Larman, C. (2004). *UML y patrones: una introducción al análisis y diseño orientado a objetos y al proceso unificado* (2.ª ed.). Pearson.
- Martin, R. C. (2009). *Código limpio: manual de estilo para el desarrollo ágil de software*. Anaya Multimedia.
- Martin, R. C. (2018). *Arquitectura limpia: guía para especialistas en la estructura y el diseño de software*. Anaya Multimedia.
- Gamma, E., Helm, R., Johnson, R. y Vlissides, J. (2003). *Patrones de diseño*. Addison-Wesley.
- Chacon, S. y Straub, B. (2014). *Pro Git* (2.ª ed.). Apress. https://git-scm.com/book/es/v2
- Oracle. *Java SE 17 Documentation*. https://docs.oracle.com/en/java/javase/17/
- OpenJFX. *JavaFX 21 Documentation*. https://openjfx.io/
- Google. *Gson*. https://github.com/google/gson
- ZXing Project. *ZXing (“Zebra Crossing”)*. https://github.com/zxing/zxing
- Firyn, B. *Webcam Capture*. https://github.com/sarxos/webcam-capture
- Loyverse POS en Capterra. https://www.capterra.es/software/150632/loyverse-pos
- Alegra. *Cómo aprovechar al máximo Alegra si tienes un negocio de retail*. https://blog.alegra.com/colombia/como-aprovechar-al-maximo-alegra-si-tienes-un-negocio-de-retail/
- Odoo. *Inventory*. https://www.odoo.com/app/inventory
