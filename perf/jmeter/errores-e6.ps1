function Convertir-E6ErrorSeguro {
    param([Parameter(Mandatory=$true)][Management.Automation.ErrorRecord]$Registro,
        [Parameter(Mandatory=$true)][string]$Etapa,[string[]]$Secretos=@())
    $limpiar={param([string]$Texto)
        if($null -eq $Texto){return ''}
        foreach($secreto in $Secretos){if(-not [string]::IsNullOrEmpty($secreto)){$Texto=$Texto.Replace($secreto,'[REDACTADO]')}}
        $Texto=$Texto -replace '(?i)Bearer\s+[^\s,;]+','Bearer [REDACTADO]'
        $Texto=$Texto -replace 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[JWT_REDACTADO]'
        $Texto=$Texto -replace '(?is)\{[^\r\n]*(?:"correo"|"contrasena"|"token")[^\r\n]*\}','[CUERPO_REDACTADO]'
        $Texto=$Texto -replace '(?i)(password|contrasena|jwt_secret|authorization|token)\s*[:=]\s*("[^"]*"|''[^'']*''|[^\s,;]+)','$1=[REDACTADO]'
        $Texto=$Texto -replace '(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}','[CORREO_REDACTADO]'
        $Texto=$Texto -replace '(?i)[A-Z]:\\[^\r\n"'']*?appointech-e6-[a-f0-9]+\\[^\s"'']*','[PAQUETE_PRIVADO]'
        $Texto
    }
    $cadena=@();$ex=$Registro.Exception
    while($null -ne $ex){
        $cadena+=[pscustomobject]@{tipo=$ex.GetType().FullName;mensaje=(& $limpiar $ex.Message)}
        $ex=$ex.InnerException
    }
    # No serializar TargetObject, ErrorDetails, PositionMessage, Line, Data ni variables.
    [pscustomobject]@{fechaUTC=[datetime]::UtcNow.ToString('o');etapa=$Etapa;
        tipo=$Registro.Exception.GetType().FullName;mensaje=(& $limpiar $Registro.Exception.Message);
        idError=(& $limpiar $Registro.FullyQualifiedErrorId);categoria=[string]$Registro.CategoryInfo.Category;
        archivo=[IO.Path]::GetFileName($Registro.InvocationInfo.ScriptName);
        linea=$Registro.InvocationInfo.ScriptLineNumber;columna=$Registro.InvocationInfo.OffsetInLine;
        excepciones=$cadena}
}

function Guardar-E6ErrorSeguro {
    param([Management.Automation.ErrorRecord]$Registro,[string]$Etapa,[string]$Archivo,[string[]]$Secretos=@())
    $seguro=Convertir-E6ErrorSeguro -Registro $Registro -Etapa $Etapa -Secretos $Secretos
    $seguro|ConvertTo-Json -Depth 6 -Compress|Add-Content -LiteralPath $Archivo -Encoding UTF8
    $seguro
}
