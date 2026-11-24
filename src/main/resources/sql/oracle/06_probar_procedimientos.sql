-- =====================================================================
-- TBalance - Oracle
-- 06. Probar las funciones y los procedimientos (opcional)
--
-- Ejecutar como TBALANCE con F5 en SQL Developer, DESPUÉS de que TBalance
-- se haya conectado una vez (así ya existen las funciones y los
-- procedimientos). Crea un negocio de prueba, vende, abona y anula, muestra
-- los resultados y al final deshace todo con ROLLBACK.
-- =====================================================================

SET SERVEROUTPUT ON

INSERT INTO PERFIL (ID, NOMBRE, TIPO_NEGOCIO, FECHA_CREACION)
VALUES ('PRUEBA', 'Negocio de prueba', 'TIENDA', SYSDATE);

INSERT INTO PRODUCTO (PERFIL_ID, ID, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MIN)
VALUES ('PRUEBA', '1', 'Arroz 500 g', 2500, 3500, 10, 2);
INSERT INTO PRODUCTO (PERFIL_ID, ID, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MIN)
VALUES ('PRUEBA', '2', 'Aceite 1 L', 9000, 12000, 5, 1);

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
