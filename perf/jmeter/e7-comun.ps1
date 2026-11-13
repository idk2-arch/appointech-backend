. (Join-Path $PSScriptRoot 'preparar-e2.ps1')
. (Join-Path $PSScriptRoot 'preparar-e6.ps1')
. (Join-Path $PSScriptRoot 'procesos-e6.ps1')
. (Join-Path $PSScriptRoot 'recursos-e7.ps1')

function Argumentos-E7Java([string]$JMeterHome,[string]$Plan,[string]$Directorio,[string]$Paquete,[int]$Usuarios,[int]$Rampa,[switch]$Dashboard){
    $a=@('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-Duser.timezone=America/Bogota',
        '-jar',(Join-Path $JMeterHome 'bin/ApacheJMeter.jar'),'-n','-t',$Plan,'-l',"$Directorio/e7.jtl",'-j',"$Directorio/jmeter.log",
        "-Jpaquete=$Paquete","-Jsoporte=$(Join-Path $PSScriptRoot 'e7-motor.groovy')","-Jusuarios=$Usuarios","-Jrampa=$Rampa",
        '-Jhttpclient4.retrycount=0','-Jhttpclient4.request_sent_retry_enabled=false','-Jhttpclient4.idletimeout=0',
        '-Jsampleresult.timestamp.start=true','-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF',
        '-Jsample_variables=e7Hilo,e7Ciclo,e7Paso,e7Tecnico,e7Fecha,e7Id,e7Contenido,e7Sql,e7TipoFallo,e7Incierta')
    $guardado=@('timestamp_format=ms','default_delimiter=,','print_field_names=true','time=true','label=true','response_code=true',
        'response_message=true','thread_name=true','successful=true','assertion_results_failure_message=true','bytes=true','sent_bytes=true',
        'thread_counts=true','url=true','latency=true','idle_time=true','connect_time=true','output_format=csv','assertion_results=none',
        'response_data=false','response_data.on_error=false','samplerData=false','requestHeaders=false','responseHeaders=false','subresults=false',
        'encoding=false','data_type=false','filename=false','hostname=false','sample_count=false')
    foreach($g in $guardado){$a+="-Jjmeter.save.saveservice.$g"}
    if($Dashboard){$a+=@('-e','-o',"$Directorio/dashboard")}
    $a
}
function Java-E7 {
    $java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}
    if(-not $java -or -not(Test-Path -LiteralPath $java)){$java=(Get-Command java.exe -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source}
    $java
}
function Invocar-E7Java([string]$Java,[string[]]$Argumentos,[string]$Directorio,[int]$TimeoutSegundos=3600,[string[]]$Secretos=@()){
    $si=New-Object Diagnostics.ProcessStartInfo
    $si.FileName=$Java
    $si.Arguments=(@($Argumentos|ForEach-Object{'"'+($_ -replace '(\\*)"','$1$1\"' -replace '(\\+)$','$1$1')+'"'}) -join ' ')
    $si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true
    $p=[Diagnostics.Process]::Start($si);$forzado=$false;$code=$null
    try{
        $stdout=$p.StandardOutput.ReadToEndAsync();$stderr=$p.StandardError.ReadToEndAsync()
        if(-not $p.WaitForExit($TimeoutSegundos*1000)){$forzado=$true;$p.Kill();$p.WaitForExit()}
        $code=$p.ExitCode
        $texto=$stdout.GetAwaiter().GetResult()+"`n"+$stderr.GetAwaiter().GetResult()
        foreach($s in $Secretos){if($s){$texto=$texto.Replace($s,'[REDACTADO]')}}
        $texto=$texto -replace 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[JWT_REDACTADO]'
        [IO.File]::WriteAllText((Join-Path $Directorio 'java-consola.log'),$texto,[Text.UTF8Encoding]::new($false))
        @{PID=$p.Id;codigo=$code;cierreForzado=$forzado;timeoutSegundos=$TimeoutSegundos;terminado=$true}|ConvertTo-Json|
            Set-Content (Join-Path $Directorio 'java-final.json') -Encoding UTF8
        if($forzado -or $code -ne 0){throw 'E7: JMeter termino con fallo o watchdog.'}
    }finally{if(-not $p.HasExited){$p.Kill();$p.WaitForExit()};$p.Dispose()}
}
function Confirmar-E7Fuentes {
    $referencia=Get-Content (Join-Path (Split-Path $PSScriptRoot) 'inspecciones/e7-diseno-20261111-164507/alcance-y-fuentes.json') -Raw|ConvertFrom-Json
    $raiz=Split-Path (Split-Path $PSScriptRoot)
    foreach($f in $referencia.fuentes){if((Get-FileHash (Join-Path $raiz $f.archivo)).Hash -ine $f.sha256){throw 'E7: cambio una fuente del contrato inspeccionado.'}}
}
function Huella-E7($Compose,$Aislamiento){
    # Descubrir todas las tablas de negocio, sin log_sistema ni metadatos Flyway.
    $nombres=@(Consultar-E2 $Compose $Aislamiento "BEGIN READ ONLY; SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('log_sistema','flyway_schema_history') ORDER BY tablename; ROLLBACK;")
    if(-not $nombres.Count -or @($nombres|Where-Object{$_ -notmatch '^[a-z_]+$'}).Count){throw 'E7: esquema no identificable.'}
    $partes=@(foreach($tabla in $nombres){
        "SELECT '$tabla' AS tabla,count(*) AS cantidad,md5(coalesce(string_agg(md5(row_to_json(t)::text),'' ORDER BY row_to_json(t)::text),'')) AS huella FROM $tabla t"
    })
    $sql="BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY; SELECT json_agg(s ORDER BY tabla) FROM ("+($partes -join ' UNION ALL ')+")s; ROLLBACK;"
    $filas=Consultar-E2 $Compose $Aislamiento $sql|Out-String|ConvertFrom-Json
    foreach($fila in $filas){$fila}
}
