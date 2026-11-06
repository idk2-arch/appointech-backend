-- RF25: el historial de pagos se ordena por fecha de pago descendente (los pendientes, sin fecha, al final) y
-- filtra por rango de fechas; este indice sirve ese orden y ese rango sin ordenar toda la tabla.
CREATE INDEX IF NOT EXISTS idx_pago_pagado_en ON pago (pagado_en DESC NULLS LAST, id DESC);
