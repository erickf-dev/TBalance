-- =====================================================================
-- TBalance - Oracle
-- 06. Probar las funciones y los procedimientos (opcional)
--
-- Ejecutar como TBALANCE con F5 en SQL Developer, DESPUÉS de que TBalance
-- se haya conectado una vez (así ya existen las funciones y los
-- procedimientos). Crea un negocio de prueba con los procedimientos para
-- guardar, lee sus productos con una función de consulta, vende, abona y
-- anula, muestra los resultados y al final deshace todo con ROLLBACK.
-- =====================================================================

SET SERVEROUTPUT ON

-- 0. Crear el negocio y dos productos con los procedimientos para guardar
--    (los mismos que usa TBalance; P_NUEVO = 1 agrega, 0 cambia)
BEGIN
  SP_GUARDAR_PERFIL(P_NUEVO => 1, P_ID => 'PRUEBA', P_NOMBRE => 'Negocio de prueba', P_DESCRIPCION => NULL,
                    P_COLOR => NULL, P_TIPO_NEGOCIO => 'TIENDA', P_FECHA_CREACION => SYSDATE, P_ACTIVO => 1);
  SP_GUARDAR_PRODUCTO(P_NUEVO => 1, P_PERFIL_ID => 'PRUEBA', P_ID => '1', P_NOMBRE => 'Arroz 500 g',
                      P_CODIGO => NULL, P_DESCRIPCION => NULL, P_CATEGORIA => NULL, P_CATEGORIA_ID => NULL,
                      P_PROVEEDOR_ID => NULL, P_PRECIO_COMPRA => 2500, P_PRECIO_VENTA => 3500, P_STOCK => 10,
                      P_STOCK_MIN => 2, P_TIPO => NULL, P_REQUIERE_VENCIMIENTO => 0, P_FECHA_VENCIMIENTO => NULL,
                      P_ACTIVO => 1);
  SP_GUARDAR_PRODUCTO(P_NUEVO => 1, P_PERFIL_ID => 'PRUEBA', P_ID => '2', P_NOMBRE => 'Aceite 1 L',
                      P_CODIGO => NULL, P_DESCRIPCION => NULL, P_CATEGORIA => NULL, P_CATEGORIA_ID => NULL,
                      P_PROVEEDOR_ID => NULL, P_PRECIO_COMPRA => 9000, P_PRECIO_VENTA => 12000, P_STOCK => 5,
                      P_STOCK_MIN => 1, P_TIPO => NULL, P_REQUIERE_VENCIMIENTO => 0, P_FECHA_VENCIMIENTO => NULL,
                      P_ACTIVO => 1);
END;
/

-- Leer los productos con la función de consulta (devuelve un cursor)
DECLARE
  c SYS_REFCURSOR;
  p PRODUCTO%ROWTYPE;
BEGIN
  c := FN_LISTAR_PRODUCTOS('PRUEBA');
  LOOP
    FETCH c INTO p;
    EXIT WHEN c%NOTFOUND;
    DBMS_OUTPUT.PUT_LINE('Producto ' || p.ID || ': ' || p.NOMBRE || ', stock ' || p.STOCK);
  END LOOP;
  CLOSE c;
END;
/

-- 1. Valor del inventario: 10 x 2500 + 5 x 9000 = 70000
SELECT FN_VALOR_INVENTARIO('PRUEBA') AS VALOR_INVENTARIO FROM DUAL;

-- 2. Venta fiada de 2 arroces y 1 aceite (total 19000)
DECLARE
  v_total NUMBER;
BEGIN
  SP_REGISTRAR_VENTA('PRUEBA', 'VTA-PRUEBA-1', 'Ana Pérez', '1065123456', 'Fiado', NULL, NULL,
                     SYSTIMESTAMP, NULL, 'Prueba', SYSTIMESTAMP,
                     '[{"productoId":"1","nombreProducto":"Arroz 500 g","cantidad":2,"precioUnitario":3500},'
                     || '{"productoId":"2","nombreProducto":"Aceite 1 L","cantidad":1,"precioUnitario":12000}]',
                     v_total);
  DBMS_OUTPUT.PUT_LINE('Venta registrada. Total: ' || v_total);
END;
/

-- Stock después de vender: arroz 8, aceite 4
SELECT ID, NOMBRE, STOCK FROM PRODUCTO WHERE PERFIL_ID = 'PRUEBA' ORDER BY ID;

-- 3. Saldo del cliente: 19000
SELECT FN_SALDO_CLIENTE('PRUEBA', '1065123456') AS DEBE FROM DUAL;

-- 4. Ganancia de hoy: 2 x (3500-2500) + 1 x (12000-9000) = 5000
SELECT FN_UTILIDAD_PERIODO('PRUEBA', TRUNC(SYSDATE), TRUNC(SYSDATE)) AS UTILIDAD FROM DUAL;

-- 5. Abono de 5000 (queda debiendo 14000)
DECLARE
  v_cliente VARCHAR2(150 CHAR);
BEGIN
  SP_REGISTRAR_ABONO('PRUEBA', 'ABO-PRUEBA-1', '1065123456', 5000, 'Efectivo', NULL, NULL,
                     SYSTIMESTAMP, NULL, 'Prueba', v_cliente);
  DBMS_OUTPUT.PUT_LINE('Abono de ' || v_cliente || '. Ahora debe: '
                       || FN_SALDO_CLIENTE('PRUEBA', '1065123456'));
END;
/

-- 6. Un abono que pasa de la deuda: debe salir el mensaje "El abono pasa de lo que debe..."
BEGIN
  DECLARE
    v_cliente VARCHAR2(150 CHAR);
  BEGIN
    SP_REGISTRAR_ABONO('PRUEBA', 'ABO-PRUEBA-2', '1065123456', 50000, 'Efectivo', NULL, NULL,
                       SYSTIMESTAMP, NULL, 'Prueba', v_cliente);
  END;
EXCEPTION
  WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Rechazado como se esperaba: ' || SQLERRM);
END;
/

-- 7. Vender más de lo que hay: debe salir "Stock insuficiente para Aceite 1 L. Disponible: 4"
DECLARE
  v_total NUMBER;
BEGIN
  SP_REGISTRAR_VENTA('PRUEBA', 'VTA-PRUEBA-2', NULL, NULL, 'Efectivo', NULL, NULL,
                     SYSTIMESTAMP, NULL, 'Prueba', SYSTIMESTAMP,
                     '[{"productoId":"2","nombreProducto":"Aceite 1 L","cantidad":9,"precioUnitario":12000}]',
                     v_total);
EXCEPTION
  WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Rechazado como se esperaba: ' || SQLERRM);
END;
/

-- 8. Anular la venta: el stock vuelve a 10 y 5 y queda en AUDITORIA
BEGIN
  SP_ANULAR_VENTA('PRUEBA', 'VTA-PRUEBA-1', 'Prueba de anulación', SYSTIMESTAMP,
                  'H-PRUEBA-1', NULL, 'Prueba', 'Dueño', 'Negocio de prueba');
END;
/

SELECT ID, NOMBRE, STOCK FROM PRODUCTO WHERE PERFIL_ID = 'PRUEBA' ORDER BY ID;
SELECT ID, ESTADO, MOTIVO_ANULACION FROM VENTA WHERE PERFIL_ID = 'PRUEBA';
SELECT ACCION, DETALLE FROM AUDITORIA WHERE ID = 'H-PRUEBA-1';

-- Deshacer todo lo de la prueba
ROLLBACK;
