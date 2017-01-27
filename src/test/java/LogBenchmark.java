import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFunctionMultiplexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 */
public class LogBenchmark {

    public static void main(String[] args){
        final LogFunction NOP_FUNCTION = (name, time, level, marker, content, error) -> {
            //NOP
        };
        TPLogger.setLogFunction(NOP_FUNCTION);
        measure();
        measure();
        measure();
        final int nop = measure();

        TPLogger.setLogFunction(new LogFunctionMultiplexer(NOP_FUNCTION, NOP_FUNCTION, NOP_FUNCTION, NOP_FUNCTION));
        measure();
        measure();
        measure();
        final int muxnop = measure();

        final PrintStream realOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {

            }
        }));

        TPLogger.setLogFunction(LogFunction.SIMPLE_LOG_FUNCTION);
        measure();
        measure();
        measure();
        final int simple = measure();

        TPLogger.setLogFunction(new FileLogFunction(new File("test logs")));
        measure();
        measure();
        measure();
        final int file = measure();

        TPLogger.setLogFunction(new LogFunctionMultiplexer(LogFunction.SIMPLE_LOG_FUNCTION, TPLogger.getLogFunction()));
        measure();
        measure();
        measure();
        final int s_and_f = measure();

        System.setOut(realOut);

        System.out.println("NOP:    "+nop+" ms");
        System.out.println("MNOP:   "+muxnop+" ms");
        System.out.println("Simple: "+simple+" ms");
        System.out.println("File:   "+file+" ms");
        System.out.println("C + F:  "+s_and_f+" ms");
    }


    public static int measure(){
        final Logger log = LoggerFactory.getLogger("BENCH_LOG");
        //noinspection ThrowableNotThrown
        final Exception testException = new Exception();
        final int[] ints = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};

        final long start = System.currentTimeMillis();
        final int statements = 1_000_000;
        for (int i = 0; i < statements; i++) {
            switch (i & 7) {
                case 0:
                    log.info("Simple message");
                    break;
                case 1:
                    log.info("String substitution message {}", "string substitute");
                    break;
                case 2:
                    log.info("Rejected message {}", testException);
                    break;
                case 3:
                    log.info("Int substitution message {}", 42);
                    break;
                case 4:
                    log.info("Array substitution message {}", ints);
                    break;
                case 5:
                    log.info("Exception message", testException);
                    break;
                case 6:
                    log.info("3 parameter message {} {} {}", "string", 42, ints);
                    break;
                case 7:
                    log.info("Message with no template spots", "String", 42, ints, testException);
                    break;
            }
        }
        final long end = System.currentTimeMillis();
        //return (int) (statements / ((end - start)/1000.0));
        return (int) (end - start);
    }
}
