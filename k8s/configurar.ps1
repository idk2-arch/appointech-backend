param(
  [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9.-]+$')][string]$HostPublico,
  [Parameter(Mandatory=$true)][string]$ImagenFrontend,
  [string]$ImagenBackend = 'ghcr.io/idk2-arch/appointech-backend-backend:latest',
  [string]$ImagenNotificaciones = 'ghcr.io/idk2-arch/appointech-backend-notificaciones:latest',
  [Parameter(Mandatory=$true)][string]$CorreoAcme,
  [ValidateSet('staging','prod')][string]$Acme = 'staging',
  [ValidateRange(1,2)][int]$ReplicasBackend = 1
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Guardar($nombre, $objeto) {
  [IO.File]::WriteAllText((Join-Path $PSScriptRoot $nombre), ($objeto | ConvertTo-Json -Depth 100), $utf8)
}
$config = Get-Content -Raw (Join-Path $PSScriptRoot '01-configuracion.yaml') | ConvertFrom-Json
$config.data.CORS_ORIGINS = "https://$HostPublico"
$config.data.PAGOS_URL_RETORNO = "https://$HostPublico/cliente/pago/resultado"
Guardar '01-configuracion.yaml' $config
foreach ($par in @(@('03-backend.yaml',$ImagenBackend),@('04-notificaciones.yaml',$ImagenNotificaciones),@('05-frontend.yaml',$ImagenFrontend))) {
  $doc = Get-Content -Raw (Join-Path $PSScriptRoot $par[0]) | ConvertFrom-Json
  $deploy = $doc.items | Where-Object kind -eq 'Deployment'
  $deploy.spec.template.spec.containers[0].image = $par[1]
  if ($deploy.metadata.name -eq 'backend') { $deploy.spec.replicas = $ReplicasBackend }
  Guardar $par[0] $doc
}
$ingress = Get-Content -Raw (Join-Path $PSScriptRoot '06-ingress.yaml') | ConvertFrom-Json
foreach ($item in ($ingress.items | Where-Object kind -eq 'Ingress')) {
  $item.spec.rules[0].host = $HostPublico
  if ($item.spec.tls) {
    $item.spec.tls[0].hosts = @($HostPublico)
    $item.metadata.annotations.'cert-manager.io/cluster-issuer' = "letsencrypt-$Acme"
  }
}
Guardar '06-ingress.yaml' $ingress
$issuers = Get-Content -Raw (Join-Path $PSScriptRoot 'infra/clusterissuers.yaml') | ConvertFrom-Json
foreach ($item in $issuers.items) { $item.spec.acme.email = $CorreoAcme }
Guardar 'infra/clusterissuers.yaml' $issuers
