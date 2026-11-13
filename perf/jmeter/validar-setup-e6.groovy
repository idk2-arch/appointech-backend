import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.apache.jmeter.util.JMeterUtils
import org.apache.jmeter.threads.JMeterContextService
import org.apache.jmeter.threads.JMeterVariables
import org.apache.jmeter.engine.StandardJMeterEngine
import org.apache.jmeter.protocol.java.sampler.JSR223Sampler

// Ejecuta unicamente el JSR223 de setUp real. No inicia motor, hilos de carga ni HTTP.
class MotorLocal extends StandardJMeterEngine {
    int paradas=0
    @Override void stopTest(boolean now) { paradas++ }
}
File home=new File(args[0]);File source=new File(args[1]);File output=new File(args[2])
output.mkdirs()
JMeterUtils.setJMeterHome(home.absolutePath)
JMeterUtils.loadJMeterProperties(new File(home,'bin/jmeter.properties').absolutePath)
JMeterUtils.initLocale()
def props=JMeterUtils.getJMeterProperties()
def ctx=JMeterContextService.getContext()
def mapper=new ObjectMapper()
def cita=[id:201,tecnicoId:null,especialidadId:1,descripcionProblema:'FICTICIO',direccionServicio:'FICTICIA',
    fechaHoraInicio:null,fechaHoraFin:null,estado:'CANCELADA',tecnicoNombre:null,tecnicoApellido:null,
    tecnicoTelefono:null,especialidadNombre:'FICTICIA',tecnicoFotoUrl:null,agendamientoManual:false,
    motivoSolicitudEspecial:null,asignadaManualmente:false,asignacionManual:null]
def valido=[identidades:[[ordinal:1,usuarioId:2,clienteId:1,correo:'cliente1@perf.test',rol:'CLIENTE',
    token:'FICTICIO_NO_JWT',totalNotificaciones:23,notificaciones:(1..20).collect{[id:it,tipo:'CITA_CANCELADA',
    titulo:'FICTICIO',mensaje:'FICTICIO',citaId:null,leida:false,creadaEn:'2026-01-01T00:00:00']},citas:[cita]]],
    especialidades:[[id:1,nombre:'FICTICIA',descripcion:null]],tiposNotificacion:['CITA_CANCELADA'],estadosCita:['CANCELADA']]
def incompleto=mapper.readTree(mapper.writeValueAsBytes(valido));incompleto.path('identidades').get(0).remove('token')
def casos=[valida:valido,ausente:null,nula:mapper.nullNode(),tipoIncorrecto:[],arregloVacio:[identidades:[]],identidadIncompleta:incompleto]
def resultados=[]
casos.each{ name,data ->
    props.remove('e6Datos');props.remove('e6Mapper');props.remove('e6Listo')
    def variables=new JMeterVariables();ctx.setVariables(variables);ctx.setThreadNum(0)
    def motor=new MotorLocal();ctx.setEngine(motor)
    File input=new File(output,name+'-ficticio.json');File status=new File(output,name+'-estado.json')
    if(name!='ausente') mapper.writeValue(input,data)
    props.setProperty('paquete',input.absolutePath);props.setProperty('setupSalida',status.absolutePath)
    props.setProperty('usuarios','1');props.setProperty('soporte',source.absolutePath)
    def setup=new JSR223Sampler();setup.setScriptLanguage('groovy');setup.setCacheKey('true')
    setup.setFilename(source.absolutePath);setup.setParameters('preparar')
    def result=setup.sample(null)
    boolean accepted=props.get('e6Listo')==true && status.exists() && mapper.readTree(status).path('estado').asText()=='listo'
    resultados<<[caso:name,aceptado:accepted,esperado:name=='valida',paradasMotorLocal:motor.paradas]
    if(input.exists()) input.delete()
}
def informe=[casos:resultados,todosCorrectos:resultados.every{it.aceptado==it.esperado},HTTP:0,motorCargaIniciado:false,
    fuente:source.name,metodo:'JSR223Sampler.sample del mismo setUp, con contexto local y JSON ficticio']
new File(output,'validacion-setup.json').text=JsonOutput.prettyPrint(JsonOutput.toJson(informe))
println JsonOutput.toJson(informe)
if(!informe.todosCorrectos) System.exit(1)
