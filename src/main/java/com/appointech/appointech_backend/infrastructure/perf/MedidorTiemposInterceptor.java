package com.appointech.appointech_backend.infrastructure.perf;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Mide cada invocación y registra [LENTO] (a stdout, como el resto de logs) si supera el umbral. */
class MedidorTiemposInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MedidorTiemposInterceptor.class);

    private final long umbralMs;
    private final ThreadLocal<Boolean> midiendo = ThreadLocal.withInitial(() -> false);
    private static final String MEDIDA = MedidorTiemposInterceptor.class.getName() + ".medida";

    MedidorTiemposInterceptor(long umbralMs) {
        this.umbralMs = umbralMs;
    }

    @Override
    public Object invoke(MethodInvocation invocacion) throws Throwable {
        ServletRequestAttributes atributos = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a ? a : null;
        if (midiendo.get() || (atributos != null && atributos.getRequest().getAttribute(MEDIDA) != null)) {
            return invocacion.proceed();
        }
        if (atributos != null) atributos.getRequest().setAttribute(MEDIDA, true);
        midiendo.set(true);
        long inicio = System.nanoTime();
        try {
            return invocacion.proceed();
        } finally {
            midiendo.remove();
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
            if (duracionMs >= umbralMs) {
                log.warn("[LENTO] {}.{} tardó {} ms (umbral {} ms)",
                        ClassUtils.getUserClass(invocacion.getThis()).getSimpleName(),
                        invocacion.getMethod().getName(), duracionMs, umbralMs);
            }
        }
    }
}
