import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.jmeter.modifiers.JSR223PreProcessor
import org.apache.jmeter.extractor.JSR223PostProcessor
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

def mapper=new ObjectMapper()
def output={String name -> new File(props.getProperty('salida'),name)}
def waitUntil={long deadline -> while(System.nanoTime()<deadline) {long left=deadline-System.nanoTime();LockSupport.parkNanos(Math.min(left,20000000L));if(Thread.currentThread().isInterrupted())throw new InterruptedException()} }
def delegate={String action ->
 def p=action=='antes'?new JSR223PreProcessor():new JSR223PostProcessor()
 p.setScriptLanguage('groovy');p.setCacheKey('true');p.setFilename(props.getProperty('soporte'));p.setParameters(action);p.process()
}
def ttl={root,long seconds ->
 long now=System.currentTimeMillis().intdiv(1000)
 for(def i:root.path('identidades')) {
  def c=mapper.readTree(Base64.urlDecoder.decode(i.path('token').asText().split('\\.')[1]))
  if(c.path('exp').asLong()<now+seconds || c.path('iat').asLong()>now || c.path('nbf').asLong()>now || c.path('rol').asText()!='CLIENTE' || c.path('sub').asText()!=i.path('correo').asText() || c.path('id').asLong()!=i.path('usuarioId').asLong()) throw new IllegalStateException('E8: identidad o vigencia insuficiente')
 }
}
String action=Parameters
boolean fake=props.getProperty('ficticio','false')=='true'
boolean wire=props.getProperty('validacionWire','false')=='true'
if(action=='preparar') {
 SampleResult.setIgnore()
 def config=mapper.readTree(new File(props.getProperty('configuracion')))
 if(config.path('usuarios').asInt()!=50 || config.path('tiemposSeg').path('medicion').asInt()!=600 || config.path('ritmo').path('objetivoHTTPPorSegundoGlobal').asInt()!=10)throw new IllegalStateException('E8: configuracion no autorizada')
 // Ejecuta el setup y los validadores E6 reales con respuestas locales del paquete.
 def setup=new org.apache.jmeter.protocol.java.sampler.JSR223Sampler();setup.setScriptLanguage('groovy');setup.setCacheKey('true');setup.setFilename(props.getProperty('soporte'));setup.setParameters('preparar');setup.sample(null)
 if(props.get('e6Listo')!=true)throw new IllegalStateException('E8: setup E6 fallo')
 ttl(props.get('e6Datos'),1060)
 def s=[barrier:new CyclicBarrier(50),alive:new AtomicInteger(0),group:new AtomicReference(),inflight:new AtomicInteger(0),lock:new Object(),last:0L,started:false,finished:new AtomicInteger(0),failures:new AtomicInteger(0),logs:new ConcurrentHashMap()]
 if(wire && (props.getProperty('host')!='127.0.0.1' || !(props.getProperty('puerto')==~/[0-9]+/) || props.getProperty('puerto')=='18080'))throw new IllegalStateException('E8: destino ficticio local no autorizado')
 if(wire && !props.get('e6Datos').path('identidades').every{it.path('token').asText().endsWith('.FICTICIO')})throw new IllegalStateException('E8: prohibido enviar tokens reales al servidor ficticio')
 s.period=5000L;s.spacing=100L;s.warm=(fake||wire)?10000L:60000L;s.measure=(fake||wire)?20000L:600000L
 s.writer=new PrintWriter(output('monitor-hilos.csv'),'UTF-8');s.writer.println('epochMs,hilosActivos,httpEnCurso');s.writer.flush()
 s.monitor=Executors.newSingleThreadScheduledExecutor({Runnable task -> def t=new Thread(task,'e8-monitor');t.setDaemon(true);return t} as ThreadFactory)
 s.monitor.scheduleAtFixedRate({s.writer.println(System.currentTimeMillis().toString()+','+(s.group.get()==null?0:s.group.get().numberOfActiveThreads())+','+s.inflight.get());s.writer.flush()} as Runnable,0, fake?100:1000,TimeUnit.MILLISECONDS)
 props.put('e8Estado',s);return
}
def s=props.get('e8Estado')
if(action=='barrera') {
 SampleResult.setIgnore();s.group.compareAndSet(null,ctx.getThreadGroup());s.alive.incrementAndGet();vars.put('e8Continuar','true');vars.put('e8Cursor','0')
 s.barrier.await(40,TimeUnit.SECONDS)
 synchronized(s.lock){if(!s.started){ttl(props.get('e6Datos'),860);mapper.writeValue(output('vigencia-calentamiento.json'),[valida:true,identidades:50,presupuestoSeg:860,epochMs:System.currentTimeMillis(),valoresTokenRegistrados:false]);s.epoch=System.currentTimeMillis()+1000;s.nano=System.nanoTime()+1000000000L;s.started=true
  mapper.writeValue(output('reloj.json'),[inicioCalentamiento:s.epoch,inicioMedicion:s.epoch+s.warm,finMedicion:s.epoch+s.warm+s.measure,inicioMonotonoNs:s.nano.toString(),calentamientoMs:s.warm,medicionMs:s.measure,periodoMs:s.period,desfaseMs:s.spacing,ficticio:(fake||wire),servidorFicticioLocal:wire])}}
 def threadLog=new PrintWriter(output('turnos-'+(ctx.getThreadNum()+1)+'.jsonl'),'UTF-8');vars.putObject('e8Log',threadLog);s.logs.put(ctx.getThreadNum(),threadLog);return
}
if(action=='turno') {
 SampleResult.setIgnore();vars.put('e8Enviar','false')
 int thread=ctx.getThreadNum();int cursor=vars.get('e8Cursor').toInteger();int nw=(s.warm/s.period) as int;int nm=(s.measure/s.period) as int
 if(cursor>=nw+nm){waitUntil(s.nano+(s.warm+s.measure)*1000000L);vars.put('e8Continuar','false');return}
 boolean warm=cursor<nw;int k=warm?cursor:cursor-nw;long offset=(warm?0:s.warm)+k*s.period+thread*s.spacing;long due=s.nano+offset*1000000L
 vars.put('e8Cursor',(cursor+1).toString());vars.put('e8Fase',warm?'calentamiento':'medicion');vars.put('e8Turno',k.toString());vars.put('e8Plan',(s.epoch+offset).toString());vars.put('e6Cuenta',((k+thread%5)%5).toString())
 waitUntil(due)
 boolean admitted=false;long admissionNano=0L;String reason='turno_perdido'
 synchronized(s.lock){
  long now=System.nanoTime();long next=due
  // Un turno deja 50ms de guarda para preprocesar antes del siguiente limite.
  // Llegar tarde no autoriza consumir el turno siguiente ni comenzar tras la fase.
  if(now<due+50000000L){waitUntil(next);now=System.nanoTime();long bucket=(now-s.nano).intdiv(1000000000L)
   if(s.bucket!=bucket){s.bucket=bucket;s.bucketCount=0}
   if(now<due+50000000L && now-s.last>=50000000L && s.bucketCount<10){
   if(!warm && !s.measureTTL){ttl(props.get('e6Datos'),800);mapper.writeValue(output('vigencia-medicion.json'),[valida:true,identidades:50,presupuestoSeg:800,epochMs:System.currentTimeMillis(),valoresTokenRegistrados:false]);s.measureTTL=true}
   s.last=now;s.bucketCount++;admissionNano=now;admitted=true
  }}
 }
 def log=vars.getObject('e8Log');log.println(mapper.writeValueAsString([hilo:thread+1,fase:vars.get('e8Fase'),turno:k,plan:s.epoch+offset,admitido:admitted,admisionNano:admitted?admissionNano.toString():null,endpoint:[0,1,2,0,1][(k+thread%5)%5],motivo:admitted?'admitido':reason]));log.flush()
 if(admitted){vars.put('e8Enviar','true')};return
}
if(action=='antes') {
 if(fake){
  // Misma rama de autenticacion del sampler real, sin invocar sample()/red.
  def original=ctx.getCurrentSampler();def dummy=new HTTPSamplerProxy();dummy.setHeaderManager(new org.apache.jmeter.protocol.http.control.HeaderManager())
  try {ctx.setCurrentSampler(dummy);delegate('antes');def hm=dummy.getHeaderManager();def auth=(0..<hm.getHeaders().size()).collect{hm.getHeader(it)}.findAll{it.getName()=='Authorization'}
   if(vars.get('e6Tipo')=='2'){if(auth.size()!=0)throw new IllegalStateException('E8: cabecera publica inesperada')}
   else if(auth.size()!=1 || auth[0].getValue()!='Bearer '+vars.get('e6Token'))throw new IllegalStateException('E8: autenticacion no preparada')
  }finally{ctx.setCurrentSampler(original)}
 }else{delegate('antes')}
 if(!fake){
  def hm=sampler.getHeaderManager();def auth=hm==null?[]:(0..<hm.getHeaders().size()).collect{hm.getHeader(it)}.findAll{it.getName()=='Authorization'}
  boolean valid=vars.get('e6Tipo')=='2'?auth.isEmpty():(auth.size()==1 && auth[0].getValue()=='Bearer '+vars.get('e6Token'))
  if(!valid){ctx.getEngine().stopTest(true);throw new IllegalStateException('E8: cabecera de identidad incorrecta; detener')}
 }
 if(!(vars.get('e6Umbral') in ['300','800']) || vars.get('e6TipoFallo')!='ninguno')throw new IllegalStateException('E8: preprocesador no completo')
 if(!fake){sampler.setName('E8 GET '+['notificaciones','historial','especialidades'][vars.get('e6Tipo').toInteger()])}
 vars.put('e8RetrasoMs',Math.max(0L,System.currentTimeMillis()-vars.get('e8Plan').toLong()).toString());s.inflight.incrementAndGet();return
}
if(action=='respuestaFicticia') {
 int kind=vars.get('e6Tipo').toInteger();def root=props.get('e6Datos');def i=root.path('identidades').get(ctx.getThreadNum())
 SampleResult.setSampleLabel('E8 GET '+['notificaciones','historial','especialidades'][kind]);SampleResult.setResponseCode('200');SampleResult.setSuccessful(true)
 SampleResult.setResponseData(mapper.writeValueAsBytes(kind==0?i.path('notificaciones'):kind==1?i.path('citas'):root.path('especialidades')))
 if(kind==0)SampleResult.setResponseHeaders('X-Total-Count: '+i.path('totalNotificaciones').asText()+'\r\n')
 if(props.getProperty('falloHTTP','')=='401' && ctx.getThreadNum()==0){SampleResult.setResponseCode('401');SampleResult.setResponseData('','UTF-8')}
 Thread.sleep(3);return
}
if(action=='despues') {
 try{delegate('validar');if(prev.getResponseCode() in ['401','403'] || vars.get('e6TipoFallo')=='Contenido'){s.failures.incrementAndGet();ctx.getEngine().stopTest(false)}}finally{s.inflight.decrementAndGet()};return
}
if(action=='finHilo') {SampleResult.setIgnore();vars.getObject('e8Log').close();s.finished.incrementAndGet();s.alive.decrementAndGet();return}
if(action=='cerrar') {
 SampleResult.setIgnore();s.monitor.shutdown();s.monitor.awaitTermination(5,TimeUnit.SECONDS);s.writer.println(System.currentTimeMillis().toString()+','+s.group.get().numberOfActiveThreads()+','+s.inflight.get());s.writer.close();s.logs.values().each{it.close()}
 mapper.writeValue(output('motor-final.json'),[cierreEpochMs:System.currentTimeMillis(),hilosCerrados:s.finished.get(),hilosActivos:s.group.get().numberOfActiveThreads(),contadorLogicoPendiente:s.alive.get(),httpEnCurso:s.inflight.get(),fallosContrato:s.failures.get(),monitorCerrado:s.monitor.isTerminated(),medicionTTL:s.measureTTL==true])
 props.remove('e6Datos');props.remove('e6Mapper');props.remove('e6Listo');props.remove('e8Estado');return
}
throw new IllegalStateException('E8: accion desconocida')
