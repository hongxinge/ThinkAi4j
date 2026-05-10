package com.thinkai4j.agent;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiAgent {

    String name() default "";

    String description() default "";
}
