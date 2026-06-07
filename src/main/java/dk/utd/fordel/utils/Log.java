package dk.utd.fordel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static RuntimeException giveUp(Logger logger, Throwable e, String message) {
        logger.atError().setCause(e).log(message);
        return new RuntimeException(message, e);
    }

    public static RuntimeException giveUp(Logger logger, Throwable e) {
        return giveUp(logger, e, "Fatal error");
    }

}
