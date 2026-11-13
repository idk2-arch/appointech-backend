# Cierre de analisis E8: resistencia continua

Se reanaliza exclusivamente el JTL existente. No se ejecuto HTTP ni JMeter en este cierre. La corrida valida de resistencia es la de continuacion; el intento anterior de cuatro muestras permanece como fallo del plan HeaderManager durante calentamiento, excluido.

Fuentes: [JTL](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/e8.jtl>), [reloj](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/reloj.json>), [analisis original](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/analisis-e8.json>), [recursos](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/recursos.csv>), [auditoria previa](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/auditoria-continuacion-final.json>).

## Ventanas y definiciones

Calentamiento: 2026-11-12T03:08:49.825Z a 2026-11-12T03:09:49.825Z, 60s, 600 HTTP. Medicion: 2026-11-12T03:09:49.825Z a 2026-11-12T03:19:49.825Z, 600s, 5996 HTTP. Inicios de cierre: 0. Se conservan separados; el calentamiento no aporta a aceptacion.
Tasas de inicios: numero de timestamp en [a,b) /60s por minuto. HTTP200 recibidos y conformes se asignan por timestamp+elapsed en [a,b), pudiendo cruzar una frontera. Conforme=HTTP200+contenido valido+elapsed dentro de 800ms para notificaciones/historial o 300ms para especialidades. Latencias, errores y percentiles se asignan por inicio e incluyen toda la respuesta aunque termine en otra ventana; p95/p99 nearest-rank ceil(p*N). No se cambia ningun umbral.
En los 600s: 5996/600 = 9.9933 iniciadas/s y 9.9933 conformes recibidas/s. Cuatro de 6000 turnos omitidos (0.0667%); dos en minuto 1 y dos en minuto 6, registrados sin catch-up. No se presenta como 10.0000 exactos. Los diez minutos se recalcularon contra el JTL, sin alterar el analisis original.

## Medicion minuto a minuto

Cada celda de percentiles indica p95 / p99, en ms. N/H/E=notificaciones/historial/especialidades. C/D/T/HTTP=fallos de contenido, duracion, timeout y HTTP numerico distinto de 200 (categorias potencialmente solapadas).

|Min|HTTP iniciadas/s|HTTP200 recibidas/s|Conformes/s|n N/H/E|N p95/p99|H p95/p99|E p95/p99|Errores C/D/T/HTTP|
|---|---:|---:|---:|---|---|---|---|---|
|1|9.967|9.967|9.967|239/240/119|39/97|53/281|22/108|0/0/0/0|
|2|10.000|10.000|10.000|240/240/120|23/39|27/42|20/36|0/0/0/0|
|3|10.000|10.000|10.000|240/240/120|22/28|26/31|21/22|0/0/0/0|
|4|10.000|10.000|10.000|240/240/120|27/48|28/42|22/28|0/0/0/0|
|5|10.000|10.000|10.000|240/240/120|25/38|26/30|20/23|0/0/0/0|
|6|9.967|9.967|9.967|240/238/120|26/36|27/31|22/54|0/0/0/0|
|7|10.000|10.000|10.000|240/240/120|25/40|32/53|23/57|0/0/0/0|
|8|10.000|10.000|10.000|240/240/120|24/37|25/32|19/21|0/0/0/0|
|9|10.000|10.000|10.000|240/240/120|25/39|28/45|22/30|0/0/0/0|
|10|10.000|10.000|10.000|240/240/120|23/26|24/28|19/20|0/0/0/0|

CPU en % crudo Docker (sin normalizar), memoria de contenedor en MiB, no heap. Las columnas de memoria indican media / maximo; CPU media / maximo. Capturas backend/PG por minuto: 29 o 30, no un valor por segundo.

|Min|Hilos min/max|HTTP solapadas max/media|CPU backend|Mem backend MiB|CPU PostgreSQL|Mem PostgreSQL MiB|Capturas/servicio|
|---|---|---|---|---|---|---|---:|
|1|50/50|4/0.225|4.317/16.000|635.814/636.500|1.918/9.640|160.917/161.200|29|
|2|50/50|2/0.184|3.037/6.060|635.770/636.700|1.929/7.250|159.887/161.000|30|
|3|50/50|1/0.180|3.079/6.250|635.952/637.100|1.661/4.030|159.672/160.000|29|
|4|50/50|2/0.193|3.613/7.380|635.950/636.800|2.048/7.410|159.637/160.000|30|
|5|50/50|1/0.185|3.365/11.530|635.890/637.300|1.709/5.950|159.662/160.000|29|
|6|50/50|1/0.185|3.222/5.670|635.797/636.500|1.830/6.330|159.647/160.000|30|
|7|50/50|2/0.197|3.998/10.980|635.803/636.400|2.367/9.350|159.690/160.200|29|
|8|50/50|1/0.179|3.096/7.360|635.927/636.800|2.110/7.900|159.773/163.400|30|
|9|50/50|1/0.188|2.962/5.800|636.003/637.500|1.749/5.700|159.610/160.100|29|
|10|50/50|1/0.177|2.574/5.940|636.117/637.400|1.444/4.530|159.640/160.100|30|

## Primer minuto frente al ultimo y tendencia observable

|Magnitud|Minuto 1|Minuto 10|Cambio|
|---|---:|---:|---|
|Iniciadas/s y conformes/s|9.9667|10.0000|598 a 600 solicitudes; sin deterioro de ritmo|
|notificaciones p95/p99 ms|39/97|23/26|Ambos menores al final|
|historial p95/p99 ms|53/281|24/28|Ambos menores al final|
|especialidades p95/p99 ms|22/108|19/20|Ambos menores al final|
|backend-perf CPU media %|4.317|2.574|-1.743 puntos|
|backend-perf memoria media MiB|635.814|636.117|0.303 MiB|
|db-perf CPU media %|1.918|1.444|-0.474 puntos|
|db-perf memoria media MiB|160.917|159.640|-1.277 MiB|

Las colas de latencia son mayores en el primer minuto y menores en el ultimo; hay fluctuaciones intermedias (por ejemplo minuto 7) y no una degradacion progresiva de los percentiles en esta ventana. Esto es compatible con estabilizacion posterior al calentamiento, pero no identifica cache, JIT, GC u otra causa. No se transforma esa observacion en una garantia futura.
La media de memoria backend cambia solo 0.303 MiB; el primer valor muestreado fue 636.000 y el ultimo 635.600 MiB. PostgreSQL baja 1.277 MiB en media entre minutos extremos. Las medias fluctuan; el maximo backend de cada minuto no crece monotonicamente. **No se observa una deriva grande y sostenida de memoria de contenedor en estos diez minutos; esto NO demuestra ausencia de fuga.** Faltan mediciones de heap/objetos retenidos/GC, mas duracion y replicas para excluir fugas lentas. Cero errores HTTP no aporta por si solo esa prueba.
El p99 de especialidades usa alrededor de 120 observaciones/minuto; describe muy pocas muestras de cola, con precision limitada. Las corridas previas y su calentamiento no constituyen replicas controladas.

## Calentamiento, recuperacion y alcance

|Fase|n|Iniciadas/s|Conformes recibidas/s|N p95/p99|H p95/p99|E p95/p99|
|---|---:|---:|---:|---|---|---|
|Calentamiento (60s)|600|10.0000|10.0000|25/80|26/55|19/25|
|Medicion (600s)|5996|9.9933|9.9933|25/45|28/49|21/32|

La captura real conserva 348 pares completos, hueco maximo 3525ms, y cubre calentamiento, 600s, drenaje y recuperacion. [Recuperacion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/recuperacion.json>): nueve GET correctos y dentro de umbral. [Integridad](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/integridad.json>): cantidades/huellas iguales en 15 tablas; salud y limites sin cambios, sin reinicios/OOM.
50 hilos permanecieron activos (600 observaciones), pero HTTP solapadas maximo 4, media 0.18931. No se infiere concurrencia HTTP de 50 ni saturacion; fue resistencia de lecturas a ritmo limitado. La conclusion se limita a 10 minutos, tres rutas y datos repetidos. El error de dashboard se reparo offline en el turno anterior; el estado original del lanzador y su conciliacion siguen preservados.

Guia del profesor, pagina 6: 50 usuarios y duracion sugerida 10-15 minutos, observar estabilidad, memoria, degradacion y recuperacion. Se observo el extremo inferior de duracion; no se afirman quince minutos ni ausencia de fuga. Fuente: [PDF](</C:/Users/Juan David/Downloads/ESCENARIOS OBLIGATORIOS DE PRUEBAS DE CARGA Y ESTRÉS.pdf>).
