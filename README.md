# tproll
Lightweight logging backend for SLF4J

Simple logging backend, designed to have minimal overhead and easy setup, while allowing for huge customizability.

## Install
This is a SLF4J backend, so make sure you have [SLF4J API](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22slf4j-api%22).
Then, get it from jitpack: [![](https://jitpack.io/v/Darkyenus/tproll.svg)](https://jitpack.io/#Darkyenus/tproll)

For convenience:
```
jitpack at https://jitpack.io
org.slf4j:slf4j-api:1.7.22
com.github.Darkyenus:tproll:v1.2.5
```

## Use
If you have tproll and SLF4J api on classpath, you can start using SLF4J logging as usual.
By default, the backend is logging to the stdout/err only, and from info level up.
All configuration is done through code. No XML, no json, no new languages to learn.
To change log level, call `com.darkyen.tproll.TPLogger.DEBUG()` and other static functions.

**What happens, when I log something?** The message goes through SLF4J API to the `TPLogger` class,
which performs early log level check and then performs parameter substitution. Result goes to the `LogFunction` (set globally, for all loggers, through `TPLogger.setLogFunction(func)`).

`LogFunction` is an interface, which handles what should happen with the message next. By default, it is printed out to stdout/err (through `LogFunction.SIMPLE_LOG_FUNCTION`).
This logger is generally enough early in development and it is still useful later.

Next logical step is logging to a **file**. For this, use `FileLogFunction`, which logs to a file (or multiple files, one file for each restart, this can be configured).
Convenience constructor is provided, which could be enough for most applications (`new FileLogFunction(new File("<log directory>"))`).
If you need something more advanced/specific, you can create your own `ILogFileHandler` or just `LogFileCreationStrategy` for the default `LogFileHandler`.
This may look like a lot of classes, but don't worry, most are short and don't do many things, they are split for customizability.

**What if I want to log to a file AND to stdout/err at the same time?** Just use `LogFunctionMultiplexer` with the ultimate logging functions as constructor parameter.
This class can (and is primarily built to) route various log messages to different LogFunctions using `Marker`s. See the JavaDoc!

**tproll uses/doesn't use colors, how do I tell it to turn them on/off?**
Color is used only when outputting to the stdout/err, so don't worry about it polluting log files.
Color support detection is very basic. To override it, set either Java property `tproll.color` (`$ java -Dtproll.color=true -jar ...`)
or environment variable `TPROLL_COLOR` to `true` to enable color or `false` to disable.

## Example
**TL;DR**, what do I need to copy/paste to leverage this framework?

```java
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

private static final Logger LOG = LoggerFactory.getLogger(Main.class);

public static void main(String[] args) throws Exception {
    TPLogger.setLogFunction(
            new LogFunctionMultiplexer(
                    LogFunction.DEFAULT_LOG_FUNCTION, // Log to console
                    new FileLogFunction(new File("test logs")) // & Log to file in "test logs" directory
            ));

    LOG.warn("tproll {} !", (System.currentTimeMillis() & 1) == 0 ? "is great" : "rules");

    //Add this line to log unhandled exceptions in your threads
    TPLogger.attachUnhandledExceptionLogger();
}
```
