-- =============================================================================
-- TBalance - Procedimientos almacenados en Oracle (PL/SQL)
-- =============================================================================
-- Las operaciones que tocan varias tablas a la vez se hacen dentro de Oracle,
-- en un solo viaje desde Java:
--
--   SP_REGISTRAR_VENTA  revisa el stock, guarda la venta y su detalle y
--                       descuenta las unidades vendidas
--   SP_ANULAR_VENTA     anula la venta, devuelve las unidades al inventario y
--                       deja la anulación en el historial (AUDITORIA)
--   SP_REGISTRAR_ABONO  revisa que el abono no pase de la deuda y lo guarda
--
-- Transacción: los procedimientos no hacen COMMIT. TBalance los llama dentro
-- de una transacción y hace COMMIT si todo salió bien o ROLLBACK si algo falló,
-- así nunca queda una venta a medias. Si los prueban a mano en SQL Developer,
-- terminen con COMMIT o ROLLBACK (ver 06_probar_procedimientos.sql).
--
-- Errores: usan RAISE_APPLICATION_ERROR con códigos -20101 a -20199, igual que
-- los disparadores. TBalance muestra el mensaje tal cual al usuario.
--
-- Requiere 04_funciones.sql (SP_REGISTRAR_ABONO usa FN_SALDO_CLIENTE).
-- =============================================================================

-- p_items trae los productos de la venta en formato JSON, por ejemplo:
--   [{"productoId":"1001","nombreProducto":"Arroz","cantidad":2,"precioUnitario":3500}]
-- y se lee como si fuera una tabla con JSON_TABLE.
CREATE OR REPLACE PROCEDURE SP_REGISTRAR_VENTA (
  p_perfil            IN  VARCHAR2,
  p_id                IN  VARCHAR2,
  p_cliente           IN  VARCHAR2,
  p_cliente_documento IN  VARCHAR2,
  p_metodo_pago       IN  VARCHAR2,
  p_observacion       IN  VARCHAR2,
  p_referencia_pago   IN  VARCHAR2,
  p_fecha             IN  TIMESTAMP,
  p_usuario_id        IN  VARCHAR2,
  p_vendedor          IN  VARCHAR2,
  p_fecha_registro    IN  TIMESTAMP,
  p_items             IN  CLOB,
  p_total             OUT NUMBER
) IS
  v_nombre  PRODUCTO.NOMBRE%TYPE;
  v_stock   PRODUCTO.STOCK%TYPE;
  v_activo  PRODUCTO.ACTIVO%TYPE;
  v_lineas  NUMBER := 0;
  v_total   NUMBER := 0;
BEGIN
  -- 1. Revisar TODOS los productos antes de cambiar nada. FOR UPDATE bloquea
  --    la fila del producto para que otro computador no venda las mismas
  --    unidades al mismo tiempo.
  FOR r IN (SELECT j.PRODUCTO_ID, SUM(j.CANTIDAD) AS CANTIDAD, MIN(j.LINEA) AS PRIMERA
              FROM JSON_TABLE(p_items, '$[*]'
                     COLUMNS (LINEA       FOR ORDINALITY,
                              PRODUCTO_ID VARCHAR2(30 CHAR) PATH '$.productoId',
                              CANTIDAD    NUMBER            PATH '$.cantidad')) j
             GROUP BY j.PRODUCTO_ID
             ORDER BY PRIMERA) LOOP
    BEGIN
      SELECT NOMBRE, STOCK, ACTIVO
        INTO v_nombre, v_stock, v_activo
        FROM PRODUCTO
       WHERE PERFIL_ID = p_perfil AND ID = r.PRODUCTO_ID
         FOR UPDATE;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20101, 'Producto no encontrado: ' || r.PRODUCTO_ID);
    END;
    IF v_activo = 0 THEN
      RAISE_APPLICATION_ERROR(-20102, 'El producto ' || v_nombre || ' está deshabilitado');
    END IF;
    IF v_stock < r.CANTIDAD THEN
      RAISE_APPLICATION_ERROR(-20103,
        'Stock insuficiente para ' || v_nombre || '. Disponible: ' || v_stock);
    END IF;
    v_lineas := v_lineas + 1;
  END LOOP;

  IF v_lineas = 0 THEN
    RAISE_APPLICATION_ERROR(-20104, 'La venta debe tener al menos un producto');
  END IF;

  -- 2. Encabezado de la venta (el total se completa en el paso 4).
  INSERT INTO VENTA (PERFIL_ID, ID, CLIENTE, METODO_PAGO, OBSERVACION, REFERENCIA_PAGO,
                     FECHA, ESTADO, TOTAL, USUARIO_ID, VENDEDOR, FECHA_REGISTRO, CLIENTE_DOCUMENTO)
  VALUES (p_perfil, p_id, p_cliente, p_metodo_pago, p_observacion, p_referencia_pago,
          NVL(p_fecha, TRUNC(SYSDATE)), 'COMPLETADA', 0, p_usuario_id, p_vendedor,
          NVL(p_fecha_registro, SYSTIMESTAMP), p_cliente_documento);

  -- 3. Detalle: una fila por producto. El costo se toma del precio de compra
  --    actual del producto, para saber después cuánto se ganó de verdad.
  FOR i IN (SELECT j.LINEA, j.PRODUCTO_ID, j.NOMBRE_PRODUCTO, j.CANTIDAD, j.PRECIO_UNITARIO,
                   p.PRECIO_COMPRA
              FROM JSON_TABLE(p_items, '$[*]'
                     COLUMNS (LINEA           FOR ORDINALITY,
                              PRODUCTO_ID     VARCHAR2(30 CHAR)  PATH '$.productoId',
                              NOMBRE_PRODUCTO VARCHAR2(150 CHAR) PATH '$.nombreProducto',
                              CANTIDAD        NUMBER             PATH '$.cantidad',
                              PRECIO_UNITARIO NUMBER             PATH '$.precioUnitario')) j
              JOIN PRODUCTO p ON p.PERFIL_ID = p_perfil AND p.ID = j.PRODUCTO_ID
             ORDER BY j.LINEA) LOOP
    INSERT INTO DETALLE_VENTA (PERFIL_ID, VENTA_ID, LINEA, PRODUCTO_ID, NOMBRE_PRODUCTO,
                               CANTIDAD, PRECIO_UNITARIO, SUBTOTAL, COSTO_UNITARIO)
    VALUES (p_perfil, p_id, i.LINEA, i.PRODUCTO_ID, i.NOMBRE_PRODUCTO,
            i.CANTIDAD, NVL(i.PRECIO_UNITARIO, 0), i.CANTIDAD * NVL(i.PRECIO_UNITARIO, 0),
            GREATEST(0, NVL(i.PRECIO_COMPRA, 0)));
    v_total := v_total + i.CANTIDAD * NVL(i.PRECIO_UNITARIO, 0);
  END LOOP;

  -- 4. Descontar el stock (el disparador TRG_PRODUCTO_VALIDAR vuelve a
  --    revisar que no quede negativo) y guardar el total.
  FOR r IN (SELECT j.PRODUCTO_ID, SUM(j.CANTIDAD) AS CANTIDAD
              FROM JSON_TABLE(p_items, '$[*]'
                     COLUMNS (PRODUCTO_ID VARCHAR2(30 CHAR) PATH '$.productoId',
                              CANTIDAD    NUMBER            PATH '$.cantidad')) j
             GROUP BY j.PRODUCTO_ID) LOOP
    UPDATE PRODUCTO
       SET STOCK = STOCK - r.CANTIDAD
     WHERE PERFIL_ID = p_perfil AND ID = r.PRODUCTO_ID;
  END LOOP;

  UPDATE VENTA SET TOTAL = v_total WHERE PERFIL_ID = p_perfil AND ID = p_id;
  p_total := v_total;
END;
/

-- Anula una venta: queda en el historial con el motivo, las unidades vuelven
-- al inventario y se anota en AUDITORIA quién la anuló.
CREATE OR REPLACE PROCEDURE SP_ANULAR_VENTA (
  p_perfil         IN VARCHAR2,
  p_id             IN VARCHAR2,
  p_motivo         IN VARCHAR2,
  p_fecha          IN TIMESTAMP,
  p_auditoria_id   IN VARCHAR2,
  p_usuario_id     IN VARCHAR2,
  p_usuario        IN VARCHAR2,
  p_rol            IN VARCHAR2,
  p_perfil_nombre  IN VARCHAR2
) IS
  v_estado  VENTA.ESTADO%TYPE;
  v_motivo  VARCHAR2(300 CHAR);
  v_fecha   TIMESTAMP := NVL(p_fecha, SYSTIMESTAMP);
BEGIN
  BEGIN
    SELECT ESTADO INTO v_estado
      FROM VENTA
     WHERE PERFIL_ID = p_perfil AND ID = p_id
       FOR UPDATE;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20111, 'Venta no encontrada');
  END;
  IF v_estado = 'ANULADA' THEN
    RAISE_APPLICATION_ERROR(-20112, 'Ya estaba anulada');
  END IF;
  v_motivo := TRIM(p_motivo);
  IF v_motivo IS NULL THEN
    RAISE_APPLICATION_ERROR(-20113, 'Escriba el motivo de la anulación');
  END IF;

  UPDATE VENTA
     SET ESTADO = 'ANULADA',
         FECHA_ANULACION = v_fecha,
         MOTIVO_ANULACION = v_motivo,
         ANULADA_POR = p_usuario
   WHERE PERFIL_ID = p_perfil AND ID = p_id;

  -- devolver las unidades de cada producto de la venta
  FOR d IN (SELECT PRODUCTO_ID, SUM(CANTIDAD) AS CANTIDAD
              FROM DETALLE_VENTA
             WHERE PERFIL_ID = p_perfil AND VENTA_ID = p_id
             GROUP BY PRODUCTO_ID) LOOP
    UPDATE PRODUCTO
       SET STOCK = STOCK + d.CANTIDAD
     WHERE PERFIL_ID = p_perfil AND ID = d.PRODUCTO_ID;
  END LOOP;

  INSERT INTO AUDITORIA (ID, FECHA, USUARIO_ID, USUARIO, ROL, PERFIL_ID, PERFIL, ACCION, DETALLE)
  VALUES (NVL(p_auditoria_id, 'H-SP-' || p_id || '-' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3')),
          v_fecha, p_usuario_id, NVL(p_usuario, USER), p_rol, p_perfil, p_perfil_nombre,
          'Anuló una venta', SUBSTR('ID ' || p_id || '. Motivo: ' || v_motivo, 1, 480));
END;
/

-- Registra el abono de un cliente que debe fiado. p_clave es la cédula del
-- cliente (o "N:nombre" para ventas viejas sin cédula). Devuelve en
-- p_cliente_nombre el nombre del cliente.
CREATE OR REPLACE PROCEDURE SP_REGISTRAR_ABONO (
  p_perfil          IN  VARCHAR2,
  p_id              IN  VARCHAR2,
  p_clave           IN  VARCHAR2,
  p_monto           IN  NUMBER,
  p_metodo_pago     IN  VARCHAR2,
  p_referencia_pago IN  VARCHAR2,
  p_nota            IN  VARCHAR2,
  p_fecha           IN  TIMESTAMP,
  p_usuario_id      IN  VARCHAR2,
  p_registrado_por  IN  VARCHAR2,
  p_cliente_nombre  OUT VARCHAR2
) IS
  v_ventas  NUMBER;
  v_cliente VENTA.CLIENTE%TYPE;
  v_saldo   NUMBER;
BEGIN
  IF p_monto IS NULL OR p_monto <= 0 THEN
    RAISE_APPLICATION_ERROR(-20121, 'Escriba cuánto abona el cliente');
  END IF;

  SELECT COUNT(*) INTO v_ventas
    FROM VENTA
   WHERE PERFIL_ID = p_perfil
     AND METODO_PAGO = 'Fiado'
     AND NVL(ESTADO, 'COMPLETADA') <> 'ANULADA'
     AND FN_CLAVE_CLIENTE(CLIENTE_DOCUMENTO, CLIENTE) = p_clave;
  IF v_ventas = 0 THEN
    RAISE_APPLICATION_ERROR(-20122, 'Ese cliente no tiene fiados en este negocio');
  END IF;

  -- nombre del cliente: el de su primera venta fiada que tenga nombre
  SELECT MAX(CLIENTE) KEEP (DENSE_RANK FIRST ORDER BY LENGTH(ID), ID)
    INTO v_cliente
    FROM VENTA
   WHERE PERFIL_ID = p_perfil
     AND METODO_PAGO = 'Fiado'
     AND NVL(ESTADO, 'COMPLETADA') <> 'ANULADA'
     AND TRIM(CLIENTE) IS NOT NULL
     AND FN_CLAVE_CLIENTE(CLIENTE_DOCUMENTO, CLIENTE) = p_clave;

  v_saldo := FN_SALDO_CLIENTE(p_perfil, p_clave);
  IF v_saldo < 0.5 THEN
    RAISE_APPLICATION_ERROR(-20123, v_cliente || ' no debe nada');
  END IF;
  IF p_monto > v_saldo + 0.5 THEN
    RAISE_APPLICATION_ERROR(-20124, 'El abono pasa de lo que debe ' || v_cliente || '. Debe $ '
      || TO_CHAR(ROUND(v_saldo), 'FM999G999G999G990', 'NLS_NUMERIC_CHARACTERS = '',.'''));
  END IF;

  INSERT INTO ABONO (PERFIL_ID, ID, CLIENTE_DOCUMENTO, CLIENTE, MONTO, METODO_PAGO, REFERENCIA_PAGO,
                     NOTA, FECHA, USUARIO_ID, REGISTRADO_POR, ESTADO)
  VALUES (p_perfil, p_id, p_clave, v_cliente, p_monto, p_metodo_pago, p_referencia_pago,
          p_nota, NVL(p_fecha, SYSTIMESTAMP), p_usuario_id, p_registrado_por, 'COMPLETADA');

  p_cliente_nombre := v_cliente;
END;
/
