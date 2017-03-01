package com.tooploox.ussd.utils;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 17:37
 */
@FunctionalInterface
public interface Predicate<T> {
    
    boolean apply(T t);
}
