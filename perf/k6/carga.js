// PRUEBA DE CARGA: rampa de subida, meseta y bajada con el reparto realista de escenarios.
// Variables (k6 run -e NOMBRE=valor): BASE_URL, ADMIN_CONTRASENA (obligatoria), USUARIOS, RAMPA_SUBIDA, MESETA,
// RAMPA_BAJADA (duraciones k6: 30s, 2m...), UMBRAL_LECTURA_MS, UMBRAL_LOGIN_MS, UMBRAL_ESCRITURA_MS, MAX_ERRORES.
// Detalle completo en perf/README.md
import { config, aEntero } from './lib/config.js';
import { preparar, iteracion } from './lib/flujos.js';
import {
  umbralesCarga, tablaEscenarios, estadisticas, ms, porcentaje, contador, archivosDeSalida,
} from './lib/resumen.js';

const usuarios = aEntero('USUARIOS', 50);
const subida = __ENV.RAMPA_SUBIDA || '30s';
const meseta = __ENV.MESETA || '2m';
const bajada = __ENV.RAMPA_BAJADA || '30s';

export const options = {
  scenarios: {
    carga: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: subida, target: usuarios },
        { duration: meseta, target: usuarios },
        { duration: bajada, target: 0 },
      ],
      gracefulRampDown: '10s',
      exec: 'usuario',
    },
  },
  thresholds: umbralesCarga(),
};

export function setup() {
  return preparar();
}

export function usuario(datos) {
  iteracion(datos);
}

export function handleSummary(datos) {
  const total = estadisticas(datos);
  const cumplen = Object.values(datos.metrics).every((m) => !m.thresholds
    || Object.values(m.thresholds).every((t) => t.ok));
  const texto = [
    '# Resultado de la prueba de carga',
    '',
    `- Usuarios virtuales en la meseta: ${usuarios} (subida ${subida}, meseta ${meseta}, bajada ${bajada})`,
    `- Destino: ${config.baseUrl}`,
    `- Peticiones totales: ${total.peticiones}; p95 global: ${ms(total.p95)}; errores: ${porcentaje(total.errores)}`,
    `- Citas confirmadas: ${contador(datos, 'citas_confirmadas')}; conflictos de franja esperados (409): ${contador(datos, 'confirmar_conflicto_esperado')}; consultas sin franjas: ${contador(datos, 'franjas_vacias')}`,
    `- Umbrales: ${cumplen ? 'TODOS CUMPLIDOS' : 'HAY UMBRALES INCUMPLIDOS (ver columna "cumple" y errores)'}`,
    '',
    tablaEscenarios(datos),
  ].join('\n');
  const json = {
    tipo: 'carga',
    destino: config.baseUrl,
    usuarios,
    etapas: { subida, meseta, bajada },
    umbralesCumplidos: cumplen,
    global: total,
    metricas: datos.metrics,
  };
  return archivosDeSalida('carga', json, texto);
}
