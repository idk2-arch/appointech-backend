-- Día que el cliente estaba consultando cuando pidió una solicitud manual (opcional). Así el administrador sabe para
-- qué fecha quería la cita. Es NULL en las solicitudes anteriores y en las que no indicaron fecha.
ALTER TABLE cita ADD COLUMN fecha_deseada DATE;
