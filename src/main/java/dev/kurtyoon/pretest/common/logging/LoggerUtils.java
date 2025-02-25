package dev.kurtyoon.pretest.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {
    private LoggerUtils() {}

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void info(Logger logger, String message, Object... args) {
        logger.info(message, args);
    }

    public static void warn(Logger logger, String message, Object... args) {
        logger.info(message, args);
    }

    public static void error(Logger logger, String message, Object... args) {
        logger.error(message, args);
    }
}
