// Configuracion comun de las pruebas k6. Todo se parametriza con variables de entorno (k6 run -e NOMBRE=valor ...).

function entero(nombre, porDefecto) {
  const valor = parseInt(__ENV[nombre], 10);
  return Number.isNaN(valor) ? porDefecto : valor;
}

function decimal(nombre, porDefecto) {
  const valor = parseFloat(__ENV[nombre]);
  return Number.isNaN(valor) ? porDefecto : valor;
}

export const config = {
  baseUrl: __ENV.BASE_URL || 'http://localhost:18080',
  // Clientes sembrados por db/perf: cliente1@perf.test .. cliente{NUM_CLIENTES}@perf.test
  numClientes: entero('NUM_CLIENTES', 5000),
  clienteContrasena: __ENV.CLIENTE_CONTRASENA || 'Perf12345',
  adminCorreo: __ENV.ADMIN_CORREO || 'admin@appointech.com',
  // Sin valor por defecto a proposito: la clave del administrador no se escribe en el repositorio
  adminContrasena: __ENV.ADMIN_CONTRASENA || '',
  // Tecnicos sembrados: ids 1..numTecnicos en una base perf recien creada
  numTecnicos: entero('NUM_TECNICOS', 50),
  // Pausa entre acciones de un usuario virtual (segundos, entre minimo y maximo)
  pensarMin: decimal('PENSAR_MIN', 0.5),
  pensarMax: decimal('PENSAR_MAX', 1.5),
  // Umbrales (milisegundos para p95; fraccion para errores)
  umbralLecturaMs: entero('UMBRAL_LECTURA_MS', 500),
  umbralLoginMs: entero('UMBRAL_LOGIN_MS', 1500),
  umbralEscrituraMs: entero('UMBRAL_ESCRITURA_MS', 1500),
  maxErrores: decimal('MAX_ERRORES', 0.01),
  // Coordenadas de referencia de los datos sembrados (los clientes viven en +-0.1 grados)
  latCentro: 4.3365,
  lonCentro: -74.3638,
  // Dias hacia adelante con franjas sembradas (hoy + 0..13)
  diasConFranjas: 14,
  directorioResultados: __ENV.RESULTADOS_DIR || 'perf/resultados',
};

// Escenarios: nombre de la etiqueta "escenario", peso en el reparto y grupo de umbral.
// Reparto realista: mucha lectura y consulta, pocas escrituras, el login es un paso relativamente raro.
export const escenarios = [
  { nombre: 'lectura_publica', peso: 15, umbral: 'lectura' },
  { nombre: 'login', peso: 10, umbral: 'login' },
  { nombre: 'disponibilidad', peso: 25, umbral: 'lectura' },
  { nombre: 'crear_confirmar', peso: 10, umbral: 'escritura' },
  { nombre: 'notificaciones', peso: 20, umbral: 'lectura' },
  { nombre: 'admin', peso: 20, umbral: 'lectura' },
];

export function umbralP95(grupo) {
  if (grupo === 'login') return config.umbralLoginMs;
  if (grupo === 'escritura') return config.umbralEscrituraMs;
  return config.umbralLecturaMs;
}


export function aEntero(nombre, porDefecto) {
  return entero(nombre, porDefecto);
}
