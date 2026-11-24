-- =============================================================================
-- TBalance - Funciones en Oracle (PL/SQL)
-- =============================================================================
-- Cálculos que se repiten y que conviene hacer dentro de la base: así Java no
-- tiene que traer todas las filas a memoria para sumar.
--
--   FN_CLAVE_CLIENTE     cómo se identifica a un cliente de fiado
--   FN_SALDO_CLIENTE     cuánto debe todavía un cliente
--   FN_VALOR_INVENTARIO  cuánto costó todo lo que hay en inventario
--   FN_UTILIDAD_PERIODO  cuánto se ganó con las ventas de unas fechas
--
-- TBalance las crea solo al conectarse (cambio v8). También se pueden ejecutar
-- con F5 en SQL Developer, conectado como TBALANCE. El usuario necesita el
-- permiso CREATE PROCEDURE (ver 00_crear_usuario.sql).
-- =============================================================================

-- Un cliente de fiado se reconoce por su cédula. Si una venta vieja no tiene
-- cédula, se usa "N:" + el nombre en minúsculas, sin tildes y sin espacios de
-- más (igual que CreditService.claveCliente en Java).
CREATE OR REPLACE FUNCTION FN_CLAVE_CLIENTE (
  p_documento IN VARCHAR2,
  p_nombre    IN VARCHAR2
) RETURN VARCHAR2 DETERMINISTIC IS
  v_nombre VARCHAR2(400 CHAR);
BEGIN
  IF TRIM(p_documento) IS NOT NULL THEN
    RETURN p_documento;
  END IF;
  v_nombre := TRANSLATE(LOWER(p_nombre),
                        'áàäâãéèëêíìïîóòöôõúùüûñç',
                        'aaaaaeeeeiiiiooooouuuunc');
  v_nombre := REGEXP_REPLACE(v_nombre, '^\s+|\s+$', '');
  v_nombre := REGEXP_REPLACE(v_nombre, '\s+', ' ');
  RETURN 'N:' || v_nombre;
END;
/

-- Lo que debe un cliente: ventas fiadas vigentes menos abonos vigentes.
-- Nunca devuelve menos de 0.
CREATE OR REPLACE FUNCTION FN_SALDO_CLIENTE (
  p_perfil IN VARCHAR2,
  p_clave  IN VARCHAR2
) RETURN NUMBER IS
  v_fiado   NUMBER := 0;
  v_abonado NUMBER := 0;
BEGIN
  SELECT NVL(SUM(TOTAL), 0)
    INTO v_fiado
    FROM VENTA
   WHERE PERFIL_ID = p_perfil
     AND METODO_PAGO = 'Fiado'
     AND NVL(ESTADO, 'COMPLETADA') <> 'ANULADA'
     AND FN_CLAVE_CLIENTE(CLIENTE_DOCUMENTO, CLIENTE) = p_clave;

  SELECT NVL(SUM(MONTO), 0)
    INTO v_abonado
    FROM ABONO
   WHERE PERFIL_ID = p_perfil
     AND CLIENTE_DOCUMENTO = p_clave
     AND ESTADO <> 'ANULADA';

  RETURN GREATEST(0, ROUND(v_fiado - v_abonado, 2));
END;
/

-- Cuánto costó lo que hay en inventario: stock x precio de compra.
CREATE OR REPLACE FUNCTION FN_VALOR_INVENTARIO (
  p_perfil IN VARCHAR2
) RETURN NUMBER IS
  v_valor NUMBER := 0;
BEGIN
  SELECT NVL(SUM(STOCK * PRECIO_COMPRA), 0)
    INTO v_valor
    FROM PRODUCTO
   WHERE PERFIL_ID = p_perfil;
  RETURN v_valor;
END;
/

-- Ganancia de las ventas completadas entre dos días (los dos incluidos):
-- (precio de venta - costo) x cantidad. El costo es el que quedó guardado en
-- la venta; si la venta es vieja y no lo tiene, el precio de compra actual del
-- producto. Las unidades sin ningún costo no se cuentan.
CREATE OR REPLACE FUNCTION FN_UTILIDAD_PERIODO (
  p_perfil IN VARCHAR2,
  p_desde  IN DATE,
  p_hasta  IN DATE
) RETURN NUMBER IS
  v_utilidad NUMBER := 0;
BEGIN
  SELECT NVL(SUM((x.PRECIO_UNITARIO - x.COSTO) * x.CANTIDAD), 0)
    INTO v_utilidad
    FROM (SELECT d.PRECIO_UNITARIO, d.CANTIDAD,
                 CASE WHEN d.COSTO_UNITARIO > 0 THEN d.COSTO_UNITARIO
                      WHEN p.PRECIO_COMPRA > 0 THEN p.PRECIO_COMPRA
                      ELSE 0 END AS COSTO
            FROM VENTA v
            JOIN DETALLE_VENTA d ON d.PERFIL_ID = v.PERFIL_ID AND d.VENTA_ID = v.ID
            LEFT JOIN PRODUCTO p ON p.PERFIL_ID = d.PERFIL_ID AND p.ID = d.PRODUCTO_ID
           WHERE v.PERFIL_ID = p_perfil
             AND v.ESTADO = 'COMPLETADA'
             AND v.FECHA IS NOT NULL
             AND TRUNC(v.FECHA) BETWEEN TRUNC(p_desde) AND TRUNC(p_hasta)) x
   WHERE x.COSTO > 0;
  RETURN v_utilidad;
END;
/
