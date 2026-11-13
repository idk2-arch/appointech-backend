'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const nombres=['crear','consultar_creada','editar','consultar_editada','borrar','verificar_ausencia'];
const read=f=>JSON.parse(fs.readFileSync(f,'utf8').replace(/^\uFEFF/,''));
const jsonl=f=>fs.readFileSync(f,'utf8').replace(/^\uFEFF/,'').trim().split(/\r?\n/).filter(Boolean).map(l=>JSON.parse(l));
function csv(file){let value='',row=[],quoted=false,all=[];const s=fs.readFileSync(file,'utf8').replace(/^\uFEFF/,'');
 for(let i=0;i<s.length;i++){const c=s[i];if(c==='"'){if(quoted&&s[i+1]==='"'){value+='"';i++;}else quoted=!quoted;}
 else if(c===','&&!quoted){row.push(value);value='';}else if(c==='\n'&&!quoted){row.push(value.replace(/\r$/,''));value='';if(row.length>1)all.push(row);row=[];}else value+=c;}
 if(value||row.length){row.push(value);all.push(row);}assert(!quoted,'CSV truncado');const head=all.shift();assert(head,'CSV sin cabecera');
 return all.map(r=>{assert.equal(r.length,head.length,'Columnas CSV');return Object.fromEntries(head.map((h,i)=>[h,r[i]]));});
}
function stats(rr){const times=rr.map(r=>+r.elapsed).sort((a,b)=>a-b);const q=p=>times.length?times[Math.ceil(p*times.length)-1]:null;
 return {muestras:rr.length,codigos:Object.fromEntries([...new Set(rr.map(r=>r.responseCode))].map(c=>[c,rr.filter(r=>r.responseCode===c).length])),
 contenidoValido:rr.filter(r=>r.e7Contenido==='true').length,fallosContenido:rr.filter(r=>r.e7TipoFallo==='Contenido').length,
 fallosIntegridad:rr.filter(r=>r.e7TipoFallo==='Integridad').length,fallidas:rr.filter(r=>r.success!=='true').length,
 escriturasInciertas:rr.filter(r=>r.e7Incierta==='true').length,timeouts:rr.filter(r=>/TimeoutException|timed out/i.test(r.responseCode+' '+r.responseMessage)).length,
 transporte:rr.filter(r=>r.e7TipoFallo==='Transporte').length,duracionMs:{media:times.length?times.reduce((a,b)=>a+b,0)/times.length:null,p95:q(.95),p99:q(.99),max:times.at(-1)??null},
 umbralDuracionCRUD:null,excesosDuracion:null,notaUmbral:'Excesos no evaluables: el contrato E7 no define un SLA de duracion; no se modifican ni se heredan umbrales de otros endpoints.'};
}
function overlap(rr){const events=rr.filter(r=>+r.elapsed>0).flatMap(r=>[[+r.timeStamp,1],[+r.timeStamp+(+r.elapsed),-1]]).sort((a,b)=>a[0]-b[0]||a[1]-b[1]);let n=0,max=0;for(const [,d]of events){n+=d;max=Math.max(max,n);}return max;}
function analyze(dir){const cfg=read(path.join(dir,'configuracion.json')),end=read(path.join(dir,'motor-final.json'));
 const rows=csv(path.join(dir,'e7.jtl'));assert(rows.length,'JTL vacio');const seen=new Map(),createdIds=new Set();
 for(const r of rows){const h=+r.e7Hilo,c=+r.e7Ciclo,p=+r.e7Paso;assert(h>=1&&h<=cfg.usuarios&&c>=1&&c<=20&&p>=1&&p<=6);
 assert.equal(r.label,'E7 '+nombres[p-1]);assert.equal(+r.e7Tecnico,+cfg.tecnicos[h-1]);assert(+r.elapsed>=0);
 const date=new Date(cfg.baseFecha+'T00:00:00Z');date.setUTCDate(date.getUTCDate()+20*Math.floor((h-1)/50)+c-1);assert.equal(r.e7Fecha,date.toISOString().slice(0,10));
 const key=`${h}:${c}`;if(!seen.has(key))seen.set(key,[]);const list=seen.get(key);assert.equal(p,list.length+1,'Pasos dependientes fuera de orden');
 if(r.e7Id){assert(/^[1-9][0-9]*$/.test(r.e7Id));if(p===1){assert(!createdIds.has(r.e7Id),'ID reutilizado');createdIds.add(r.e7Id);}else assert.equal(r.e7Id,list[0].e7Id,'ID de otro ciclo');}
 if(list.length)assert(+r.timeStamp>=+list.at(-1).timeStamp+(+list.at(-1).elapsed),'Orden temporal');list.push(r);
 }
 const diarios=fs.readdirSync(dir).filter(f=>/^hilo-\d+\.jsonl$/.test(f)).flatMap(f=>jsonl(path.join(dir,f)));
 const results=diarios.filter(x=>x.evento==='resultado'),skipped=diarios.filter(x=>x.evento==='omitida');assert.equal(results.length,rows.length);
 const unique=new Set(diarios.filter(x=>['resultado','omitida'].includes(x.evento)).map(x=>`${x.hilo}:${x.ciclo}:${x.paso}`));
 assert.equal(unique.size,cfg.usuarios*20*6,'Faltan pasos, realizados u omitidos');assert.equal(rows.length+skipped.length,unique.size);
 for(const r of rows){const journal=results.find(x=>x.hilo===+r.e7Hilo&&x.ciclo===+r.e7Ciclo&&x.paso===+r.e7Paso);assert(journal);assert.equal(journal.codigo,r.responseCode);assert.equal(journal.id??'',r.e7Id);}
 assert.equal(end.hilos.length,cfg.usuarios);for(const h of end.hilos){assert.equal(h.ciclos,20);assert.equal(h.pasos+h.omitidos,120);}
 const checks=jsonl(path.join(dir,'conciliacion-sql.jsonl'));
 const checkKeys=new Set();
 for(const check of checks){assert([1,3,5].includes(check.paso));const key=`${check.hilo}:${check.ciclo}:${check.paso}`;assert(!checkKeys.has(key),'Checkpoint duplicado');checkKeys.add(key);const row=rows.find(r=>+r.e7Hilo===check.hilo&&+r.e7Ciclo===check.ciclo&&+r.e7Paso===check.paso);assert(row&&row.e7Id===check.id);}
 const complete=[...seen.values()].filter(rr=>rr.length===6&&rr.every(r=>r.success==='true'&&r.e7Contenido==='true')).length;
 if(!end.aborto){assert.equal(rows.length,cfg.usuarios*120);assert.equal(complete,cfg.usuarios*20);assert.equal(checks.length,cfg.usuarios*20*3);assert(rows.every(r=>r.e7Incierta==='false'));for(const [key]of seen)for(const p of [1,3,5])assert(checkKeys.has(`${key}:${p}`),'Falta checkpoint de ciclo');}
 if(cfg.falloPaso){assert(end.aborto);const fault=rows.find(r=>+r.e7Paso===cfg.falloPaso);assert(fault&&fault.e7Incierta==='true');assert.equal(rows.length,cfg.falloPaso);assert.equal(skipped.length,120-cfg.falloPaso);}
 if(cfg.falloSinId){assert(end.aborto);assert.equal(rows.length,1);assert.equal(rows[0].e7Id,'');assert.equal(skipped.length,119);}
 let start=Infinity,last=-Infinity;for(const r of rows){start=Math.min(start,+r.timeStamp);last=Math.max(last,+r.timeStamp+(+r.elapsed));}
 const operations=nombres.map((operacion,i)=>({operacion,...stats(rows.filter(r=>+r.e7Paso===i+1)),omitidos:skipped.filter(x=>x.paso===i+1).length}));
 const out={version:1,ficticio:cfg.ficticio,usuarios:cfg.usuarios,ciclosPorHilo:20,cuentaAdministrativaCompartida:true,tecnicosDistintos:new Set(cfg.tecnicos).size,hilosPorTecnico:cfg.usuarios/new Set(cfg.tecnicos).size,
  ...stats(rows),omitidos:skipped.length,ciclosCompletos:complete,ciclosEsperados:cfg.usuarios*20,aborto:end.aborto,motivo:end.motivo,
  checkpointsSQL:checks.length,checkpointsInvalidos:checks.filter(c=>c.valido!==true).length,propiedadYEdicionVerificadasEnSQL:checks.filter(c=>c.valido===true&&(c.paso===1||c.paso===3)).length,
  ausenciaVerificadaEnSQL:checks.filter(c=>c.paso===5&&c.valido===true).length,observadorCerrado:end.observadorCerrado,
  ventanaHTTPseg:(last-start)/1000,maxHilosActivos:Math.max(...rows.map(r=>+r.allThreads)),maxHTTPEnCurso:overlap(rows),porOperacion:operations,
  instrumentacion:'Checkpoints SQL de solo lectura entre pasos: el elapsed del sampler es HTTP; la ventana total y el ritmo incluyen las esperas SQL. No representa throughput CRUD sin instrumentacion.',
  reintentosEscritura:0,privacidad:'Sin cuerpos, tokens ni cabeceras persistidos.'};
 fs.writeFileSync(path.join(dir,'analisis-e7.json'),JSON.stringify(out,null,2));return out;
}
function report(dir){const j=read(path.join(dir,'analisis-e7.json'));let s=`# E7 CRUD: ${j.usuarios} hilos, 20 ciclos\n\nUna sola cuenta administrativa compartida y ${j.tecnicosDistintos} tecnicos distintos. ${j.instrumentacion}\n\n`;
 s+=`Muestras: ${j.muestras}; omitidas: ${j.omitidos}; ciclos completos: ${j.ciclosCompletos}/${j.ciclosEsperados}; aborto: ${j.aborto}. Escrituras inciertas: ${j.escriturasInciertas}; reintentos de escritura: 0.\n\n`;
 s+='| Operacion | Muestras | Codigos | Contenido valido | Fallidas | Media ms | p95 ms | p99 ms | Timeouts | Inciertas | Omitidas |\n|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|\n';
 for(const p of j.porOperacion)s+=`| ${p.operacion} | ${p.muestras} | ${JSON.stringify(p.codigos)} | ${p.contenidoValido} | ${p.fallidas} | ${p.duracionMs.media?.toFixed(2)??'-'} | ${p.duracionMs.p95??'-'} | ${p.duracionMs.p99??'-'} | ${p.timeouts} | ${p.escriturasInciertas} | ${p.omitidos} |\n`;
 s+=`\nNo se ha definido ni alterado un umbral SLA de CRUD: se informan duraciones, no se reutilizan los umbrales de lecturas E6. Se mantienen conexion 5000 ms y respuesta 10000 ms.\n\nSQL: ${j.checkpointsSQL} checkpoints; ${j.propiedadYEdicionVerificadasEnSQL} verificaciones de propietario/estado creado o editado; ${j.ausenciaVerificadaEnSQL} ausencias tras DELETE. Hilos activos maximos: ${j.maxHilosActivos}; HTTP solapadas maximas: ${j.maxHTTPEnCurso}.\n\n`;
 for(const f of ['conciliacion-final.json','integridad.json','integridad-verificada.json','validacion-recursos.json','recursos-resumen.json','diagnostico.json','recuperacion.json','privacidad.json'])if(fs.existsSync(path.join(dir,f)))s+=`## ${f}\n\n\`\`\`json\n${JSON.stringify(read(path.join(dir,f)),null,2)}\n\`\`\`\n\n`;
 s+='Los logs de auditoria y los avances de secuencia no se comparan como tablas de negocio inmutables. No se borran datos previos ni residuos de propiedad incierta. Detener tras la fase autorizada; no ejecutar otra fase ni E8. Excesos de duracion CRUD: no evaluables sin un SLA acordado; no se inventan umbrales.\n';
 fs.writeFileSync(path.join(dir,'INFORME.md'),s);
}
module.exports={analyze,report,csv};
if(require.main===module)try{const j=analyze(process.argv[2]);report(process.argv[2]);console.log(JSON.stringify({muestras:j.muestras,omitidos:j.omitidos,ciclosCompletos:j.ciclosCompletos,aborto:j.aborto,ficticio:j.ficticio}));}catch(e){console.error('E7 analisis: '+e.message);process.exitCode=1;}
