# Incidente técnico de la hora 12: producto reportado por un comprador

> *"El comprador informa un problema con CAC001. El administrador debe bloquear el producto, identificar su
> origen y explicar las cantidades afectadas. No debe borrarse el historial."*

Este documento reúne lo que pide el reto para este escenario: **tarjeta de cambio**, **criterio de aceptación
ajustado** y **prueba de regresión**, más el guion para mostrarlo.

---

## 1. Tarjeta de cambio

| Campo | Contenido |
|---|---|
| **Título** | Retiro de producto por incidente reportado por un comprador |
| **Tipo** | Cambio por incidente (no planeado en el backlog inicial) |
| **Historias afectadas** | HU03 (trazabilidad), HU09 (liberar/bloquear con motivo), HU10 (auditoría) |
| **Prioridad** | P0 — afecta la seguridad del consumidor |
| **Problema** | Bloquear el producto reportado no bastaba: lo que se produjo a partir de él seguía disponible, y el administrador tenía que sumar a mano los kilos afectados recorriendo la trazabilidad. |
| **Solución** | Análisis de impacto del lote (origen, afectados y mismo origen, con kg en bodega) y bloqueo en un paso del lote reportado y sus derivados, con motivo obligatorio y alerta de retiro. |
| **Cambios técnicos** | `GET /api/lotes/{id}/impacto` · `POST /api/lotes/{id}/bloqueo-incidente` · `ImpactoLoteService` · sección "Incidente reportado" en Trazabilidad |
| **Riesgo** | Bajo: solo agrega endpoints y bloquea con el mismo `EstadoLoteService` de HU09; no borra ni modifica datos existentes. |
| **Verificación** | `IncidenteHora12IntegrationTest` (5 pruebas) + suite completa `./mvnw test` como regresión |
| **Rama / PR** | `feature/incidente-retiro` |

---

## 2. Criterio de aceptación ajustado (HU09)

**Antes:**
> Dado un lote, cuando el administrador lo bloquea con un motivo, entonces el lote queda BLOQUEADO y el cambio
> queda en su historial.

**Después (ajustado por el incidente):**
> **Dado** un producto reportado por un comprador,
> **cuando** el administrador analiza su impacto y lo bloquea con un motivo,
> **entonces**:
> 1. El sistema muestra los **lotes de finca de origen**.
> 2. Muestra los **lotes afectados** (el producto y todo lo producido a partir de él) y los del **mismo origen**,
>    con los **kg que siguen en bodega** de cada grupo.
> 3. Quedan **BLOQUEADOS el producto y todos sus derivados** (y, si el administrador lo decide, los del mismo
>    origen); un lote bloqueado no se puede volver a transformar.
> 4. Se genera una **alerta de retiro** que explica el incidente, el origen y los kg retenidos.
> 5. **No se borra nada**: los lotes, transformaciones, historial de estados y auditoría se conservan, y la
>    ficha pública muestra el producto como "En revisión de calidad".

---

## 3. Prueba de regresión

Prueba específica del escenario: `backend/src/test/java/com/cacaotech/backend/IncidenteHora12IntegrationTest.java`

| # | Prueba | Qué verifica |
|---|---|---|
| 1 | Identificar origen y cantidades | Origen = lote de finca; afectados = producto + derivado (115 kg); mismo origen = 46 kg |
| 2 | Bloquear producto y derivados | Se bloquean 2 lotes; los del mismo origen no; el producto ya no se puede transformar |
| 3 | Incluir los del mismo origen | Se bloquean los 5 lotes (161 kg retenidos) |
| 4 | No se borra el historial | Mismo número de lotes, historial de estados con el motivo, auditoría, ficha pública con el recorrido completo y alerta de retiro |
| 5 | Permisos y validación | Solo el administrador; el motivo es obligatorio |

**Cómo ejecutarla** (desde `backend`, con `DB_PASSWORD` configurada):

```bash
./mvnw test -Dtest=IncidenteHora12IntegrationTest
```

**Regresión completa** (confirma que el cambio no rompió nada de lo anterior):

```bash
./mvnw test
```

Las pruebas corren en una transacción que se revierte al final: no dejan datos en la base de datos.

---

## 4. Guion para mostrarlo en la hora 12

1. Entrar como **administrador** → **Trazabilidad** → buscar el código reportado (por ejemplo `CAC001`).
2. **Identificar el origen:** la línea de tiempo "De dónde viene" muestra la finca y cada transformación con kg,
   merma y responsable.
3. Pulsar **Analizar impacto** → explicar las cifras: lotes de origen, kg afectados en bodega y kg del mismo origen.
4. Pulsar **Bloquear afectados** → escribir lo que reportó el comprador → decidir si incluir los del mismo origen →
   **Bloquear**.
5. Mostrar que **no se borró nada**:
   - el **historial de estados** del lote con el motivo,
   - la **auditoría** (`CAMBIAR_ESTADO` de cada lote),
   - la **alerta de retiro** en el Panel / Mermas y costos,
   - la **ficha pública**: el recorrido sigue completo y el producto aparece "En revisión de calidad".
6. Ejecutar la **prueba de regresión** (`./mvnw test`) y mostrar `BUILD SUCCESS`.
