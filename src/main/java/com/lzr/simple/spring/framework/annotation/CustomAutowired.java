package com.lzr.simple.spring.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomAutowired {
    String value() default "";
}
