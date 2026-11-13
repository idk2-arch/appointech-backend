// PRUEBA DE ESTRES: escalones consecutivos x1, x2, x4, x8 de los usuarios base, con el mismo reparto de escenarios.
// Informa en que escalon empiezan a subir los errores y los tiempos (el recurso que se agota se ve con
// perf/ejecutar.ps1, que muestrea docker stats y los logs del pool mientras corre esta prueba).
// Variables: BASE_URL, ADMIN_CONTRASENA (obligatoria), USUARIOS_BASE (20), ESCALON_SEGUNDOS (60), MAX_ERRORES (0.01 = "empiezan
// a subir"), MAX_ERRORES_ESTRES (0.10 = limite de estres), UMBRAL_LECTURA_MS, UMBRAL_LOGIN_MS, UMBRAL_ESCRITURA_MS.
import { config, escenarios, umbralP95, aEntero } from './lib/config.js';
import { preparar, iteracion } from './lib/flujos.js';
import {
  umbralesEstres, estadisticas, tablaMarkdown, tablaEscenarios, ms, porcentaje, archivosDeSalida,
} from './lib/resumen.js';

const usuariosBase = aEntero('USUARIOS_BASE', 20);
const segundosPorEscalon = aEntero('ESCALON_SEGUNDOS', 60);
const maxErroresEstres = parseFloat(__ENV.MAX_ERRORES_ESTRES) || 0.1;
const MULTIPLICADORES = [1, 2, 4, 8];

const escalones = MULTIPLICADORES.map((factor, i) => ({
  nombre: `x${factor}`,
  vus: usuariosBase * factor,
  desdeSeg: i * segundosPorEscalon,
  hastaSeg: (i + 1) * segundosPorEscalon,
}));

const scenarios = {};
for (const escalon of escalones) {
  scenarios[`escalon_${escalon.nombre}`] = {
    executor: 'constant-vus',
    vus: escalon.vus,
    duration: `${segundosPorEscalon}s`,
    startTime: `${escalon.desdeSeg}s`,
    gracefulStop: '5s',
    exec: 'usuario',
    tags: { escalon: escalon.nombre },
  };
}

export const options = {
  scenarios,
  thresholds: umbralesEstres(escalones, maxErroresEstres),
};

export function setup() {
  return preparar();
}

export function usuario(datos) {
  iteracion(datos);
}

// Primer escalon (en orden) que cumple la condicion, o null
function primerEscalon(analisis, condicion) {
  const encontrado = analisis.find(condicion);
  return encontrado ? encontrado.nombre : null;
}

function peorEscenarioDeTiempos(datos, escalon) {
  const excedidos = escenarios
    .map((e) => ({ nombre: e.nombre, p95: estadisticas(datos, `escalon:${escalon},escenario:${e.nombre}`).p95, umbral: umbralP95(e.umbral) }))
    .filter((e) => e.p95 !== null && e.p95 >= e.umbral);
  return excedidos.map((e) => `${e.nombre} (${ms(e.p95)} > ${e.umbral} ms)`);
}

export function handleSummary(datos) {
  const analisis = escalones.map((escalon) => {
    const s = estadisticas(datos, `escalon:${escalon.nombre}`);
    return {
      ...escalon,
      ...s,
      escenariosLentos: peorEscenarioDeTiempos(datos, escalon.nombre),
    };
  });
  const base = analisis[0];
  const escalonErrores = primerEscalon(analisis, (a) => (a.errores || 0) > config.maxErrores);
  const escalonLimite = primerEscalon(analisis, (a) => (a.errores || 0) >= maxErroresEstres);
  const escalonTiempos = primerEscalon(analisis, (a) => a.escenariosLentos.length > 0);
  const escalonDegradacion = primerEscalon(analisis, (a) => base.p95 !== null && a.p95 !== null && a !== base && a.p95 > base.p95 * 2);

  const tabla = tablaMarkdown(
    ['escalon', 'usuarios virtuales', 'peticiones', 'p95 global', 'promedio', 'errores', 'escenarios sobre su umbral p95'],
    analisis.map((a) => [a.nombre, a.vus, a.peticiones, ms(a.p95), ms(a.promedio), porcentaje(a.errores),
      a.escenariosLentos.length ? a.escenariosLentos.join('; ') : '-']),
  );
  const detalle = analisis.map((a) => `### Escalon ${a.nombre} (${a.vus} usuarios)\n\n${tablaEscenarios(datos, `escalon:${a.nombre}`)}`).join('\n\n');
  const texto = [
    '# Resultado de la prueba de estres',
    '',
    `- Destino: ${config.baseUrl}; usuarios base ${usuariosBase}; ${segundosPorEscalon} s por escalon`,
    `- Los errores empiezan a superar ${porcentaje(config.maxErrores)} en: ${escalonErrores || 'ningun escalon'}`,
    `- Los errores llegan al limite de estres (${porcentaje(maxErroresEstres)}) en: ${escalonLimite || 'ningun escalon'}`,
    `- Algun escenario supera su umbral p95 por primera vez en: ${escalonTiempos || 'ningun escalon'}`,
    `- El p95 global se duplica respecto a ${base.nombre} por primera vez en: ${escalonDegradacion || 'ningun escalon'}`,
    '- Recurso que se agota: ver perf/ejecutar.ps1 (CPU/memoria de docker stats y estado del pool en los logs)',
    '',
    tabla,
    '',
    detalle,
  ].join('\n');
  const json = {
    tipo: 'estres',
    destino: config.baseUrl,
    inicioEpochMs: Date.now() - datos.state.testRunDurationMs,
    escalones: analisis,
    quiebre: { escalonErrores, escalonLimite, escalonTiempos, escalonDegradacion },
    metricas: datos.metrics,
  };
  return archivosDeSalida('estres', json, texto);
}
