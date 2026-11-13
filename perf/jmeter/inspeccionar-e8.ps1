[CmdletBinding()]
param([string]$Configuracion,[string]$TokensCsv,
    [string]$PaqueteE6,[ValidateSet('Preparacion','Calentamiento','Medicion')][string]$Etapa='Preparacion',[string]$Salida,[switch]$ExigirVigencia)
# Unicamente archivos locales y aritmetica. No cargar ejecutores, no HTTP, no Docker, no JMeter.
$ErrorActionPreference='Stop'
if(-not $Configuracion){$Configuracion=Join-Path $PSScriptRoot '../diseno/e8-resistencia.json'}
if(-not $TokensCsv){$TokensCsv=Join-Path $PSScriptRoot 'tokens.csv'}
$nodeArgs=@((Join-Path $PSScriptRoot 'inspeccionar-e8.js'),'--config',$Configuracion,'--etapa',$Etapa)
if($PaqueteE6){$nodeArgs+=@('--paquete',$PaqueteE6)}else{$nodeArgs+=@('--tokens',$TokensCsv)}
if($Salida){$nodeArgs+=@('--salida',$Salida)}
if($ExigirVigencia){$nodeArgs+='--exigir-vigencia'}
& node.exe @nodeArgs
exit $LASTEXITCODE
