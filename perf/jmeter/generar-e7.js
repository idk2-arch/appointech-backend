'use strict';
const fs=require('fs'),path=require('path');
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const sp=(n,v)=>`<stringProp name="${n}">${esc(v)}</stringProp>`;
const bp=(n,v)=>`<boolProp name="${n}">${v}</boolProp>`;
const node=(xml,children='')=>xml+`<hashTree>${children}</hashTree>`;
const jsr=(action,type='JSR223Sampler')=>node(`<${type} guiclass="TestBeanGUI" testclass="${type}" testname="E7 local ${action}">${sp('scriptLanguage','groovy')}${sp('cacheKey','true')}${sp('filename','${__P(soporte)}')}${sp('parameters',action)}</${type}>`);
function group(type,name,children,threads='1',ramp='0'){
 return node(`<${type} guiclass="${type}Gui" testclass="${type}" testname="${name}">${sp('ThreadGroup.on_sample_error',type==='ThreadGroup'?'continue':'stoptestnow')}<elementProp name="ThreadGroup.main_controller" elementType="LoopController">${bp('LoopController.continue_forever','false')}${sp('LoopController.loops','1')}</elementProp>${sp('ThreadGroup.num_threads',threads)}${sp('ThreadGroup.ramp_time',ramp)}${bp('ThreadGroup.scheduler','false')}</${type}>`,children);
}
function generate(fake=false){
 const headers=node(`<HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="E7 cabeceras privadas"><collectionProp name="HeaderManager.headers"><elementProp name="" elementType="Header">${sp('Header.name','Authorization')}${sp('Header.value','${e7Autorizacion}')}</elementProp><elementProp name="" elementType="Header">${sp('Header.name','Content-Type')}${sp('Header.value','application/json')}</elementProp><elementProp name="" elementType="Header">${sp('Header.name','Accept')}${sp('Header.value','application/json')}</elementProp></collectionProp></HeaderManager>`);
 const http=`<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="\${e7Etiqueta}">
 ${sp('HTTPSampler.protocol','http')}${sp('HTTPSampler.domain','localhost')}${sp('HTTPSampler.port','18080')}${sp('HTTPSampler.method','${e7Metodo}')}${sp('HTTPSampler.path','${e7Ruta}')}
 ${sp('HTTPSampler.implementation','HttpClient4')}${sp('HTTPSampler.connect_timeout','5000')}${sp('HTTPSampler.response_timeout','10000')}
 ${bp('HTTPSampler.follow_redirects','false')}${bp('HTTPSampler.auto_redirects','false')}${bp('HTTPSampler.use_keepalive','true')}${bp('HTTPSampler.postBodyRaw','true')}
 <elementProp name="HTTPsampler.Arguments" elementType="Arguments"><collectionProp name="Arguments.arguments"><elementProp name="" elementType="HTTPArgument">${bp('HTTPArgument.always_encode','false')}${sp('Argument.value','${e7Cuerpo}')}${sp('Argument.metadata','=')}</elementProp></collectionProp></elementProp></HTTPSamplerProxy>`;
 const fakeSample=`<JSR223Sampler guiclass="TestBeanGUI" testclass="JSR223Sampler" testname="\${e7Etiqueta}">${sp('scriptLanguage','groovy')}${sp('cacheKey','true')}${sp('filename','${__P(soporte)}')}${sp('parameters','ficticio')}</JSR223Sampler>`;
 const sample=node(fake?fakeSample:http,(fake?'':headers)+jsr('validar','JSR223PostProcessor'));
 const conditional=node(`<IfController guiclass="IfControllerPanel" testclass="IfController" testname="E7 solo paso admitido">${sp('IfController.condition','${e7Ejecutar}')}${bp('IfController.evaluateAll','false')}${bp('IfController.useExpression','true')}</IfController>`,sample);
 const steps=node(`<LoopController guiclass="LoopControlPanel" testclass="LoopController" testname="E7 seis pasos">${bp('LoopController.continue_forever','true')}${sp('LoopController.loops','6')}</LoopController>`,jsr('paso')+conditional);
 const cycles=node(`<LoopController guiclass="LoopControlPanel" testclass="LoopController" testname="E7 veinte ciclos">${bp('LoopController.continue_forever','true')}${sp('LoopController.loops','20')}</LoopController>`,jsr('ciclo')+steps);
 return `<?xml version="1.0" encoding="UTF-8"?><jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3"><hashTree>`+
 node(`<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="E7 CRUD 50-100-150 x20">${bp('TestPlan.functional_mode','false')}</TestPlan>`,
 group('SetupThreadGroup','E7 preparar sin HTTP',jsr('preparar'))+
 group('ThreadGroup','E7 CRUD',jsr('barrera')+cycles+jsr('fin'),'${__P(usuarios,50)}','${__P(rampa,20)}')+
 group('PostThreadGroup','E7 cerrar',jsr('cerrar')))+`</hashTree></jmeterTestPlan>`;
}
module.exports={generate};
if(require.main===module){fs.writeFileSync(path.join(__dirname,'e7-crud.jmx'),generate(false));fs.writeFileSync(path.join(__dirname,'e7-ficticio.jmx'),generate(true));console.log('E7: XML real y ficticio generados; no HTTP.');}
