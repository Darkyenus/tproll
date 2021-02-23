package unit;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.util.SimpleMarker;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.impl.StaticLoggerBinder;

import static unit.Util.stripAnsi;

/**
 *
 */
public class RenderableMarkerTest {

	@Test
	public void renderableMarker() {
		final Logger log = StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger("TEST");
		final StringBuilder logSb = new StringBuilder();
		TPLogger.setLogFunction(new SimpleLogFunction(null, null) {
			@Override
			protected void logLine(byte level, @NotNull CharSequence formattedContent) {
				logSb.append(formattedContent);
			}
		});
		log.info(new SimpleMarker.Renderable("mark1"), "Marked.");
		Assert.assertEquals("[INFO | mark1] TEST: Marked.", stripAnsi(logSb.toString()));
		logSb.setLength(0);

		final SimpleMarker.Renderable mark1 = new SimpleMarker.Renderable("mark1");
		mark1.add(new SimpleMarker.Renderable("mark2"));
		log.info(mark1, "Marked.");
		Assert.assertEquals("[INFO | mark1 | mark2] TEST: Marked.", stripAnsi(logSb.toString()));
		logSb.setLength(0);
	}

}
