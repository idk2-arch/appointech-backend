BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
WITH elegibles AS (
 SELECT u.id AS usuario_id,u.correo,u.rol,c.id AS cliente_id,
 row_number() OVER(ORDER BY substring(u.correo FROM '^cliente([0-9]+)')::int) AS ordinal
 FROM usuario u JOIN cliente c ON c.usuario_id=u.id
 WHERE u.activo AND u.rol='CLIENTE' AND u.correo ~ '^cliente[0-9]+@perf.test$'
 AND EXISTS(SELECT 1 FROM cita x WHERE x.cliente_id=c.id)
 AND EXISTS(SELECT 1 FROM notificacion n WHERE n.usuario_id=u.id)
)
SELECT json_build_object('ordinal',e.ordinal,'usuarioId',e.usuario_id,'clienteId',e.cliente_id,
 'correo',e.correo,'rol',e.rol,
 'totalNotificaciones',(SELECT count(*) FROM notificacion WHERE usuario_id=e.usuario_id),
 'notificaciones',(SELECT json_agg(row_to_json(n) ORDER BY n."creadaEn" DESC,n.id DESC) FROM (
 SELECT id,tipo,titulo,mensaje,cita_id AS "citaId",leida,creada_en AS "creadaEn"
 FROM notificacion WHERE usuario_id=e.usuario_id ORDER BY creada_en DESC,id DESC LIMIT 20)n),
 'citas',(SELECT json_agg(row_to_json(x) ORDER BY orden_fecha DESC,id DESC) FROM (
 SELECT x.id,x.tecnico_id AS "tecnicoId",x.especialidad_id AS "especialidadId",
 x.descripcion_problema AS "descripcionProblema",x.direccion_servicio AS "direccionServicio",
 x.fecha_hora_inicio AS "fechaHoraInicio",x.fecha_hora_fin AS "fechaHoraFin",x.estado,
 ut.nombre AS "tecnicoNombre",ut.apellido AS "tecnicoApellido",ut.telefono AS "tecnicoTelefono",
 s.nombre AS "especialidadNombre",
 CASE WHEN ut.foto_clave IS NULL THEN NULL ELSE '/api/fotos/'||ut.foto_clave END AS "tecnicoFotoUrl",
 x.agendamiento_manual AS "agendamientoManual",x.motivo_solicitud_especial AS "motivoSolicitudEspecial",
 x.asignacion_manual AS "asignadaManualmente",NULL AS "asignacionManual",x.creada_en AS orden_fecha
 FROM cita x LEFT JOIN tecnico t ON t.id=x.tecnico_id LEFT JOIN usuario ut ON ut.id=t.usuario_id
 LEFT JOIN especialidad s ON s.id=x.especialidad_id
 WHERE x.cliente_id=e.cliente_id ORDER BY x.creada_en DESC,x.id DESC)x))
FROM elegibles e WHERE ordinal<=__N__ ORDER BY ordinal;
ROLLBACK;
