import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import org.apache.jmeter.assertions.AssertionResult
import org.apache.jmeter.modifiers.JSR223PreProcessor
import org.apache.jmeter.extractor.JSR223PostProcessor
import org.apache.jmeter.samplers.SampleResult as Resultado
import java.time.LocalDateTime
import java.nio.file.Files
import com.fasterxml.jackson.databind.JsonNode

// Nunca usar la coercion booleana de JsonNode para acreditar existencia.
def paqueteValido={ Object candidate,int n ->
    if(!(candidate instanceof JsonNode)) return false
    JsonNode root=(JsonNode)candidate
    if(root.isNull() || root.isMissingNode() || !root.isObject()) return false
    def identities=root.get('identidades')
    if(identities==null || !identities.isArray() || identities.size()==0 || identities.size()!=n) return false
    for(String field:['especialidades','tiposNotificacion','estadosCita']) {
        def value=root.get(field)
        if(value==null || !value.isArray() || value.size()==0) return false
    }
    def positive={node -> node!=null && node.isIntegralNumber() && node.canConvertToLong() && node.longValue()>0}
    def users=new HashSet<Long>();def clients=new HashSet<Long>()
    for(int i=0;i<identities.size();i++) {
        def f=identities.get(i)
        if(f==null || !f.isObject() || !positive(f.get('usuarioId')) || !positive(f.get('clienteId')) ||
           !users.add(f.path('usuarioId').longValue()) || !clients.add(f.path('clienteId').longValue()) ||
           !f.path('ordinal').isIntegralNumber() || f.path('ordinal').intValue()!=i+1 ||
           !f.path('correo').isTextual() || !(f.path('correo').asText()==~/cliente[1-9][0-9]*@perf\.test/) ||
           !f.path('rol').isTextual() || f.path('rol').asText()!='CLIENTE' ||
           !f.path('token').isTextual() || f.path('token').asText().isBlank() ||
           !positive(f.get('totalNotificaciones'))) return false
        for(String field:['notificaciones','citas']) {
            def rows=f.get(field)
            if(rows==null || !rows.isArray() || rows.size()==0) return false
            def ids=new HashSet<Long>()
            for(def row:rows) if(row==null || !row.isObject() || !positive(row.get('id')) || !ids.add(row.path('id').longValue())) return false
        }
        if(f.path('notificaciones').size()!=Math.min(20L,f.path('totalNotificaciones').longValue())) return false
        for(def row:f.path('notificaciones')) for(String key:['tipo','titulo','mensaje','citaId','leida','creadaEn'])
            if(!row.has(key)) return false
        for(def row:f.path('citas')) for(String key:['tecnicoId','especialidadId','descripcionProblema','direccionServicio',
            'fechaHoraInicio','fechaHoraFin','estado','tecnicoNombre','tecnicoApellido','tecnicoTelefono','especialidadNombre',
            'tecnicoFotoUrl','agendamientoManual','motivoSolicitudEspecial','asignadaManualmente','asignacionManual'])
            if(!row.has(key)) return false
    }
    return true
}

def paths=['/api/notificaciones?page=0&size=20','/api/cliente/citas','/api/especialidades']
def labels=['E6 GET notificaciones','E6 GET historial','E6 GET especialidades']
if(Parameters=='preparar') {
    SampleResult.setIgnore()
    try {
        def mapper=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        def root=mapper.readTree(new File(props.getProperty('paquete')))
        Files.delete(new File(props.getProperty('paquete')).toPath())
        if(!paqueteValido(root,props.getProperty('usuarios','100').toInteger()))
            throw new IllegalStateException()
        props.put('e6Datos',root);props.put('e6Mapper',mapper)
        vars.put('e6Preparando','true')
        // Ejercitar las mismas fuentes y cache real antes de la rampa, sin HTTP.
        for(int k=0;k<3;k++) {
            vars.put('e6IndiceForzado',k.toString())
            def pre=new JSR223PreProcessor();pre.setScriptLanguage('groovy');pre.setCacheKey('true')
            pre.setFilename(props.getProperty('soporte'));pre.setParameters('antes');pre.process()
            def fake=new Resultado();fake.sampleStart();fake.setResponseCode('200');fake.setSuccessful(true)
            def identity=root.path('identidades').get(0)
            fake.setResponseData(mapper.writeValueAsBytes(k==0?identity.path('notificaciones'):k==1?identity.path('citas'):root.path('especialidades')))
            if(k==0) fake.setResponseHeaders('X-Total-Count: '+identity.path('totalNotificaciones').asText()+'\r\n')
            fake.sampleEnd();ctx.setPreviousResult(fake)
            def post=new JSR223PostProcessor();post.setScriptLanguage('groovy');post.setCacheKey('true')
            post.setFilename(props.getProperty('soporte'));post.setParameters('validar');post.process()
            if(!fake.isSuccessful()) throw new IllegalStateException()
        }
        vars.remove('e6Preparando');vars.remove('e6IndiceForzado')
        def http=org.apache.http.impl.client.HttpClients.createDefault();http.close()
        mapper.writeValue(new File(props.getProperty('setupSalida')),[estado:'listo',identidades:root.path('identidades').size(),solicitudesHTTP:0])
        props.put('e6Listo',true)
    } catch(Throwable ignored) {
        props.remove('e6Listo')
        try {new ObjectMapper().writeValue(new File(props.getProperty('setupSalida')),[estado:'fallido',tipo:ignored.getClass().getName(),solicitudesHTTP:0])} catch(Throwable omitido) {}
        ctx.getEngine().stopTest(true)
        throw new IllegalStateException('Preparacion local E6 fallida, sin imprimir paquete')
    }
    return
}
if(Parameters=='antes') {
    boolean warm=vars.get('e6Preparando')=='true'
    def root=props.get('e6Datos')
    int thread=ctx.getThreadNum()
    if(!(root instanceof JsonNode) || root.isNull() || root.isMissingNode() || !root.isObject() ||
       !root.path('identidades').isArray() || root.path('identidades').size()==0 ||
       (!warm && props.get('e6Listo')!=true) || thread<0 || thread>=root.path('identidades').size() ||
       !root.path('identidades').get(thread).isObject()) {
        ctx.getEngine().stopTest(true);throw new IllegalStateException('Falta identidad preparada E6')
    }
    int count=(vars.get('e6Cuenta')?:'0').toInteger()
    int kind=warm?vars.get('e6IndiceForzado').toInteger():[0,1,2,0,1][count%5]
    vars.put('e6Cuenta',(count+1).toString());vars.put('e6Hilo',(thread+1).toString())
    vars.put('e6Iteracion',(count.intdiv(5)+1).toString());vars.put('e6Paso',(count%5+1).toString())
    vars.put('e6Tipo',kind.toString());vars.put('e6Etiqueta',labels[kind]);vars.put('e6Ruta',paths[kind])
    vars.put('e6Token',kind==2?'':root.path('identidades').get(thread).path('token').asText())
    if(!warm) {
        sampler.setName(labels[kind]);sampler.setPath(paths[kind])
        def headers=sampler.getHeaderManager()
        headers.removeHeaderNamed('Authorization')
        if(kind!=2) headers.add(new org.apache.jmeter.protocol.http.control.Header('Authorization','Bearer '+vars.get('e6Token')))
    }
    vars.put('e6TipoFallo','ninguno');vars.put('e6ContenidoValido','false')
    vars.put('e6Umbral',kind==2?'300':'800')
    return
}
if(Parameters=='validar') {
    def fail={String type ->
        vars.put('e6TipoFallo',type)
        def a=new AssertionResult(type+' - E6')
        a.setFailure(true);a.setFailureMessage(type+' - '+(type=='Contenido'?'contrato, pertenencia, orden o campos inesperados':'consultar codigo y mensaje'))
        prev.addAssertionResult(a);prev.setSuccessful(false)
    }
    String code=prev.getResponseCode()?:''
    def clean={String value ->
        def token=vars.get('e6Token')
        if(token) value=value.replace(token,'[REDACTADO]')
        value.replaceAll(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/,'[REDACTADO]')
             .replaceAll(/(?i)Bearer\s+\S+/,'Bearer [REDACTADO]')
    }
    prev.setResponseCode(clean(code));prev.setResponseMessage(code==~/[0-9]{3}/?'HTTP '+code:clean(prev.getResponseMessage()?:''))
    try {
        if(code!='200') {fail(code==~/[0-9]{3}/?'HTTP':'Transporte');return}
        def root=props.get('e6Datos');def identity=root.path('identidades').get(ctx.getThreadNum())
        int kind=vars.get('e6Tipo').toInteger()
        def expected=kind==0?identity.path('notificaciones'):kind==1?identity.path('citas'):root.path('especialidades')
        def actual=props.get('e6Mapper').readTree(prev.getResponseData())
        if(actual==null || !actual.isArray() || actual.size()!=expected.size() || actual.size()==0) {fail('Contenido');return}
        if(kind==0) {
            def headers=prev.getResponseHeaders().readLines().findAll{it==~/(?i)X-Total-Count:.*/}
            if(headers.size()!=1 || headers[0].split(':',2)[1].trim()!=identity.path('totalNotificaciones').asText()) {fail('Contenido');return}
        }
        def ids=new HashSet<Long>()
        def byId=expected.collectEntries{[(it.path('id').longValue()):it]}
        for(int i=0;i<actual.size();i++) {
            def a=actual.get(i);def id=a.path('id')
            if(!a.isObject() || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue()<=0 || !ids.add(id.longValue())) {fail('Contenido');return}
            def e=kind==2?byId[id.longValue()]:expected.get(i)
            if(e==null || id!=e.path('id')) {fail('Contenido');return}
            for(def field:e.fieldNames().toList()) {
                if(field=='orden_fecha') continue
                def av=a.path(field);def ev=e.path(field)
                if(av.isMissingNode()) {fail('Contenido');return}
                if(field in ['creadaEn','fechaHoraInicio','fechaHoraFin'] && !ev.isNull()) {
                    if(!av.isTextual() || LocalDateTime.parse(av.asText())!=LocalDateTime.parse(ev.asText())) {fail('Contenido');return}
                } else if(av!=ev) {fail('Contenido');return}
            }
            if(kind==0 && (!a.path('leida').isBoolean() || !root.path('tiposNotificacion').any{it.asText()==a.path('tipo').asText()})) {fail('Contenido');return}
            if(kind==1 && (!a.path('agendamientoManual').isBoolean() || !a.path('asignadaManualmente').isBoolean() || !root.path('estadosCita').any{it.asText()==a.path('estado').asText()})) {fail('Contenido');return}
        }
        vars.put('e6ContenidoValido','true')
    } catch(Throwable ignored) {fail('Contenido')}
    finally {vars.remove('e6Token')}
    return
}
if(Parameters=='cerrar') {
    SampleResult.setIgnore();props.remove('e6Datos');props.remove('e6Mapper');props.remove('e6Listo');return
}
throw new IllegalStateException('Accion local E6 desconocida')
