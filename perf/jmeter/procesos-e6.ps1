function Seleccionar-E6ProcesosCarga {
    param([object[]]$Procesos)
    $ahora=[DateTime]::UtcNow.ToString('o')
    $perf=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot))
    $registros=@(Get-ChildItem $perf -Filter recolector-identidad.json -Recurse -File | ForEach-Object {try{Get-Content $_.FullName -Raw|ConvertFrom-Json}catch{}})
    $propios=@{}; $evidencia=@{}
    foreach($p in $Procesos){
        $cmd=[string]$p.CommandLine
        $r=@($registros|Where-Object { $_.pid -eq $p.ProcessId -and [Math]::Abs(([DateTime]::Parse($_.inicioUTC).ToUniversalTime()-$p.CreationDate.ToUniversalTime()).TotalMilliseconds) -lt 1 })
        if($r.Count -or ($cmd.IndexOf($perf,[StringComparison]::OrdinalIgnoreCase) -ge 0 -and $cmd -match '(?i)(ApacheJMeter|org\.apache\.jmeter|recursos-perf\.ps1)')){$propios[[int]$p.ProcessId]=$true
            $evidencia[[int]$p.ProcessId]=@($r|ForEach-Object { $_.directorio })
            $pat=[regex]::Escape($perf)+'[\\/][^"\r\n]*?\.(?:jtl|jmx|log|json)(?="|\s|$)'
            $evidencia[[int]$p.ProcessId]+=@([regex]::Matches($cmd,$pat)|ForEach-Object Value)
        }
    }
    foreach($p in $Procesos){
        $cmd=[string]$p.CommandLine;$motivo=$null
        if($p.Name -match '^(?i)java(w)?\.exe$' -and $cmd -match '(?i)(ApacheJMeter\.jar|org\.apache\.jmeter\.(NewDriver|JMeter))'){$motivo='motor_JMeter'}
        elseif($p.Name -ieq 'docker.exe' -and $cmd -match '(?i)(?:^|\s)stats(?:\s|$)'){$motivo='docker_stats'}
        elseif($propios.ContainsKey([int]$p.ProcessId)){$motivo='recolector_identificado'}
        if($motivo){
            $propio=$propios.ContainsKey([int]$p.ProcessId) -or $propios.ContainsKey([int]$p.ParentProcessId)
            [pscustomobject]@{capturaUTC=$ahora;Name=$p.Name;ProcessId=$p.ProcessId;ParentProcessId=$p.ParentProcessId;inicioUTC=if($p.CreationDate){$p.CreationDate.ToUniversalTime().ToString('o')}else{$null};ejecutable=$p.ExecutablePath;motivo=$motivo;rutasCorrida=@($evidencia[[int]$p.ProcessId]);propio=$propio;bloquea=$propio;criterio=if($propio){'ruta perf en motor/recolector o registro PID+inicio o hijo directo'}else{'pertenencia no identificada; solo informativo'};huellaArgumentos=if($cmd){$sha=[Security.Cryptography.SHA256]::Create();try{[BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($cmd))).Replace('-','')}finally{$sha.Dispose()}}else{$null}}
        }
    }
}
