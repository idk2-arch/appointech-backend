-- ============================================================================
-- DATOS DE CARGA SOLO PARA EL PERFIL "perf" (carpeta db/perf, fuera de db/migration)
-- Migracion repetible: Flyway la aplica despues de todas las versionadas (V1..Vn), sin depender de su numeracion.
-- Contrasena de TODOS los usuarios de prueba: Perf12345  (hash bcrypt de costo 10, igual que BCryptPasswordEncoder)
--   5000 clientes  : cliente1@perf.test .. cliente5000@perf.test
--     50 tecnicos  : tecnico1@perf.test .. tecnico50@perf.test (2 especialidades c/u, radios de 5 a 30 km)
--   Franjas de los proximos 14 dias, 340 citas, pagos, calificaciones, 40 solicitudes manuales y notificaciones.
-- Idempotente: si ya existen los datos no duplica nada.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------- USUARIOS Y CLIENTES ----------
-- Un solo hash para todos: bcrypt a costo 10 en 5000 filas tardaria minutos; el costo de login en la prueba es el real.
CREATE TEMP TABLE perf_hash AS SELECT crypt('Perf12345', gen_salt('bf', 10)) AS valor;

INSERT INTO usuario (nombre, apellido, correo, contrasena, telefono, tipo_documento, numero_documento, rol, activo)
SELECT 'Cliente', 'Perf' || n, 'cliente' || n || '@perf.test', (SELECT valor FROM perf_hash),
       '31' || lpad(n::text, 8, '0'), 'CC', 'PC' || lpad(n::text, 8, '0'), 'CLIENTE', TRUE
FROM generate_series(1, 5000) AS n
ON CONFLICT (correo) DO NOTHING;

INSERT INTO cliente (usuario_id, direccion, latitud, longitud)
SELECT u.id, 'Calle ' || n || ' # 1-23 Perf',
       4.3365 + ((n * 37) % 200 - 100) / 1000.0,
       -74.3638 + ((n * 53) % 200 - 100) / 1000.0
FROM generate_series(1, 5000) AS n
JOIN usuario u ON u.correo = 'cliente' || n || '@perf.test'
ON CONFLICT (usuario_id) DO NOTHING;

-- ---------- TECNICOS ----------
INSERT INTO usuario (nombre, apellido, correo, contrasena, telefono, tipo_documento, numero_documento, rol, activo)
SELECT 'Tecnico', 'Perf' || n, 'tecnico' || n || '@perf.test', (SELECT valor FROM perf_hash),
       '32' || lpad(n::text, 8, '0'), 'CC', 'PT' || lpad(n::text, 8, '0'), 'TECNICO', TRUE
FROM generate_series(1, 50) AS n
ON CONFLICT (correo) DO NOTHING;

-- Radios distintos (5..30 km) y bases dispersas alrededor del centro de referencia (+-0.08 grados)
INSERT INTO tecnico (usuario_id, direccion_base, latitud_base, longitud_base, radio_cobertura_km)
SELECT u.id, 'Base tecnico ' || n,
       4.3365 + ((n * 17) % 160 - 80) / 1000.0,
       -74.3638 + ((n * 29) % 160 - 80) / 1000.0,
       (ARRAY[5, 8, 10, 12, 15, 20, 25, 30])[1 + n % 8]
FROM generate_series(1, 50) AS n
JOIN usuario u ON u.correo = 'tecnico' || n || '@perf.test'
ON CONFLICT (usuario_id) DO NOTHING;

-- Tecnicos numerados por el numero del correo (tecnico7@perf.test -> 7), sin suponer ids concretos
CREATE TEMP TABLE perf_tecnico AS
SELECT substring(u.correo FROM 8 FOR length(u.correo) - 17)::int AS n, t.id AS tecnico_id
FROM tecnico t JOIN usuario u ON u.id = t.usuario_id
WHERE u.correo LIKE 'tecnico%@perf.test';

CREATE TEMP TABLE perf_especialidad AS
SELECT row_number() OVER (ORDER BY id)::int AS e, id AS especialidad_id
FROM especialidad WHERE activo;

-- Cada tecnico atiende 2 de las especialidades activas, rotando
INSERT INTO tecnico_especialidad (tecnico_id, especialidad_id)
SELECT pt.tecnico_id, pe.especialidad_id
FROM perf_tecnico pt
JOIN perf_especialidad pe ON (pt.n + pe.e) % (SELECT count(*) FROM perf_especialidad) < 2
ON CONFLICT DO NOTHING;

-- ---------- FRANJAS DE TRABAJO: hoy y los proximos 13 dias ----------
-- Manana 08-12 para todos; tarde 14-18 salvo los tecnicos multiplos de 5 (solo manana)
INSERT INTO franja_tecnico (tecnico_id, fecha, hora_inicio, hora_fin)
SELECT pt.tecnico_id, CURRENT_DATE + d, TIME '08:00', TIME '12:00'
FROM perf_tecnico pt CROSS JOIN generate_series(0, 13) AS d
WHERE NOT EXISTS (SELECT 1 FROM franja_tecnico f WHERE f.tecnico_id = pt.tecnico_id);

INSERT INTO franja_tecnico (tecnico_id, fecha, hora_inicio, hora_fin)
SELECT pt.tecnico_id, CURRENT_DATE + d, TIME '14:00', TIME '18:00'
FROM perf_tecnico pt CROSS JOIN generate_series(0, 13) AS d
WHERE pt.n % 5 <> 0
  AND NOT EXISTS (SELECT 1 FROM franja_tecnico f WHERE f.tecnico_id = pt.tecnico_id AND f.hora_inicio = TIME '14:00');

-- ---------- CITAS ----------
CREATE TEMP TABLE perf_cliente AS
SELECT c.id AS cliente_id, substring(u.correo FROM 8 FOR length(u.correo) - 17)::int AS n,
       u.telefono, c.direccion, c.latitud, c.longitud
FROM cliente c JOIN usuario u ON u.id = c.usuario_id
WHERE u.correo LIKE 'cliente%@perf.test';

CREATE TEMP TABLE perf_cita_semilla (
    clave INT PRIMARY KEY, cliente_n INT, tecnico_n INT, estado estado_cita, inicio TIMESTAMP, manual BOOLEAN
);

-- 150 finalizadas y 50 canceladas en los ultimos 30 dias; 100 confirmadas a futuro; 40 solicitudes manuales
INSERT INTO perf_cita_semilla
SELECT i, i, 1 + i % 50, 'FINALIZADA', (CURRENT_DATE - (1 + i % 30)) + TIME '08:00' + (i % 4) * INTERVAL '2 hours', FALSE
FROM generate_series(1, 150) AS i;
INSERT INTO perf_cita_semilla
SELECT 150 + i, 150 + i, 1 + i % 50, 'CANCELADA', (CURRENT_DATE - (1 + i % 30)) + TIME '14:00', FALSE
FROM generate_series(1, 50) AS i;
INSERT INTO perf_cita_semilla
SELECT 200 + i, 200 + i, 1 + i % 50, 'CONFIRMADA', (CURRENT_DATE + 1 + (i / 50) * 3) + TIME '08:00', FALSE
FROM generate_series(1, 100) AS i;
INSERT INTO perf_cita_semilla
SELECT 300 + i, 300 + i, NULL, 'PENDIENTE', NULL, TRUE
FROM generate_series(1, 40) AS i;

-- Solo para clientes que aun no tienen citas (idempotente)
INSERT INTO cita (cliente_id, tecnico_id, especialidad_id, descripcion_problema, direccion_servicio, latitud, longitud,
                  fecha_hora_inicio, fecha_hora_fin, estado, diagnostico, creada_en, actualizada_en,
                  agendamiento_manual, telefono_contacto, motivo_solicitud_especial, fecha_deseada)
SELECT pc.cliente_id,
       pt.tecnico_id,
       -- la especialidad del tecnico (si hay tecnico) o una cualquiera rotando
       CASE WHEN pt.tecnico_id IS NULL
            THEN (SELECT especialidad_id FROM perf_especialidad WHERE e = 1 + s.clave % (SELECT count(*) FROM perf_especialidad))
            ELSE (SELECT te.especialidad_id FROM tecnico_especialidad te WHERE te.tecnico_id = pt.tecnico_id
                  ORDER BY te.especialidad_id LIMIT 1 OFFSET s.clave % 2) END,
       'Problema de prueba de carga ' || s.clave,
       pc.direccion, pc.latitud, pc.longitud,
       s.inicio,
       s.inicio + INTERVAL '2 hours',
       s.estado,
       CASE WHEN s.estado = 'FINALIZADA' THEN 'Diagnostico de prueba ' || s.clave END,
       COALESCE(s.inicio, NOW()) - INTERVAL '3 days',
       COALESCE(s.inicio, NOW()),
       s.manual,
       CASE WHEN s.manual THEN pc.telefono END,
       CASE WHEN s.manual THEN 'SIN_HORARIOS' END,
       CASE WHEN s.manual THEN CURRENT_DATE + s.clave % 14 END
FROM perf_cita_semilla s
JOIN perf_cliente pc ON pc.n = s.cliente_n
LEFT JOIN perf_tecnico pt ON pt.n = s.tecnico_n
WHERE NOT EXISTS (SELECT 1 FROM cita c WHERE c.cliente_id = pc.cliente_id);

-- Pagos: una fila por cita sembrada (cotizacion segun la especialidad); las finalizadas, pagadas en efectivo
INSERT INTO pago (cita_id, monto_cotizado, monto_final, metodo, estado, pagado_en)
SELECT c.id, COALESCE(e.costo_referencia, 80000),
       CASE WHEN c.estado = 'FINALIZADA' THEN COALESCE(e.costo_referencia, 80000) END,
       CASE WHEN c.estado = 'FINALIZADA' THEN 'EFECTIVO'::metodo_pago END,
       CASE WHEN c.estado = 'FINALIZADA' THEN 'PAGADO'::estado_pago ELSE 'PENDIENTE'::estado_pago END,
       CASE WHEN c.estado = 'FINALIZADA' THEN c.fecha_hora_fin END
FROM cita c
JOIN especialidad e ON e.id = c.especialidad_id
JOIN perf_cliente pc ON pc.cliente_id = c.cliente_id
ON CONFLICT (cita_id) DO NOTHING;

-- Calificaciones para 100 de las 150 finalizadas (las otras 50 quedan sin calificar)
INSERT INTO calificacion (cita_id, puntuacion, comentario, creada_en)
SELECT c.id, 1 + (c.id % 5)::int, 'Comentario de prueba', c.fecha_hora_fin + INTERVAL '1 day'
FROM cita c JOIN perf_cliente pc ON pc.cliente_id = c.cliente_id
WHERE c.estado = 'FINALIZADA' AND pc.n <= 100
ON CONFLICT (cita_id) DO NOTHING;

-- ---------- NOTIFICACIONES: 3 por cliente para los primeros 1000 (parte sin leer) ----------
INSERT INTO notificacion (usuario_id, tipo, titulo, mensaje, leida, creada_en)
SELECT u.id, (ARRAY['SOLICITUD_RECIBIDA', 'CITA_CONFIRMADA', 'CITA_FINALIZADA'])[k],
       'Aviso de prueba ' || k, 'Mensaje de prueba de carga ' || k, k % 2 = 0,
       NOW() - (k * n % 40) * INTERVAL '1 hour'
FROM generate_series(1, 1000) AS n
CROSS JOIN generate_series(1, 3) AS k
JOIN usuario u ON u.correo = 'cliente' || n || '@perf.test'
WHERE NOT EXISTS (SELECT 1 FROM notificacion x WHERE x.usuario_id = u.id);

DROP TABLE perf_cita_semilla;
DROP TABLE perf_cliente;
DROP TABLE perf_especialidad;
DROP TABLE perf_tecnico;
DROP TABLE perf_hash;
