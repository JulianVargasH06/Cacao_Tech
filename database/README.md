# Database - PostgreSQL

## Persistencia

Los datos viven en PostgreSQL (`cacao_db`), no en memoria: reiniciar el backend o el equipo no borra nada.
Al arrancar, el backend solo crea los usuarios de prueba si no existen, así que tampoco duplica registros.

## Respaldo y recuperación

Desde la raíz del proyecto, en PowerShell:

```powershell
$env:DB_PASSWORD = "<tu contraseña de postgres>"
.\database\respaldo.ps1
```

Crea `database\respaldos\cacao_db_<fecha>.dump` (la carpeta no se sube a git).

Para recuperar, **detén primero el backend** y luego:

```powershell
.\database\restaurar.ps1              # usa el respaldo más reciente
.\database\restaurar.ps1 -Archivo database\respaldos\cacao_db_20261016_101500.dump
```

- **Sin pérdida:** la base queda exactamente como estaba al momento del respaldo.
- **Sin duplicación:** `pg_restore --clean` borra cada tabla antes de recrearla, en lugar de sumar filas encima.
- **Seguro:** corre en una sola transacción; si algo falla, la base queda como estaba.

Ambos scripts aceptan `-BaseDatos cacao_demo` para trabajar con la base de la demostración.

### Prueba del jurado: reiniciar y recuperar

1. Registrar algunos lotes y ejecutar `respaldo.ps1`.
2. Detener y volver a iniciar el backend: los lotes siguen ahí.
3. Registrar un lote más (cambio posterior al respaldo).
4. Detener el backend, ejecutar `restaurar.ps1` e iniciar el backend.
5. Los lotes del paso 1 están completos, sin repetidos, y el del paso 3 ya no aparece porque es posterior al respaldo.
