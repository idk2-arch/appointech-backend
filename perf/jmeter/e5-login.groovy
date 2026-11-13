import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import org.apache.jmeter.save.CSVSaveService
import org.apache.jmeter.assertions.AssertionResult
import org.apache.jmeter.modifiers.JSR223PreProcessor
import org.apache.jmeter.extractor.JSR223PostProcessor
import org.apache.jmeter.samplers.SampleResult as Resultado
import java.nio.file.Files
import java.nio.charset.StandardCharsets

if (Parameters == 'preparar') {
    SampleResult.setIgnore()
    def inicio = System.currentTimeMillis()
    try {
        def mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        def credenciales = [:]
        Files.newBufferedReader(new File(props.getProperty('credenciales')).toPath(), StandardCharsets.UTF_8).withCloseable { lector ->
            def cabecera = CSVSaveService.csvReadFile(lector, ',' as char)
            if (cabecera == null || cabecera.size()!=2 || cabecera[0].replace('\uFEFF','')!='correo' || cabecera[1]!='contrasena')
                throw new IllegalStateException('Encabezado CSV no valido')
            while (true) {
                def fila = CSVSaveService.csvReadFile(lector, ',' as char)
                if (fila == null || fila.length==0) break
                if (fila.length!=2 || !(fila[0] ==~ /cliente[1-9][0-9]*@perf\.test/) || fila[1].isBlank() || credenciales.containsKey(fila[0]))
                    throw new IllegalStateException('Cuenta sintetica CSV no valida o duplicada')
                credenciales[fila[0]]=fila[1]
            }
        }
        int usuarios=props.getProperty('usuarios','50').toInteger()
        def preparados=(1..usuarios).collect { i ->
            def correo='cliente'+i+'@perf.test'
            def secreto=credenciales[correo]
            if(!secreto) throw new IllegalStateException('Falta cuenta sintetica')
            [cuerpo:mapper.writeValueAsString([correo:correo,contrasena:secreto]),secreto:secreto]
        }
        props.put('e5Cuentas',preparados);props.put('e5Mapper',mapper)
        vars.put('e5Preparando','true')
        // Se ejercita el mismo archivo mediante los elementos JMeter y su cache real.
        def pre=new JSR223PreProcessor()
        pre.setScriptLanguage('groovy');pre.setCacheKey('true');pre.setFilename(props.getProperty('soporte'));pre.setParameters('antes');pre.process()
        def ficticio=new Resultado();ficticio.sampleStart();ficticio.setResponseCode('200');ficticio.setSuccessful(true)
        ficticio.setResponseData(mapper.writeValueAsBytes([exito:true,datos:[token:'FICTICIO_NO_JWT']]))
        ficticio.sampleEnd();ctx.setPreviousResult(ficticio)
        def post=new JSR223PostProcessor()
        post.setScriptLanguage('groovy');post.setCacheKey('true');post.setFilename(props.getProperty('soporte'));post.setParameters('validar');post.process()
        if(!ficticio.isSuccessful()) throw new IllegalStateException('Contrato ficticio no validado')
        new org.apache.jmeter.protocol.http.sampler.HTTPSampleResult()
        Class.forName('org.apache.jmeter.protocol.http.sampler.HTTPHC4Impl')
        // Inicializar tambien bibliotecas del cliente, sin execute/send ni conexion.
        def clienteLocal=org.apache.http.impl.client.HttpClients.createDefault()
        clienteLocal.close()
        props.put('e5Listo',true)
        mapper.writeValue(new File(props.getProperty('preparacionSalida')),
            [inicioEpochMs:inicio,finEpochMs:System.currentTimeMillis(),cuentas:usuarios,solicitudesHTTP:0])
    } catch(Throwable ignorado) {
        ctx.getEngine().stopTest(true)
        throw new IllegalStateException('Preparacion local E5 fallida; no iniciar carga')
    }
    return
}
if (Parameters == 'antes') {
    def inicio=System.currentTimeMillis()
    def cuentas=props.get('e5Cuentas')
    if(!cuentas || ctx.getThreadNum()>=cuentas.size() || (vars.get('e5Preparando')!='true' && props.get('e5Listo')!=true)) {
        ctx.getEngine().stopTest(true);throw new IllegalStateException('Identidad E5 no preparada')
    }
    vars.remove('e5Token')
    vars.put('e5Hilo',(ctx.getThreadNum()+1).toString())
    vars.put('e5Iteracion',((vars.get('e5Iteracion')?:'0').toInteger()+1).toString())
    vars.put('e5TipoFallo','ninguno');vars.put('e5ContenidoValido','false')
    vars.put('e5Cuerpo',cuentas[ctx.getThreadNum()].cuerpo)
    vars.put('e5PreInicioEpochMs',inicio.toString())
    vars.put('e5PreFinEpochMs',System.currentTimeMillis().toString())
    return
}
if (Parameters == 'validar') {
    def fallar={String tipo ->
        vars.put('e5TipoFallo',tipo)
        def a=new AssertionResult(tipo+' - login')
        a.setFailure(true);a.setFailureMessage(tipo+' - '+(tipo=='Contenido'?'JSON, exito booleano o datos.token no validos':'consultar codigo y mensaje de respuesta'))
        prev.addAssertionResult(a);prev.setSuccessful(false)
    }
    vars.remove('e5Cuerpo')
    def codigo=prev.getResponseCode()?:''
    try {
        if(codigo ==~ /[0-9]{3}/) prev.setResponseMessage('HTTP '+codigo)
        else {
            def secreto=props.get('e5Cuentas')[ctx.getThreadNum()].secreto
            def limpiar={String texto -> texto.replace(secreto,'[REDACTADO]')
                .replaceAll(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/,'[REDACTADO]')
                .replaceAll(/(?i)Bearer\s+\S+/,'Bearer [REDACTADO]')}
            prev.setResponseCode(limpiar(codigo));prev.setResponseMessage(limpiar(prev.getResponseMessage()?:''))
        }
        if(codigo!='200') fallar(codigo==~/[0-9]{3}/?'HTTP':'Transporte')
        else {
            try {
                def j=props.get('e5Mapper').readTree(prev.getResponseData())
                def token=j?.path('datos')?.path('token')
                if(!j?.isObject() || !j.path('exito').isBoolean() || !j.path('exito').booleanValue() ||
                    !token?.isTextual() || token.textValue().isBlank()) fallar('Contenido')
                else {vars.put('e5Token',token.textValue());vars.put('e5ContenidoValido','true')}
            } catch(Throwable ignorado) {fallar('Contenido')}
        }
    } finally {vars.remove('e5Token')}
    return
}
if (Parameters == 'cerrar') {
    SampleResult.setIgnore()
    props.remove('e5Cuentas');props.remove('e5Mapper');props.remove('e5Listo')
    return
}
throw new IllegalStateException('Accion E5 desconocida')
