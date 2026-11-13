# E6 sostenido, 300 usuarios: recursos parciales

Nota documental añadida durante la inspección de E7. No repite E6, no cambia sus resultados y no implementa E7.

Corrida: `perf/resultados/e6-sostenido-20261111-163033-3edb7724625d47959406bb744830cd8f/fase-300`.

- JMeter terminó con código 0. El JTL tiene 102.705 muestras conciliadas; la corrida global **no quedó completamente validada** porque falló el recolector.
- `recursos.csv` conserva 119 filas: 59 pares completos backend/base y una fila adicional de backend. No se corrigió, interpoló ni completó el CSV.
- Backend: 60 capturas, última a +114,557 s desde el origen; faltan los últimos 25,443 s antes del final sostenido en +140 s.
- Base: 59 capturas, última a +112,473 s; faltan los últimos 27,527 s del sostenido. No hay capturas del cierre HTTP de 1,478 s.
- CPU y memoria solo describen los máximos muestreados durante la cobertura disponible. No son máximos garantizados de toda la fase ni métricas atribuibles por endpoint.
- `Receive-Job` notificó un conflicto de escritura de `recursos.csv` a las 2026-11-11T21:34:08.9466026Z. Esa hora es la recepción del error, no necesariamente su ocurrencia en el escritor. No se registró el propietario del bloqueo; no se atribuye a un PID leído posteriormente.
- El error saltó las asignaciones posteriores a JMeter: `diagnostico.json` conserva contadores iniciales de cero y preparación no validada. `revision-posterior.json` explica la diferencia con el setup listo y el JTL real, sin sobrescribir el diagnóstico original.
- La integridad de datos se conservó; recuperación: tres HTTP 200 válidos en 22, 7 y 30 ms. Eso no subsana la captura parcial de recursos.
- La preparación interrumpida anterior a E6-200 añadió calentamiento fuera de su JTL. Las diferencias entre 100 y 200 no son un efecto aislado demostrado del número de usuarios.

Evidencias originales: [informe revisado](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e6-sostenido-20261111-163033-3edb7724625d47959406bb744830cd8f/fase-300/INFORME-REVISADO.md>), [revisión posterior](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e6-sostenido-20261111-163033-3edb7724625d47959406bb744830cd8f/fase-300/revision-posterior.json>) y [CSV de recursos](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e6-sostenido-20261111-163033-3edb7724625d47959406bb744830cd8f/fase-300/recursos.csv>).

Antes de una futura implementación de E7 se debe revisar y validar sin HTTP el ciclo de vida del recolector. Esta nota no realiza esa implementación o validación.
