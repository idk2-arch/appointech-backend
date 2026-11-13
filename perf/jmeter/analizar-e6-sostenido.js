'use strict';
const fs=require('fs'), path=require('path'), assert=require('assert');
const labels=['E6 GET notificaciones','E6 GET historial','E6 GET especialidades'];
const routes=['http://localhost:18080/api/notificaciones?page=0&size=20','http://localhost:18080/api/cliente/citas','http://localhost:18080/api/especialidades'];
const sequence=[0,1,2,0,1];
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
const save=(d,n,v)=>fs.writeFileSync(path.join(d,n),JSON.stringify(v,null,2));
function* csv(text){let row=[],value='',quoted=false;for(let i=0;i<text.length;i++){
 const c=text[i];if(c==='"'){if(quoted&&text[i+1]==='"'){value+='"';i++;}else quoted=!quoted;}
 else if(c===','&&!quoted){row.push(value);value='';}
 else if(c==='\n'&&!quoted){row.push(value.replace(/\r$/,''));value='';yield row;row=[];}
 else value+=c;
 }if(value||row.length){row.push(value);yield row;}}
function rows(p){const iter=csv(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));const head=iter.next().value;assert(head,'CSV vacio');
 return {head,*[Symbol.iterator](){for(const row of iter){if(row.length===1&&!row[0])continue;assert.equal(row.length,head.length,'Columnas CSV');yield Object.fromEntries(head.map((h,i)=>[h,row[i]]));}}};}
function phase(t,b){if(t<b.inicioEpochMs)return 'antes';if(t<b.finRampaEpochMs)return 'rampa';if(t<b.finSostenidoEpochMs)return 'sostenido';return 'cierre';}
function stats(rr,seconds,received=rr){const v=rr.map(r=>r.ms).sort((a,b)=>a-b),n=v.length;
 const q=p=>n?v[Math.ceil(p*n)-1]:null;const count=f=>rr.filter(f).length;
 const valid=r=>r.code==='200'&&r.valid;
 const compliant=r=>valid(r)&&r.ms<=r.limit;
 const rate=numerator=>({numerador:numerator,denominadorSegundos:seconds,porSegundo:seconds>0?numerator/seconds:null});
 return {muestras:n,HTTP200:count(r=>r.code==='200'),otrosHTTP:count(r=>/^\d{3}$/.test(r.code)&&r.code!=='200'),
 timeouts:count(r=>r.failure==='Transporte'&&/SocketTimeoutException|timed out|TimeoutException/i.test(r.code+' '+r.message)),
 transporte:count(r=>r.failure==='Transporte'),contenido:count(r=>r.failure==='Contenido'),sobreUmbral:count(r=>r.ms>r.limit),
 HTTP200Validos:count(valid),HTTP200ValidosLentos:count(r=>valid(r)&&r.ms>r.limit),fallidas:count(r=>!r.success),porcentajeFallidas:n?100*count(r=>!r.success)/n:null,
 HTTP200ValidosRecibidosEnIntervalo:received.filter(valid).length,
 tasas:{iniciadas:rate(n),HTTP200Recibidos:rate(received.filter(r=>r.code==='200').length),contenidoYDuracionRecibidos:rate(received.filter(compliant).length)},
 throughputTotalInicios:seconds>0?n/seconds:null,throughputHTTP200ValidosRecibidos:seconds>0?received.filter(valid).length/seconds:null,
 mediaMs:n?v.reduce((a,b)=>a+b,0)/n:null,medianaMs:n?(v[Math.floor((n-1)/2)]+v[Math.floor(n/2)])/2:null,p90Ms:q(.90),p95Ms:q(.95),p99Ms:q(.99),maxMs:n?v[n-1]:null,
 codigos:Object.fromEntries([...new Set(rr.map(r=>r.code))].map(c=>[c,count(r=>r.code===c)]))};}
function overlap(rr,a,b){const e=[];for(const r of rr){const s=Math.max(a,r.start),f=Math.min(b,r.end);if(f>s){e.push([s,1],[f,-1]);}}
 e.sort((x,y)=>x[0]-y[0]||x[1]-y[1]);let active=0,max=0,area=0,t=a;for(const [at,delta]of e){area+=active*(at-t);active+=delta;max=Math.max(max,active);t=at;}return {max,media:b>a?area/(b-a):null};}
function memory(v){const m=/^(\d+(?:\.\d+)?)(B|KiB|MiB|GiB|TiB)$/.exec(v);assert(m,'Unidad memoria');return Number(m[1])*({B:1/1048576,KiB:1/1024,MiB:1,GiB:1024,TiB:1048576})[m[2]];}
function analyze(dir,fake=false){const cfg=read(path.join(dir,'ejecucion.json')),b=read(path.join(dir,'tiempo-inicio.json'));
 assert.equal(!!cfg.ficticio,fake,'Modo real/ficticio');assert.equal(b.finRampaEpochMs-b.inicioEpochMs,cfg.rampa*1000);assert.equal(b.finSostenidoEpochMs-b.finRampaEpochMs,cfg.sostenido*1000);
 const data=rows(path.join(dir,'e6.jtl'));const required='timeStamp elapsed label responseCode responseMessage threadName success failureMessage bytes sentBytes grpThreads allThreads URL Latency IdleTime Connect e6Hilo e6Iteracion e6Paso e6TipoFallo e6ContenidoValido e6Umbral e6CicloAdmitidoEpochMs'.split(' ');
 assert(required.every(x=>data.head.includes(x)),'Columnas JTL');const mm=[];const threads=new Map();
 for(const x of data){const label=fake?x.label.replace(/^FICTICIO /,''):x.label,k=labels.indexOf(label),s=Number(x.timeStamp),ms=Number(x.elapsed),step=Number(x.e6Paso),cycle=Number(x.e6Iteracion),h=Number(x.e6Hilo);
 assert(k>=0&&(!fake?x.URL===routes[k]:true),'Solo GET medidos');assert(['ninguno','HTTP','Transporte','Contenido'].includes(x.e6TipoFallo));assert(h>=1&&h<=cfg.usuarios&&cycle>=1&&step>=1&&step<=5&&sequence[step-1]===k,'Identidad/secuencia');
 assert(Number(x.e6Umbral)===(k===2?300:800),'Umbral');assert(s>=b.inicioEpochMs&&Number.isFinite(ms)&&ms>=0);assert(Number(x.e6CicloAdmitidoEpochMs)<b.finSostenidoEpochMs,'Ciclo admitido tras plazo');
 const r={start:s,end:s+ms,ms,k,h,cycle,step,admitted:Number(x.e6CicloAdmitidoEpochMs),code:x.responseCode,message:x.responseMessage,failure:x.e6TipoFallo,valid:x.e6ContenidoValido==='true',success:x.success==='true',limit:Number(x.e6Umbral),active:Number(x.allThreads)};
 assert(Number.isFinite(r.admitted)&&r.admitted>=b.inicioEpochMs&&r.admitted<=r.start,'Admision de ciclo incoherente');
 assert.equal(r.success,r.code==='200'&&r.valid&&r.ms<=r.limit,'Resultado vs HTTP/contenido/duracion');
 mm.push(r);if(!threads.has(h))threads.set(h,[]);threads.get(h).push(r);}
 assert(mm.length>0&&threads.size===cfg.usuarios,'Hilos/muestras');const threadRows=[];
 for(const [h,list]of threads){list.sort((a,b)=>a.start-b.start||a.cycle-b.cycle||a.step-b.step);assert.equal(list.length%5,0,'Ciclo incompleto');const keys=new Set();for(const r of list){keys.add(r.cycle+':'+r.step);}assert.equal(keys.size,list.length);const cycles=list.length/5;
 for(let c=1;c<=cycles;c++){
  const group=list.filter(r=>r.cycle===c).sort((a,b)=>a.step-b.step);
  for(let p=1;p<=5;p++)assert(keys.has(c+':'+p),'Ciclos consecutivos');
  assert(group.every(r=>r.admitted===group[0].admitted),'Admision distinta dentro del ciclo');
  for(let i=1;i<group.length;i++)assert(group[i].start>=group[i-1].end,'Orden/solapamiento dentro de hilo');
  if(group.some(r=>r.start>=b.finSostenidoEpochMs))assert.equal(c,cycles,'Ciclo nuevo despues del cierre');
 }
 for(let i=1;i<list.length;i++)assert(list[i].start>=list[i-1].end,'Solapamiento entre ciclos de un hilo');
 threadRows.push({hilo:h,ciclos:cycles,muestras:list.length,primerInicio:Math.min(...list.map(r=>r.start)),ultimoFin:Math.max(...list.map(r=>r.end))});}
 assert.equal(mm.filter(r=>r.k===0).length,mm.length*.4);assert.equal(mm.filter(r=>r.k===1).length,mm.length*.4);
 const closes=read(path.join(dir,'hilos-tiempo.json'));assert.equal(closes.hilos.length,cfg.usuarios,'Cierres');for(const h of closes.hilos){assert(h.finLocalEpochMs>=b.finSostenidoEpochMs,'Hilo termino antes del plazo');assert.equal(h.muestras,threads.get(h.hilo).length);}
 let first=Infinity,last=-Infinity;for(const r of mm){first=Math.min(first,r.start);last=Math.max(last,r.end);}
 const windows=[['rampa',b.inicioEpochMs,b.finRampaEpochMs],['sostenido',b.finRampaEpochMs,b.finSostenidoEpochMs],['cierre',b.finSostenidoEpochMs,Math.max(last,b.finSostenidoEpochMs)]];
 const rec=fake?[]:[...rows(path.join(dir,'recursos.csv'))];const periods=[];
 for(const [name,a,z]of windows){const rr=mm.filter(r=>phase(r.start,b)===name),received=mm.filter(r=>r.end>=a&&(name==='cierre'?r.end<=z:r.end<z)),sec=(z-a)/1000;
 const per=labels.map((label,k)=>({endpoint:label,proporcionPorcentaje:rr.length?100*rr.filter(r=>r.k===k).length/rr.length:null,...stats(rr.filter(r=>r.k===k),sec,received.filter(r=>r.k===k)),HTTPEnCurso:overlap(mm.filter(r=>r.k===k),a,z)}));
 const recursos=['backend-perf','db-perf'].map(service=>{const rs=rec.filter(r=>r.servicio===service&&Number(r.epochMs)>=a&&Number(r.epochMs)<z);return {servicio:service,capturas:rs.length,maxCPUMuestreada:rs.length?Math.max(...rs.map(r=>Number(r.cpuPct))):null,maxMemoriaMiB:rs.length?Math.max(...rs.map(r=>memory(r.memoriaUsada))):null};});
 periods.push({periodo:name,inicioEpochMs:a,finEpochMs:z,duracionSeg:sec,...stats(rr,sec,received),porEndpoint:per,maxHilosActivosObservados:rr.length?rr.reduce((v,r)=>Math.max(v,r.active),0):null,HTTPEnCurso:overlap(mm,a,z),recursos});}
 const timeline=[];for(let a=b.inicioEpochMs;a<last;a+=10000){const z=Math.min(a+10000,last),rr=mm.filter(r=>r.start>=a&&r.start<z);timeline.push({inicioRelSeg:(a-b.inicioEpochMs)/1000,finRelSeg:(z-b.inicioEpochMs)/1000,muestras:rr.length,fallidas:rr.filter(r=>!r.success).length,maxHilosActivos:rr.length?rr.reduce((v,r)=>Math.max(v,r.active),0):null,HTTPEnCurso:overlap(mm,a,z)});}
 const times=rec.filter(r=>r.servicio==='backend-perf').map(r=>Number(r.epochMs)),gaps=times.slice(1).map((t,i)=>t-times[i]);
 const gruposRecursos=new Map();
 for(const r of rec){if(!gruposRecursos.has(r.epochMs))gruposRecursos.set(r.epochMs,[]);gruposRecursos.get(r.epochMs).push(r.servicio);}
 const paresIncompletos=[...gruposRecursos].filter(([,servicios])=>servicios.length!==2||new Set(servicios).size!==2).map(([epochMs,servicios])=>({epochMs:Number(epochMs),servicios}));
 const coberturaRecursos=['backend-perf','db-perf'].map(servicio=>{
  const ts=rec.filter(r=>r.servicio===servicio).map(r=>Number(r.epochMs));
  const first=ts.length?Math.min(...ts):null,last=ts.length?Math.max(...ts):null;
  return {servicio,capturas:ts.length,primeraEpochMs:first,ultimaEpochMs:last,ultimaRelativaSeg:last===null?null:(last-b.inicioEpochMs)/1000,sinCapturasHastaFinSostenidoSeg:last===null?null:Math.max(0,(b.finSostenidoEpochMs-last)/1000)};
 });
 const nombres=['rampa','sostenido','cierre'];
 const cruces=nombres.map(inicio=>{
  const counts=predicate=>Object.fromEntries(nombres.map(fin=>[fin,mm.filter(r=>phase(r.start,b)===inicio&&phase(r.end,b)===fin&&predicate(r)).length]));
  return {inicio,finales:counts(()=>true),finalesConformes:counts(r=>r.code==='200'&&r.valid&&r.ms<=r.limit),finalesSobreUmbral:counts(r=>r.ms>r.limit)};
 });
 assert.equal(periods.reduce((sum,p)=>sum+p.muestras,0),mm.length,'Conciliacion de inicios');
 assert.equal(periods.reduce((sum,p)=>sum+p.tasas.HTTP200Recibidos.numerador,0),mm.filter(r=>r.code==='200').length,'Conciliacion de HTTP200 recibidos');
 assert.equal(periods.reduce((sum,p)=>sum+p.tasas.contenidoYDuracionRecibidos.numerador,0),mm.filter(r=>r.code==='200'&&r.valid&&r.ms<=r.limit).length,'Conciliacion de respuestas conformes');
 const duracionPorEndpoint=labels.map((endpoint,k)=>({endpoint,porFase:Object.fromEntries(periods.map(p=>[p.periodo,p.porEndpoint[k].sobreUmbral])),total:mm.filter(r=>r.k===k&&r.ms>r.limit).length}));
 for(const e of duracionPorEndpoint)assert.equal(Object.values(e.porFase).reduce((a,b)=>a+b,0),e.total,'Conciliacion duracion por endpoint');
 const duracionPorFase=Object.fromEntries(periods.map(p=>{
  assert.equal(p.porEndpoint.reduce((sum,e)=>sum+e.sobreUmbral,0),p.sobreUmbral,'Conciliacion duracion por fase');
  return [p.periodo,p.sobreUmbral];
 }));
 const totalDuracion=mm.filter(r=>r.ms>r.limit).length;
 assert.equal(Object.values(duracionPorFase).reduce((a,b)=>a+b,0),totalDuracion,'Conciliacion duracion global');
 const out={versionAnalisis:2,ficticio:fake,fase:cfg.usuarios,tiempos:b,
  recursosValidacionPosterior:{filas:rec.length,paresCompletos:gruposRecursos.size-paresIncompletos.length,paresIncompletos,cobertura:coberturaRecursos,cierreRecolectorValidado:fs.existsSync(path.join(dir,'validacion-recursos.json'))},
  conciliacionDuracion:{valida:true,regla:'Fase asignada por inicio de la peticion; exceso si elapsed > umbral del endpoint.',porEndpoint:duracionPorEndpoint,porFase:duracionPorFase,total:totalDuracion},
  definiciones:{iniciadas:'Numero de muestras con inicio en la ventana / segundos de ventana.',HTTP200Recibidos:'Numero de muestras HTTP 200 con fin en la ventana / segundos de ventana, sin exigir contenido ni duracion.',contenidoYDuracionRecibidos:'Numero de muestras HTTP 200 con contenido valido y elapsed <= umbral del endpoint, con fin en la ventana / segundos de ventana.',ventanas:'Rampa y sostenido [inicio,fin); cierre [finSostenido,ultimoFin] incluye el ultimo final. Fin = timeStamp + elapsed; no incluye trabajo posterior del postprocesador.',percentiles:'Rango mas cercano, ceil(p*n), sobre elapsed de peticiones iniciadas en cada ventana; no son percentiles de los finales recibidos.'},
  conciliacion:{valida:true,total:mm.length,iniciosPorVentana:Object.fromEntries(periods.map(p=>[p.periodo,p.muestras])),crucesInicioFin:cruces,cierre:{muestras:mm.filter(r=>r.start>=b.finSostenidoEpochMs).length,hilos:new Set(mm.filter(r=>r.start>=b.finSostenidoEpochMs).map(r=>r.h)).size,porPaso:Object.fromEntries([1,2,3,4,5].map(p=>[p,mm.filter(r=>r.start>=b.finSostenidoEpochMs&&r.step===p).length])),todosEnUltimoCicloAdmitidoAntesDelPlazo:true,drenajeMs:last-b.finSostenidoEpochMs}},
  reglaClasificacion:'Cohortes de latencia/fallos por inicio; tasas de respuestas por fin. Las muestras que cruzan limites no se mueven de cohorte de inicio ni se duplican.',
  ventanaHTTPseg:(last-first)/1000,primeraMuestraEpochMs:first,ultimaFinalEpochMs:last,...stats(mm,(last-first)/1000,mm),periodos:periods,evolucion10s:timeline,hilos:threadRows.sort((a,b)=>a.hilo-b.hilo),
  maxHilosActivos:mm.reduce((v,r)=>Math.max(v,r.active),0),HTTPEnCursoGlobal:overlap(mm,first,last),fallosContenido:mm.filter(r=>r.failure==='Contenido').length,
  recursosCapturasPorServicio:Object.fromEntries(coberturaRecursos.map(r=>[r.servicio,r.capturas])),cadenciaMs:gaps.length?{min:Math.min(...gaps),max:Math.max(...gaps),media:gaps.reduce((a,b)=>a+b,0)/gaps.length,intervalos:gaps.length}:null,
  advertencias:['Categorias pueden solaparse: fallidas cuenta muestras unicas.','Timeout = tiempo hasta abandonar espera, no final de servidor.','CPU maxima muestreada no es pico absoluto; memoria de contenedor no es heap.','Hilos y solapamiento JMeter no prueban ejecucion simultanea en servidor.']};
 save(dir,fake?'analisis-ficticio.json':'analisis-sostenido-v2.json',out);return out;}
function selftest(){const b={inicioEpochMs:0,finRampaEpochMs:1000,finSostenidoEpochMs:3000};assert.equal(phase(999,b),'rampa');assert.equal(phase(1000,b),'sostenido');assert.equal(phase(2999,b),'sostenido');assert.equal(phase(3000,b),'cierre');
 const r={start:999,end:1001,ms:2,code:'200',valid:true,success:true,failure:'ninguno',limit:800};assert.equal(phase(r.start,b),'rampa');assert.equal(phase(r.end,b),'sostenido');assert.equal(stats([r],1,[]).throughputHTTP200ValidosRecibidos,0);assert.equal(stats([],2,[r]).throughputHTTP200ValidosRecibidos,.5);
 const slow={...r,ms:801},invalid={...r,valid:false},httpError={...r,code:'500'};
 const rates=stats([r],2,[r,slow,invalid,httpError]).tasas;
 assert.deepEqual(rates.iniciadas,{numerador:1,denominadorSegundos:2,porSegundo:.5});
 assert.equal(rates.HTTP200Recibidos.numerador,3);assert.equal(rates.contenidoYDuracionRecibidos.numerador,1);
 const atLimit={...r,ms:300,limit:300},overLimit={...r,ms:301,limit:300};
 assert.equal(stats([],1,[atLimit,overLimit]).tasas.contenidoYDuracionRecibidos.numerador,1);
 assert.equal(stats([],0,[]).tasas.HTTP200Recibidos.porSegundo,null);
 assert.equal(overlap([{start:0,end:10},{start:10,end:20}],0,20).max,1);return {limitesPorInicio:true,recepcionPorFin:true,finalesAntesDeInicios:true,HTTP:0};}
module.exports={analyze,phase,stats,overlap,selftest};
if(require.main===module){try{if(process.argv.includes('--selftest'))console.log(JSON.stringify(selftest()));else{const r=analyze(process.argv[2],process.argv.includes('--ficticio'));console.log(JSON.stringify({muestras:r.muestras,fallidas:r.fallidas,periodos:r.periodos.map(p=>({periodo:p.periodo,muestras:p.muestras,fallidas:p.fallidas})),ficticio:r.ficticio}));}}catch(e){console.error('E6 analisis detenido: '+e.message);process.exitCode=1;}}
