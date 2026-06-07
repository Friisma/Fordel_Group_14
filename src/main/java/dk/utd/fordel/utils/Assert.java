package dk.utd.fordel.utils;

/**
 * This is a util class to write small assertions, so that they are hidden from
 * jacoco (the coverage engine).
 */
public class Assert {

    public final static void isNotNull(Object expected, String msg) {
        if (expected == null) {
            throw new IllegalArgumentException(msg);
        }
    }
    
    public final static void isTrue(boolean expected, String msg) {
        if (!expected) {
            throw new AssertionError(msg);
        }
    }

    public final static void isNull(Object expected, String msg) {
        if (expected != null) {
            throw new IllegalArgumentException(msg);
        }
    }

}
