# Detiene y elimina el entorno perf (la base es tmpfs: no queda nada). NO usa -v ni toca el compose de desarrollo.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$archivoEnv = Join-Path $PSScriptRoot '.env.perf'
if (-not (Test-Path $archivoEnv)) { Write-Host 'No hay entorno perf iniciado (falta perf\.env.perf).'; exit 0 }
Push-Location $raiz
try { docker compose --env-file $archivoEnv -f (Join-Path $PSScriptRoot 'docker-compose.perf.yml') down } finally { Pop-Location }
