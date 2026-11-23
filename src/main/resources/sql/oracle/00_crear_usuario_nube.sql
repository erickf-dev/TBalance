-- =====================================================================
-- TBalance - Oracle Autonomous Database (Oracle Cloud, Always Free)
-- Crea el usuario TBALANCE dentro de la base en la nube.
--
-- Ejecútelo UNA vez en Database Actions > SQL, conectado como ADMIN.
-- Cambie la contraseña: entre 12 y 30 caracteres, con mayúscula, minúscula
-- y número, sin comillas dobles y sin la palabra "admin".
-- Después TBalance crea las tablas solo, al pulsar "Guardar y usar".
-- =====================================================================

CREATE USER TBALANCE IDENTIFIED BY "Cambie_Esta_Clave_2026";
ALTER USER TBALANCE QUOTA UNLIMITED ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE TRIGGER, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE TO TBALANCE;
