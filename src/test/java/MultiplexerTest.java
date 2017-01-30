import com.darkyen.tproll.LogFunction;
import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.LogFunctionMultiplexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 *
 */
public class MultiplexerTest {
    public static void main(String[] args){
        final LogFunctionMultiplexer mux = new LogFunctionMultiplexer();
        final LogFunctionMultiplexer.MuxMarker DEFAULT = mux.addMuxTarget(new TestLogger("DEFAULT"), true);
        final LogFunctionMultiplexer.MuxMarker A = mux.addMuxTarget(new TestLogger("<A>"), false);
        final LogFunctionMultiplexer.MuxMarker B = mux.addMuxTarget(new TestLogger("<B>"), false);
        final LogFunctionMultiplexer.MuxMarker C = mux.addMuxTarget(new TestLogger("<C>"), false);
        final LogFunctionMultiplexer.MuxMarker D = mux.addMuxTarget(new TestLogger("<D>"), false);
        final LogFunctionMultiplexer.MuxMarker E = mux.addMuxTarget(new TestLogger("<E>"), false);
        final LogFunctionMultiplexer.MuxMarker F = mux.addMuxTarget(new TestLogger("<F>"), false);
        final LogFunctionMultiplexer.MuxMarker G = mux.addMuxTarget(new TestLogger("<G>"), false);
        final LogFunctionMultiplexer.MuxMarker H = mux.addMuxTarget(new TestLogger("<H>"), false);
        TPLogger.setLogFunction(mux);

        final Logger LOG = LoggerFactory.getLogger("MultiplexerTest");

        LOG.info("Default");

        LOG.info(A, "A");
        LOG.info(B, "B");
        LOG.info(C, "C");
        LOG.info(D, "D");
        LOG.info(E, "E");
        LOG.info(F, "F");
        LOG.info(G, "G");
        LOG.info(H, "H");

        final LogFunctionMultiplexer.MuxMarker AB = A.newCompound(B);
        LOG.info(AB, "AB");
        final LogFunctionMultiplexer.MuxMarker ABC = AB.newCompound(C);
        LOG.info(ABC, "ABC");
        final LogFunctionMultiplexer.MuxMarker ABCD = ABC.newCompound(D);
        LOG.info(ABCD, "ABCD");

        final LogFunctionMultiplexer.MuxMarker BDFH = B.copy();
        BDFH.add(D);
        BDFH.add(F);
        BDFH.add(H);
        LOG.info(BDFH, "Hello");
    }

    private static class TestLogger implements LogFunction {

        private final String prefix;

        private TestLogger(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void log(String name, long time, byte level, Marker marker, CharSequence content) {
            LogFunction.SIMPLE_LOG_FUNCTION.log(name, time, level, marker, prefix+content);
        }
    }
}
