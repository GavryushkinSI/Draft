package ru.app.draft.aop;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import ru.app.draft.models.MetricItem;

import java.util.List;
import java.util.function.BiFunction;

import static ru.app.draft.store.Store.METRICS;

@Aspect
@Log4j2
@Component
public class Logger {

    @Around(value = "execution(* *(..)) && @annotation(ru.app.draft.annotations.Audit)")
    public Object around(ProceedingJoinPoint point) {
        long start = System.currentTimeMillis();
        Object result = null;
        try {
            result = point.proceed();
//            log.info(String.format("Success time execute methodName %s: %s ms",point.getSignature().getName(), System.currentTimeMillis() - start));
            METRICS.computeIfPresent("methods", (s, metricItems) -> {
                metricItems.add(new MetricItem(point.getSignature().getName(),System.currentTimeMillis() - start));
                return metricItems;
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }
}