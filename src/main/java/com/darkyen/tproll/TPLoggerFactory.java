package com.darkyen.tproll;

import org.slf4j.ILoggerFactory;

/**
 * Instantiated by SLF4J to create {@link TPLogger} instances.
 */
@SuppressWarnings("WeakerAccess")
public final class TPLoggerFactory implements ILoggerFactory {

    /** When true (default), only last dot-delimited part of logger name is used.
     * (It is assumed, that the name is of a class, with package prepended.) */
    public static boolean USE_SHORT_NAMES = true;

    @Override
    public TPLogger getLogger(String name) {
        if(USE_SHORT_NAMES){
            final int i = name.lastIndexOf('.');
            if(i != -1 && i < name.length() - 3){
                name = name.substring(i+1);
            }
        }
        return new TPLogger(name);
    }
}
