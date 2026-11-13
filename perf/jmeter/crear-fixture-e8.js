const fs=require('fs');const root={identidades:[],especialidades:[{id:1,nombre:'FICTICIO',descripcion:null}],tiposNotificacion:['FICTICIO'],estadosCita:['CONFIRMADA']};
for(let h=1;h<=50;h++){
 const payload={sub:`cliente${h}@perf.test`,rol:'CLIENTE',id:h,iat:Math.floor(Date.now()/1000)-5,exp:Math.floor(Date.now()/1000)+86400};
 const token=[{alg:'HS256'},payload].map(x=>Buffer.from(JSON.stringify(x)).toString('base64url')).join('.')+'.FICTICIO';
 const cita={id:h};for(const k of ['tecnicoId','especialidadId','descripcionProblema','direccionServicio','fechaHoraInicio','fechaHoraFin','tecnicoNombre','tecnicoApellido','tecnicoTelefono','especialidadNombre','tecnicoFotoUrl','motivoSolicitudEspecial','asignacionManual'])cita[k]=null;Object.assign(cita,{estado:'CONFIRMADA',agendamientoManual:false,asignadaManualmente:false});
 root.identidades.push({ordinal:h,usuarioId:h,clienteId:h,correo:payload.sub,rol:'CLIENTE',token,totalNotificaciones:1,notificaciones:[{id:h,tipo:'FICTICIO',titulo:'LOCAL',mensaje:'LOCAL',citaId:h,leida:false,creadaEn:'2030-01-01T00:00:00'}],citas:[cita]});
}
fs.writeFileSync(process.argv[2],JSON.stringify(root));
