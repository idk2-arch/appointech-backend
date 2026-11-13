function Iniciar-MuestreoPerf($Aislamiento, [string]$Directorio, [switch]$Ficticio) {
    $ids = @($Aislamiento.BackendId, $Aislamiento.DbId)
    $inspeccion = if($Ficticio){@()}else{(Invoke-E2Docker inspect @ids | Out-String | ConvertFrom-Json)}
    $limites = foreach ($c in $inspeccion) {
        [pscustomobject]@{
            servicio=$c.Config.Labels.'com.docker.compose.service'
            id=$c.Id; memoriaBytes=$c.HostConfig.Memory
            nanoCpus=$c.HostConfig.NanoCpus; cpuQuota=$c.HostConfig.CpuQuota
            cpuPeriod=$c.HostConfig.CpuPeriod; cpuSet=$c.HostConfig.CpusetCpus
        }
    }
    $limites | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $Directorio 'limites.json') -Encoding UTF8
    $csv = Join-Path $Directorio 'recursos.csv'
    $detener = Join-Path $Directorio 'muestreo.fin'
    $listo = Join-Path $Directorio 'muestreo.listo'
    $dockerExe = (Get-Command docker.exe -CommandType Application -ErrorAction Stop |
        Select-Object -First 1).Source
    $job = Start-Job -ArgumentList $dockerExe,$ids[0],$ids[1],$csv,$detener,$listo,$Ficticio.IsPresent -ScriptBlock {
        param($exe,$backend,$base,$archivo,$fin,$disponible,$falso)
        $yo=Get-Process -Id $PID
        @{directorio=(Split-Path $archivo);pid=$PID;inicioUTC=$yo.StartTime.ToUniversalTime().ToString('o');ejecutable=$yo.Path}|ConvertTo-Json|Set-Content (Join-Path (Split-Path $archivo) 'recolector-identidad.json')
        $ErrorActionPreference = 'Stop'
        'epochMs,contenedor,servicio,cpuPct,memoriaUsada,memoriaLimite,memPct' | Set-Content -LiteralPath $archivo -Encoding ASCII
        while (-not (Test-Path -LiteralPath $fin)) {
            $reloj = [Diagnostics.Stopwatch]::StartNew()
            $inicio = New-Object Diagnostics.ProcessStartInfo
            $inicio.FileName = $exe
            $inicio.Arguments = 'stats --no-stream --format "{{json .}}" ' + $backend + ' ' + $base
            $inicio.UseShellExecute = $false
            $inicio.CreateNoWindow = $true
            $inicio.RedirectStandardOutput = $true
            $inicio.RedirectStandardError = $true
            if($falso){
                $t=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                foreach($svc in @('backend-perf','db-perf')){"$t,ficticio,$svc,0,1MiB,1GiB,0.1"|Add-Content -LiteralPath $archivo -Encoding ASCII}
                if(-not(Test-Path $disponible)){New-Item -ItemType File $disponible|Out-Null}
                Start-Sleep -Milliseconds 100
                continue
            }
            $p = [Diagnostics.Process]::Start($inicio)
            try {
                $salida = $p.StandardOutput.ReadToEndAsync()
                $errorDocker = $p.StandardError.ReadToEndAsync()
                if (-not $p.WaitForExit(5000)) { throw 'docker stats no respondio en cinco segundos.' }
                if ($p.ExitCode -ne 0) { throw 'docker stats termino con error.' }
                $t = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                $filas = @($salida.GetAwaiter().GetResult() -split '\r?\n' | Where-Object { $_ } | ForEach-Object { ConvertFrom-Json $_ })
                if ($filas.Count -ne 2) { throw 'docker stats no devolvio los dos servicios perf.' }
                foreach ($f in $filas) {
                    $servicio = if ($backend.StartsWith($f.ID)) { 'backend-perf' } elseif ($base.StartsWith($f.ID)) { 'db-perf' } else { throw 'Contenedor inesperado en docker stats.' }
                    $memoria = $f.MemUsage -split '\s+/\s+',2
                    "$t,$($f.Name),$servicio,$($f.CPUPerc.TrimEnd('%')),$($memoria[0]),$($memoria[1]),$($f.MemPerc.TrimEnd('%'))" | Add-Content -LiteralPath $archivo -Encoding ASCII
                }
                # Add-Content ya cerro ambos escritores. El consumidor solo consulta esta senal.
                if (-not (Test-Path -LiteralPath $disponible)) {
                    New-Item -ItemType File -Path $disponible -ErrorAction Stop | Out-Null
                }
            } finally {
                if (-not $p.HasExited) { $p.Kill(); $p.WaitForExit() }
                $p.Dispose()
            }
            $espera = 1000 - $reloj.ElapsedMilliseconds
            if ($espera -gt 0) { Start-Sleep -Milliseconds $espera }
        }
    }
    [pscustomobject]@{ Job=$job; Csv=$csv; Fin=$detener; Listo=$listo }
}

function Esperar-MuestreoPerf($Muestreo, [int]$TimeoutSegundos = 12) {
    $limite = (Get-Date).AddSeconds($TimeoutSegundos)
    do {
        if ($Muestreo.Job.State -notin @('Running','NotStarted')) {
            throw 'El recolector termino antes de confirmar su disponibilidad.'
        }
        if ($Muestreo.Job.State -eq 'Running' -and (Test-Path -LiteralPath $Muestreo.Listo)) { return }
        Start-Sleep -Milliseconds 100
    } while ((Get-Date) -lt $limite)
    throw 'El recolector no confirmo la primera captura de ambos servicios en el plazo previsto.'
}

function Validar-MuestreoPerf([string]$Csv) {
    # Se invoca unicamente despues de Wait-Job y de retirar el trabajo escritor.
    $filas = @(Import-Csv -LiteralPath $Csv -ErrorAction Stop)
    $columnas = 'epochMs,contenedor,servicio,cpuPct,memoriaUsada,memoriaLimite,memPct'.Split(',')
    if ($filas.Count -lt 2 -or (Compare-Object $columnas @($filas[0].PSObject.Properties.Name) -CaseSensitive)) {
        throw 'El CSV final de recursos esta vacio o tiene un encabezado inesperado.'
    }
    foreach ($f in $filas) {
        $epoch = 0L; $cpu = 0.0; $mem = 0.0
        if (-not [long]::TryParse($f.epochMs, [ref]$epoch) -or $epoch -le 0 -or
            -not [double]::TryParse($f.cpuPct, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$cpu) -or
            -not [double]::TryParse($f.memPct, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$mem) -or
            [double]::IsNaN($cpu) -or [double]::IsInfinity($cpu) -or $cpu -lt 0 -or
            [double]::IsNaN($mem) -or [double]::IsInfinity($mem) -or $mem -lt 0 -or
            [string]::IsNullOrWhiteSpace($f.contenedor) -or
            $f.servicio -notin @('backend-perf','db-perf') -or
            $f.memoriaUsada -notmatch '^\d+(\.\d+)?(B|KiB|MiB|GiB|TiB)$' -or
            $f.memoriaLimite -notmatch '^\d+(\.\d+)?(B|KiB|MiB|GiB|TiB)$') {
            throw 'Fila invalida en el CSV final de recursos.'
        }
    }
    foreach ($grupo in ($filas | Group-Object epochMs)) {
        if ($grupo.Count -ne 2 -or @($grupo.Group.servicio | Select-Object -Unique).Count -ne 2) {
            throw 'Captura incompleta o duplicada en el CSV final de recursos.'
        }
    }
    [pscustomobject]@{filas=$filas.Count; capturasPorServicio=($filas.Count/2); csvValido=$true}
}

function Detener-MuestreoPerf($Muestreo) {
    $terminadoAntesDePedirCierre = $Muestreo.Job.State -notin @('Running','NotStarted')
    New-Item -ItemType File -Path $Muestreo.Fin -Force | Out-Null
    $terminado = Wait-Job $Muestreo.Job -Timeout 12
    try {
        if (-not $terminado) {
            Stop-Job $Muestreo.Job
            throw 'El recolector no se detuvo en el tiempo previsto.'
        }
        Receive-Job $Muestreo.Job -ErrorAction Stop | Out-Null
        if ($terminadoAntesDePedirCierre) { throw 'El recolector termino prematuramente antes de solicitar su cierre.' }
    } finally {
        Remove-Job $Muestreo.Job -Force
    }
    Validar-MuestreoPerf $Muestreo.Csv
}
