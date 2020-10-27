package org.slf4j.impl;

import com.darkyen.tproll.TPLoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

@SuppressWarnings("unused")
public final class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();

    public static StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    /**
     * Declare the version of the SLF4J API this implementation is compiled
     * against. The value of this field is usually modified with each release.
     * Do NOT make this final (compiler constant folding problems).
     */
    public static String REQUESTED_API_VERSION = "1.7.30";

    private StaticLoggerBinder() {
    }

    private static final TPLoggerFactory getLoggerFactory_cache = new TPLoggerFactory();
    public ILoggerFactory getLoggerFactory() {
        return getLoggerFactory_cache;
    }

    private static final String getLoggerFactoryClassStr_cache = TPLoggerFactory.class.getName();
    public String getLoggerFactoryClassStr() {
        return getLoggerFactoryClassStr_cache;
    }
}
