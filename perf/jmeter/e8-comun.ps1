. (Join-Path $PSScriptRoot 'e7-comun.ps1')
function Argumentos-E8([string]$JMeterDirectorio,[string]$Directorio,[string]$Paquete,[switch]$Ficticio){
    $plan=if($Ficticio){'e8-ficticio.jmx'}else{'e8-resistencia.jmx'}
    $a=@(Argumentos-E7Java $JMeterDirectorio (Join-Path $PSScriptRoot $plan) $Directorio $Paquete 50 20)
    for($i=0;$i -lt $a.Count;$i++){
        if($a[$i] -like '*e7.jtl'){$a[$i]="$Directorio/e8.jtl"}
        if($a[$i] -like '-Jsoporte=*'){$a[$i]='-Jsoporte='+(Join-Path $PSScriptRoot 'e6-lecturas.groovy')}
        if($a[$i] -like '-Jsample_variables=*'){$a[$i]='-Jsample_variables=e6Hilo,e6Tipo,e6TipoFallo,e6ContenidoValido,e6Umbral,e8Fase,e8Turno,e8Plan,e8RetrasoMs'}
    }
    $a+=@("-Jmotor=$(Join-Path $PSScriptRoot 'e8-motor.groovy')","-Jconfiguracion=$(Join-Path (Split-Path $PSScriptRoot) 'diseno/e8-resistencia.json')","-Jsalida=$Directorio","-JsetupSalida=$Directorio/setup.json","-Jrampa=$(if($Ficticio){1}else{20})","-Jficticio=$($Ficticio.IsPresent.ToString().ToLowerInvariant())",'-JvalidacionWire=false','-Jhost=localhost','-Jpuerto=18080')
    $a
}
function Hashes-E8 {
    foreach($f in @('servidor-ficticio-e8.js','generar-e8.js','e8-resistencia.jmx','e8-ficticio.jmx','e8-motor.groovy','analizar-e8.js','e8-comun.ps1','validar-e8.ps1','ejecutar-e8.ps1','probar-analisis-e8.js','crear-fixture-e8.js','inspeccionar-e8.js','e6-lecturas.groovy','preparar-e6.ps1','preparar-e2.ps1','e7-comun.ps1','recursos-e7.ps1','recursos-perf.ps1','procesos-e6.ps1','errores-e6.ps1')){@{archivo=$f;sha256=(Get-FileHash (Join-Path $PSScriptRoot $f)).Hash}}
    @{archivo='../diseno/e8-resistencia.json';sha256=(Get-FileHash (Join-Path (Split-Path $PSScriptRoot) 'diseno/e8-resistencia.json')).Hash}
}
function Convertir-E8Canonico($Valor){
    if($null -eq $Valor){return $null}
    if($Valor -is [pscustomobject]){$o=[ordered]@{};foreach($k in @($Valor.PSObject.Properties.Name|Sort-Object)){$o[$k]=Convertir-E8Canonico $Valor.$k};return $o}
    if($Valor -is [System.Collections.IList]){return ,@($Valor|ForEach-Object{Convertir-E8Canonico $_})}
    return $Valor
}
function Confirmar-E8Contenido($Respuesta,$Esperado,[Nullable[long]]$Total){
    Confirmar-E6Respuesta $Respuesta $Esperado $Total
    $parsed=$Respuesta.texto|ConvertFrom-Json
    $actual=@(foreach($row in $parsed){$row})
    $expected=@($Esperado)
    for($i=0;$i -lt $actual.Count;$i++){
        foreach($p in $expected[$i].PSObject.Properties){
            if($p.Name -eq 'orden_fecha'){continue}
            $v=$actual[$i].PSObject.Properties[$p.Name]
            if($null -eq $v){throw 'E8: campo ausente en recuperacion'}
            if($p.Name -in @('creadaEn','fechaHoraInicio','fechaHoraFin') -and $null -ne $p.Value){
                if([DateTime]::Parse([string]$v.Value,[Globalization.CultureInfo]::InvariantCulture) -ne [DateTime]::Parse([string]$p.Value,[Globalization.CultureInfo]::InvariantCulture)){throw 'E8: fecha distinta en recuperacion'}
            }elseif(((Convertir-E8Canonico $v.Value)|ConvertTo-Json -Depth 8 -Compress) -cne ((Convertir-E8Canonico $p.Value)|ConvertTo-Json -Depth 8 -Compress)){throw 'E8: contenido distinto en recuperacion'}
        }
    }
}
