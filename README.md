# TBalance — Sistema de Inventario

Programa de escritorio en **Java** para llevar el inventario y las ventas de negocios pequeños. Una persona puede manejar varios negocios; cada uno tiene sus productos, proveedores, entradas, salidas, ventas y fiados. Cada persona entra con su usuario y su rol (dueño, administrador o cajero). También tiene informes de ventas (Excel y PDF), historial de quién hizo qué, copias de seguridad, venta por QR con la cámara o con un celular y un asistente con inteligencia artificial.

La interfaz se abre en una ventana de **Microsoft Edge** (o de JavaFX si no hay Edge). Los datos se guardan en **Oracle**: las consultas y los cambios se hacen con funciones y procedimientos en PL/SQL que el programa llama con JDBC. Todo funciona sin internet excepto el asistente.

Proyecto de aula de **Programación de Computadores III (SS462)** — Universidad Popular del Cesar. Docente: Ing. Esp. Alfredo Bautista. Parte del sistema TBalance de Programación II. Versión actual: **v3.5.0**.

## Equipo

| Integrante | GitHub | Rol |
|---|---|---|
| Kevin Jhosep Mercado | [@kjhosepmercadom](https://github.com/kjhosepmercadom) | Líder e integrador: servicios, controladores, base de datos, historial y fiados |
| Reinaldo Padilla Pedroza | [@rjpadillap](https://github.com/rjpadillap) | Persistencia con archivos, copias, lector QR, usuarios y roles, asistente por rol y documentación |
| Dair José Quintero | [@djosequintero](https://github.com/djosequintero) | Modelo del negocio, interfaz, ventana de Edge y celular como lector QR |

## Documentación

| Fase | Documento |
|---|---|
| 1 | [Especificación y planificación](docs/FASE1_Especificacion.md) |
| 2 | [Arquitectura y diseño](docs/FASE2_Arquitectura_y_Diseno.md) |
| 3 | [Desarrollo e implementación](docs/FASE3_Desarrollo.md) |
| 4 | [Documentación técnica](docs/FASE4_Documentacion_Tecnica.md) |
| — | [Trazabilidad GRASP, SOLID y código limpio](docs/TRAZABILIDAD.md) |
| — | [Guía: base de datos en la nube (varios computadores)](docs/GUIA_NUBE.md) |

## Cómo ejecutar

**IntelliJ IDEA:** `File > Open`, elegir la carpeta del proyecto, esperar a que Maven descargue las dependencias y ejecutar `sistemainventario.Launcher`. SDK: **JDK 21**.

**Terminal:**

```bash
mvn clean javafx:run
```

La primera vez, TBalance pide crear la **cuenta del dueño**.

### Base de datos Oracle

Antes de la primera vez, en SQL Developer (conectado como SYSTEM) se ejecuta `src/main/resources/sql/oracle/00_crear_usuario.sql`, que crea el usuario `TBALANCE` con permiso para crear tablas, funciones y procedimientos. Al abrir TBalance aparece **Conectar con Oracle**: se escriben la dirección (por ejemplo `jdbc:oracle:thin:@localhost:1521/XEPDB1`), el usuario y la contraseña. TBalance crea solo las tablas, los disparadores, las funciones y los procedimientos. Los scripts están en `src/main/resources/sql/oracle/`. La contraseña de la base se guarda solo en `~/.inventariopro/basedatos.json`: **no se pone en el código ni se sube a GitHub**.

La base también puede estar en la nube (Oracle Autonomous Database) para que el dueño, el administrador y el cajero trabajen cada uno en su computador con los mismos datos: ver la [guía](docs/GUIA_NUBE.md).

### Asistente (opcional)

Usa la versión gratuita de Google Gemini y cada persona necesita su propia clave:

1. Entrar a https://aistudio.google.com/apikey con una cuenta de Google y crear la clave.
2. En TBalance, abrir **Pregúntale a TBalance** (arriba) o pulsar `Ctrl+K`, pegar la clave y pulsar **Guardar y probar**.

La clave se guarda solo en `~/.inventariopro/asistente.json`: **no se pone en el código ni se sube a GitHub**.

## Usuarios y roles

| Rol | Puede |
|---|---|
| Dueño | Todo: negocios, usuarios, copias de seguridad, base de datos e historial |
| Administrador | Productos, categorías, proveedores, entradas, salidas, anulaciones, informes, costos, historial; crea cajeros |
| Cajero | Vender, consultar precios y recibir abonos de fiados; anula sus propias ventas en los primeros 10 minutos |

Si el único dueño olvida su contraseña, se vuelve a la "primera vez" borrando las cuentas, sin tocar los datos del negocio:
- En SQL Developer, como TBALANCE: `DELETE FROM NEGOCIO_USUARIO; DELETE FROM USUARIO; COMMIT;`

## Estructura

```
src/main/java/sistemainventario/
├── AppContext, Main, Launcher   arranque
├── model/        clases del negocio (producto, venta, abono, usuario...)
├── exception/    errores del proyecto
├── repository/   llamar a las funciones y procedimientos de Oracle
├── ia/           conexión con el asistente (Gemini)
├── service/      reglas del negocio
├── bridge/       controladores que atienden la pantalla
├── view/         ventana de Edge y ventana de JavaFX
├── dispositivo/  cámara y celular
└── util/         utilidades (fechas, textos, QR, contraseñas)
src/main/resources/html/   interfaz (HTML, CSS y JavaScript)
src/main/resources/sql/    scripts de Oracle (tablas, funciones y procedimientos)
docs/                      documentación, diagramas e imágenes
```

## Ramas

`main` (versiones entregadas) ← `dev` (integración) ← `feature/<integrante>-<tema>` (con Pull Request aprobado por otro integrante). Detalle en la [Fase 1](docs/FASE1_Especificacion.md#53-estrategia-de-ramas).
