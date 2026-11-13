[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Directorio)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path (Split-Path $PSScriptRoot) 'resultados'))+[IO.Path]::DirectorySeparatorChar
$d=[IO.Path]::GetFullPath($Directorio)
if(-not $d.StartsWith($root,[StringComparison]::OrdinalIgnoreCase) -or -not(Test-Path "$d/E8-REAL-INICIADO.json")){throw 'No es una corrida E8 propia identificada'}
$log=Get-Content "$d/jmeter.log" -Raw
if($log -notmatch 'Notifying test listeners of end of test'){throw 'No se acredita que el motor HTTP haya terminado'}
$e8Candidates=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -eq 'java.exe' -and $_.CommandLine.IndexOf($d,[StringComparison]::OrdinalIgnoreCase) -ge 0 -and $_.CommandLine -match 'ApacheJMeter[.]jar' -and $_.CommandLine -match 'e8-resistencia[.]jmx'})
$snapshot=@(foreach($p in $e8Candidates){@{capturaUTC=[DateTime]::UtcNow.ToString('o');PID=$p.ProcessId;ParentPID=$p.ParentProcessId;inicioUTC=$p.CreationDate.ToUniversalTime().ToString('o');ejecutable=$p.ExecutablePath;corrida=$d;motivo='Motor ya finalizado; monitor no daemon impide salir tras parada por contenido';propio=$true}})
ConvertTo-Json -InputObject $snapshot -Depth 6|Set-Content (Join-Path $d ('diagnostico-cierre-'+[Guid]::NewGuid().ToString('N')+'.json')) -Encoding UTF8
if($e8Candidates.Count -gt 2){throw 'Mas de dos candidatos; no terminar'}
if($e8Candidates.Count -eq 2 -and $e8Candidates[0].ParentProcessId -ne $e8Candidates[1].ProcessId -and $e8Candidates[1].ParentProcessId -ne $e8Candidates[0].ProcessId){throw 'No son lanzador e hijo de la misma corrida; no terminar'}
foreach($p in @($e8Candidates|Sort-Object CreationDate -Descending)){
    $current=Get-CimInstance Win32_Process -Filter "ProcessId=$($p.ProcessId)"
    if($null -eq $current){continue}
    if($current.CreationDate -ne $p.CreationDate -or $current.ExecutablePath -cne $p.ExecutablePath){throw 'Identidad de proceso cambio; no terminar'}
    Stop-Process -Id $p.ProcessId -ErrorAction Stop
}
@{cerrados=$e8Candidates.Count;soloProcesoPropio=$true;motivo='Cierre tras motor detenido; sin repetir carga';fechaUTC=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json|Set-Content "$d/cierre-interrumpido.json" -Encoding UTF8
