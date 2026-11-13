// Flujos de usuario compartidos por carga.js y estres.js. Cada peticion lleva la etiqueta "escenario" para poder
// medir p95 y errores por escenario (y, en el estres, por escalon).
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { config, escenarios } from './config.js';

export const sinFranjas = new Counter('franjas_vacias');
export const conflictosConfirmar = new Counter('confirmar_conflicto_esperado');
export const citasCreadas = new Counter('citas_confirmadas');

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// El estado de un usuario virtual (VU) vive entre iteraciones: inicia sesion una vez y reutiliza el token
let sesion = null;

const ESTADOS_OK = http.expectedStatuses({ min: 200, max: 399 });
// Al confirmar, un 409/400 por franja ocupada (otro VU la tomo antes) es el resultado esperado de la concurrencia
const ESTADOS_CONFIRMAR = http.expectedStatuses({ min: 200, max: 299 }, 409);

function aleatorio(minimo, maximo) {
  return minimo + Math.random() * (maximo - minimo);
}

function elegir(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}

export function pensar() {
  sleep(aleatorio(config.pensarMin, config.pensarMax));
}

// Fecha "hoy + dias" en zona horaria de Colombia (UTC-5, sin horario de verano), formato yyyy-MM-dd
function fechaEnDias(dias) {
  return new Date(Date.now() - 5 * 3600 * 1000 + dias * 86400 * 1000).toISOString().slice(0, 10);
}

function cabeceras(token, escenario, extra) {
  return {
    headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` },
    tags: { escenario },
    ...extra,
  };
}

function cuerpoJson(respuesta) {
  try {
    return respuesta.json();
  } catch (e) {
    return null;
  }
}

function numeroCliente() {
  return 1 + Math.floor(Math.random() * config.numClientes);
}

function login(correo, contrasena, escenario) {
  const respuesta = http.post(
    `${config.baseUrl}/api/auth/login`,
    JSON.stringify({ correo, contrasena }),
    { headers: JSON_HEADERS, tags: { escenario } },
  );
  const cuerpo = cuerpoJson(respuesta);
  const token = cuerpo && cuerpo.datos ? cuerpo.datos.token : null;
  check(respuesta, { 'login 200 con token': (r) => r.status === 200 && !!token });
  return token;
}

// Se ejecuta una sola vez antes de la prueba: sesion del administrador, especialidades y comprobacion del entorno
export function preparar() {
  if (!config.adminContrasena) {
    throw new Error('Falta ADMIN_CONTRASENA (clave del administrador sembrado por la migracion V4). Ver perf/README.md');
  }
  const adminToken = login(config.adminCorreo, config.adminContrasena, 'preparacion');
  if (!adminToken) {
    throw new Error('No se pudo iniciar sesion como administrador: revisa ADMIN_CONTRASENA y BASE_URL');
  }
  const especialidades = http.get(`${config.baseUrl}/api/especialidades`, { tags: { escenario: 'preparacion' } });
  const ids = (cuerpoJson(especialidades) || []).map((e) => e.id);
  if (ids.length === 0) {
    throw new Error('GET /api/especialidades no devolvio especialidades: la base perf no esta lista');
  }
  const tecnicoIds = [];
  for (let i = 1; i <= config.numTecnicos; i++) {
    tecnicoIds.push(i);
  }
  return { adminToken, especialidadIds: ids, tecnicoIds };
}

function sesionDelCliente() {
  if (sesion === null) {
    const n = numeroCliente();
    const token = login(`cliente${n}@perf.test`, config.clienteContrasena, 'login');
    sesion = { token, n };
  }
  return sesion;
}

function ubicacionAleatoria() {
  return {
    latitud: config.latCentro + aleatorio(-0.05, 0.05),
    longitud: config.lonCentro + aleatorio(-0.05, 0.05),
  };
}

function lecturaPublica() {
  const respuesta = http.get(`${config.baseUrl}/api/especialidades`, { tags: { escenario: 'lectura_publica' } });
  check(respuesta, { 'especialidades 200': (r) => r.status === 200 });
}

function loginDeCliente() {
  const n = numeroCliente();
  const token = login(`cliente${n}@perf.test`, config.clienteContrasena, 'login');
  if (token && sesion === null) {
    sesion = { token, n };
  }
}

function consultarFranjas(datos, escenario, token) {
  const ubicacion = ubicacionAleatoria();
  const fecha = fechaEnDias(Math.floor(Math.random() * config.diasConFranjas));
  const especialidadId = elegir(datos.especialidadIds);
  const url = `${config.baseUrl}/api/disponibilidad/franjas-con-motivo?especialidadId=${especialidadId}`
    + `&fecha=${fecha}&latitud=${ubicacion.latitud}&longitud=${ubicacion.longitud}`;
  const respuesta = http.get(url, cabeceras(token, escenario));
  check(respuesta, { 'franjas 200': (r) => r.status === 200 });
  const cuerpo = cuerpoJson(respuesta);
  const franjas = cuerpo && cuerpo.franjas ? cuerpo.franjas : [];
  if (franjas.length === 0) {
    sinFranjas.add(1);
  }
  return { franjas, especialidadId, ubicacion };
}

function disponibilidad(datos) {
  consultarFranjas(datos, 'disponibilidad', sesionDelCliente().token);
}

// Crear solicitud y confirmar cita: consulta franjas, crea la solicitud y confirma una franja ofrecida
function crearYConfirmar(datos) {
  const { token } = sesionDelCliente();
  const { franjas, especialidadId, ubicacion } = consultarFranjas(datos, 'crear_confirmar', token);
  if (franjas.length === 0) {
    return;
  }
  const solicitud = http.post(
    `${config.baseUrl}/api/solicitudes`,
    JSON.stringify({
      especialidadId,
      descripcionProblema: 'Prueba de carga: el equipo no funciona',
      direccionServicio: 'Direccion de prueba de carga',
      latitud: ubicacion.latitud,
      longitud: ubicacion.longitud,
    }),
    cabeceras(token, 'crear_confirmar'),
  );
  const creada = cuerpoJson(solicitud);
  if (!check(solicitud, { 'solicitud 201': (r) => r.status === 201 }) || !creada) {
    return;
  }
  const franja = elegir(franjas);
  const confirmacion = http.post(
    `${config.baseUrl}/api/citas/confirmar`,
    JSON.stringify({ solicitudId: creada.id, tecnicoId: franja.tecnicoId, inicio: franja.inicio, fin: franja.fin }),
    cabeceras(token, 'crear_confirmar', { responseCallback: ESTADOS_CONFIRMAR }),
  );
  if (confirmacion.status === 409) {
    conflictosConfirmar.add(1);
  } else if (check(confirmacion, { 'confirmar 200': (r) => r.status === 200 })) {
    citasCreadas.add(1);
  }
}

function notificaciones() {
  const { token } = sesionDelCliente();
  const listado = http.get(`${config.baseUrl}/api/notificaciones?page=0&size=20`, cabeceras(token, 'notificaciones'));
  check(listado, { 'notificaciones 200': (r) => r.status === 200 });
  const conteo = http.get(`${config.baseUrl}/api/notificaciones/no-leidas/conteo`, cabeceras(token, 'notificaciones'));
  check(conteo, { 'conteo 200': (r) => r.status === 200 });
}

function admin(datos) {
  const pagina = Math.floor(Math.random() * 20);
  const usuarios = http.get(`${config.baseUrl}/api/admin/usuarios?page=${pagina}&size=20`, cabeceras(datos.adminToken, 'admin'));
  check(usuarios, { 'admin usuarios 200': (r) => r.status === 200 });
  const tecnicoId = elegir(datos.tecnicoIds);
  const franjas = http.get(`${config.baseUrl}/api/admin/tecnicos/${tecnicoId}/franjas`, cabeceras(datos.adminToken, 'admin'));
  check(franjas, { 'admin franjas 200': (r) => r.status === 200 });
  const manuales = http.get(`${config.baseUrl}/api/admin/solicitudes-manuales?page=0&size=20`, cabeceras(datos.adminToken, 'admin'));
  check(manuales, { 'admin solicitudes manuales 200': (r) => r.status === 200 });
}

const ACCIONES = {
  lectura_publica: lecturaPublica,
  login: loginDeCliente,
  disponibilidad,
  crear_confirmar: crearYConfirmar,
  notificaciones,
  admin,
};

const PESO_TOTAL = escenarios.reduce((suma, e) => suma + e.peso, 0);

function elegirEscenario() {
  let punto = Math.random() * PESO_TOTAL;
  for (const escenario of escenarios) {
    punto -= escenario.peso;
    if (punto <= 0) {
      return escenario.nombre;
    }
  }
  return escenarios[escenarios.length - 1].nombre;
}

// Una iteracion de un usuario virtual: elige un escenario segun el reparto y lo ejecuta
export function iteracion(datos) {
  http.setResponseCallback(ESTADOS_OK);
  ACCIONES[elegirEscenario()](datos);
  pensar();
}
