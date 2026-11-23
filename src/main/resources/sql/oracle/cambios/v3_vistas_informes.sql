-- =====================================================================
-- TBalance - Oracle 18c Express Edition
-- Cambio v3: vistas para consultar los informes desde SQL Developer.
-- Las ventas, entradas y salidas anuladas no se cuentan.
-- =====================================================================

-- Ventas por día de cada negocio
CREATE OR REPLACE VIEW V_VENTAS_POR_DIA AS
SELECT V.PERFIL_ID,
       TO_CHAR(V.FECHA, 'YYYY-MM-DD') AS DIA,
       COUNT(*)                       AS VENTAS,
       SUM(V.TOTAL)                   AS TOTAL
FROM VENTA V
WHERE NVL(V.ESTADO, 'COMPLETADA') <> 'ANULADA'
GROUP BY V.PERFIL_ID, TO_CHAR(V.FECHA, 'YYYY-MM-DD');

-- Unidades vendidas, ingresos, costo y utilidad por producto
CREATE OR REPLACE VIEW V_UTILIDAD_POR_PRODUCTO AS
SELECT D.PERFIL_ID,
       D.PRODUCTO_ID,
       MAX(D.NOMBRE_PRODUCTO)                                   AS PRODUCTO,
       SUM(D.CANTIDAD)                                          AS UNIDADES,
       SUM(D.SUBTOTAL)                                          AS VENDIDO,
       SUM(D.CANTIDAD * D.COSTO_UNITARIO)                       AS COSTO,
       SUM(D.SUBTOTAL) - SUM(D.CANTIDAD * D.COSTO_UNITARIO)     AS UTILIDAD
FROM DETALLE_VENTA D
JOIN VENTA V ON V.PERFIL_ID = D.PERFIL_ID AND V.ID = D.VENTA_ID
WHERE NVL(V.ESTADO, 'COMPLETADA') <> 'ANULADA'
GROUP BY D.PERFIL_ID, D.PRODUCTO_ID;

-- Cuánto costó lo que hay en inventario y cuánto vale a precio de venta
CREATE OR REPLACE VIEW V_VALOR_INVENTARIO AS
SELECT P.PERFIL_ID,
       COUNT(*)                          AS PRODUCTOS,
       SUM(P.STOCK)                      AS UNIDADES,
       SUM(P.STOCK * P.PRECIO_COMPRA)    AS COSTO_INVENTARIO,
       SUM(P.STOCK * P.PRECIO_VENTA)     AS VALOR_A_PRECIO_VENTA
FROM PRODUCTO P
WHERE P.ACTIVO = 1
GROUP BY P.PERFIL_ID;

-- Compras (entradas con costo) por mes
CREATE OR REPLACE VIEW V_COMPRAS_POR_MES AS
SELECT E.PERFIL_ID,
       TO_CHAR(E.FECHA, 'YYYY-MM')               AS MES,
       COUNT(*)                                  AS ENTRADAS,
       SUM(E.CANTIDAD * E.PRECIO_UNITARIO)       AS TOTAL
FROM ENTRADA E
WHERE E.ESTADO <> 'ANULADA' AND E.PRECIO_UNITARIO > 0
GROUP BY E.PERFIL_ID, TO_CHAR(E.FECHA, 'YYYY-MM');
