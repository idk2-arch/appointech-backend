-- Foto de perfil: solo se guarda la clave (UUID) del objeto en el almacenamiento; la imagen vive en el bucket.
ALTER TABLE usuario ADD COLUMN foto_clave VARCHAR(64);
