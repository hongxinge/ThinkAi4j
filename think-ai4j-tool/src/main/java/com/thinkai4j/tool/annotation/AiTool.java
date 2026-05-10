package com.thinkai4j.tool.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiTool {

    String value() default "";

    String name() default "";
}
