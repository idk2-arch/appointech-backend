import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import org.apache.jmeter.assertions.AssertionResult
import org.apache.jmeter.modifiers.JSR223PreProcessor
import org.apache.jmeter.extractor.JSR223PostProcessor
import org.apache.jmeter.samplers.SampleResult as Resultado
import java.time.LocalDateTime

def partes = Parameters.split(' ')
def accion = partes[0]
def paso = partes.length > 1 ? partes[1] : ''
def umbrales = [login:2000, consulta:300, crear:1500, cancelar:1500, detalle:800]
if (accion == 'setup') {
    SampleResult.setIgnore()
    def inicio = System.currentTimeMillis()
    try {
        def mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        def usuarios = (1..Integer.parseInt(props.getProperty('usuarios','10'))).collect { i ->
            def texto = System.getenv(props.getProperty('identidadesEnv') + i)
            if (!texto) throw new IllegalStateException('Paquete E4 ausente')
            def datos = mapper.readTree(texto)
            def login = mapper.writeValueAsString([correo:datos.path('correo').textValue(), contrasena:datos.path('contrasena').textValue()])
            def flujos = datos.path('flujos').collect { f ->
                [marca:f.path('marca').textValue(), crear:mapper.writeValueAsString(f.path('crear')),
                 cancelar:mapper.writeValueAsString([motivo:f.path('marca').textValue()])]
            }
            [correo:datos.path('correo').textValue(), secreto:datos.path('contrasena').textValue(),
             especialidadId:datos.path('especialidadId').longValue(), especialidadNombre:datos.path('especialidadNombre').textValue(),
             login:login, flujos:flujos]
        }
        props.put('e4Usuarios', usuarios); props.put('e4Mapper', mapper)
        props.put('e4Resultados',new java.util.concurrent.ConcurrentLinkedQueue())
        vars.put('e4Simulado','true')
        // Mismo archivo, cache real y caminos de preparacion/validacion, sin HTTP.
        ['login','consulta','crear','cancelar','detalle'].each { nombre ->
            def pre = new JSR223PreProcessor()
            pre.setScriptLanguage('groovy'); pre.setCacheKey('true'); pre.setFilename(props.getProperty('soporte'))
            pre.setParameters('antes ' + nombre); pre.process()
            def u=usuarios[0]; def f=vars.getObject('e4Actual')
            def cita=[id:1,tecnicoId:null,especialidadId:u.especialidadId,descripcionProblema:f.marca,
                direccionServicio:'Calle 100 # 10-20 Perf',fechaHoraInicio:null,fechaHoraFin:null,
                estado:nombre=='crear'?'PENDIENTE':'CANCELADA',especialidadNombre:u.especialidadNombre,
                tecnicoNombre:null,tecnicoApellido:null,tecnicoTelefono:null,tecnicoFotoUrl:null,
                agendamientoManual:false,motivoSolicitudEspecial:null,asignadaManualmente:false,asignacionManual:null]
            def respuesta = nombre=='login' ? [exito:true,datos:[token:'TOKEN_FICTICIO_NO_JWT',perfilCompleto:true,fotoUrl:null],
                mensaje:'Inicio de sesion exitoso',timestamp:'2026-01-01T00:00:00'] :
                nombre=='consulta' ? [[id:u.especialidadId,nombre:u.especialidadNombre,descripcion:null]] : cita
            if(nombre=='detalle') {
                respuesta=cita + [diagnostico:null,fotosElectrodomestico:[],historial:[[estadoAnterior:'PENDIENTE',
                    estadoNuevo:'CANCELADA',fecha:'2026-01-01T00:00:00',actor:u.correo,motivo:f.marca]]]
            }
            def ficticio=new Resultado();ficticio.sampleStart();ficticio.setResponseCode(nombre=='crear'?'201':'200')
            ficticio.setResponseData(mapper.writeValueAsBytes(respuesta));ficticio.setSuccessful(true);ficticio.sampleEnd()
            ctx.setPreviousResult(ficticio)
            def post=new JSR223PostProcessor()
            post.setScriptLanguage('groovy');post.setCacheKey('true');post.setFilename(props.getProperty('soporte'))
            post.setParameters('validar '+nombre);post.process()
            if(!ficticio.isSuccessful()) throw new IllegalStateException('Validacion ficticia E4 fallida')
        }
        new org.apache.jmeter.protocol.http.sampler.HTTPSampleResult()
        Class.forName('org.apache.jmeter.protocol.http.sampler.HTTPHC4Impl')
        props.put('e4Listo',true)
        mapper.writeValue(new File(props.getProperty('preparacionGenerador')),
            [inicioEpochMs:inicio,finEpochMs:System.currentTimeMillis(),usuarios:usuarios.size(),solicitudesHTTP:0])
    } catch(Throwable ignorado) {
        ctx.getEngine().stopTest(true)
        throw new IllegalStateException('Preparacion local E4 fallida; no iniciar carga')
    }
    return
}
if(accion=='antes') {
    def inicio=System.currentTimeMillis()
    if(vars.get('e4Simulado')!='true' && props.get('e4Listo')!=true) {
        ctx.getEngine().stopTest(true);throw new IllegalStateException('Preparacion E4 no disponible')
    }
    def u=props.get('e4Usuarios')[ctx.getThreadNum()]
    if(paso=='login') {
        int iteracion=(vars.get('e4Iteracion')?:'0').toInteger()+1
        def preparado=u.flujos[iteracion-1]
        vars.remove('e4Token');vars.remove('e4CitaId');vars.put('e4Continuar','false');vars.put('e4PuedeConsultar','false')
        vars.put('e4Iteracion',iteracion.toString());vars.put('e4Hilo',(ctx.getThreadNum()+1).toString())
        vars.put('e4Marca',preparado.marca)
        def actual=[marca:preparado.marca,hilo:ctx.getThreadNum()+1,iteracion:iteracion,pasos:[],funcional:true,citaId:null]
        vars.putObject('e4Actual',actual)
        if(vars.get('e4Simulado')!='true') props.get('e4Resultados').add(actual)
    }
    def preparado=u.flujos[vars.get('e4Iteracion').toInteger()-1]
    vars.put('e4Cuerpo', paso=='login'?u.login:paso=='crear'?preparado.crear:paso=='cancelar'?preparado.cancelar:'')
    vars.putObject('e4PreInicio',inicio);vars.putObject('e4PreFin',System.currentTimeMillis())
    return
}
if(accion=='validar') {
    def u=props.get('e4Usuarios')[ctx.getThreadNum()]
    def f=vars.getObject('e4Actual')
    def codigo=prev.getResponseCode()?:''
    def resultado=[paso:paso,codigo:codigo,inicioEpochMs:prev.getStartTime(),finEpochMs:prev.getEndTime(),
        elapsed:prev.getTime(),preInicioEpochMs:vars.getObject('e4PreInicio'),preFinEpochMs:vars.getObject('e4PreFin'),
        funcional:true,categoria:null,motivo:null,sobreUmbral:prev.getTime()>umbrales[paso]]
    def fallar={ String categoria,String motivo ->
        resultado.funcional=false;resultado.categoria=categoria;resultado.motivo=motivo;f.funcional=false
        def a=new AssertionResult(categoria+' - '+paso);a.setFailure(true);a.setFailureMessage(categoria+' - '+motivo)
        prev.addAssertionResult(a);prev.setSuccessful(false)
    }
    vars.remove('e4Cuerpo')
    if(codigo ==~ /[0-9]{3}/) prev.setResponseMessage('HTTP '+codigo)
    else {
        def limpiar={String s -> s.replace(u.secreto,'[REDACTADO]').replace(vars.get('e4Token')?:'TOKEN_AUSENTE_IMPOSIBLE','[REDACTADO]')
            .replaceAll(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/,'[REDACTADO]')}
        prev.setResponseCode(limpiar(codigo));prev.setResponseMessage(limpiar(prev.getResponseMessage()?:''))
        resultado.codigo=prev.getResponseCode()
    }
    if(codigo!=(paso=='crear'?'201':'200')) fallar(codigo==~/[0-9]{3}/?'HTTP':'Transporte','consultar codigo y mensaje originales')
    else {
        try {
            def j=props.get('e4Mapper').readTree(prev.getResponseData())
            def exigir={boolean condicion -> if(!condicion) throw new IllegalArgumentException('Contrato no valido')}
            def positivo={n -> n.isIntegralNumber() && n.canConvertToLong() && n.longValue()>0}
            if(paso=='login') {
                exigir(j.isObject() && j.path('exito').isBoolean() && j.path('exito').booleanValue())
                def d=j.path('datos');exigir(d.isObject() && d.path('token').isTextual() && !d.path('token').textValue().isBlank())
                exigir(d.path('perfilCompleto').isBoolean() && d.path('perfilCompleto').booleanValue())
                exigir(d.path('fotoUrl').isNull() || d.path('fotoUrl').isTextual())
                exigir(j.path('timestamp').isTextual());LocalDateTime.parse(j.path('timestamp').textValue())
                vars.put('e4Token',d.path('token').textValue())
            } else if(paso=='consulta') {
                exigir(j.isArray() && j.size()>0)
                def ids=[]
                j.each { e -> exigir(e.isObject() && positivo(e.path('id')) && e.path('nombre').isTextual())
                    exigir(e.path('descripcion').isNull() || e.path('descripcion').isTextual()); ids.add(e.path('id').longValue()) }
                exigir(ids.unique(false).size()==ids.size())
                exigir(j.any { e -> e.path('id').longValue()==u.especialidadId && e.path('nombre').textValue()==u.especialidadNombre })
            } else {
                exigir(j.isObject() && positivo(j.path('id')))
                exigir(j.path('descripcionProblema').textValue()==f.marca && j.path('direccionServicio').textValue()=='Calle 100 # 10-20 Perf')
                exigir(j.path('especialidadNombre').textValue()==u.especialidadNombre)
                exigir(j.path('estado').textValue()==(paso=='crear'?'PENDIENTE':'CANCELADA'))
                ['tecnicoId','tecnicoNombre','tecnicoApellido','tecnicoTelefono','tecnicoFotoUrl','fechaHoraInicio','fechaHoraFin','asignacionManual'].each { k -> exigir(j.has(k) && j.path(k).isNull()) }
                exigir(j.path('asignadaManualmente').isBoolean() && !j.path('asignadaManualmente').booleanValue())
                if(paso!='detalle') {
                    exigir(j.path('especialidadId').isIntegralNumber() && j.path('especialidadId').longValue()==u.especialidadId)
                    exigir(j.path('agendamientoManual').isBoolean() && !j.path('agendamientoManual').booleanValue())
                    exigir(j.has('motivoSolicitudEspecial') && j.path('motivoSolicitudEspecial').isNull())
                }
                if(paso=='crear') {
                    vars.put('e4CitaId',j.path('id').asText());vars.put('e4PuedeConsultar','true');f.citaId=j.path('id').longValue()
                } else exigir(j.path('id').asText()==vars.get('e4CitaId'))
                if(paso=='detalle') {
                    exigir(j.has('diagnostico') && j.path('diagnostico').isNull())
                    exigir(j.path('fotosElectrodomestico').isArray() && j.path('fotosElectrodomestico').size()==0)
                    def h=j.path('historial');exigir(h.isArray() && h.size()==1)
                    exigir(h[0].path('estadoAnterior').textValue()=='PENDIENTE' && h[0].path('estadoNuevo').textValue()=='CANCELADA')
                    exigir(h[0].path('actor').textValue()==u.correo && h[0].path('motivo').textValue()==f.marca)
                    exigir(h[0].path('fecha').isTextual());LocalDateTime.parse(h[0].path('fecha').textValue())
                }
            }
        } catch(Throwable ignorado) { fallar('Contenido','JSON, tipos o valores esperados no coinciden') }
    }
    f.pasos.add(resultado)
    // Una duracion fallida no corta dependencias; un fallo funcional no se borra despues.
    vars.put('e4Continuar',resultado.funcional?'true':'false')
    return
}
if(accion=='cerrar') {
    SampleResult.setIgnore()
    def filas=props.get('e4Resultados')?:[]
    filas.each { f ->
        f.completo=f.funcional && f.pasos.size()==5
        f.aprobado=f.completo && !f.pasos.any {it.sobreUmbral}
    }
    props.get('e4Mapper').writeValue(new File(props.getProperty('flujosSalida')),filas.toList())
    props.remove('e4Usuarios');props.remove('e4Mapper')
    return
}
throw new IllegalStateException('Accion E4 desconocida')
