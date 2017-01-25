package com.darkyen.tproll;

import org.slf4j.ILoggerFactory;

/**
 */
public final class TPLoggerFactory implements ILoggerFactory {

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
