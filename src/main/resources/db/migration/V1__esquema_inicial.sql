-- ============================================
-- V1: Esquema inicial AppoinTech
-- ============================================

-- Tipos enumerados
CREATE TYPE rol_usuario AS ENUM ('CLIENTE', 'TECNICO', 'ADMINISTRADOR');
CREATE TYPE tipo_documento AS ENUM ('CC', 'CE', 'PASAPORTE');
CREATE TYPE estado_cita AS ENUM ('PENDIENTE', 'CONFIRMADA', 'EN_CAMINO', 'EN_PROCESO', 'FINALIZADA', 'CANCELADA');
CREATE TYPE metodo_pago AS ENUM ('EFECTIVO', 'EN_LINEA');
CREATE TYPE estado_pago AS ENUM ('PENDIENTE', 'PAGADO', 'FALLIDO');

-- ---------- USUARIOS ----------
CREATE TABLE usuario (
                         id BIGSERIAL PRIMARY KEY,
                         nombre VARCHAR(150) NOT NULL,
                         apellido VARCHAR(150) NOT NULL,
                         correo VARCHAR(150) NOT NULL UNIQUE,
                         contrasena VARCHAR(255) NOT NULL,
                         telefono VARCHAR(20),
                         tipo_documento tipo_documento,
                         numero_documento VARCHAR(20) UNIQUE,
                         rol rol_usuario NOT NULL,
                         activo BOOLEAN NOT NULL DEFAULT TRUE,
                         creado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE cliente (
                         id BIGSERIAL PRIMARY KEY,
                         usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
                         direccion VARCHAR(255),
                         latitud DOUBLE PRECISION,
                         longitud DOUBLE PRECISION
);

CREATE TABLE tecnico (
                         id BIGSERIAL PRIMARY KEY,
                         usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
                         direccion_base VARCHAR(255),
                         latitud_base DOUBLE PRECISION,
                         longitud_base DOUBLE PRECISION,
                         radio_cobertura_km INTEGER NOT NULL DEFAULT 10
);

-- ---------- ESPECIALIDADES ----------
CREATE TABLE especialidad (
                              id BIGSERIAL PRIMARY KEY,
                              nombre VARCHAR(100) NOT NULL UNIQUE,
                              descripcion VARCHAR(255)
);

CREATE TABLE tecnico_especialidad (
                                      tecnico_id BIGINT NOT NULL REFERENCES tecnico(id) ON DELETE CASCADE,
                                      especialidad_id BIGINT NOT NULL REFERENCES especialidad(id) ON DELETE CASCADE,
                                      PRIMARY KEY (tecnico_id, especialidad_id)
);

-- ---------- CITAS ----------
CREATE TABLE cita (
                      id BIGSERIAL PRIMARY KEY,
                      cliente_id BIGINT NOT NULL REFERENCES cliente(id),
                      tecnico_id BIGINT REFERENCES tecnico(id),
                      especialidad_id BIGINT NOT NULL REFERENCES especialidad(id),
                      descripcion_problema TEXT NOT NULL,
                      direccion_servicio VARCHAR(255) NOT NULL,
                      latitud DOUBLE PRECISION,
                      longitud DOUBLE PRECISION,
                      fecha_hora_inicio TIMESTAMP,
                      fecha_hora_fin TIMESTAMP,
                      estado estado_cita NOT NULL DEFAULT 'PENDIENTE',
                      diagnostico TEXT,
                      creada_en TIMESTAMP NOT NULL DEFAULT NOW(),
                      actualizada_en TIMESTAMP
);

CREATE TABLE bloque_indisponibilidad (
                                         id BIGSERIAL PRIMARY KEY,
                                         tecnico_id BIGINT NOT NULL REFERENCES tecnico(id) ON DELETE CASCADE,
                                         inicio TIMESTAMP NOT NULL,
                                         fin TIMESTAMP NOT NULL,
                                         motivo VARCHAR(255),
                                         CONSTRAINT ck_bloque_rango CHECK (fin > inicio)
);

-- ---------- PAGOS ----------
CREATE TABLE pago (
                      id BIGSERIAL PRIMARY KEY,
                      cita_id BIGINT NOT NULL UNIQUE REFERENCES cita(id) ON DELETE CASCADE,
                      monto_cotizado NUMERIC(12,2),
                      monto_final NUMERIC(12,2),
                      metodo metodo_pago,
                      estado estado_pago NOT NULL DEFAULT 'PENDIENTE',
                      referencia_externa VARCHAR(255),
                      pagado_en TIMESTAMP
);

-- ---------- CALIFICACIONES ----------
CREATE TABLE calificacion (
                              id BIGSERIAL PRIMARY KEY,
                              cita_id BIGINT NOT NULL UNIQUE REFERENCES cita(id) ON DELETE CASCADE,
                              puntuacion SMALLINT NOT NULL,
                              comentario TEXT,
                              creada_en TIMESTAMP NOT NULL DEFAULT NOW(),
                              CONSTRAINT ck_puntuacion CHECK (puntuacion BETWEEN 1 AND 5)
);

-- ---------- NOTIFICACIONES ----------
CREATE TABLE notificacion (
                              id BIGSERIAL PRIMARY KEY,
                              usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                              tipo VARCHAR(50) NOT NULL,
                              mensaje TEXT NOT NULL,
                              leida BOOLEAN NOT NULL DEFAULT FALSE,
                              creada_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- CONFIGURACIÓN (RF17) ----------
CREATE TABLE configuracion_franjas (
                                       id BIGSERIAL PRIMARY KEY,
                                       duracion_bloque_minutos INTEGER NOT NULL DEFAULT 120,
                                       radio_maximo_km INTEGER NOT NULL DEFAULT 15,
                                       hora_inicio_jornada TIME NOT NULL DEFAULT '08:00',
                                       hora_fin_jornada TIME NOT NULL DEFAULT '18:00',
                                       actualizada_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- ÍNDICES ----------
CREATE INDEX idx_cita_tecnico_fecha ON cita(tecnico_id, fecha_hora_inicio);
CREATE INDEX idx_cita_cliente ON cita(cliente_id);
CREATE INDEX idx_cita_estado ON cita(estado);
CREATE INDEX idx_bloque_tecnico_rango ON bloque_indisponibilidad(tecnico_id, inicio, fin);
CREATE INDEX idx_notificacion_usuario ON notificacion(usuario_id, leida);