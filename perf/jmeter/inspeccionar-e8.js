'use strict';
// Preparacion exclusivamente local: no importa clientes HTTP, no llama JMeter ni Docker.
const fs=require('fs'),path=require('path'),assert=require('assert');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
function inspect({config,tokens,paquete,etapa='Preparacion',salida,exigir=false}){
 const c=read(config);assert.equal(c.usuarios,50);assert.equal(c.destino,'http://localhost:18080');assert.equal(c.tiemposSeg.medicion,600);assert.equal(c.tiemposSeg.calentamiento,60);
 assert.equal(c.ritmo.periodoPorHiloMs,5000);assert.equal(c.ritmo.desfaseEntreHilosMs,100);assert.equal(c.ritmo.objetivoHTTPPorSegundoGlobal,10);assert.equal(c.ritmo.recuperarTurnosPerdidos,false);assert.equal(c.ritmo.reintentos,0);
 assert.deepEqual(c.endpoints.map(x=>[x.nombre,x.umbralMs,x.metodo]),[['notificaciones',800,'GET'],['historial',800,'GET'],['especialidades',300,'GET']]);
 assert.equal(c.timeoutConexionMs,5000);assert.equal(c.timeoutRespuestaMs,10000);
 const slots=[],totales={},porMinuto=[];
 for(const [fase,duracion]of [['calentamiento',c.tiemposSeg.calentamiento],['medicion',c.tiemposSeg.medicion]]){
  const counts={notificaciones:0,historial:0,especialidades:0};
  for(let k=0;k<duracion*1000/5000;k++)for(let h=1;h<=50;h++){
   const t=k*5000+(h-1)*100,endpoint=c.ritmo.secuencia[(k+(h-1)%5)%5];assert(t<duracion*1000);counts[endpoint]++;
   slots.push({fase,hilo:h,turno:k,offsetMs:t,endpoint});
  }
  totales[fase]=counts;
 }
 for(let minute=0;minute<10;minute++){
  const rows=slots.filter(s=>s.fase==='medicion'&&s.offsetMs>=minute*60000&&s.offsetMs<(minute+1)*60000);
  const count=Object.fromEntries(c.endpoints.map(e=>[e.nombre,rows.filter(r=>r.endpoint===e.nombre).length]));assert.deepEqual(count,{notificaciones:240,historial:240,especialidades:120});porMinuto.push({minuto:minute+1,muestras:rows.length,porEndpoint:count});
 }
 assert.equal(slots.filter(x=>x.fase==='medicion').length,6000);assert.equal(new Set(slots.map(x=>`${x.fase}:${x.offsetMs}`)).size,6600);
 const t=c.tiemposSeg;
 const budgets={Preparacion:t.preparacionMax+t.arranqueLocal+t.calentamiento+t.medicion+t.drenajeMax+t.recuperacionMax+t.margenToken,Calentamiento:t.calentamiento+t.medicion+t.drenajeMax+t.recuperacionMax+t.margenToken,Medicion:t.medicion+t.drenajeMax+t.recuperacionMax+t.margenToken};assert(etapa in budgets);
 const now=Date.now()/1000;let input=[];
 if(paquete){const p=read(paquete);assert(Array.isArray(p.identidades));input=p.identidades.map(i=>i.token);}
 else if(tokens&&fs.existsSync(tokens)){const lines=fs.readFileSync(tokens,'utf8').replace(/^\uFEFF/,'').trim().split(/\r?\n/);assert.equal(lines.shift(),'token');input=lines.filter(Boolean).map(x=>x.trim().replace(/^"|"$/g,''));}
 const decoded=[];let invalid=0;
 for(const token of input){try{
  assert(typeof token==='string');const pieces=token.split('.');assert.equal(pieces.length,3);assert(pieces[2]);
  const header=JSON.parse(Buffer.from(pieces[0],'base64url').toString('utf8')),claim=JSON.parse(Buffer.from(pieces[1],'base64url').toString('utf8'));
  assert(['HS256','HS384','HS512'].includes(header.alg));assert(Number.isSafeInteger(claim.exp)&&claim.exp>0&&claim.exp<253402300800);assert(Number.isSafeInteger(claim.iat));
  const m=/^cliente([1-9][0-9]*)@perf[.]test$/.exec(claim.sub??'');decoded.push({ordinal:m?+m[1]:null,rol:claim.rol,id:claim.id,exp:claim.exp,iat:claim.iat,nbf:claim.nbf??null});
 }catch{invalid++;}}
 const selected=decoded.filter(x=>x.rol==='CLIENTE'&&x.ordinal>=1&&x.ordinal<=50),unique=new Set(selected.map(x=>x.ordinal));
 const good=selected.filter(x=>x.exp>=now+budgets[etapa]&&x.iat<=now&&(!x.nbf||x.nbf<=now)&&Number.isSafeInteger(x.id)&&x.id>0);
 const tokenReport={fechaUTC:new Date().toISOString(),etapa,presupuestoRestanteSeg:budgets[etapa],fuente:paquete?'paquete_privado_en_memoria':tokens?path.basename(tokens):'ninguna',filas:input.length,decodificables:decoded.length,noDecodificables:invalid,
  vencidos:decoded.filter(x=>x.exp<=now).length,candidatosCohorte50:selected.length,identidadesDistintas:unique.size,
  margenSuficiente:good.length,TTLMinCohorteSeg:selected.length?Math.floor(Math.min(...selected.map(x=>x.exp))-now):null,
  expMinUTC:decoded.length?new Date(Math.min(...decoded.map(x=>x.exp))*1000).toISOString():null,expMaxUTC:decoded.length?new Date(Math.max(...decoded.map(x=>x.exp))*1000).toISOString():null,
  vigenciaPorClaimsSuficiente:selected.length===50&&unique.size===50&&good.length===50,firmaVerificada:false,controlesServidorEjecutados:false,autorizaHTTP:false,HTTP:0,JMeter:0};
 const report={configuracionValida:true,usuarios:50,segundosMedidos:600,segundosCalentamiento:60,objetivoHTTPMedidos:6000,objetivoHTTPCalentamiento:600,
  identidadEstablePorHilo:true,teoricoSinBackend:true,totales,porMinuto,tokens:tokenReport,limitacion:'Decodificar claims no verifica firma, vigencia real en una futura hora ni correspondencia SQL. La carga necesita implementacion del reloj/ritmo y validacion ficticia antes de su autorizacion.'};
 fs.mkdirSync(salida,{recursive:true});fs.writeFileSync(path.join(salida,'preparacion-local.json'),JSON.stringify(report,null,2));
 fs.writeFileSync(path.join(salida,'calendario-teorico.csv'),'fase,hilo,turno,offsetMs,endpoint\n'+slots.sort((a,b)=>a.fase.localeCompare(b.fase)||a.offsetMs-b.offsetMs).map(s=>[s.fase,s.hilo,s.turno,s.offsetMs,s.endpoint].join(',')).join('\n')+'\n');
 console.log(JSON.stringify(report,null,2));return !exigir||tokenReport.vigenciaPorClaimsSuficiente;
}
module.exports={inspect};
if(require.main===module)try{
 const options={config:path.join(__dirname,'../diseno/e8-resistencia.json'),tokens:path.join(__dirname,'tokens.csv'),etapa:'Preparacion',salida:path.join(__dirname,'../diagnosticos/e8-preparacion-'+new Date().toISOString().replace(/[:.]/g,'-'))};
 for(let i=2;i<process.argv.length;i++){const a=process.argv[i];if(a==='--exigir-vigencia')options.exigir=true;else{const k={'--config':'config','--tokens':'tokens','--paquete':'paquete','--etapa':'etapa','--salida':'salida'}[a];assert(k);options[k]=process.argv[++i];}}
 if(!inspect(options))process.exitCode=2;
}catch{console.error('E8: inspeccion local fallida; revisar configuracion o formato, sin imprimir tokens.');process.exitCode=1;}
