// Generador de los planes JMeter (carga y estres) y del CSV de clientes. Los .jmx son el entregable; este script solo
// evita escribir a mano 30 grupos de hilos casi iguales. Uso (desde la carpeta del backend): node perf/jmeter/generar-planes.js
'use strict';
const fs = require('node:fs');
const path = require('node:path');

const DIR = __dirname;
const NUM_CLIENTES = 5000;
const CONTRASENA_CLIENTE = 'Perf12345'; // la de los usuarios sembrados por db/perf (solo entorno perf)

// ---------- utilidades XML ----------
const esc = (t) => String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
// Los valores vacios se serializan sin contenido: el formato XML de PowerShell no debe convertir filename en espacios.
const sp = (n, v) => v === '' ? `<stringProp name="${n}"/>` : `<stringProp name="${n}">${esc(v)}</stringProp>`;
const bp = (n, v) => `<boolProp name="${n}">${v}</boolProp>`;
const ip = (n, v) => `<intProp name="${n}">${v}</intProp>`;
const nodo = (xml, hijos = []) => `${xml}\n<hashTree>\n${hijos.join('\n')}\n</hashTree>`;

// ---------- elementos ----------
function cabeceras(nombre, mapa) {
  const filas = Object.entries(mapa).map(([k, v]) => `<elementProp name="" elementType="Header">${sp('Header.name', k)}${sp('Header.value', v)}</elementProp>`).join('');
  return nodo(`<HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="${esc(nombre)}" enabled="true"><collectionProp name="HeaderManager.headers">${filas}</collectionProp></HeaderManager>`);
}

function valoresPorDefecto() {
  return nodo(`<ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP Request Defaults" enabled="true">
<elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true"><collectionProp name="Arguments.arguments"/></elementProp>
${sp('HTTPSampler.domain', '${__P(host,localhost)}')}
${sp('HTTPSampler.port', '${__P(puerto,18080)}')}
${sp('HTTPSampler.protocol', '${__P(protocolo,http)}')}
${sp('HTTPSampler.contentEncoding', 'UTF-8')}
${sp('HTTPSampler.connect_timeout', '${__P(timeout_conexion,10000)}')}
${sp('HTTPSampler.response_timeout', '${__P(timeout_respuesta,30000)}')}
</ConfigTestElement>`);
}

function peticion(nombre, metodo, ruta, cuerpo, hijos = []) {
  const args = cuerpo === undefined
    ? '<elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true"><collectionProp name="Arguments.arguments"/></elementProp>'
    : `<elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true"><collectionProp name="Arguments.arguments"><elementProp name="" elementType="HTTPArgument">${bp('HTTPArgument.always_encode', 'false')}${sp('Argument.value', cuerpo)}${sp('Argument.metadata', '=')}</elementProp></collectionProp></elementProp>`;
  const muestra = nodo(`<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="${esc(nombre)}" enabled="true">
${args}
${sp('HTTPSampler.path', ruta)}
${bp('HTTPSampler.follow_redirects', 'true')}
${sp('HTTPSampler.method', metodo)}
${bp('HTTPSampler.use_keepalive', 'true')}
${bp('HTTPSampler.postBodyRaw', cuerpo === undefined ? 'false' : 'true')}
</HTTPSamplerProxy>`, hijos);
  return muestra;
}

// Codigo HTTP esperado (expresion regular que debe coincidir con todo el codigo)
function aserCodigo(regex) {
  return nodo(`<ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Codigo esperado ${esc(regex)}" enabled="true">
<collectionProp name="Asserion.test_strings">${sp('1', regex)}</collectionProp>
${sp('Assertion.custom_message', '')}
${sp('Assertion.test_field', 'Assertion.response_code')}
${bp('Assertion.assume_success', 'true')}
${ip('Assertion.test_type', 1)}
</ResponseAssertion>`);
}

function aserDuracion(propiedad, porDefecto) {
  return nodo(`<DurationAssertion guiclass="DurationAssertionGui" testclass="DurationAssertion" testname="Duracion maxima (${propiedad})" enabled="true">${sp('DurationAssertion.duration', `\${__P(${propiedad},${porDefecto})}`)}</DurationAssertion>`);
}

function extractorJson(nombre, variable, expresion, porDefecto) {
  return nodo(`<JSONPostProcessor guiclass="JSONPostProcessorGui" testclass="JSONPostProcessor" testname="${esc(nombre)}" enabled="true">
${sp('JSONPostProcessor.referenceNames', variable)}
${sp('JSONPostProcessor.jsonPathExprs', expresion)}
${sp('JSONPostProcessor.match_numbers', '1')}
${sp('JSONPostProcessor.defaultValues', porDefecto)}
</JSONPostProcessor>`);
}

function script(nombre, codigo) {
  return nodo(`<JSR223PostProcessor guiclass="TestBeanGUI" testclass="JSR223PostProcessor" testname="${esc(nombre)}" enabled="true">
${sp('scriptLanguage', 'groovy')}${sp('parameters', '')}${sp('filename', '')}${sp('cacheKey', 'true')}${sp('script', codigo)}
</JSR223PostProcessor>`);
}

function pausa() {
  return nodo(`<UniformRandomTimer guiclass="UniformRandomTimerGui" testclass="UniformRandomTimer" testname="Tiempo de pensar" enabled="true">${sp('ConstantTimer.delay', '${__P(pensar_min,500)}')}${sp('RandomTimer.range', '${__P(pensar_rango,1000)}')}</UniformRandomTimer>`);
}

function csvClientes() {
  return nodo(`<CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="Tokens sinteticos preparados por setUp" enabled="true">
${sp('delimiter', ',')}${sp('fileEncoding', 'UTF-8')}${sp('filename', '${__P(tokens_csv,tokens.csv)}')}${bp('ignoreFirstLine', 'true')}${bp('quotedData', 'false')}${bp('recycle', 'true')}${sp('shareMode', 'shareMode.all')}${bp('stopThread', 'false')}${sp('variableNames', 'tokenCsv')}
</CSVDataSet>`);
}

function unaVez(nombre, hijos) {
  return nodo(`<OnceOnlyController guiclass="OnceOnlyControllerGui" testclass="OnceOnlyController" testname="${esc(nombre)}" enabled="true"/>`, hijos);
}

function siHayFranja(hijos) {
  return nodo(`<IfController guiclass="IfControllerPanel" testclass="IfController" testname="Solo si hay franjas" enabled="true">${sp('IfController.condition', '${franjaOk}')}${bp('IfController.evaluateAll', 'false')}${bp('IfController.useExpression', 'true')}</IfController>`, hijos);
}

function grupoHilos(nombre, hilos, rampa, duracion, retardo, hijos) {
  return nodo(`<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="${esc(nombre)}" enabled="true">
${sp('ThreadGroup.on_sample_error', 'continue')}
<elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Loop Controller" enabled="true">${bp('LoopController.continue_forever', 'false')}${ip('LoopController.loops', -1)}</elementProp>
${sp('ThreadGroup.num_threads', hilos)}${sp('ThreadGroup.ramp_time', rampa)}${bp('ThreadGroup.scheduler', 'true')}${sp('ThreadGroup.duration', duracion)}${sp('ThreadGroup.delay', retardo)}${bp('ThreadGroup.same_user_on_next_iteration', 'true')}
</ThreadGroup>`, hijos);
}

function grupoPreparacion(hijos) {
  return nodo(`<SetupThreadGroup guiclass="SetupThreadGroupGui" testclass="SetupThreadGroup" testname="setUp: login del administrador (una sola vez)" enabled="true">
${sp('ThreadGroup.on_sample_error', 'stoptest')}
<elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Loop Controller" enabled="true">${bp('LoopController.continue_forever', 'false')}${ip('LoopController.loops', 1)}</elementProp>
${sp('ThreadGroup.num_threads', '1')}${sp('ThreadGroup.ramp_time', '1')}${bp('ThreadGroup.scheduler', 'false')}${sp('ThreadGroup.duration', '')}${sp('ThreadGroup.delay', '')}${bp('ThreadGroup.same_user_on_next_iteration', 'true')}
</SetupThreadGroup>`, hijos);
}

function muestraScript(nombre, codigo) {
  return nodo(`<JSR223Sampler guiclass="TestBeanGUI" testclass="JSR223Sampler" testname="${esc(nombre)}" enabled="true">
${sp('scriptLanguage', 'groovy')}${sp('parameters', '')}${sp('filename', '')}${sp('cacheKey', 'true')}${sp('script', codigo)}</JSR223Sampler>`);
}

function listener(nombre, clase, guiclass, archivo, habilitado) {
  const config = `<objProp><name>saveConfig</name><value class="SampleSaveConfiguration"><time>true</time><latency>true</latency><timestamp>true</timestamp><success>true</success><label>true</label><code>true</code><message>true</message><threadName>true</threadName><dataType>false</dataType><encoding>false</encoding><assertions>true</assertions><subresults>false</subresults><responseData>false</responseData><samplerData>false</samplerData><xml>false</xml><fieldNames>true</fieldNames><responseHeaders>false</responseHeaders><requestHeaders>false</requestHeaders><responseDataOnError>false</responseDataOnError><saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage><assertionsResultsToSave>0</assertionsResultsToSave><bytes>true</bytes><sentBytes>true</sentBytes><url>false</url><threadCounts>true</threadCounts><idleTime>true</idleTime><connectTime>true</connectTime></value></objProp>`;
  return nodo(`<ResultCollector guiclass="${guiclass}" testclass="ResultCollector" testname="${esc(nombre)}" enabled="${habilitado}">${bp('ResultCollector.error_logging', 'false')}${config}${sp('filename', archivo)}</ResultCollector>`);
}

// ---------- escenarios ----------
// peso = % de los usuarios del plan que ejecutan ese escenario (reparto realista: mucha lectura, poca escritura)
const PESOS = { lectura_publica: 23, login: 2, disponibilidad: 25, crear_confirmar: 10, notificaciones: 20, admin: 20 };
const FECHA = '${__timeShift(yyyy-MM-dd,,P${__Random(0,13)}D,,)}';
const LAT = '${__groovy(Math.round((4.3365+(Math.random()-0.5)*0.1)*100000)/100000.0)}';
const LON = '${__groovy(Math.round((-74.3638+(Math.random()-0.5)*0.1)*100000)/100000.0)}';
const QUERY_FRANJAS = `especialidadId=\${__Random(1,6)}&fecha=${FECHA}&latitud=${LAT}&longitud=${LON}`;

const SELECCIONAR_FRANJA = `import groovy.json.JsonSlurper
vars.put('franjaOk', 'false')
if (prev.getResponseCode() == '200') {
    def franjas = new JsonSlurper().parseText(prev.getResponseDataAsString()).franjas
    if (franjas != null && franjas.size() > 0) {
        def f = franjas[new Random().nextInt(franjas.size())]
        vars.put('tecnicoId', f.tecnicoId.toString())
        vars.put('inicio', f.inicio)
        vars.put('fin', f.fin)
        vars.put('franjaOk', 'true')
    }
}`;

// Cada funcion devuelve los hijos del grupo (controladores/muestras) con la etiqueta "prefijo|escenario|accion"
function loginDeSesion(p, esc2) {
  const origen = esc2 === 'admin' ? "props.get('adminToken')" : "vars.get('tokenCsv')";
  return script('Conservar token preparado sin autenticar durante la carga',
    `if (vars.get('token') == null) { def t = ${origen}; if (!t || t == 'NO_TOKEN') throw new IllegalStateException('Token de preparacion ausente'); vars.put('token', t); }`)
    .replaceAll('JSR223PostProcessor', 'JSR223PreProcessor');
}

function escenario(nombre, p) {
  const auth = cabeceras('Authorization (token del cliente)', { Authorization: 'Bearer ${token}' });
  switch (nombre) {
    case 'login':
      return [pausa(), peticion(`${p}login|POST login`, 'POST', '/api/auth/login',
        '{"correo":"cliente${__Random(1,5000)}@perf.test","contrasena":"' + CONTRASENA_CLIENTE + '"}',
        [aserCodigo('200'), aserDuracion('umbral_login', 2000)])];
    case 'lectura_publica':
      return [csvClientes(), auth, pausa(), loginDeSesion(p, nombre), peticion(`${p}lectura_publica|GET especialidades`, 'GET', '/api/especialidades', undefined, [aserCodigo('200'), aserDuracion('umbral_lectura_simple', 300)])];
    case 'disponibilidad':
      return [csvClientes(), auth, pausa(), loginDeSesion(p, nombre),
        peticion(`${p}disponibilidad|GET franjas-con-motivo`, 'GET', `/api/disponibilidad/franjas-con-motivo?${QUERY_FRANJAS}`, undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800)]),
        peticion(`${p}disponibilidad|GET franjas`, 'GET', `/api/disponibilidad/franjas?${QUERY_FRANJAS}`, undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800)])];
    case 'crear_confirmar':
      return [csvClientes(), auth, pausa(), loginDeSesion(p, nombre),
        peticion(`${p}crear_confirmar|GET franjas-con-motivo`, 'GET', `/api/disponibilidad/franjas-con-motivo?${QUERY_FRANJAS}`, undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800), script('Elegir una franja al azar', SELECCIONAR_FRANJA)]),
        siHayFranja([
          peticion(`${p}crear_confirmar|POST solicitud`, 'POST', '/api/solicitudes', `{"especialidadId":\${__Random(1,6)},"descripcionProblema":"Prueba de carga JMeter","direccionServicio":"Direccion de prueba de carga","latitud":${LAT},"longitud":${LON}}`, [aserCodigo('201'), aserDuracion('umbral_escritura', 1500), extractorJson('solicitudId', 'solicitudId', '$.id', 'NO_ID')]),
          // 409/400: la franja la tomo otro usuario virtual o la especialidad no coincide: resultado esperado de la concurrencia
          peticion(`${p}crear_confirmar|POST confirmar cita`, 'POST', '/api/citas/confirmar', '{"solicitudId":${solicitudId},"tecnicoId":${tecnicoId},"inicio":"${inicio}","fin":"${fin}"}', [aserCodigo('200|409'), aserDuracion('umbral_escritura', 1500)]),
        ])];
    case 'notificaciones':
      return [csvClientes(), auth, pausa(), loginDeSesion(p, nombre),
        peticion(`${p}notificaciones|GET listado`, 'GET', '/api/notificaciones?page=0&size=20', undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800)]),
        peticion(`${p}notificaciones|GET conteo no leidas`, 'GET', '/api/notificaciones/no-leidas/conteo', undefined, [aserCodigo('200'), aserDuracion('umbral_lectura_simple', 300)])];
    case 'admin':
      return [auth, pausa(), loginDeSesion(p, nombre),
        peticion(`${p}admin|GET usuarios paginados`, 'GET', '/api/admin/usuarios?page=${__Random(0,19)}&size=20', undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800)]),
        peticion(`${p}admin|GET franjas por tecnico`, 'GET', '/api/admin/tecnicos/${__Random(1,50)}/franjas', undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800)]),
        peticion(`${p}admin|GET solicitudes manuales`, 'GET', '/api/admin/solicitudes-manuales?page=0&size=20', undefined, [aserCodigo('200'), aserDuracion('umbral_lectura', 800)])];
    default:
      throw new Error(`Escenario desconocido: ${nombre}`);
  }
}

const hilosDe = (usuariosExpr, factor, peso) => {
  const total = `(${usuariosExpr}*${factor})`;
  // Lectura publica absorbe el redondeo: la suma coincide con N, tambien al reservar 2% para login.
  const valor = peso === 23
    ? `${total} - ` + [2, 25, 10, 20, 20].map(p => `Math.ceil(${total}*${p}/100.0)`).join(' - ')
    : `Math.ceil(${total}*${peso}/100.0)`;
  return `\${__groovy((int)(${valor}))}`;
};

function preparacion() {
  return grupoPreparacion([
    muestraScript('setUp|inicializar CSV de tokens', "java.nio.file.Files.writeString(java.nio.file.Paths.get(props.getProperty('tokens_csv', 'perf/jmeter/tokens.csv')), 'token' + System.lineSeparator())"),
    peticion('setUp|login administrador', 'POST', '/api/auth/login', '{"correo":"${__P(admin_correo,admin@appointech.com)}","contrasena":"${__groovy(System.getenv(\'APPOINTECH_JMETER_ADMIN_PASSWORD\'))}"}', [
      aserCodigo('200'), extractorJson('token admin', 'adminToken', '$.datos.token', 'NO_TOKEN'),
      script('Publicar el token como propiedad JMeter', "def token = vars.get('adminToken'); if (!token || token == 'NO_TOKEN') { prev.setSuccessful(false); ctx.getEngine().stopTest(); } else { props.put('adminToken', token); }"),
    ]),
    nodo(`<LoopController guiclass="LoopControlPanel" testclass="LoopController" testname="Generar N tokens antes de medir" enabled="true">
${bp('LoopController.continue_forever', false)}${sp('LoopController.loops', '${__P(tokens_n,240)}')}</LoopController>`, [
      peticion('setUp|token cliente', 'POST', '/api/auth/login',
        '{"correo":"cliente${__counter(FALSE,tokenIndice)}@perf.test","contrasena":"' + CONTRASENA_CLIENTE + '"}', [
          aserCodigo('200'), extractorJson('token preparado', 'tokenPreparado', '$.datos.token', 'NO_TOKEN'),
          script('Guardar exclusivamente token sintetico en CSV', "def t = vars.get('tokenPreparado'); if (!t || t == 'NO_TOKEN' || prev.getResponseCode() != '200') { prev.setSuccessful(false); ctx.getEngine().stopTest(); } else { java.nio.file.Files.writeString(java.nio.file.Paths.get(props.getProperty('tokens_csv', 'perf/jmeter/tokens.csv')), t + System.lineSeparator(), java.nio.file.StandardOpenOption.APPEND); }")
        ])
    ]),
    muestraScript('setUp|inicio medicion', "props.put('medicionPreparada', 'true')"),
  ]);
}

function plan(nombre, comentario, grupos) {
  const raiz = nodo(`<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="${esc(nombre)}" enabled="true">
${sp('TestPlan.comments', comentario)}${bp('TestPlan.functional_mode', 'false')}${bp('TestPlan.tearDown_on_shutdown', 'true')}${bp('TestPlan.serialize_threadgroups', 'false')}
<elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true"><collectionProp name="Arguments.arguments"/></elementProp>
${sp('TestPlan.user_define_classpath', '')}
</TestPlan>`, [
    valoresPorDefecto(),
    cabeceras('HTTP Header Manager (JSON)', { 'Content-Type': 'application/json', Accept: 'application/json' }),
    preparacion(),
    ...grupos,
    // Sin listeners graficos en la ejecucion real: el arbol queda deshabilitado y el .jtl lo escribe -l (ver README)
    listener('Simple Data Writer (alternativa a -l)', 'ResultCollector', 'SimpleDataWriter', '${__P(jtl_sdw,perf/resultados/sdw.jtl)}', false),
    listener('View Results Tree (solo depuracion)', 'ResultCollector', 'ViewResultsFullVisualizer', '', false),
  ]);
  return `<?xml version="1.0" encoding="UTF-8"?>\n<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">\n<hashTree>\n${raiz}\n</hashTree>\n</jmeterTestPlan>\n`;
}

// ---------- plan de carga: un grupo por escenario, rampa -> meseta (la duracion incluye la rampa) ----------
const gruposCarga = Object.entries(PESOS).map(([nombre, peso]) =>
  grupoHilos(`Carga: ${nombre} (${peso}% de los usuarios)`, hilosDe('${__P(usuarios,50)}', 1, peso), '${__P(rampa,30)}', '${__P(duracion,120)}', '0', escenario(nombre, '')));
fs.writeFileSync(path.join(DIR, 'appointech-carga.jmx'),
  plan('AppoinTech - Carga', 'Carga con reparto realista. Propiedades: host, puerto, usuarios, rampa, duracion, umbral_lectura, umbral_login, umbral_escritura, admin_contrasena (obligatoria).', gruposCarga));

// ---------- plan de estres: escalones x1, x2, x4, x8 con retardo de inicio ----------
const MULTIPLOS = [4, 6, 8];
const gruposEstres = [];
MULTIPLOS.forEach((factor, i) => {
  Object.entries(PESOS).forEach(([nombre, peso]) => {
    gruposEstres.push(grupoHilos(`Estres x${factor}: ${nombre}`, hilosDe('${__P(usuarios_base,30)}', factor, peso), '${__P(rampa_escalon,30)}',
      '${__P(escalon_seg,90)}', `\${__groovy(${i}*\${__P(escalon_seg,90)})}`, escenario(nombre, `x${factor}|`)));
  });
});
fs.writeFileSync(path.join(DIR, 'appointech-estres.jmx'),
  plan('AppoinTech - Estres', 'Escalones x4,x6,x8. Tokens preparados antes de medir; login aislado 2%; rampa 30s; arranque y sostenido separados.', gruposEstres));
// El auxiliar usa el mismo generador: no reserializa stringProp vacios como rutas con espacios.
const gruposAuxiliares = [];
[6, 8].forEach((factor, i) => {
  Object.entries(PESOS).forEach(([nombre, peso]) => {
    gruposAuxiliares.push(grupoHilos(`Estres x${factor}: ${nombre}`, hilosDe('${__P(usuarios_base,20)}', factor, peso),
      '${__P(rampa_escalon,30)}', '${__P(escalon_seg,90)}', `\${__groovy(${i}*\${__P(escalon_seg,90)})}`, escenario(nombre, `x${factor}|`)));
  });
});
fs.writeFileSync(path.join(DIR, 'appointech-estres-6x8x.jmx'),
  plan('AppoinTech - Estres 6x y 8x', 'Misma autenticacion y umbrales; escalones x1,x2,x6,x8.', gruposAuxiliares));

// ---------- clientes.csv ----------
const filas = ['correo,contrasena'];
for (let n = 1; n <= NUM_CLIENTES; n += 1) filas.push(`cliente${n}@perf.test,${CONTRASENA_CLIENTE}`);
fs.writeFileSync(path.join(DIR, 'clientes.csv'), `${filas.join('\n')}\n`);
console.log('Generados: appointech-carga.jmx, appointech-estres.jmx y clientes.csv');
