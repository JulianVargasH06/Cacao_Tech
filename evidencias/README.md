# Evidencias

Capturas que muestran el cumplimiento de los casos de prueba del informe final
([`docs/CacaoTech.SuperNova.pdf`](../docs/CacaoTech.SuperNova.pdf)).
Se tomaron sobre una base de datos aparte (`cacao_pruebas`), vacía al empezar.

| Caso | Historia | Prueba | Evidencia |
|---|---|---|---|
| CP-01 | HU01 | Registro válido de CAC001 (Rivera, 100 kg) | [Captura1.png](Captura1.png) |
| CP-02 | HU01 | Cantidad negativa rechazada | [Captura2.png](Captura2.png) |
| CP-03 | HU01 | Lote duplicado rechazado | [Captura3.png](Captura3.png) |
| CP-04 | HU02 | Transformación 100 → 80 kg con alerta de merma del 20 % | [Captura4.png](Captura4.png) |
| CP-05 | HU08 | Consumo mayor al inventario rechazado | [Captura5.png](Captura5.png) |
| CP-06 | HU08 | Reenvío de la operación sin doble consumo | [Captura6.png](Captura6.png) |
| CP-07 | HU12 | Salida de 0 kg controlada (sin división por cero) | [Captura7.png](Captura7.png) |
| CP-08 | HU09 | Operador intenta bloquear: 403 | [Captura8.png](Captura8.png) |
| CP-09 | HU09 | Bloqueo sin motivo: motivo obligatorio | [Captura9.png](Captura9.png) |
| CP-10 | HU06 | Ficha pública de CAC001 bloqueado | [Captura10.png](Captura10.png) |
| CP-11 | HU10 | Liberación de CAC002 auditada (antes / después) | [Captura11.png](Captura11.png) |
| CP-12 | HU11 | Exportación CSV filtrada por "Rivera" | [Captura12-lotes-rivera.csv](Captura12-lotes-rivera.csv) |
| CP-13 | HU13 | Login como operador: redirige a Recepción | [Captura13.png](Captura13.png) |
| CP-14 | HU07 | Persistencia tras reiniciar PostgreSQL y el servidor | [Captura14A.png](Captura14A.png) (antes) · [Captura14B.png](Captura14B.png) (después) |
