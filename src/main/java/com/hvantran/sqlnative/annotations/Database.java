package com.hvantran.sqlnative.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to specify database information, it can refer to property by using syntax {property_name}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Database {

    String url ();

    String username () default "";

    String password () default "";
}
