package com.thinkai4j.tool.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

    String description() default "";

    boolean required() default true;
}
