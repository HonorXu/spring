package com.honor.servlet.annotation;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DemoService {
	String value() default "";
}
