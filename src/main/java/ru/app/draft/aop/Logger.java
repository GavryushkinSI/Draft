package ru.app.draft.aop;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

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
            log.info(String.format("Success time execute methodName %s: %s ms",point.getSignature().getName(), System.currentTimeMillis() - start));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }
}