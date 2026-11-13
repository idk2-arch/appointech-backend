'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const {csv}=require('./analizar-e7.js');
const read=f=>JSON.parse(fs.readFileSync(f,'utf8').replace(/^\uFEFF/,''));
function integrate(rows,a,b){
 const events=[];for(const r of rows){const s=Math.max(a,+r.timeStamp),e=Math.min(b,+r.timeStamp+(+r.elapsed));if(e>s){events.push([s,1],[e,-1]);}}
 events.sort((x,y)=>x[0]-y[0]||x[1]-y[1]);let n=0,max=0,t=a,area=0;const histogram={};
 for(const [at,d]of events){const dt=at-t;area+=n*dt;histogram[n]=(histogram[n]??0)+dt;n+=d;assert(n>=0);max=Math.max(max,n);t=at;}
 histogram[n]=(histogram[n]??0)+b-t;assert.equal(n,0);
 return {max,media:b>a?area/(b-a):null,areaMs:area,porcentajeTiempoConAlMenos:Object.fromEntries([1,2,10,25,50,75,100].map(k=>[k,100*Object.entries(histogram).filter(([n])=>+n>=k).reduce((s,[,v])=>s+v,0)/(b-a)]))};
}
function audit(dir,out=dir){
 const rows=csv(path.join(dir,'e7.jtl'));const cfg=read(path.join(dir,'configuracion.json'));
 assert(rows.length);let first=Infinity,last=-Infinity,total=0;for(const r of rows){first=Math.min(first,+r.timeStamp);last=Math.max(last,+r.timeStamp+(+r.elapsed));total+=+r.elapsed;}
 const all=integrate(rows,first,last);assert.equal(all.areaMs,total,'Area HTTP vs elapsed acumulado');
 const byThread=new Map();for(const r of rows){if(!byThread.has(r.e7Hilo))byThread.set(r.e7Hilo,[]);byThread.get(r.e7Hilo).push(r);}
 let gap=0,spans=0;for(const list of byThread.values()){list.sort((a,b)=>+a.timeStamp-+b.timeStamp||+a.e7Ciclo-+b.e7Ciclo||+a.e7Paso-+b.e7Paso);spans+=+list.at(-1).timeStamp+(+list.at(-1).elapsed)-list[0].timeStamp;
 for(let i=1;i<list.length;i++){const dt=+list[i].timeStamp-(+list[i-1].timeStamp+(+list[i-1].elapsed));assert(dt>=0);gap+=dt;}}
 assert.equal(total+gap,spans);
 const timeline=[];for(let a=first;a<last;a+=5000){const b=Math.min(a+5000,last),starts=rows.filter(r=>+r.timeStamp>=a&&+r.timeStamp<b),received=rows.filter(r=>+r.timeStamp+(+r.elapsed)>=a&&(b===last?+r.timeStamp+(+r.elapsed)<=b:+r.timeStamp+(+r.elapsed)<b));timeline.push({desdeSeg:(a-first)/1000,hastaSeg:(b-first)/1000,iniciadas:starts.length,recibidas:received.length,iniciadasPorSegundo:starts.length/((b-a)/1000),recibidasPorSegundo:received.length/((b-a)/1000),HTTPEnCurso:integrate(rows,a,b)});}
 const results=fs.readdirSync(dir).filter(f=>/^hilo-\d+\.jsonl$/.test(f)).flatMap(f=>fs.readFileSync(path.join(dir,f),'utf8').trim().split(/\r?\n/).filter(Boolean).map(JSON.parse)).filter(x=>x.evento==='resultado');
 const index=new Map(results.map(r=>[`${r.hilo}:${r.ciclo}:${r.paso}`,r]));let post=0,postWrites=0;
 for(const r of rows){const event=index.get(`${r.e7Hilo}:${r.e7Ciclo}:${r.e7Paso}`);assert(event);const dt=event.epochMs-(+r.timeStamp+(+r.elapsed));assert(dt>=-2);post+=Math.max(0,dt);if([1,3,5].includes(+r.e7Paso))postWrites+=Math.max(0,dt);}
 const seconds=(last-first)/1000;
 const o={usuarios:cfg.usuarios,inicioEpochMs:first,finEpochMs:last,ventanaSeg:seconds,muestras:rows.length,
  throughputHTTP:{numerador:rows.length,denominadorSeg:seconds,porSegundo:rows.length/seconds,definicion:'Muestras HTTP finalizadas entre primer inicio y ultimo fin; incluye 201/200/204 y cualquier fallo medido.'},
  throughputHTTP200:{numerador:rows.filter(r=>r.responseCode==='200').length,denominadorSeg:seconds,porSegundo:rows.filter(r=>r.responseCode==='200').length/seconds},
  maxHilosActivos:Math.max(...rows.map(r=>+r.allThreads)),HTTPEnCurso:all,evolucion5s:timeline,
  tiemposAcumuladosSeg:{HTTP:total/1000,intervalosNoHTTPEntreMuestras:gap/1000,ventanasPorHilo:spans/1000,postHTTPHastaDiario:post/1000,postHTTPDeEscriturasHastaDiario:postWrites/1000},
  interpretacion:'elapsed mide solo el sampler HTTP. Los huecos incluyen validacion, espera en cola, SQL read-only, IPC Docker, diario y planificacion. El diario permite medir el postprocesamiento completo, no aislar tiempo puro de SQL. Sumas por hilos no son segundos de reloj.',
  juicio:'Hay concurrencia HTTP real si la media y la distribucion temporal lo muestran; no confundir hilos vivos con HTTP simultaneas ni esta carga instrumentada con saturacion maxima del backend.'};
 fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,'concurrencia-e7.json'),JSON.stringify(o,null,2));
 let text=`# E7 ${cfg.usuarios}: concurrencia del JTL existente\n\nVentana: ${seconds} s; ${rows.length}/${seconds} = ${(rows.length/seconds).toFixed(3)} HTTP/s. HTTP 200: ${o.throughputHTTP200.numerador}/${seconds} = ${o.throughputHTTP200.porSegundo.toFixed(3)}/s.\n\nHilos activos maximos: ${o.maxHilosActivos}; HTTP simultaneas maximas: ${all.max}, media temporal: ${all.media.toFixed(3)}. Distribucion (% del tiempo): ${JSON.stringify(all.porcentajeTiempoConAlMenos)}.\n\n${o.interpretacion}\n\nTiempos acumulados: ${JSON.stringify(o.tiemposAcumuladosSeg)}.\n\n| Ventana s | Iniciadas/s | Recibidas/s | HTTP media | HTTP maxima |\n|---|---:|---:|---:|---:|\n`;
 for(const p of timeline)text+=`| ${p.desdeSeg}–${p.hastaSeg} | ${p.iniciadasPorSegundo.toFixed(3)} | ${p.recibidasPorSegundo.toFixed(3)} | ${p.HTTPEnCurso.media.toFixed(3)} | ${p.HTTPEnCurso.max} |\n`;
 text+='\nHay que evaluar la media y la evolucion, no solo el pico. La conciliacion reduce el ritmo y produce rafagas; no se presenta como una prueba sin instrumentacion ni una medida aislada de saturacion.\n';
 fs.writeFileSync(path.join(out,'CONCURRENCIA.md'),text);return o;
}
module.exports={audit,integrate};
if(require.main===module)try{const r=audit(process.argv[2],process.argv[3]);console.log(JSON.stringify({ventanaSeg:r.ventanaSeg,throughput:r.throughputHTTP,hilos:r.maxHilosActivos,solapamiento:r.HTTPEnCurso,tiempos:r.tiemposAcumuladosSeg},null,2));}catch(e){console.error(e.message);process.exitCode=1;}
