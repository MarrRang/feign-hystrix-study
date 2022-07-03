package com.example.feignhystrixstudy.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CustomFeignHystrix {
    String value() default "";

    int executionIsolationThreadTimeoutInMilliseconds() default 3000;

    int executionTimeoutInMilliseconds() default 3000;

    boolean executionTimeoutEnabled() default true;

    int metricsRollingStatisticalWindowInMilliseconds() default 10000;

    int circuitBreakerRequestVolumeThreshold() default 10;

    int circuitBreakerErrorThresholdPercentage() default 50;

    int circuitBreakerSleepWindowInMilliseconds() default 5000;
}
