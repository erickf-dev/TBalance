-- =====================================================================
-- TBalance - Oracle 18c Express Edition
-- 02. Disparadores (triggers) en PL/SQL
--
-- Ejecutar conectado como TBALANCE después de 01_tablas.sql
-- (F5 "Ejecutar script"). Cada bloque PL/SQL termina con una barra "/".
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. TRG_PRODUCTO_VALIDAR
--    BEFORE INSERT OR UPDATE ON PRODUCTO, FOR EACH ROW
--    Antes de guardar un producto revisa que el stock y los precios no
--    sean negativos y deja el stock mínimo en 0 si viene vacío o negativo.
--    Si algo está mal, cancela la operación con un mensaje claro
--    (TBalance lo muestra tal cual). Además registra cuándo cambió.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PRODUCTO_VALIDAR
BEFORE INSERT OR UPDATE ON PRODUCTO
FOR EACH ROW
BEGIN
  IF :NEW.STOCK < 0 THEN
    RAISE_APPLICATION_ERROR(-20001,
      'El stock de "' || :NEW.NOMBRE || '" no puede quedar en ' || :NEW.STOCK || '.');
  END IF;

  IF :NEW.PRECIO_COMPRA < 0 OR :NEW.PRECIO_VENTA < 0 THEN
    RAISE_APPLICATION_ERROR(-20002,
      'Los precios de "' || :NEW.NOMBRE || '" no pueden ser negativos.');
  END IF;

  IF :NEW.STOCK_MIN IS NULL OR :NEW.STOCK_MIN < 0 THEN
    :NEW.STOCK_MIN := 0;
  END IF;

  :NEW.FECHA_MODIFICACION := SYSTIMESTAMP;
END;
/

-- ---------------------------------------------------------------------
-- 2. TRG_PRODUCTO_HISTORIAL_PRECIO
--    AFTER UPDATE OF PRECIO_COMPRA, PRECIO_VENTA ON PRODUCTO, FOR EACH ROW
--    Cuando cambia el precio de compra o de venta de un producto, guarda
--    el precio anterior y el nuevo en HISTORIAL_PRECIO, con la fecha y el
--    usuario de la base. Sirve para saber cuándo subió un costo y por qué
--    cambió la ganancia. La cláusula WHEN evita filas cuando el precio se
--    reescribe con el mismo valor.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PRODUCTO_HISTORIAL_PRECIO
AFTER UPDATE OF PRECIO_COMPRA, PRECIO_VENTA ON PRODUCTO
FOR EACH ROW
WHEN (OLD.PRECIO_COMPRA <> NEW.PRECIO_COMPRA OR OLD.PRECIO_VENTA <> NEW.PRECIO_VENTA)
BEGIN
  INSERT INTO HISTORIAL_PRECIO (
    PERFIL_ID, PRODUCTO_ID,
    PRECIO_COMPRA_ANTERIOR, PRECIO_COMPRA_NUEVO,
    PRECIO_VENTA_ANTERIOR, PRECIO_VENTA_NUEVO,
    FECHA_CAMBIO, USUARIO_BD)
  VALUES (
    :OLD.PERFIL_ID, :OLD.ID,
    :OLD.PRECIO_COMPRA, :NEW.PRECIO_COMPRA,
    :OLD.PRECIO_VENTA, :NEW.PRECIO_VENTA,
    SYSTIMESTAMP, USER);
END;
/
