package org.slf4j.impl;

import com.darkyen.tproll.TPLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

@SuppressWarnings("unused")
public final class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final @NotNull StaticLoggerBinder INSTANCE = new StaticLoggerBinder();

    public static @NotNull StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    /**
     * Declare the version of the SLF4J API this implementation is compiled
     * against. The value of this field is usually modified with each release.
     * Do NOT make this final (compiler constant folding problems).
     */
    public static @NotNull String REQUESTED_API_VERSION = "1.7.30";

    private StaticLoggerBinder() {
    }

    private static final @NotNull TPLoggerFactory getLoggerFactory_cache = new TPLoggerFactory();
    public @NotNull ILoggerFactory getLoggerFactory() {
        return getLoggerFactory_cache;
    }

    private static final @NotNull String getLoggerFactoryClassStr_cache = TPLoggerFactory.class.getName();
    public @NotNull String getLoggerFactoryClassStr() {
        return getLoggerFactoryClassStr_cache;
    }
}
