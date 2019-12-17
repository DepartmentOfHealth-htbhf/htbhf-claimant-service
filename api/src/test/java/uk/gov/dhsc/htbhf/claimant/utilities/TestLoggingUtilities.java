package uk.gov.dhsc.htbhf.claimant.utilities;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;
import uk.gov.dhsc.htbhf.logging.TestAppender;

import java.util.List;

@SuppressWarnings("PMD.MoreThanOneLogger")
public class TestLoggingUtilities {

    private static final TestAppender TEST_APPENDER = new TestAppender();

    public static void startRecordingLogsFor(Class clazz) {
        TEST_APPENDER.start();
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        logger.addAppender(TEST_APPENDER);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        TestAppender.clearAllEvents();
    }

    public static void stopRecordingLogsFor(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        logger.detachAppender(TEST_APPENDER);
        TestAppender.clearAllEvents();
        TEST_APPENDER.stop();
    }

    public static List<ILoggingEvent> getLogEvents() {
        return TestAppender.getEvents();
    }
}
