import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.DateTimeFileCreationStrategy;
import com.darkyen.tproll.logfunctions.FileLogFunction;
import com.darkyen.tproll.logfunctions.LogFileHandler;
import com.darkyen.tproll.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.Random;

/**
 *
 */
public class LogFileRotationTest {
    public static void main(String[] args){
        TPLogger.setLogFunction(
                new FileLogFunction(
                        new TimeFormatter.AbsoluteTimeFormatter(),
                        new LogFileHandler(
                                new File("test logs"),
                                new DateTimeFileCreationStrategy(
                                        DateTimeFileCreationStrategy.DEFAULT_DATE_TIME_FILE_NAME_FORMATTER,
                                        true,
                                        ".bacon",
                                        10, Duration.ofSeconds(10L)
                                ),
                                false, 500_000_000 /*500MB*/, Long.MAX_VALUE, true))
        );
        TPLogger.TRACE();

        final String[] lotsOfText = {"Spicy jalapeno bacon ipsum dolor amet pancetta pork consectetur, doner tri-tip sirloin et spare ribs ut chuck ullamco jerky nisi brisket ut. Drumstick in dolor nostrud alcatra, aliqua ut boudin voluptate ea ground round brisket prosciutto exercitation. Swine minim frankfurter, mollit proident fugiat ut jerky occaecat. Dolor pork andouille, occaecat kevin shankle incididunt commodo spare ribs sunt filet mignon pastrami doner in. Pork loin nisi sausage frankfurter in ut.",
                                    "Brisket eu consequat, velit pork chop nostrud sirloin short ribs aliqua ex sed kevin. Aliquip salami aliqua sunt chicken. Laborum lorem elit, short ribs cupim short loin dolore turducken aliquip tail doner ex labore in flank. Porchetta fugiat ut velit salami qui. Chicken spare ribs andouille ham hock picanha laborum tempor brisket mollit cupidatat shankle pork chop frankfurter.",
                                    "Shank short loin jowl enim burgdoggen. Ground round est sint shankle aliquip, turkey deserunt irure voluptate. Prosciutto cow pork chop pancetta pariatur occaecat ullamco pork belly ham hock et enim. Sunt pancetta laboris, do anim prosciutto pork ut jowl.",
                                    "Et cupidatat short loin voluptate shankle meatloaf pork loin. Ea porchetta bacon, irure salami quis in t-bone burgdoggen. In picanha proident kevin pork, venison pork chop aute alcatra nisi prosciutto beef. Ribeye alcatra velit, salami dolore culpa ex sint shoulder beef ribs burgdoggen pork belly ham meatloaf voluptate. In shankle beef ribs pork loin pork jerky rump hamburger minim.",
                                    "Do tempor aliquip, ut excepteur ham hock ad ground round jowl dolor pastrami. Elit in laboris beef ribs nulla ribeye consequat duis biltong tempor cillum ut proident ut pork loin. Non ribeye hamburger lorem, boudin prosciutto pork irure beef esse pork chop tail ut. Burgdoggen in occaecat pig consequat, rump flank ut pork belly ex. Irure rump ullamco in commodo ham hock. Incididunt bacon shank sausage dolore laboris ad occaecat ham hock ribeye leberkas non sed. Duis strip steak cupidatat brisket tail ham hock magna meatball culpa."};


        final Logger LOG = LoggerFactory.getLogger("FileRotationTest");

        //Log a lot of stuff
        final Random random = new Random();
        final int length = 1 + random.nextInt(5);
        for (int i = 0; i < length; i++) {
            LOG.info(lotsOfText[random.nextInt(lotsOfText.length)]);
        }
    }
}
