-- =====================================================================
-- TBalance - PostgreSQL
-- 02. Disparadores (triggers) en PL/pgSQL
--
-- Mismos dos disparadores del script de Oracle. En PostgreSQL cada
-- disparador llama a una función que devuelve TRIGGER.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. TRG_PRODUCTO_VALIDAR (BEFORE INSERT OR UPDATE ON PRODUCTO, por fila)
--    Rechaza stock o precios negativos, deja el stock mínimo en 0 si viene
--    vacío o negativo y registra la fecha de modificación.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_PRODUCTO_VALIDAR() RETURNS TRIGGER AS $$
BEGIN
  IF NEW.STOCK < 0 THEN
    RAISE EXCEPTION 'El stock de "%" no puede quedar en %.', NEW.NOMBRE, NEW.STOCK
      USING ERRCODE = 'P0001';
  END IF;

  IF NEW.PRECIO_COMPRA < 0 OR NEW.PRECIO_VENTA < 0 THEN
    RAISE EXCEPTION 'Los precios de "%" no pueden ser negativos.', NEW.NOMBRE
      USING ERRCODE = 'P0001';
  END IF;

  IF NEW.STOCK_MIN IS NULL OR NEW.STOCK_MIN < 0 THEN
    NEW.STOCK_MIN := 0;
  END IF;

  NEW.FECHA_MODIFICACION := CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS TRG_PRODUCTO_VALIDAR ON PRODUCTO;
CREATE TRIGGER TRG_PRODUCTO_VALIDAR
BEFORE INSERT OR UPDATE ON PRODUCTO
FOR EACH ROW EXECUTE FUNCTION FN_PRODUCTO_VALIDAR();

-- ---------------------------------------------------------------------
-- 2. TRG_PRODUCTO_HISTORIAL_PRECIO
--    (AFTER UPDATE OF PRECIO_COMPRA, PRECIO_VENTA ON PRODUCTO, por fila)
--    Guarda el precio anterior y el nuevo en HISTORIAL_PRECIO cuando
--    alguno de los dos cambia.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_PRODUCTO_HISTORIAL_PRECIO() RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO HISTORIAL_PRECIO (
    PERFIL_ID, PRODUCTO_ID,
    PRECIO_COMPRA_ANTERIOR, PRECIO_COMPRA_NUEVO,
    PRECIO_VENTA_ANTERIOR, PRECIO_VENTA_NUEVO,
    FECHA_CAMBIO, USUARIO_BD)
  VALUES (
    OLD.PERFIL_ID, OLD.ID,
    OLD.PRECIO_COMPRA, NEW.PRECIO_COMPRA,
    OLD.PRECIO_VENTA, NEW.PRECIO_VENTA,
    CURRENT_TIMESTAMP, CURRENT_USER);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS TRG_PRODUCTO_HISTORIAL_PRECIO ON PRODUCTO;
CREATE TRIGGER TRG_PRODUCTO_HISTORIAL_PRECIO
AFTER UPDATE OF PRECIO_COMPRA, PRECIO_VENTA ON PRODUCTO
FOR EACH ROW
WHEN (OLD.PRECIO_COMPRA <> NEW.PRECIO_COMPRA OR OLD.PRECIO_VENTA <> NEW.PRECIO_VENTA)
EXECUTE FUNCTION FN_PRODUCTO_HISTORIAL_PRECIO();
