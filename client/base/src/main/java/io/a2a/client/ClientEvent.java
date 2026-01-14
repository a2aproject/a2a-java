package io.a2a.client;

/**
 * A sealed interface representing events received by an A2A client from an agent.
 * <p>
 * ClientEvent is the base type for all events that clients receive during agent interactions.
 * The sealed interface ensures type safety by restricting implementations to three known subtypes:
 * <ul>
 *   <li>{@link MessageEvent} - contains complete messages with content parts</li>
 *   <li>{@link TaskEvent} - contains complete task state, typically final states</li>
 *   <li>{@link TaskUpdateEvent} - contains incremental task updates (status or artifact changes)</li>
 * </ul>
 * <p>
 * <b>Event flow:</b> When a client sends a message to an agent, the agent's response is delivered
 * as a stream of ClientEvent instances to registered event consumers. The event type and sequence
 * depend on the agent's capabilities and the task's lifecycle:
 * <p>
 * <b>Simple blocking response:</b>
 * <pre>
 * User → Agent
 * Agent → MessageEvent (contains agent's text response)
 * </pre>
 * <p>
 * <b>Streaming task execution:</b>
 * <pre>
 * User → Agent
 * Agent → TaskEvent (SUBMITTED)
 * Agent → TaskUpdateEvent (WORKING)
 * Agent → TaskUpdateEvent (artifact update with partial results)
 * Agent → TaskUpdateEvent (artifact update with more results)
 * Agent → TaskUpdateEvent (COMPLETED)
 * </pre>
 * <p>
 * <b>Typical usage pattern:</b>
 * <pre>{@code
 * client.addConsumer((event, agentCard) -> {
 *     switch (event) {
 *         case MessageEvent me -> {
 *             // Simple message response
 *             System.out.println("Response: " + me.getMessage().parts());
 *         }
 *         case TaskEvent te -> {
 *             // Complete task state (usually final)
 *             Task task = te.getTask();
 *             System.out.println("Task " + task.id() + ": " + task.status().state());
 *         }
 *         case TaskUpdateEvent tue -> {
 *             // Incremental update
 *             Task currentTask = tue.getTask();
 *             UpdateEvent update = tue.getUpdateEvent();
 *
 *             if (update instanceof TaskStatusUpdateEvent statusUpdate) {
 *                 System.out.println("Status changed to: " +
 *                     currentTask.status().state());
 *             } else if (update instanceof TaskArtifactUpdateEvent artifactUpdate) {
 *                 System.out.println("New content: " +
 *                     artifactUpdate.artifact().parts());
 *             }
 *         }
 *     }
 * });
 * }</pre>
 * <p>
 * <b>Legacy vs current protocol:</b> In older versions of the A2A protocol, agents returned
 * {@link MessageEvent} for simple responses and {@link TaskEvent} for task-based responses.
 * The current protocol (v1.0+) uses {@link TaskUpdateEvent} for streaming updates during
 * task execution, providing finer-grained visibility into agent progress.
 *
 * @see MessageEvent
 * @see TaskEvent
 * @see TaskUpdateEvent
 * @see ClientBuilder#addConsumer(java.util.function.BiConsumer)
 */
public sealed interface ClientEvent permits MessageEvent, TaskEvent, TaskUpdateEvent {
}
