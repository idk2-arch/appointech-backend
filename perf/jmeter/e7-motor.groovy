import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import org.apache.jmeter.assertions.AssertionResult

/** Instrumentacion de E7; no modifica el backend ni ejecuta escrituras SQL. */
class E7Motor {
    final Map cfg
    final boolean ficticio
    final AtomicBoolean abortar = new AtomicBoolean(false)
    final AtomicLong ids = new AtomicLong(900001)
    final Map<Long,Map> tabla = new ConcurrentHashMap<>()
    final Map<Integer,Map> hilos = new ConcurrentHashMap<>()
    final BlockingQueue<Map> pendientes = new LinkedBlockingQueue<>()
    final CyclicBarrier barrera
    final BufferedWriter sqlDiario
    final AtomicBoolean cerrarSql = new AtomicBoolean(false)
    Thread trabajador
    volatile String motivo = ''
    volatile long inicioEpoch = 0
    static final List<String> nombres = ['crear','consultar_creada','editar','consultar_editada','borrar','verificar_ausencia']

    E7Motor(Map c) {
        cfg=c; ficticio=(c.ficticio==true)
        if ((!ficticio && !(c.usuarios in [50,100,150])) || c.ciclos!=20 || c.tecnicos.size()!=c.usuarios ||
            c.tecnicos.toSet().size()!=Math.min(c.usuarios as int,50) || c.destino!='http://localhost:18080' ||
            c.timeoutConexionMs!=5000 || c.timeoutRespuestaMs!=10000) throw new IllegalStateException('E7: configuracion no autorizada')
        if(c.usuarios>=50 && c.tecnicos.countBy{it}.values().any{it!=c.usuarios/50})throw new IllegalStateException('E7: reparto no uniforme')
        if (!ficticio && (!(c.dbId==~ /[a-f0-9]{64}/) || !(c.dbUsuario==~ /[A-Za-z0-9_]+/) || !c.token))
            throw new IllegalStateException('E7: paquete privado incompleto')
        new File(c.salida.toString()).mkdirs()
        sqlDiario=escritor('conciliacion-sql.jsonl')
        barrera=new CyclicBarrier(c.usuarios as int, { inicioEpoch=System.currentTimeMillis() } as Runnable)
        trabajador=new Thread({ observar() } as Runnable,'e7-sql-solo-lectura')
        trabajador.setDaemon(true); trabajador.start()
    }
    BufferedWriter escritor(String nombre) {
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(cfg.salida.toString(),nombre)), 'UTF-8'))
    }
    static void anotar(BufferedWriter w, Map m) { w.write(JsonOutput.toJson(m));w.newLine();w.flush() }
    void detener(String razon) { if(abortar.compareAndSet(false,true)) motivo=razon }
    Map hilo(int h) {
        hilos.computeIfAbsent(h,{ int n -> [hilo:n,ciclo:0,paso:0,id:null,creada:null,creadaSql:null,
            hechos:0,omitidos:0,diario:escritor('hilo-'+n+'.jsonl')] })
    }
    void esperar(int h) {
        hilo(h)
        try { barrera.await(60,TimeUnit.SECONDS) }
        catch(Exception ignored) { detener('barrera'); throw new IllegalStateException('E7: barrera incompleta, sin CRUD') }
    }
    void ciclo(int h) {
        Map t=hilo(h);t.ciclo++;t.paso=0;t.id=null;t.creada=null;t.creadaSql=null
    }
    String fecha(int h,int ciclo){LocalDate.parse(cfg.baseFecha.toString()).plusDays(20L*((h-1).intdiv(50))+ciclo-1).toString()}
    void preparar(int h, def vars) {
        Map t=hilo(h);t.paso++
        int c=t.ciclo as int,p=t.paso as int
        if(c<1||c>20||p<1||p>6)throw new IllegalStateException('E7: bucles incorrectos')
        String fecha=fecha(h,c)
        long tecnico=(cfg.tecnicos[h-1] as Number).longValue()
        vars.put('e7Hilo',h.toString());vars.put('e7Ciclo',c.toString());vars.put('e7Paso',p.toString())
        vars.put('e7Tecnico',tecnico.toString());vars.put('e7Fecha',fecha)
        vars.put('e7Id',t.id==null?'':t.id.toString());vars.put('e7Contenido','false');vars.put('e7Sql','no_aplica')
        vars.put('e7Incierta','false');vars.put('e7TipoFallo','ninguno')
        vars.put('e7Etiqueta','E7 '+nombres[p-1])
        String base='/api/admin/tecnicos/'+tecnico+'/franjas'
        vars.put('e7Metodo',p==1?'POST':p==3?'PUT':p==5?'DELETE':'GET')
        vars.put('e7Ruta',p in [3,5]?base+'/'+(t.id?:'ID_NO_ADMITIDO'):p in [2,4,6]?base+'?desde='+fecha+'&hasta='+fecha:base)
        String ini=p==3?'09:00':'08:00'
        String fin=java.time.LocalTime.parse(ini).plusMinutes((cfg.duracionMinutos as Number).longValue()).toString()
        vars.put('e7Cuerpo',p in [1,3]?JsonOutput.toJson([fecha:fecha,horaInicio:ini,horaFin:fin]):'')
        vars.put('e7Autorizacion',ficticio?'FICTICIO':'Bearer '+cfg.token)
        if(abortar.get()) {
            t.omitidos++;vars.put('e7Ejecutar','false')
            anotar(t.diario as BufferedWriter,[evento:'omitida',hilo:h,ciclo:c,paso:p,motivo:motivo]);return
        }
        if(p>1 && t.id==null) {detener('dependencia_sin_id');throw new IllegalStateException('E7: falta ID propio')}
        vars.put('e7Ejecutar','true')
        anotar(t.diario as BufferedWriter,[evento:'intencion',epochMs:System.currentTimeMillis(),hilo:h,ciclo:c,paso:p,
            tecnicoId:tecnico,fecha:fecha,id:t.id?.toString()])
    }
    Map esperado(int h, int p) {
        Map t=hilo(h)
        String ini=p in [1,2]?'08:00':'09:00'
        [hilo:h,ciclo:t.ciclo,paso:p,id:t.id,tecnicoId:(cfg.tecnicos[h-1] as Number).longValue(),
            fecha:fecha(h,t.ciclo as int),
            horaInicio:ini,horaFin:java.time.LocalTime.parse(ini).plusMinutes((cfg.duracionMinutos as Number).longValue()).toString(),
            propietario:(cfg.administradorId as Number).longValue(),creadaSql:t.creadaSql]
    }
    void simular(int h, def vars, def sample) {
        Map t=hilo(h); int p=t.paso as int
        Map e=esperado(h,p); Object body
        if(p==1){long id=ids.addAndGet(7);body=[id:id,tecnicoId:e.tecnicoId,fecha:e.fecha,horaInicio:e.horaInicio,
            horaFin:e.horaFin,bloques:1,bloquesOcupados:0,creadaEn:'2026-01-01T00:00:00'];tabla[id]=body+[propietario:e.propietario]}
        else if(p==3){Map f=tabla[t.id as Long];f.horaInicio=e.horaInicio;f.horaFin=e.horaFin;body=f.findAll{k,v->k!='propietario'}}
        else if(p==5){tabla.remove(t.id as Long);body=''}
        else body=tabla.values().findAll{it.tecnicoId==e.tecnicoId&&it.fecha==e.fecha}.collect{it.findAll{k,v->k!='propietario'}}
        int fault=cfg.falloPaso?cfg.falloPaso as int:0
        boolean timeout=(h==1&&t.ciclo==1&&p==fault)
        sample.setSampleLabel(vars.get('e7Etiqueta'))
        sample.setResponseCode(timeout?'Non HTTP response code: java.net.SocketTimeoutException':p==1?'201':p==5?'204':'200')
        sample.setResponseMessage(timeout?'FICTICIO timeout':'FICTICIO SIN HTTP')
        if(cfg.falloSinId==true&&h==1&&t.ciclo==1&&p==1)body=[:]
        sample.setResponseData(body instanceof String?body:JsonOutput.toJson(body),'UTF-8')
        sample.setSuccessful(!timeout)
    }
    void validar(int h, def vars, def prev) {
        Map t=hilo(h);int p=t.paso as int;Map e=esperado(h,p)
        boolean escritura=p in [1,3,5],contenido=false,incierta=false
        String fallo='ninguno';String code=prev.getResponseCode()
        try {
            String expectedCode=p==1?'201':p==5?'204':'200'
            if(code!=expectedCode){fallo=code==~ /\d{3}/?'HTTP':'Transporte';throw new IllegalStateException('codigo')}
            String texto=prev.getResponseDataAsString()
            if(p==5){if(texto.trim())throw new IllegalStateException('delete_con_cuerpo')}
            else if(p==6){def rows=new JsonSlurper().parseText(texto);if(!(rows instanceof List)||rows.size()!=0)throw new IllegalStateException('ausencia')}
            else {
                def parsed=new JsonSlurper().parseText(texto)
                Map f
                if(p in [2,4]){
                    if(!(parsed instanceof List)||parsed.size()!=1)throw new IllegalStateException('array')
                    f=parsed[0] as Map
                }else {if(!(parsed instanceof Map))throw new IllegalStateException('objeto');f=parsed as Map}
                if(!(f.id instanceof Number)||new BigDecimal(f.id.toString()).scale()>0||f.id.longValue()<=0)throw new IllegalStateException('id')
                if(p==1){t.id=f.id.longValue();vars.put('e7Id',t.id.toString())}
                if(f.id.longValue()!=t.id || !(f.tecnicoId instanceof Number)||f.tecnicoId.longValue()!=e.tecnicoId ||
                   f.fecha!=e.fecha||java.time.LocalTime.parse(f.horaInicio.toString()).toString()!=e.horaInicio||
                   java.time.LocalTime.parse(f.horaFin.toString()).toString()!=e.horaFin||f.bloques!=1||f.bloquesOcupados!=0)
                    throw new IllegalStateException('pertenencia_campos')
                LocalDateTime creada=LocalDateTime.parse(f.creadaEn.toString())
                if(t.creada==null)t.creada=creada
                else if(Math.abs(ChronoUnit.NANOS.between(t.creada as LocalDateTime,creada))>1000)throw new IllegalStateException('creadaEn')
            }
            contenido=true
        }catch(Exception ignored){if(fallo=='ninguno')fallo='Contenido';incierta=escritura;detener('respuesta_'+fallo)}
        if(contenido && escritura) {
            try {
                Map q=esperado(h,p);q.futuro=new CompletableFuture<Map>();pendientes.put(q)
                Map resultado=(q.futuro as CompletableFuture).get(30,TimeUnit.SECONDS)
                vars.put('e7Sql',resultado.valido?'true':'false')
                if(!resultado.valido)throw new IllegalStateException('SQL no coincide')
                if(p==1)t.creadaSql=resultado.creadaEn
            }catch(Exception ignored){fallo='Integridad';incierta=true;detener('conciliacion_sql');vars.put('e7Sql','false')}
        }
        vars.put('e7Contenido',contenido.toString());vars.put('e7TipoFallo',fallo);vars.put('e7Incierta',incierta.toString())
        prev.setSuccessful(fallo=='ninguno')
        if(fallo!='ninguno'){def a=new AssertionResult('E7 contrato/integridad');a.setFailure(true);a.setFailureMessage('E7 '+fallo);prev.addAssertionResult(a)}
        // Nunca persistir cuerpo, cabeceras, mensaje de excepcion de red ni credenciales.
        prev.setResponseMessage(fallo=='ninguno'?'E7 OK':'E7 '+fallo)
        t.hechos++
        anotar(t.diario as BufferedWriter,[evento:'resultado',epochMs:System.currentTimeMillis(),hilo:h,ciclo:t.ciclo,paso:p,
            id:t.id?.toString(),codigo:code,contenido:contenido,fallo:fallo,incierta:incierta,sql:vars.get('e7Sql')])
    }
    void observar() {
        while(!cerrarSql.get()||!pendientes.isEmpty()) {
            Map first=pendientes.poll(100,TimeUnit.MILLISECONDS);if(first==null)continue
            List<Map> batch=[first];Thread.sleep(ficticio?1:75);pendientes.drainTo(batch,49)
            try {
                List<Map> results=ficticio?batch.collect{Map q->
                    Map f=tabla[q.id as Long];boolean ok
                    if(q.paso==5)ok=(f==null&&!tabla.values().any{it.tecnicoId==q.tecnicoId&&it.fecha==q.fecha})
                    else ok=f!=null&&f.id==q.id&&f.tecnicoId==q.tecnicoId&&f.fecha==q.fecha&&f.horaInicio==q.horaInicio&&f.horaFin==q.horaFin&&f.propietario==q.propietario
                    [hilo:q.hilo,ciclo:q.ciclo,paso:q.paso,id:q.id,valido:ok,creadaEn:f?.creadaEn]
                }:consultar(batch)
                if(results.size()!=batch.size())throw new IllegalStateException('SQL incompleto')
                batch.each{q->
                    Map r=results.find{it.hilo==q.hilo&&it.ciclo==q.ciclo&&it.paso==q.paso&&it.id==q.id}
                    if(r==null)throw new IllegalStateException('SQL sin correlacion')
                    anotar(sqlDiario,r+[id:r.id?.toString(),epochMs:System.currentTimeMillis()]);(q.futuro as CompletableFuture).complete(r)
                }
            }catch(Exception ignored){detener('observador_sql');batch.each{(it.futuro as CompletableFuture).completeExceptionally(new IllegalStateException('E7: observador SQL fallo'))}}
        }
    }
    List<Map> consultar(List<Map> batch) {
        String values=batch.collect{q->
            "(${q.hilo},${q.ciclo},${q.paso},${q.id},${q.tecnicoId},'${q.fecha}'::date,'${q.horaInicio}'::time,'${q.horaFin}'::time,${q.propietario},"+
                (q.creadaSql?"'${q.creadaSql}'::timestamp":"NULL::timestamp")+")"
        }.join(',')
        String sql="""BEGIN READ ONLY;
SET LOCAL statement_timeout='5s';
WITH e(h,c,p,id,tid,fecha,ini,fin,autor,creada) AS (VALUES ${values})
SELECT json_agg(json_build_object('hilo',e.h,'ciclo',e.c,'paso',e.p,'id',e.id,
'valido',CASE WHEN e.p=5 THEN f.id IS NULL AND NOT EXISTS(SELECT 1 FROM franja_tecnico z WHERE z.tecnico_id=e.tid AND z.fecha=e.fecha)
ELSE f.id IS NOT NULL AND f.tecnico_id=e.tid AND f.fecha=e.fecha AND f.hora_inicio=e.ini AND f.hora_fin=e.fin AND f.creada_por=e.autor
AND (e.creada IS NULL OR f.creada_en=e.creada) END,
'creadaEn',to_char(f.creada_en,'YYYY-MM-DD"T"HH24:MI:SS.US')) ORDER BY e.h)
FROM e LEFT JOIN franja_tecnico f ON f.id=e.id;
ROLLBACK;"""
        Process process=new ProcessBuilder([cfg.docker.toString(),'exec','-i',cfg.dbId.toString(),'psql','-X','-q','-t','-A','-U',cfg.dbUsuario.toString(),'-d','appointech_perf','-v','ON_ERROR_STOP=1']).start()
        StringBuilder output=new StringBuilder(),error=new StringBuilder()
        Thread stdout=process.consumeProcessOutputStream(output)
        Thread stderr=process.consumeProcessErrorStream(error)
        try {
            process.outputStream.withWriter('UTF-8'){it.write(sql)}
            if(!process.waitFor(12,TimeUnit.SECONDS))throw new IllegalStateException('SQL timeout')
            if(process.exitValue()!=0)throw new IllegalStateException('SQL error')
            stdout.join(1000);stderr.join(1000)
            if(stdout.isAlive()||stderr.isAlive())throw new IllegalStateException('SQL streams no cerrados')
            def result=new JsonSlurper().parseText(output.toString().trim())
            if(!(result instanceof List))throw new IllegalStateException('SQL no array')
            result as List<Map>
        }finally{if(process.isAlive()){process.destroyForcibly();process.waitFor()}}
    }
    void finalizarHilo(int h) {
        Map t=hilo(h)
        anotar(t.diario as BufferedWriter,[evento:'fin_hilo',hilo:h,ciclos:t.ciclo,pasos:t.hechos,omitidos:t.omitidos]);(t.diario as BufferedWriter).close()
    }
    void cerrar() {
        cerrarSql.set(true);trabajador.join(15000)
        if(trabajador.isAlive())throw new IllegalStateException('E7: SQL no cerro')
        sqlDiario.close()
        new File(cfg.salida.toString(),'motor-final.json').text=JsonOutput.toJson([usuarios:cfg.usuarios,ciclos:cfg.ciclos,
            inicioEpochMs:inicioEpoch,finEpochMs:System.currentTimeMillis(),aborto:abortar.get(),motivo:motivo,
            HTTPFicticio:ficticio,tablaFicticiaRestante:ficticio?tabla.size():null,
            hilos:hilos.values().collect{[hilo:it.hilo,ciclos:it.ciclo,pasos:it.hechos,omitidos:it.omitidos]},observadorCerrado:true])
        cfg.token=null
    }
}

if(Parameters=='preparar') {
    SampleResult.setIgnore()
    String json=new File(props.getProperty('paquete')).getText('UTF-8')
    if(json.startsWith('\uFEFF'))json=json.substring(1)
    Map cfg=new JsonSlurper().parseText(json) as Map
    props.put('e7Motor',new E7Motor(cfg));return
}
E7Motor motor=props.get('e7Motor') as E7Motor
if(motor==null)throw new IllegalStateException('E7: falta preparacion')
int h=ctx.getThreadNum()+1
switch(Parameters) {
    case 'barrera':SampleResult.setIgnore();motor.esperar(h);break
    case 'ciclo':SampleResult.setIgnore();motor.ciclo(h);break
    case 'paso':SampleResult.setIgnore();motor.preparar(h,vars);break
    case 'ficticio':motor.simular(h,vars,SampleResult);break
    case 'validar':motor.validar(h,vars,prev);break
    case 'fin':SampleResult.setIgnore();motor.finalizarHilo(h);break
    case 'cerrar':SampleResult.setIgnore();motor.cerrar();props.remove('e7Motor');break
    default:throw new IllegalStateException('E7: accion desconocida')
}
