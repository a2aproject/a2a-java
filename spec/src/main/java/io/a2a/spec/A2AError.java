package io.a2a.spec;

/**
 * Marker interface for all A2A protocol error events.
 * <p>
 * This interface extends {@link Event} and serves as a common type for all
 * error-related events in the A2A protocol. It provides a way to categorize
 * and handle error events uniformly across the system.
 * </p>
 * <p>
 * Implementations of this interface represent various types of errors that
 * can occur during A2A protocol operations, such as validation errors,
 * processing errors, or communication failures.
 * </p>
 */
public interface A2AError extends Event {
}
