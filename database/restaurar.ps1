# Recupera la base de datos de CacaoTech desde un respaldo hecho con respaldo.ps1.
#
# Uso (desde la raíz del proyecto, con el backend DETENIDO):
#   $env:DB_PASSWORD = "<tu contraseña de postgres>"
#   .\database\restaurar.ps1                                   # el respaldo más reciente de cacao_db
#   .\database\restaurar.ps1 -Archivo database\respaldos\cacao_db_20261016_101500.dump
#
# Sin pérdida ni duplicación: --clean borra cada tabla antes de recrearla con los datos del respaldo,
# así que la base queda exactamente como estaba al momento del respaldo (no se suman filas repetidas).
# Todo corre en una sola transacción: si algo falla, la base queda como estaba antes de restaurar.

param(
    [string]$Archivo,
    [string]$BaseDatos = "cacao_db",
    [string]$Usuario = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" }),
    [string]$Servidor = "localhost",
    [int]$Puerto = 5432
)

$ErrorActionPreference = "Stop"

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

if (-not $Archivo) {
    $ultimo = Get-ChildItem (Join-Path $PSScriptRoot "respaldos\${BaseDatos}_*.dump") -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $ultimo) { throw "No hay respaldos de $BaseDatos en database\respaldos" }
    $Archivo = $ultimo.FullName
}
if (-not (Test-Path $Archivo)) { throw "No existe el archivo $Archivo" }

Write-Host "Restaurando $BaseDatos desde $Archivo ..."
$pgRestore = Buscar-Herramienta "pg_restore"
& $pgRestore --host $Servidor --port $Puerto --username $Usuario --dbname $BaseDatos `
    --clean --if-exists --no-owner --single-transaction --exit-on-error $Archivo
if ($LASTEXITCODE -ne 0) { throw "pg_restore terminó con error ($LASTEXITCODE). La base no se modificó." }

Write-Host "Base de datos recuperada. Ya puedes iniciar el backend." -ForegroundColor Green
