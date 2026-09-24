-- Los clientes que entran con Google no tienen contraseña, teléfono ni documento
ALTER TABLE usuario ALTER COLUMN contrasena DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN telefono DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN tipo_documento DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN numero_documento DROP NOT NULL;