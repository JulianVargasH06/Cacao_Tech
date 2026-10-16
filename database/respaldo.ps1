# Respaldo de la base de datos de CacaoTech con pg_dump (formato personalizado, comprimido).
#
# Uso (desde la raíz del proyecto):
#   $env:DB_PASSWORD = "<tu contraseña de postgres>"
#   .\database\respaldo.ps1                      # respalda cacao_db
#   .\database\respaldo.ps1 -BaseDatos cacao_demo
#
# Deja el archivo en database\respaldos\<base>_<fecha>.dump (esa carpeta no se sube a git).

param(
    [string]$BaseDatos = "cacao_db",
    [string]$Usuario = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" }),
    [string]$Servidor = "localhost",
    [int]$Puerto = 5432
)

$ErrorActionPreference = "Stop"

# pg_dump: del PATH o de la versión más reciente instalada en Program Files
function Buscar-Herramienta([string]$nombre) {
    $enPath = Get-Command $nombre -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }
    $instalada = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*\bin\$nombre.exe" -ErrorAction SilentlyContinue |
        Sort-Object { [int]$_.Directory.Parent.Name } -Descending | Select-Object -First 1
    if ($instalada) { return $instalada.FullName }
    throw "No se encontró $nombre. Instala PostgreSQL o agrega su carpeta bin al PATH."
}

if (-not $env:DB_PASSWORD) {
    throw 'Define primero la contraseña: $env:DB_PASSWORD = "..."'
}
$env:PGPASSWORD = $env:DB_PASSWORD

$carpeta = Join-Path $PSScriptRoot "respaldos"
New-Item -ItemType Directory -Force $carpeta | Out-Null
$archivo = Join-Path $carpeta ("{0}_{1}.dump" -f $BaseDatos, (Get-Date -Format "yyyyMMdd_HHmmss"))

$pgDump = Buscar-Herramienta "pg_dump"
& $pgDump --host $Servidor --port $Puerto --username $Usuario --format custom --file $archivo $BaseDatos
if ($LASTEXITCODE -ne 0) { throw "pg_dump terminó con error ($LASTEXITCODE)" }

Write-Host "Respaldo creado: $archivo" -ForegroundColor Green
