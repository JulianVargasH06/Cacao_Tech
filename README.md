# CacaoTech · Trazabilidad del cacao

Plataforma para asociaciones cacaoteras: registra los lotes que llegan de cada finca, sus transformaciones
(fermentación, secado, tostado, empaque), las mermas con alertas automáticas, los costos y el margen de venta,
y permite que un comprador consulte el origen de su producto sin ver información interna.

| Capa | Tecnología |
|---|---|
| Frontend | Angular 20 + PrimeNG 20 (`frontend/`) |
| Backend | Spring Boot 3.2 · Java 21 · JWT (`backend/`) |
| Base de datos | PostgreSQL (`database/`) |

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@cacaotech.com` | `admin123` |
| Operador | `operador@cacaotech.com` | `operador123` |
| Comprador | sin cuenta: **Consultar un producto** en la página de inicio | — |

---

## 1. Arrancar para la demostración

Se usa la base **`cacao_demo`** con el perfil **`demo`**, que carga datos de ejemplo la primera vez
(4 fincas, el producto `CAC-2026-101-EMP` de la finca al empaque, una mezcla con alerta de merma y el lote `CAC001`).

### Terminal 1 · Backend

```powershell
cd backend
$env:DB_PASSWORD = "<contraseña de postgres>"
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:DB_URL = "jdbc:postgresql://localhost:5432/cacao_demo"
./mvnw spring-boot:run
```

Está listo cuando el log muestra `Started BackendApplication`.
Las variables `$env:` solo existen en esa terminal: si se cierra, hay que volver a definirlas.

### Terminal 2 · Frontend

```powershell
cd frontend
npm start
```

### Direcciones

| Qué | URL |
|---|---|
| Aplicación | http://localhost:4200 |
| Swagger (API) | http://localhost:8080/swagger-ui/index.html |

### Antes de que llegue el jurado: volver al punto limpio

Si se probó algo y la base quedó con datos de ensayo, se restaura el respaldo limpio:

1. Detener el backend (`Ctrl+C` en la Terminal 1).
2. En otra terminal, desde la raíz del proyecto:
   ```powershell
   $env:DB_PASSWORD = "<contraseña de postgres>"
   powershell -ExecutionPolicy Bypass -File .\database\restaurar.ps1 -BaseDatos cacao_demo
   ```
3. Volver a arrancar el backend (`./mvnw spring-boot:run` en la Terminal 1).

Estado esperado: `CAC001` en **PENDIENTE**, `CAC-2026-101-EMP` **LIBERADO**, `CAC-2026-MEZ-01-SEC` **BLOQUEADO**
por merma del 24,4 %, `CAC-2026-104` con **420 kg** sin tocar.

---

## 2. Guion de las pruebas del jurado

| # | Prueba | Dónde | Resultado esperado |
|---|---|---|---|
| 1 | Crear un lote válido | Operador → Recepción | Mensaje verde; aparece en historial e inventario |
| 2 | Cantidad negativa o campo vacío | Operador → Recepción | Mensaje en rojo bajo el campo; no se guarda |
| 3 | Transformación 100 kg → 80 kg | Operador → Transformaciones | Merma 20 %, alerta (> 15 %), lote resultante bloqueado |
| 4 | Costo 20.000 COP/kg y margen 20 % | Admin → Mermas y costos | Fórmula y supuestos visibles |
| 5 | Consultar como comprador | Incógnito → Consultar un producto | Sin costos, kg internos, usuarios ni motivos |
| 6 | Reiniciar y recuperar un respaldo | Terminal | Registros conservados; sin pérdida ni duplicados |
| 7 | Consumo excesivo y reenvío | Transformaciones + Swagger | Rechazo; el reenvío no consume dos veces |
| 8 | Bloquear CAC001 y ver su ficha | Admin → Trazabilidad | Bloqueado visible, origen e historial conservados |
| 9 | Liberar como operador + auditoría | Swagger + Admin → Productos / Auditoría | 403 al operador; cambio del admin auditado |

### Prueba 1 · Crear un lote válido
Operador → **Recepción**: código `JURADO-001`, productor `Finca Prueba`, origen `Neiva, Huila`, `100` kg, fecha de hoy.
Se ve el mensaje verde y el lote en el inventario con su identificador.

### Prueba 2 · Datos inválidos
En **Recepción**, cantidad `-50` y productor u origen vacíos → *"La cantidad debe ser mayor que 0"*,
*"El productor es obligatorio"*. No se guarda. El backend repite las validaciones (400 con el detalle del campo).

### Prueba 3 · Transformación 100 → 80 kg
Operador → **Transformaciones**: `SECADO`, entrada `JURADO-001` con `100` kg, salida `JURADO-001-SEC` con `80` kg.
El formulario muestra la merma antes de registrar. Luego, como admin:
- **Mermas y costos**: 20 kg (20 %) sobre el umbral del 15 % y una **alerta activa** que explica el porqué.
- **Trazabilidad** → `JURADO-001-SEC`: viene de `JURADO-001`, estado **BLOQUEADO** automático.

### Prueba 4 · Costo y margen
Admin → **Mermas y costos** → costos de la transformación anterior → agregar `MATERIA_PRIMA`, `100 kg de cacao seco`, `1600000`.
- Costo / kg: 1.600.000 ÷ 80 kg = **$20.000**.
- Precio de venta `25000` → calculadora → **Margen 20 %**, $5.000 por kg, con el cálculo
  ($25.000 − $20.000) ÷ $25.000 × 100.
- El recuadro **"Cómo se calcula"** muestra las fórmulas y los supuestos.

### Prueba 5 · Comprador
Ventana de incógnito → http://localhost:4200 → **Consultar un producto** → `JURADO-001-SEC`
(o `CAC-2026-101-EMP` para un producto liberado con todo su recorrido).
Se ven estado, productor, origen y recorrido. **No** se ven costos, kg internos, usuarios ni el motivo del bloqueo.

### Prueba 6 · Reiniciar y recuperar
1. Respaldo (con el backend encendido), desde la raíz:
   `powershell -ExecutionPolicy Bypass -File .\database\respaldo.ps1 -BaseDatos cacao_demo`
2. `Ctrl+C` en el backend y `./mvnw spring-boot:run` → los lotes siguen ahí; el log dice
   *"Datos de demostración ya cargados: no se vuelven a crear"*.
3. Registrar un lote posterior, por ejemplo `JURADO-003`.
4. `Ctrl+C` en el backend → `restaurar.ps1 -BaseDatos cacao_demo` → arrancar el backend.
5. Todo lo anterior al respaldo está **una sola vez** y `JURADO-003` **ya no aparece**.

> Restaurar usa el respaldo más reciente. Después de esta prueba, para volver al punto limpio indique el archivo:
> `restaurar.ps1 -BaseDatos cacao_demo -Archivo database\respaldos\<archivo limpio>.dump`

### Prueba 7 · Consumo excesivo y reenvío
- **Consumo excesivo**: Transformaciones → `CAC-2026-104` (420 kg) con entrada `500` →
  aviso *"Supera lo disponible del lote"*; el backend rechaza *"solo tiene 420 kg disponibles"*.
- **Reenvío** (Swagger, ver [sección 3](#3-swagger-iniciar-sesión-y-autorizar)): como operador,
  `POST /api/transformaciones` con **Idempotency-Key** `prueba-jurado-1` y este cuerpo
  (el `loteId` de `CAC-2026-104` sale de `GET /api/lotes?codigo=CAC-2026-104`):
  ```json
  {
    "tipo": "FERMENTACION",
    "fecha": "2026-10-16T10:00:00",
    "entradas": [{ "loteId": 0, "cantidadKg": 10 }],
    "salidas": [{ "codigo": "CAC-2026-104-REENVIO", "cantidadKg": 9 }],
    "observaciones": "Prueba de reenvío"
  }
  ```
  Primer *Execute* → **201**. Segundo *Execute* igual → **200** con el **mismo id**.
  `CAC-2026-104` queda con **410 kg** (no 400).

### Prueba 8 · Bloquear CAC001
Admin → **Trazabilidad** → `CAC001` → **Analizar impacto** (finca El Mirador, Palermo; afecta a `CAC001` y `CAC001-FER`)
→ **Bloquear afectados** con un motivo. Resultado: estado **BLOQUEADO**, el historial conserva lo anterior y suma el bloqueo,
y la ficha pública dice **"Bloqueado · en revisión de calidad"** sin mostrar el motivo.

### Prueba 9 · Liberar como operador y auditoría
- **App**: el operador no tiene *Productos* en el menú y no puede entrar a `/admin/productos`.
- **Swagger** como operador: `PATCH /api/lotes/{id}/estado` con
  `{ "estado": "LIBERADO", "motivo": "Intento del operador" }` → **403**
  *"No tienes permiso para realizar esta operación"*. `CAC001` sigue bloqueado.
- **Admin** → **Productos** → `CAC001` → **Liberar** con motivo → **Auditoría** muestra quién, cuándo y
  BLOQUEADO → LIBERADO con el motivo.

---

## 3. Swagger: iniciar sesión y autorizar

1. http://localhost:8080/swagger-ui/index.html
2. `POST /api/auth/login` → **Try it out** →
   `{ "correo": "operador@cacaotech.com", "password": "operador123" }` → **Execute**.
3. Copiar el valor de `token` (sin comillas) → botón **Authorize** → pegar → **Authorize** → **Close**.
4. Para buscar el id de un lote: `GET /api/lotes` con el parámetro `codigo`.

---

## 4. Respaldo y recuperación

Detalle completo en [`database/README.md`](database/README.md).

| Acción | Comando (desde la raíz) | Backend |
|---|---|---|
| Respaldar | `powershell -ExecutionPolicy Bypass -File .\database\respaldo.ps1 -BaseDatos cacao_demo` | Puede seguir encendido |
| Restaurar | `powershell -ExecutionPolicy Bypass -File .\database\restaurar.ps1 -BaseDatos cacao_demo` | **Detenido** |

Los respaldos quedan en `database\respaldos\` (no se suben a git). Ambos scripts necesitan `$env:DB_PASSWORD`.

---

## 5. Problemas frecuentes

| Mensaje | Causa y solución |
|---|---|
| `la autentificación password falló para el usuario «postgres»` | Falta `$env:DB_PASSWORD` en esa terminal o la contraseña está mal escrita. |
| `la base de datos «cacao_demo» está siendo utilizada por otros usuarios` | El backend (o pgAdmin) sigue conectado: detenerlo con `Ctrl+C`. |
| `la ejecución de scripts está deshabilitada en este sistema` | Ejecutar los `.ps1` con `powershell -ExecutionPolicy Bypass -File ...`. |
| El frontend muestra errores de conexión | El backend no está encendido o aún no terminó de arrancar. |
| `Port 8080 was already in use` | Ya hay otro backend corriendo: cerrar esa terminal o detenerlo con `Ctrl+C`. |

---

## 6. Desarrollo

- **Base normal**: `cacao_db`. Sin las variables de demo, el backend usa esa base (solo hace falta `$env:DB_PASSWORD`).
- **Pruebas del backend** (97 tests; usan `cacao_db` y revierten sus cambios):
  ```powershell
  cd backend
  $env:DB_PASSWORD = "<contraseña de postgres>"
  ./mvnw clean test
  ```
- **Crear la base demo desde cero** (con el backend detenido):
  ```powershell
  & "C:\Program Files\PostgreSQL\18\bin\dropdb.exe" -U postgres --force cacao_demo
  & "C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres cacao_demo
  ```
  Al arrancar con el perfil `demo` se cargan de nuevo los datos de ejemplo.
- **Escenario del incidente** (bloqueo de un producto y sus derivados): [`docs/incidente-hora-12.md`](docs/incidente-hora-12.md).
