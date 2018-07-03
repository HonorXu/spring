package com.honor.servlet.annotation;

import java.lang.annotation.*;


@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DemoAutowired {
	String value() default "";
}
