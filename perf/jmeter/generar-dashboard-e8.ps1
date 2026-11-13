[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Directorio,[string]$JMeterHome=(Join-Path $env:LOCALAPPDATA 'Programs/JMeter'))
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'e8-comun.ps1')
$d=[IO.Path]::GetFullPath($Directorio)
if(-not(Test-Path "$d/motor-final.json") -or -not(Test-Path "$d/medicion-dashboard.jtl")){throw 'Falta cierre o copia de medicion'}
# Solo -g (reporte); nunca -t/-n ni peticiones HTTP.
$save=@(Argumentos-E8 $JMeterHome $d 'no-se-usa'|Where-Object{$_ -like '-Jjmeter.save.saveservice.*' -or $_ -like '-Jsample_variables=*'})
$argsReport=@('-Djava.awt.headless=true','-jar',(Join-Path $JMeterHome 'bin/ApacheJMeter.jar'),'-g',"$d/medicion-dashboard.jtl",'-o',"$d/dashboard-final",'-j',"$d/dashboard-final.log")+$save
$local=Join-Path $d 'reporte-offline';New-Item -ItemType Directory -Path $local -ErrorAction Stop|Out-Null
Invocar-E7Java (Java-E7) $argsReport $local 120
$code=0
@{modo='generar_reporte_sin_carga';codigo=$code;HTTP=0;JMeterCarga=$false;JTLOriginalModificado=$false}|ConvertTo-Json|Set-Content "$d/dashboard-reparacion.json" -Encoding UTF8
if($code -ne 0){throw 'Dashboard E8 fallo; conservar evidencia'}
