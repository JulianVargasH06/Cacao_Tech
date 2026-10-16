# Bitácora de uso de IA · CacaoTech

Registro de cómo se usó inteligencia artificial durante la hackathon (15 y 16 de octubre de 2026),
qué partes del proyecto se hicieron con ella y cómo se validó su trabajo.

## 1. Herramientas

| Herramienta | Quién la usó | Para qué | Acceso al proyecto |
|---|---|---|---|
| **Claude Code** (Anthropic, modelo Claude Opus), aplicación de escritorio | villarreal-dev18 (Frontend 1 / operaciones; luego también backend y Frontend 2) | Desarrollo guiado y directo, pruebas, diagnóstico y documentación | Lectura y edición del repositorio local, terminal (compilar, probar, git) y navegador integrado para probar la app |
| **Antigravity** (Google), modelo **Gemini Flash** | Julian, Ana y villarreal-dev18 | Estructura inicial del proyecto, inicialización de Angular y Spring Boot, configuración de PrimeNG, lógica y pruebas de merma, costos y margen, entidades JPA, seguridad JWT y recepción de lotes | Editor con agente: trabaja sobre los archivos del proyecto |
| **ChatGPT** (OpenAI) | Ana | Configuración de `application.properties` | Conversación aparte: el resultado se llevó al proyecto a mano |

Repositorio: https://github.com/JulianVargasH06/Cacao_Tech

### Cómo identificar el trabajo hecho con IA

- **Commits**: los que terminan con la línea `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Pull requests**: los que terminan con `🤖 Generated with Claude Code`.
- Antes de adoptar esa convención hubo commits hechos con la IA **sin** esa línea; se identificaron
  cruzando el historial de git con los registros de las sesiones de Claude Code y se listan en la [sección 3](#3-registro-por-commit).

## 2. Modos de trabajo

| Modo | Cuándo | Cómo funcionó | Papel humano |
|---|---|---|---|
| **Guiado** | 15/10 noche – 16/10 madrugada (Frontend 1) | Claude explicaba cada paso y proponía el código; el integrante creaba los archivos y pegaba el código; Claude revisaba, compilaba y preparaba el commit. | Decidir cada paso, escribir/pegar el código, aprobar antes de avanzar, crear y fusionar los PR. |
| **Integración** | 16/10 madrugada | Claude conectó el Frontend 1 con el backend real y completó endpoints del backend que faltaban. | Revisar, probar en el navegador con el backend local y fusionar los PR. |
| **Desarrollo directo** | 16/10 mañana | A pedido del integrante ("hazlo tú"), Claude desarrolló en una rama nueva, compiló, probó y subió la rama con un PR propuesto. | Definir qué construir, revisar el resultado, aprobar y fusionar cada PR, ensayar las pruebas del jurado. |

Al inicio Claude hizo cambios sin guía y el integrante pidió **revertirlos**; desde ahí se acordó trabajar paso a paso.
Esa decisión explica el modo guiado de la primera fase.

## 3. Registro por commit

### 3.1 Fase guiada · Frontend 1 (operaciones)

Commits hechos desde Claude Code sin la línea de coautoría (anteriores a la convención), salvo el primero.

| Commit | Fecha | PR | Descripción | Aporte de la IA |
|---|---|---|---|---|
| `6abe3ae` | 15/10 22:11 | #2 | Base compartida del frontend | Estructura base, tema visual (cristal + tonos cacao), layout. **Lleva la línea de Claude.** |
| `a6287ee` | 15/10 23:00 | #4 | Rutas del operador con MainLayout | Guía y código propuesto |
| `b489aa6` | 15/10 23:28 | #5 | Página de inicio de sesión (HU13) | Guía y código propuesto |
| `b3635fd` | 15/10 23:40 | #5 | authGuard y rolGuard | Guía y código propuesto |
| `2e4a0b0` | 16/10 00:58 | #6 | Recepción de lotes con validaciones (HU01) | Guía y código propuesto |
| `ba8ba0b` | 16/10 01:29 | #8 | Tabla de inventario por lote (HU08) | Guía y código propuesto |
| `cc5d24b` | 16/10 01:51 | #8 | Búsqueda, estado y errores en inventario (HU08) | Guía y código propuesto |
| `be5d2da` | 16/10 02:46 | #9 | Registro de transformaciones con validación de existencias (HU02, HU08) | Guía y código propuesto |
| `80bcb3c` | 16/10 02:57 | #10 | Modelo Lote alineado con el backend; menú activo | Guía y código propuesto |
| `9bf3932` | 16/10 03:09 | #12 | Historial de operaciones con auditoría (HU02, HU03, HU10) | Guía y código propuesto |

### 3.2 Fase de integración con el backend real

| Commit | Fecha | PR | Descripción | Aporte de la IA |
|---|---|---|---|---|
| `7ae2503` | 16/10 03:42 | #14 | Login conectado al backend real | Código y pruebas en navegador |
| `54149d8` | 16/10 04:15 | #15 | Operador de prueba, nombre en login, GET /api/lotes, credenciales de BD por variables de entorno | Código backend |
| `55c73f6` | 16/10 05:06 | #30 | Transformaciones en el backend con consumo de inventario (RF02) | Código backend y tests |
| `6878a0d` | 16/10 05:22 | #31 | Recepción, inventario, transformaciones e historial con el API real | Código frontend |
| `4977312` | 16/10 05:44 | #32 | Auditoría de recepciones y transformaciones, GET /api/auditoria (HU10) | Código backend y tests |
| `82d0d99` | 16/10 05:50 | #33 | Historial con auditoría real y cambios legibles (HU10) | Código frontend |
| `63ba387` | 16/10 06:21 | #34 | Alertas por merma con bloqueo automático, incidencias, liberar/bloquear (HU04, HU09) | Código backend y tests |
| `4ac0ca1` | 16/10 06:30 | #35 | Consulta pública de trazabilidad sin token ni datos privados (HU06) | Código backend y tests |

### 3.3 Fase de desarrollo directo (commits con la línea de Claude)

| Commit | Fecha | PR | Descripción |
|---|---|---|---|
| `482c2c3` | 16/10 07:01 | #36 | Backend: filtros y CSV de lotes, costos y margen, gestión de usuarios (HU05, HU11, HU14) |
| `ef93938` | 16/10 07:07 | #37 | Frontend 2: modelos y servicios según los endpoints |
| `c8a6b0f` | 16/10 07:09 | #37 | Panel del administrador y rutas /admin |
| `1ff9738` | 16/10 07:11 | #37 | Ficha comercial pública sin login (HU06) |
| `4f8821f` | 16/10 07:13 | #37 | Gestión de productos: liberar y bloquear con motivo (HU09) |
| `6cecd0f` | 16/10 07:16 | #37 | Análisis de mermas, costos y alertas (HU04, HU05) |
| `e0077e3` | 16/10 07:18 | #37 | Trazabilidad interna de lotes (HU03) |
| `714278f` | 16/10 07:19 | #37 | Auditoría y exportación filtrada de lotes (HU10, HU11) |
| `d98a2cc` | 16/10 07:45 | #37 | Corrección: la auditoría muestra solo los campos que cambiaron |
| `e3b3400` | 16/10 07:45 | #37 | Gestión de usuarios: crear, asignar rol, activar/desactivar (HU14) |
| `b74b6ad` | 16/10 07:54 | #38 | La ficha pública como entrada principal para compradores |
| `31dfe7d` | 16/10 08:40 | #39 | Landing page informativa (a partir de un prompt con requisitos escrito por el equipo) |
| `0a25954` | 16/10 08:08 | #41 | Datos de demostración con el perfil `demo` |
| `b5ca8af` | 16/10 08:24 | #40 | Incidente de producto y retiro (escenario de la hora 12) |
| `facc592` | 16/10 08:57 | #42 | Corrección de espaciado en la landing (detectado por el integrante) |
| `afb456f` | 16/10 09:09 | #43 | Huecos de las pruebas del jurado: lote CAC001, fórmula del margen visible, reenvío idempotente, scripts de respaldo |
| `7a5aacd` | 16/10 09:46 | #44 | Ficha pública: "Bloqueado · en revisión de calidad" (pedido del integrante) |
| `26d762f` | 16/10 10:40 | #45 | README con guía de arranque y guion de las pruebas del jurado |
| — | 16/10 | — | Esta bitácora |

### 3.4 Commits con Antigravity y ChatGPT

Estos commits no tienen una línea de coautoría; la herramienta de cada uno se registra según lo informado por el equipo.

| Commit | Fecha | Autor | Descripción | IA |
|---|---|---|---|---|
| `d509c72` | 15/10 16:14 | Julian | Estructura inicial del proyecto | Antigravity · Gemini Flash |
| `72050f5` | 15/10 17:21 | Julian | Inicializar frontend con Angular | Antigravity · Gemini Flash |
| `a249786` | 15/10 17:29 | Julian | Inicializar backend Spring Boot | Antigravity · Gemini Flash |
| `3f4cb99` | 15/10 18:21 | Ana | application.properties | **ChatGPT** |
| `d512d63` | 15/10 18:25 | villarreal-dev18 | PrimeNG y configuración base (PR #1) | Antigravity · Gemini Flash |
| `cd4090c` | 15/10 18:46 | Ana | Creación de lote | Antigravity · Gemini Flash |
| `5ef65d0` | 15/10 22:00 | Julian | Calculadora de merma y pruebas unitarias | Antigravity · Gemini Flash |
| `c10e0d7` | 15/10 22:39 | Julian | Lógica pura de merma, costos y margen | Antigravity · Gemini Flash |
| `aded100` | 16/10 01:06 | Ana | Entidades JPA, repositorio Lote, seguridad base, Swagger | Antigravity · Gemini Flash |
| `0485ae6` | 16/10 02:58 | Ana | Autenticación JWT, seguridad y DataSeeder | Antigravity · Gemini Flash |
| `c966929` | 16/10 03:54 | Ana | Recepción de lotes RF01 | Antigravity · Gemini Flash |

Resumen: **10 commits con Antigravity** (Julian 5, Ana 4, villarreal-dev18 1) y **1 con ChatGPT** (Ana).
Con esto, todos los commits del proyecto (sin contar los de merge de los PR) quedan registrados con su herramienta de IA.

## 4. Uso de IA fuera del código

| Actividad | Qué hizo la IA |
|---|---|
| Pruebas en navegador | Probó cada pantalla contra el backend local, en escritorio y celular, y detectó errores de UX (botón que no respondía al primer clic, desbordes en celular). |
| Revisión contra las pruebas del jurado | Comparó las 9 pruebas con el código y detectó 4 huecos (CAC001 inexistente, fórmula no visible, reenvío sin protección, sin respaldo). |
| Ensayo de las pruebas | Guió el ensayo paso a paso; **el integrante ejecutó cada prueba** y confirmó los resultados con capturas. |
| Diagnóstico de errores | Interpretó un `BUILD FAILURE` (faltaba la contraseña de la BD en la terminal) y errores de `dropdb`. |
| Documentación | README, guía del incidente (`docs/incidente-hora-12.md`), guía de respaldo (`database/README.md`), descripciones de los PR y esta bitácora. |

## 5. Validación humana y controles

- **El trabajo de Antigravity y ChatGPT** lo revisó e integró cada integrante en sus ramas; la lógica de merma,
  costos y margen quedó cubierta por pruebas unitarias (`MermaCalculoTest`, `MermaAlertaTest`, `MargenVentaTest`, `DistribucionCostoTest`).
- **Ningún cambio llegó a `main` sin revisión humana**: la IA subía ramas; el integrante abría, revisaba y fusionaba cada PR en GitHub.
- **Compilación y tests** antes de cada commit: `mvnw clean compile` / `test-compile` en el backend y `ng build` en el frontend.
  La suite final tiene **97 tests en verde**, ejecutada por el integrante.
- **Las 9 pruebas del jurado se ensayaron a mano** con la app real antes de la presentación.
- **Credenciales**: la contraseña de la base de datos nunca se escribió en archivos ni en commits;
  se usa solo por variables de entorno (`DB_PASSWORD`).
- **Restricciones respetadas**: en la landing no se modificaron autenticación, interceptores ni guardias, y no se
  instalaron dependencias nuevas.

## 6. Lecciones

- Trabajar **por ramas pequeñas con PR** permitió revisar lo que hacía la IA y revertir sin perder trabajo.
- Pedir a la IA **comparar el sistema con los criterios del jurado** encontró huecos que no se veían usando la app.
- La IA puede afirmar que algo funciona solo con haberlo compilado; **ensayar en la app real** fue lo que dio confianza.
- Marcar los commits con **`Co-Authored-By`** desde el principio habría facilitado esta bitácora.
