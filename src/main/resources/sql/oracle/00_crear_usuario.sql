-- =====================================================================
-- TBalance - Oracle 18c Express Edition
-- 00. Crear el usuario (esquema) de TBalance
--
-- Ejecutar UNA sola vez en SQL Developer conectado como SYSTEM
-- a la base conectable XEPDB1 (servicio XEPDB1, puerto 1521).
-- Cambie la clave 'tbalance' si lo desea; la misma clave se escribe
-- después en TBalance > Copias de seguridad > Base de datos.
-- =====================================================================

CREATE USER TBALANCE IDENTIFIED BY tbalance
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON USERS;

GRANT CREATE SESSION, CREATE TABLE, CREATE TRIGGER, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE TO TBALANCE;

-- Si el usuario TBALANCE ya existía de antes, basta con ejecutar esta línea
-- (CREATE PROCEDURE sirve para las funciones y los procedimientos):
-- GRANT CREATE PROCEDURE TO TBALANCE;
