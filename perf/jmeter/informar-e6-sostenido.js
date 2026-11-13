'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
function report(dir){
 const j=read(path.join(dir,'analisis-sostenido-v2.json'));
 assert(j.versionAnalisis===2&&j.conciliacion.valida,'Requiere conciliacion v2 valida');
 const cfg=read(path.join(dir,'ejecucion.json'));
 const rate=r=>`${r.numerador} / ${r.denominadorSegundos} = ${r.porSegundo===null?'no definida':r.porSegundo.toFixed(3)}`;
 let s=`# E6 sostenido: ${j.fase} usuarios, análisis revisado\n\nAnálisis del JTL existente; este generador no ejecuta HTTP. Los archivos originales permanecen intactos. Fuente: e6.jtl; detalle recalculado: analisis-sostenido-v2.json.\n\n`;
 if(fs.existsSync(path.join(dir,'revision-posterior.json'))){
  const revision=read(path.join(dir,'revision-posterior.json'));
  s+='**La corrida no quedó completamente validada: falló el recolector de recursos.** El JTL sí es analizable: '+revision.JTLContado+' muestras conciliadas y JMeter con código '+revision.JMeterCodigo+'. No se repitió la carga.\n\n';
  s+=revision.advertencia+'\n\n'+revision.falloRecolector+' '+revision.identidadDelBloqueador+' Véase revision-posterior.json.\n\n';
 }
 s+=`Configuración: ${cfg.rampa} s de rampa + ${cfg.sostenido} s sostenidos; mezcla total 40/40/20; umbrales notificaciones/historial/especialidades 800/800/300 ms.\n\n`;
 s+='## Definiciones\n\n';
 for(const [name,value] of Object.entries(j.definiciones))s+=`- **${name}**: ${value}\n`;
 s+='\nHTTP 200 recibido no garantiza contenido válido ni duración aceptable. La tasa conforme exige ambas condiciones y HTTP 200. Los fallos de duración y percentiles pertenecen a la cohorte de inicio, mientras que los numeradores de respuestas pertenecen a la ventana de finalización. Cumplir duración significa elapsed <= umbral; el fallo es elapsed > umbral. Las tasas de cierre describen drenaje, no capacidad sostenida.\n\n';
 s+='## Conciliación\n\n';
 s+=`${j.periodos.map(p=>p.muestras).join(' + ')} = **${j.muestras} muestras** (rampa + sostenido + cierre). Ninguna muestra se pierde o duplica.\n\n`;
 if(j.conciliacionDuracion){
  const d=j.conciliacionDuracion;
  s+='Excesos de duración por **inicio** de la petición:\n\n| Endpoint | Rampa | Sostenido | Cierre | Total |\n|---|---:|---:|---:|---:|\n';
  for(const e of d.porEndpoint)s+=`| ${e.endpoint.replace('E6 GET ','')} | ${e.porFase.rampa} | ${e.porFase.sostenido} | ${e.porFase.cierre} | ${e.total} |\n`;
  s+=`| Total | ${d.porFase.rampa} | ${d.porFase.sostenido} | ${d.porFase.cierre} | ${d.total} |\n\n`;
  s+=`${d.porFase.rampa} + ${d.porFase.sostenido} + ${d.porFase.cierre} = ${d.total}. La cifra de sostenido es un subconjunto del total de la corrida. Las respuestas recibidas siguen clasificadas por finalización, sin mover los fallos de duración a otra cohorte de inicio.\n\n`;
 }
 s+='| Ventana de inicio → ventana de fin | Rampa | Sostenido | Cierre |\n|---|---:|---:|---:|\n';
 for(const r of j.conciliacion.crucesInicioFin)s+=`| ${r.inicio} | ${r.finales.rampa} | ${r.finales.sostenido} | ${r.finales.cierre} |\n`;
 s+='\nSubconjunto de fallos de duración de la misma matriz (no se suma de nuevo al total):\n\n| Ventana de inicio → ventana de fin | Rampa | Sostenido | Cierre |\n|---|---:|---:|---:|\n';
 for(const r of j.conciliacion.crucesInicioFin)s+=`| ${r.inicio} | ${r.finalesSobreUmbral.rampa} | ${r.finalesSobreUmbral.sostenido} | ${r.finalesSobreUmbral.cierre} |\n`;
 const c=j.conciliacion.cierre;
 s+=`\nLas ${c.muestras} muestras iniciadas en cierre pertenecen a ${c.hilos} hilos y a su último ciclo, admitido antes del plazo. Distribución por paso del ciclo N,H,E,N,H: ${Object.entries(c.porPaso).map(([p,n])=>`${p}: ${n}`).join('; ')}. El plan evalúa el plazo entre ciclos y completa los cinco GET del ciclo admitido. El drenaje HTTP duró ${c.drenajeMs} ms. Se verificaron admisiones consistentes por ciclo, ciclos consecutivos completos, orden sin solapamiento dentro de cada hilo y ausencia de admisiones fuera de plazo. Esto es comportamiento del plan, no muestras auxiliares ni duplicadas.\n\n`;
 s+='## Tasas globales\n\nCada celda muestra numerador / segundos = peticiones/s. No se utiliza el p95 global para evaluar conjuntamente umbrales distintos.\n\n';
 s+='| Ventana | Iniciadas/s | HTTP 200 recibidos/s | HTTP 200 con contenido y duración conformes/s |\n|---|---:|---:|---:|\n';
 for(const p of j.periodos)s+=`| ${p.periodo} | ${rate(p.tasas.iniciadas)} | ${rate(p.tasas.HTTP200Recibidos)} | ${rate(p.tasas.contenidoYDuracionRecibidos)} |\n`;
 s+=`| Total observado (${j.ventanaHTTPseg} s entre primer inicio y último fin) | ${rate(j.tasas.iniciadas)} | ${rate(j.tasas.HTTP200Recibidos)} | ${rate(j.tasas.contenidoYDuracionRecibidos)} |\n`;
 if(j.fase===100)s+='\nPara 100 usuarios, 9.526 / 20 = 476,30 iniciadas/s. El valor 471,30/s del informe anterior corresponde a 9.426 HTTP 200 finalizados en rampa / 20 s. La diferencia son 100 peticiones iniciadas en rampa que terminaron en sostenido. En sostenido coinciden los totales de inicios y finales por compensación de dos cruces de 100 peticiones, no porque sean las mismas cohortes.\n\n';
 s+='## Por endpoint\n\nPercentiles y fallos por inicio; tasas de respuestas por fin. Cada tasa incluye numerador y denominador.\n\n';
 s+='| Ventana | Endpoint (umbral ms) | Muestras iniciadas | p95 ms | Fallos duración | Timeouts | Iniciadas/s | HTTP 200 recibidos/s | Contenido y duración conformes/s | HTTP solapadas media/máxima |\n|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n';
 for(const p of j.periodos)for(const e of p.porEndpoint){const name=e.endpoint.replace('E6 GET ','');s+=`| ${p.periodo} | ${name} (${name==='especialidades'?300:800}) | ${e.muestras} | ${e.p95Ms} | ${e.sobreUmbral} | ${e.timeouts} | ${rate(e.tasas.iniciadas)} | ${rate(e.tasas.HTTP200Recibidos)} | ${rate(e.tasas.contenidoYDuracionRecibidos)} | ${e.HTTPEnCurso?e.HTTPEnCurso.media.toFixed(3)+' / '+e.HTTPEnCurso.max:'sin cálculo'} |\n`;}
 s+=`\nTotal: ${j.HTTP200} HTTP 200; ${j.HTTP200Validos} con contenido válido; ${j.tasas.contenidoYDuracionRecibidos.numerador} conformes en contenido y duración; ${j.fallidas} muestras fallidas, ${j.sobreUmbral} sobre umbral, ${j.timeouts} timeouts, ${j.transporte} errores de transporte, ${j.fallosContenido} fallos de contenido.\n\n`;
 s+='## Concurrencia y recursos\n\n| Ventana | Hilos activos máximos observados | HTTP solapadas media/máxima |\n|---|---:|---:|\n';
 for(const p of j.periodos)s+=`| ${p.periodo} | ${p.maxHilosActivosObservados} | ${p.HTTPEnCurso.media.toFixed(3)} / ${p.HTTPEnCurso.max} |\n`;
 s+='\n| Ventana | Servicio | Capturas | CPU máxima muestreada % Docker | Memoria máxima MiB |\n|---|---|---:|---:|---:|\n';
 for(const p of j.periodos)for(const r of p.recursos)s+=`| ${p.periodo} | ${r.servicio} | ${r.capturas} | ${r.maxCPUMuestreada??'sin captura'} | ${r.maxMemoriaMiB??'sin captura'} |\n`;
 s+=`\nCadencia backend: ${JSON.stringify(j.cadenciaMs)} ms. Porcentaje CPU Docker sin normalizar al límite; máximos muestreados, no picos absolutos. Hilos y solapamiento JMeter no demuestran ejecución simultánea en servidor.\n\n`;
 if(j.recursosValidacionPosterior){
  const r=j.recursosValidacionPosterior;
  s+=`Cobertura: ${r.filas} filas, ${r.paresCompletos} pares completos y ${r.paresIncompletos.length} capturas incompletas.\n\n| Servicio | Capturas | Última captura relativa al inicio (s) | Tramo final sostenido sin capturas (s) |\n|---|---:|---:|---:|\n`;
  for(const c of r.cobertura)s+=`| ${c.servicio} | ${c.capturas} | ${c.ultimaRelativaSeg} | ${c.sinCapturasHastaFinSostenidoSeg} |\n`;
  if(!r.cierreRecolectorValidado||r.paresIncompletos.length)s+='**Recursos incompletos: el CSV no valida como una serie completa de pares backend/base. Los máximos son únicamente los observados en las capturas disponibles y no representan toda la ventana. No se ha repetido la carga ni reparado el CSV original.**\n\n';
 }
 s+='CPU y memoria se midieron por contenedor y ventana, compartidas por los tres endpoints. No hay medición que permita atribuir estos recursos a un endpoint concreto. El solapamiento de cada endpoint sí se calcula con sus propios intervalos HTTP, incluyendo las peticiones que cruzan límites de ventanas.\n\n';
 s+='## Cierre, integridad y recuperación\n\n';
 for(const f of ['diagnostico.json','validacion-recursos.json','integridad.json','privacidad.json'])if(fs.existsSync(path.join(dir,f)))s+=`- ${f}: ${JSON.stringify(read(path.join(dir,f)))}\n`;
 if(fs.existsSync(path.join(dir,'recuperacion.csv')))s+='\nControles de recuperación (duración de las respuestas separada del tiempo entre controles):\n\n```csv\n'+fs.readFileSync(path.join(dir,'recuperacion.csv'),'utf8').trim()+'\n```\n';
 const before=read(path.join(dir,'estado-antes.json')),after=read(path.join(dir,'estado-despues.json'));
 s+='\nEstado de los contenedores antes/después:\n\n';
 for(const a of before){const z=after.find(r=>r.servicio===a.servicio);s+=`- ${a.servicio}: misma identidad ${a.id===z?.id}, reinicios ${a.reinicios} → ${z?.reinicios}, OOM ${z?.oom}, salud ${z?.salud}; límite memoria ${a.memoria} bytes, nanoCpus ${a.nanoCpus}.\n`;}
 s+='\nEl aislamiento se exige antes de cualquier autenticación HTTP y se vuelve a comprobar antes de JMeter. Evidencias: configuración de ejecución, estado de contenedores, límites y controles previos. No se infiere salud actual de una medición histórica.\n';
 if(fs.existsSync(path.join(dir,'comprobaciones-previas.json')))s+='\nComprobaciones guardadas antes de HTTP: '+JSON.stringify(read(path.join(dir,'comprobaciones-previas.json')))+'\n';
 if(fs.existsSync(path.join(dir,'preparacion.json')))s+='\nPreparación externa de esta corrida, excluida del JTL y con calentamiento del backend: '+JSON.stringify(read(path.join(dir,'preparacion.json')))+'\n';
 if(fs.existsSync(path.join(dir,'contexto-medicion.json'))){
  const context=read(path.join(dir,'contexto-medicion.json'));
  s+='\n## Contexto de la preparación\n\n';
  for(const note of context.notas)s+=note+'\n\n';
 }
 if(j.fase>=200)s+='\nLa preparación interrumpida anterior a la fase de 200 añadió autenticación y lecturas de calentamiento fuera del JTL. Las corridas son secuenciales y tienen distinto historial térmico y de caché; la diferencia entre 100 y 200 (ni con 300) no identifica un efecto aislado del número de usuarios.\n';
 const file=path.join(dir,'INFORME-REVISADO.md');fs.writeFileSync(file,s);console.log(file);
}
module.exports={report};
if(require.main===module)try{report(process.argv[2]);}catch(e){console.error(e.message);process.exitCode=1;}
