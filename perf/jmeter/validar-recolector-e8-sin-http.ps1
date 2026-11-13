[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Salida)
$ErrorActionPreference='Stop'
$Salida=[IO.Path]::GetFullPath($Salida)
. (Join-Path $PSScriptRoot 'recursos-e7.ps1')
function Probar-Cobertura([object[]]$Filas,[long]$Inicio,[long]$Fin){
    $pares=@($Filas|Group-Object epochMs)
    $completos=$Filas.Count -ge 2 -and @($pares|Where-Object{$_.Count -ne 2 -or @($_.Group.servicio|Select-Object -Unique).Count -ne 2}).Count -eq 0
    $ts=@($pares|ForEach-Object{[long]$_.Name}|Sort-Object)
    $hueco=0L;for($i=1;$i -lt $ts.Count;$i++){$hueco=[math]::Max($hueco,$ts[$i]-$ts[$i-1])}
    [pscustomobject]@{completa=($completos -and $ts.Count -gt 0 -and $ts[0] -le $Inicio -and $ts[-1] -ge $Fin -and $hueco -le 6000);
        paresCompletos=$completos;maxHuecoMs=$hueco;primera=$(if($ts.Count){$ts[0]}else{$null});ultima=$(if($ts.Count){$ts[-1]}else{$null})}
}
New-Item -ItemType Directory -Path $Salida -ErrorAction Stop|Out-Null
$m=$null
try{
    # -Ficticio no llama Docker ni backend. Solo genera dos filas sinteticas por captura.
    $m=Iniciar-E7Recursos ([pscustomobject]@{BackendId='ficticio-backend';DbId='ficticio-db'}) $Salida -Ficticio
    Esperar-MuestreoPerf $m
    $warm=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();Start-Sleep -Milliseconds 200
    $inicio=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();Start-Sleep -Milliseconds 1200
    $fin=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();Start-Sleep -Milliseconds 200
    $inicioRec=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();Start-Sleep -Milliseconds 400
    $finRec=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();Start-Sleep -Milliseconds 200
    $validado=Detener-E7Recursos $m;$m=$null
    $filas=@(Import-Csv (Join-Path $Salida 'recursos.csv'))
    $cobertura=Probar-Cobertura $filas $warm $finRec
    if(-not $cobertura.completa){throw 'E8: cobertura ficticia incompleta.'}
    $cortadas=@($filas|Where-Object{[long]$_.epochMs -lt $finRec})
    if((Probar-Cobertura $cortadas $warm $finRec).completa){throw 'E8: no detecta perdida de cobertura final.'}
    $sinPareja=@($filas|Select-Object -Skip 1)
    if((Probar-Cobertura $sinPareja $warm $finRec).completa){throw 'E8: no detecta captura sin pareja.'}
    @{completa=$true;HTTP=0;JMeter=0;backendConsultado=$false;recursosRealesConsultados=$false;
        tipo='Prueba local acotada del recolector, no resistencia E8 de 600 s';medicionFicticiaMs=($fin-$inicio);inicioMedicion=$inicio;finMedicion=$fin;
        inicioCalentamiento=$warm;inicioRecuperacion=$inicioRec;finRecuperacion=$finRec;cobertura=$cobertura;CSV=$validado;
        detectaFaltaFinal=$true;detectaParejaIncompleta=$true;escritorCerrado=$true}|
        ConvertTo-Json -Depth 7|Set-Content (Join-Path $Salida 'prueba-recursos.json') -Encoding UTF8
    Get-Content (Join-Path $Salida 'prueba-recursos.json') -Raw
}finally{if($m){Detener-E7Recursos $m|Out-Null}}
