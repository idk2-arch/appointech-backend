# E7 tiene un escritor persistente. No modifica el recolector ni las evidencias E6.
. (Join-Path $PSScriptRoot 'recursos-perf.ps1')
function Iniciar-E7Recursos($Aislamiento,[string]$Directorio,[switch]$Ficticio){
    $docker=(Get-Command docker.exe -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source
    $csv=Join-Path $Directorio 'recursos.csv';$fin=Join-Path $Directorio 'muestreo.fin';$listo=Join-Path $Directorio 'muestreo.listo'
    $job=Start-Job -ArgumentList $docker,$Aislamiento.BackendId,$Aislamiento.DbId,$csv,$fin,$listo,$Ficticio.IsPresent -ScriptBlock {
        param($exe,$backend,$base,$archivo,$detener,$disponible,$falso)
        $ErrorActionPreference='Stop';$writer=$null;$stream=$null;$capturas=0;$fallo=$false
        try{
            $yo=Get-Process -Id $PID
            @{directorio=(Split-Path $archivo);pid=$PID;inicioUTC=$yo.StartTime.ToUniversalTime().ToString('o');ejecutable=$yo.Path}|
                ConvertTo-Json|Set-Content (Join-Path (Split-Path $archivo) 'recolector-identidad.json') -Encoding UTF8
            $stream=[IO.FileStream]::new($archivo,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::ReadWrite)
            $writer=[IO.StreamWriter]::new($stream,[Text.UTF8Encoding]::new($false))
            $writer.WriteLine('epochMs,contenedor,servicio,cpuPct,memoriaUsada,memoriaLimite,memPct');$writer.Flush()
            while(-not(Test-Path -LiteralPath $detener)){
                if($falso){
                    $t=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                    $lineas=@("$t,ficticio,backend-perf,0,1MiB,1GiB,0.1","$t,ficticio,db-perf,0,1MiB,1GiB,0.1")
                }else{
                    $si=New-Object Diagnostics.ProcessStartInfo
                    $si.FileName=$exe;$si.Arguments='stats --no-stream --format "{{json .}}" '+$backend+' '+$base
                    $si.UseShellExecute=$false;$si.CreateNoWindow=$true;$si.RedirectStandardOutput=$true;$si.RedirectStandardError=$true
                    $p=[Diagnostics.Process]::Start($si)
                    try{
                        $stdout=$p.StandardOutput.ReadToEndAsync();$stderr=$p.StandardError.ReadToEndAsync()
                        if(-not $p.WaitForExit(6000)){throw 'E7: docker stats excedio su plazo.'}
                        if($p.ExitCode -ne 0){throw 'E7: docker stats fallo.'}
                        $rows=@($stdout.GetAwaiter().GetResult() -split '\r?\n'|Where-Object{$_}|ForEach-Object{ConvertFrom-Json $_})
                        if($rows.Count -ne 2){throw 'E7: falta un servicio en stats.'}
                        $t=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                        $lineas=@(foreach($r in $rows){
                            $servicio=if($backend.StartsWith($r.ID)){'backend-perf'}elseif($base.StartsWith($r.ID)){'db-perf'}else{throw 'E7: stats ajeno.'}
                            $mem=$r.MemUsage -split '\s+/\s+',2
                            "$t,$($r.Name),$servicio,$($r.CPUPerc.TrimEnd('%')),$($mem[0]),$($mem[1]),$($r.MemPerc.TrimEnd('%'))"
                        })
                        if(@($rows.ID|Select-Object -Unique).Count -ne 2){throw 'E7: stats duplicado.'}
                    }finally{if(-not $p.HasExited){$p.Kill();$p.WaitForExit()};$p.Dispose()}
                }
                # El par ya esta validado antes de escribir; no se reabre el archivo entre filas.
                $writer.Write(($lineas -join "`n")+"`n");$writer.Flush();$capturas++
                if(-not(Test-Path -LiteralPath $disponible)){New-Item -ItemType File -Path $disponible|Out-Null}
                Start-Sleep -Milliseconds $(if($falso){50}else{100})
            }
        }catch{$fallo=$true;throw 'E7: recolector fallo; revisar su cierre sin atribuir un propietario no observado.'}
        finally{
            if($writer){$writer.Dispose()}elseif($stream){$stream.Dispose()}
            @{cerrado=$true;fallo=$fallo;capturasPorServicio=$capturas;escritorPersistente=$true}|
                ConvertTo-Json|Set-Content (Join-Path (Split-Path $archivo) 'estado-recolector.json') -Encoding UTF8
        }
    }
    [pscustomobject]@{Job=$job;Csv=$csv;Fin=$fin;Listo=$listo}
}
function Detener-E7Recursos($Muestreo){
    $temprano=$Muestreo.Job.State -notin @('Running','NotStarted')
    New-Item -ItemType File -Path $Muestreo.Fin -Force|Out-Null
    try{
        if(-not(Wait-Job $Muestreo.Job -Timeout 15)){Stop-Job $Muestreo.Job;throw 'E7: recolector no cerro.'}
        Receive-Job $Muestreo.Job -ErrorAction Stop|Out-Null
        if($temprano){throw 'E7: recolector termino antes de solicitar cierre.'}
    }finally{Remove-Job $Muestreo.Job -Force}
    Validar-MuestreoPerf $Muestreo.Csv
}
