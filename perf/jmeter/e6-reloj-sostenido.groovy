import com.fasterxml.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

// Samplers locales ignorados por -l; nunca realizan HTTP.
SampleResult.setIgnore()
if(Parameters=='calentar') {
    props.put('e6RelojCierres',new ConcurrentHashMap())
    props.remove('e6Reloj')
    return
}
if(Parameters=='iniciar') {
    synchronized(props) {
        if(props.get('e6Reloj')==null) {
            long epoch=System.currentTimeMillis();long nano=System.nanoTime()
            long ramp=props.getProperty('rampa','20').toLong()*1000L
            long sustained=props.getProperty('sostenido','120').toLong()*1000L
            def t=[inicioEpochMs:epoch,finRampaEpochMs:epoch+ramp,finSostenidoEpochMs:epoch+ramp+sustained,
                inicioNanos:nano,deadlineNanos:nano+(ramp+sustained)*1000000L,
                origen:'Primer hilo, inicializador local previo a GET; no primer GET',
                regla:'Muestras por inicio: rampa [inicio,finRampa), sostenido [finRampa,finSostenido), cierre despues']
            props.put('e6Reloj',t)
            def publico=t.findAll{k,v -> !(k in ['deadlineNanos','inicioNanos'])}
            new ObjectMapper().writeValue(new File(props.getProperty('tiempoSalida')),publico)
        }
    }
    vars.put('e6ArranqueLocal',System.currentTimeMillis().toString())
    def t=props.get('e6Reloj');long now=System.nanoTime()
    vars.put('e6Continuar',(now<t.deadlineNanos).toString())
    vars.put('e6CicloAdmitidoEpochMs',(t.inicioEpochMs+(now-t.inicioNanos).intdiv(1000000L)).toString())
    return
}
if(Parameters=='ciclo') {
    int count=(vars.get('e6Cuenta')?:'0').toInteger()
    if(count==0 || count%5!=0) throw new IllegalStateException('Ciclo sostenido E6 incompleto')
    def t=props.get('e6Reloj');long now=System.nanoTime()
    vars.put('e6Continuar',(now<t.deadlineNanos).toString())
    if(now<t.deadlineNanos) vars.put('e6CicloAdmitidoEpochMs',(t.inicioEpochMs+(now-t.inicioNanos).intdiv(1000000L)).toString())
    return
}
if(Parameters=='finHilo') {
    props.get('e6RelojCierres').put(ctx.getThreadNum()+1,
        [hilo:ctx.getThreadNum()+1,inicioLocalEpochMs:vars.get('e6ArranqueLocal').toLong(),
         finLocalEpochMs:System.currentTimeMillis(),muestras:(vars.get('e6Cuenta')?:'0').toInteger()])
    return
}
if(Parameters=='cerrar') {
    def t=props.get('e6Reloj')
    if(t!=null) {
        def rows=props.get('e6RelojCierres').values().toList().sort{it.hilo}
        new ObjectMapper().writeValue(new File(props.getProperty('hilosTiempoSalida')),
            [hilos:rows,finTodosEpochMs:System.currentTimeMillis(),parada:'No iniciar otro ciclo tras plazo; completar el ciclo empezado'])
    }
    props.remove('e6Reloj');props.remove('e6RelojCierres')
    return
}
throw new IllegalStateException('Accion de reloj E6 desconocida')
