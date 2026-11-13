// Umbrales por escenario/escalon y generacion de los resumenes (JSON + texto legible) a partir de los datos de k6.
import { config, escenarios, umbralP95 } from './config.js';

// ---------- Umbrales ----------

// Una expresion siempre verdadera sirve para que k6 calcule y reporte la sub-metrica aunque no sea un limite real
const SOLO_MEDIR = 'max>=0';

export function umbralesCarga() {
  const umbrales = {
    http_req_failed: [`rate<${config.maxErrores}`],
    http_req_duration: [SOLO_MEDIR],
  };
  for (const e of escenarios) {
    umbrales[`http_req_duration{escenario:${e.nombre}}`] = [`p(95)<${umbralP95(e.umbral)}`];
    umbrales[`http_req_failed{escenario:${e.nombre}}`] = [`rate<${config.maxErrores}`];
    umbrales[`http_reqs{escenario:${e.nombre}}`] = ['count>=0'];
  }
  return umbrales;
}

export function umbralesEstres(escalones, maxErroresEstres) {
  const umbrales = {};
  for (const escalon of escalones) {
    umbrales[`http_req_failed{escalon:${escalon.nombre}}`] = [`rate<${maxErroresEstres}`];
    umbrales[`http_req_duration{escalon:${escalon.nombre}}`] = [SOLO_MEDIR];
    umbrales[`http_reqs{escalon:${escalon.nombre}}`] = ['count>=0'];
    for (const e of escenarios) {
      const etiqueta = `escalon:${escalon.nombre},escenario:${e.nombre}`;
      umbrales[`http_req_duration{${etiqueta}}`] = [`p(95)<${umbralP95(e.umbral)}`];
      umbrales[`http_req_failed{${etiqueta}}`] = ['rate>=0'];
      umbrales[`http_reqs{${etiqueta}}`] = ['count>=0'];
    }
  }
  return umbrales;
}

// ---------- Lectura de metricas ----------

function valor(datos, metrica, clave) {
  const m = datos.metrics[metrica];
  return m && m.values && m.values[clave] !== undefined ? m.values[clave] : null;
}

export function estadisticas(datos, etiquetas) {
  const filtro = etiquetas ? `{${etiquetas}}` : '';
  return {
    peticiones: valor(datos, `http_reqs${filtro}`, 'count') || 0,
    p95: valor(datos, `http_req_duration${filtro}`, 'p(95)'),
    promedio: valor(datos, `http_req_duration${filtro}`, 'avg'),
    errores: valor(datos, `http_req_failed${filtro}`, 'rate'),
  };
}

export function ms(numero) {
  return numero === null || numero === undefined ? '-' : `${numero.toFixed(0)} ms`;
}

export function porcentaje(fraccion) {
  return fraccion === null || fraccion === undefined ? '-' : `${(fraccion * 100).toFixed(2)} %`;
}

function filaMarkdown(columnas) {
  return `| ${columnas.join(' | ')} |`;
}

export function tablaMarkdown(encabezados, filas) {
  return [
    filaMarkdown(encabezados),
    filaMarkdown(encabezados.map(() => '---')),
    ...filas.map(filaMarkdown),
  ].join('\n');
}

export function tablaEscenarios(datos, etiquetasBase) {
  const filas = escenarios.map((e) => {
    const etiquetas = etiquetasBase ? `${etiquetasBase},escenario:${e.nombre}` : `escenario:${e.nombre}`;
    const s = estadisticas(datos, etiquetas);
    const umbral = umbralP95(e.umbral);
    const cumple = s.p95 === null ? '-' : (s.p95 < umbral ? 'si' : 'NO');
    return [e.nombre, s.peticiones, ms(s.p95), ms(s.promedio), porcentaje(s.errores), `< ${umbral} ms`, cumple];
  });
  return tablaMarkdown(['escenario', 'peticiones', 'p95', 'promedio', 'errores', 'umbral p95', 'cumple'], filas);
}

export function contador(datos, nombre) {
  return valor(datos, nombre, 'count') || 0;
}

// ---------- Salida ----------

export function marcaDeTiempo() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

// handleSummary: escribe JSON y texto con marca de tiempo y copias "-ultimo", y muestra el texto en pantalla
export function archivosDeSalida(prefijo, datosJson, texto) {
  const dir = config.directorioResultados;
  const marca = marcaDeTiempo();
  const json = JSON.stringify(datosJson, null, 2);
  return {
    [`${dir}/${prefijo}-${marca}.json`]: json,
    [`${dir}/${prefijo}-${marca}.md`]: texto,
    [`${dir}/${prefijo}-ultimo.json`]: json,
    [`${dir}/${prefijo}-ultimo.md`]: texto,
    stdout: `\n${texto}\n`,
  };
}
