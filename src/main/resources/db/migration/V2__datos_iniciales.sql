INSERT INTO especialidad (nombre, descripcion) VALUES
('Lavadoras', 'Reparación y mantenimiento de lavadoras'),
('Neveras', 'Refrigeradores y congeladores'),
('Estufas', 'Estufas de gas y eléctricas'),
('Secadoras', 'Reparación de secadoras');

INSERT INTO configuracion_franjas
(duracion_bloque_minutos, radio_maximo_km, hora_inicio_jornada, hora_fin_jornada)
VALUES (120, 15, '08:00', '18:00');