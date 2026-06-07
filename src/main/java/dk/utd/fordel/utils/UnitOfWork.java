package dk.utd.fordel.utils;

/**
 * UnitOfWork is a pattern to group a set of operations that should
 * either all succeed or all fail.
 * 
 * It also opens a connection to the database and closes it when
 * the unit of work is done.
 * 
 * You should call this in a try-with-resources block to ensure that
 * the connection is closed even if an exception is thrown.
 */
public interface UnitOfWork extends AutoCloseable {

    UnitOfWork begin() throws UnitOfWorkException;

    void commit() throws UnitOfWorkException;

    void rollback() throws UnitOfWorkException;

    void close() throws UnitOfWorkException;

    class UnitOfWorkException extends Exception {
        public UnitOfWorkException(String msg) {
            super(msg);
        }

        public UnitOfWorkException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

}
