package me.saif.betterenderchests.utils;

/**
 * Thrown when stored enderchest data cannot be turned back into items.
 * Callers must never treat this as "empty chest": the stored data is still
 * intact and has to stay untouched in the database.
 */
public class ItemDeserializationException extends RuntimeException {

    public ItemDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemDeserializationException(String message) {
        super(message);
    }
}
