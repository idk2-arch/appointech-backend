'use strict';
const fs=require('fs'),path=require('path'),http=require('http');
const d=process.argv[2],root=JSON.parse(fs.readFileSync(path.join(d,'paquete-ficticio.json'),'utf8'));const identities=new Map(root.identidades.map(i=>[i.token,i]));
const report={HTTP:0,erroresCabecera:0,rutasAjenas:0,publicasSinAuthorization:0,identidades:root.identidades.map(i=>({ordinal:i.ordinal,notificaciones:0,historial:0,cabecerasCorrectas:true})),valoresTokenGuardados:false,backendRealHTTP:0};
const server=http.createServer((req,res)=>{report.HTTP++;let body=[];res.setHeader('Content-Type','application/json');
 if(req.method!=='GET'||!['/api/notificaciones?page=0&size=20','/api/cliente/citas','/api/especialidades'].includes(req.url)){report.rutasAjenas++;res.writeHead(404);res.end('[]');return}
 if(req.url==='/api/especialidades'){if(req.headers.authorization){report.erroresCabecera++;res.writeHead(400)}else{report.publicasSinAuthorization++;body=root.especialidades}}
 else{const header=req.headers.authorization??'',i=header.startsWith('Bearer ')?identities.get(header.slice(7)):null;if(!i){report.erroresCabecera++;res.writeHead(401)}else{const row=report.identidades[i.ordinal-1];if(req.url.startsWith('/api/notificaciones')){row.notificaciones++;body=i.notificaciones;res.setHeader('X-Total-Count',String(i.totalNotificaciones))}else{row.historial++;body=i.citas}}}
 res.end(JSON.stringify(body));
});
server.listen(0,'127.0.0.1',()=>{fs.writeFileSync(path.join(d,'servidor-listo.json'),JSON.stringify({puerto:server.address().port,PID:process.pid,host:'127.0.0.1',ficticio:true}));});
const timer=setInterval(()=>{if(fs.existsSync(path.join(d,'servidor.fin'))){clearInterval(timer);server.close(()=>{report.cerrado=true;report.completa=report.erroresCabecera===0&&report.rutasAjenas===0&&report.publicasSinAuthorization>0&&report.identidades.every(i=>i.notificaciones>0&&i.historial>0);fs.writeFileSync(path.join(d,'servidor-resumen.json'),JSON.stringify(report,null,2));});server.closeIdleConnections();}},100);
process.on('uncaughtException',()=>{clearInterval(timer);server.close();process.exitCode=1;});
