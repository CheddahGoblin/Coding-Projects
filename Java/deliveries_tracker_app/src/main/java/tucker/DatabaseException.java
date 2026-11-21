package tucker;

/** Thrown when the database can't be opened, read, or written. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }
}
