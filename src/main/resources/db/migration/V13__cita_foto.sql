-- Fotos del electrodomestico que el cliente adjunta a su cita. La imagen vive en el bucket; aqui solo su clave.
CREATE TABLE cita_foto (
                           id BIGSERIAL PRIMARY KEY,
                           cita_id BIGINT NOT NULL REFERENCES cita(id) ON DELETE CASCADE,
                           clave VARCHAR(150) NOT NULL,
                           creado_en TIMESTAMP NOT NULL
);

CREATE INDEX idx_cita_foto_cita ON cita_foto(cita_id);
