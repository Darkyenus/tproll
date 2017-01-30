package com.darkyen.tproll.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Optimized PrintWriter instance for writing into supplied StringBuilder.
 *
 * Is NOT thread safe!
 */
public final class StringBuilderWriter extends PrintWriter {

    private final StringBuilder sb;
    private static final char LINE_END = '\n';

    public StringBuilderWriter(StringBuilder sb) {
        super(NOP_WRITER, false);
        this.sb = sb;
        this.out = null;
    }

    @Override
    public void flush() {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public void write(int c) {
        sb.appendCodePoint(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        sb.append(buf, off, len);
    }

    @Override
    public void write(char[] buf) {
        sb.append(buf);
    }

    @Override
    public void write(String s, int off, int len) {
        sb.append(s, off, len);
    }

    @Override
    public void write(String s) {
        sb.append(s);
    }

    @Override
    public void print(boolean b) {
        sb.append(b);
    }

    @Override
    public void print(char c) {
        sb.append(c);
    }

    @Override
    public void print(int i) {
        sb.append(i);
    }

    @Override
    public void print(long l) {
        sb.append(l);
    }

    @Override
    public void print(float f) {
        sb.append(f);
    }

    @Override
    public void print(double d) {
        sb.append(d);
    }

    @Override
    public void print(char[] s) {
        sb.append(s);
    }

    @Override
    public void print(String s) {
        sb.append(s);
    }

    @Override
    public void print(Object obj) {
        sb.append(obj);
    }

    @Override
    public void println() {
        sb.append(LINE_END);
    }

    @Override
    public void println(boolean x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(char x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(int x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(long x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(float x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(double x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(char[] x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(String x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public void println(Object x) {
        sb.append(x).append(LINE_END);
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        return format(format, args);
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object... args) {
        return format(l, format, args);
    }

    @Override
    public PrintWriter format(String format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    @Override
    public PrintWriter format(Locale l, String format, Object... args) {
        try {
            sb.append(String.format(format, args));
        } catch (IllegalFormatException e) {
            setError();
        }
        return this;
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        sb.append(csq);
        return this;
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public PrintWriter append(char c) {
        sb.append(c);
        return this;
    }

    private static final Writer NOP_WRITER = new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            //NOP
        }

        @Override
        public void flush() throws IOException {
            //NOP
        }

        @Override
        public void close() throws IOException {
            //NOP
        }
    };
}
