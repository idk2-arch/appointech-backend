# Levanta el entorno de pruebas aislado (base temporal + backend con perfil perf) y espera a que responda.
# Uso:  powershell -File perf\iniciar.ps1 [-Cpus 2] [-Memoria 1g] [-PoolMax 10] [-UmbralLentoMs 300] [-Reconstruir]
# Genera secretos aleatorios en perf\.env.perf la primera vez (archivo ignorado por git; no se imprime).
param(
    [string]$Cpus = '2',
    [string]$Memoria = '1g',
    [int]$PoolMax = 10,
    [int]$UmbralLentoMs = 300,
    [int]$PuertoApi = 18080,
    [switch]$Reconstruir
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$archivoEnv = Join-Path $PSScriptRoot '.env.perf'

function Nuevo-Secreto([int]$bytes) {
    $b = New-Object byte[] $bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
    return [Convert]::ToBase64String($b).TrimEnd('=').Replace('+', 'A').Replace('/', 'B')
}

if (-not (Test-Path $archivoEnv)) {
    @(
        "PERF_DB_PASSWORD=$(Nuevo-Secreto 18)",
        "PERF_JWT_SECRET=$(Nuevo-Secreto 48)"
    ) | Set-Content -Path $archivoEnv -Encoding ascii
    Write-Host "Secretos generados en perf\.env.perf (no se imprimen)."
}

# Parametros de esta ejecucion (se agregan al entorno del proceso; el compose los lee con ${PERF_...})
$env:PERF_BACKEND_CPUS = $Cpus
$env:PERF_BACKEND_MEM = $Memoria
$env:PERF_DB_POOL_MAX = "$PoolMax"
$env:PERF_UMBRAL_LENTO_MS = "$UmbralLentoMs"
$env:PERF_API_PORT = "$PuertoApi"

$compose = @('compose', '--env-file', $archivoEnv, '-f', (Join-Path $PSScriptRoot 'docker-compose.perf.yml'))
$argumentos = $compose + @('up', '-d', '--build')
if ($Reconstruir) { $argumentos += '--force-recreate' }
Push-Location $raiz
try { & docker @argumentos } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo (codigo $LASTEXITCODE)" }

Write-Host "Esperando a que el backend perf responda en http://localhost:$PuertoApi ..."
$limite = (Get-Date).AddMinutes(4)
while ((Get-Date) -lt $limite) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:$PuertoApi/api/especialidades" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) {
            Write-Host "Listo: API en http://localhost:$PuertoApi (CPU=$Cpus, memoria=$Memoria, pool=$PoolMax)."
            exit 0
        }
    } catch { Start-Sleep -Seconds 3 }
}
throw "El backend perf no respondio en 4 minutos. Revisa: docker logs appointech-perf-backend-perf-1"
