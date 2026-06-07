package de.steffzilla.weighttracker.util;

/**
 * Wraps a LiveData value that must be consumed exactly once (e.g. Snackbar messages).
 * Prevents re-delivery after config changes.
 */
public class Event<T> {

    private final T content;
    private boolean consumed = false;

    public Event(T content) {
        this.content = content;
    }

    public T getContentIfNotConsumed() {
        if (consumed) return null;
        consumed = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}