# TBalance — Sistema de Inventario

Programa de escritorio en **Java** para llevar el inventario y las ventas de negocios pequeños. Una persona puede manejar varios negocios; cada uno tiene sus productos, proveedores, entradas, salidas, ventas y fiados. Cada persona entra con su usuario y su rol (dueño, administrador o cajero). También tiene informes de ventas (Excel y PDF), historial de quién hizo qué, copias de seguridad, venta por QR con la cámara o con un celular y un asistente con inteligencia artificial.

La interfaz se abre en una ventana de **Microsoft Edge** (o de JavaFX si no hay Edge). Los datos se guardan en **PostgreSQL**, **Oracle** o archivos **JSON**, según se elija en el programa. Todo funciona sin internet excepto el asistente.

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

### Base de datos (opcional)

En **Copias de seguridad → Dónde se guardan los datos** se elige PostgreSQL u Oracle, se escriben la dirección, el usuario y la contraseña, y se pulsa **Guardar y usar**. TBalance crea las tablas y copia los datos que ya había. Los scripts están en `src/main/resources/sql/`. La contraseña de la base se guarda solo en `~/.inventariopro/basedatos.json`: **no se pone en el código ni se sube a GitHub**.

La base también puede estar en la nube (Oracle Cloud o Neon) para que el dueño, el administrador y el cajero trabajen cada uno en su computador con los mismos datos: ver la [guía](docs/GUIA_NUBE.md).

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
- Con archivos: borrar `~/.inventariopro/data/usuarios.json`.
- Con PostgreSQL u Oracle: `DELETE FROM NEGOCIO_USUARIO; DELETE FROM USUARIO; COMMIT;`

## Estructura

```
src/main/java/sistemainventario/
├── AppContext, Main, Launcher   arranque
├── model/        clases del negocio (producto, venta, abono, usuario...)
├── exception/    errores del proyecto
├── repository/   guardar y leer datos (archivos JSON, PostgreSQL y Oracle)
├── ia/           conexión con el asistente (Gemini)
├── service/      reglas del negocio
├── bridge/       controladores que atienden la pantalla
├── view/         ventana de Edge y ventana de JavaFX
├── dispositivo/  cámara y celular
└── util/         utilidades (fechas, textos, QR, contraseñas)
src/main/resources/html/   interfaz (HTML, CSS y JavaScript)
src/main/resources/sql/    scripts de PostgreSQL y Oracle
docs/                      documentación, diagramas e imágenes
```

## Ramas

`main` (versiones entregadas) ← `dev` (integración) ← `feature/<integrante>-<tema>` (con Pull Request aprobado por otro integrante). Detalle en la [Fase 1](docs/FASE1_Especificacion.md#53-estrategia-de-ramas).
