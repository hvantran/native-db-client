package com.hvantran.sqlnative.utils;

import com.hvantran.sqlnative.interfaces.AppException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

public class ObjectUtils {

    private ObjectUtils() {

    }

    public static void checkThenThrow(boolean predicate) {
        checkThenThrow(predicate, "The condition must be TRUE");
    }

    public static void checkThenThrow(boolean predicate, String message) {
        if (predicate) {
            throw new AppException(message);
        }
    }

    public static <T> void checkThenThrow(Predicate<T> predicate, T instance, String message) {
        if (predicate.test(instance)) {
            throw new AppException(message);
        }
    }

    public static <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass, Object instance, Method method) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        Class<?> superclass = instance.getClass().getSuperclass();
        try {
            return Optional.ofNullable(superclass
                    .getDeclaredMethod(method.getName())
                    .getAnnotation(annotationClass));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }
    public static <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass, Object instance) {
        T annotation = instance.getClass().getAnnotation(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        return Optional.of(instance.getClass().getSuperclass().getAnnotation(annotationClass));
    }
}
