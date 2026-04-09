package org.a2aproject.sdk.server.agentexecution;

import org.a2aproject.sdk.server.events.EventQueue;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;

/**
 * Core business logic interface for implementing A2A agent functionality.
 * <p>
 * This is the primary extension point where agent developers implement their agent's behavior -
 * LLM interactions, data processing, external API calls, or any custom logic. Along with an
 * {@link org.a2aproject.sdk.spec.AgentCard}, implementing this interface is the minimum requirement to
 * create a functioning A2A agent.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * The {@link org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler} executes AgentExecutor methods
 * asynchronously in a background thread pool when requests arrive from transport layers.
 * Your implementation should:
 * <ul>
 *   <li>Use the {@link AgentEmitter} to send messages, update task status, and stream artifacts</li>
 *   <li>Handle cancellation via the {@link #cancel(RequestContext, AgentEmitter)} method</li>
 *   <li>Be thread-safe if maintaining state across invocations</li>
 * </ul>
 *
 * <h2>Threading Model</h2>
 * <ul>
 *   <li>{@code execute()} runs in the agent-executor thread pool (background thread)</li>
 *   <li>Events are consumed by Vert.x worker threads that return responses to clients</li>
 *   <li>Don't block waiting for events to be consumed - enqueue and return</li>
 *   <li>Multiple {@code execute()} calls may run concurrently for different tasks</li>
 * </ul>
 *
 * <h2>CDI Integration</h2>
 * Provide your AgentExecutor via CDI producer:
 * <pre>{@code
 * @ApplicationScoped
 * public class MyAgentExecutorProducer {
 *     @Inject
 *     MyService myService;  // Your business logic
 *
 *     @Produces
 *     public AgentExecutor agentExecutor() {
 *         return new MyAgentExecutor(myService);
 *     }
 * }
 * }</pre>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class WeatherAgentExecutor implements AgentExecutor {
 *     private final WeatherService weatherService;
 *
 *     public WeatherAgentExecutor(WeatherService weatherService) {
 *         this.weatherService = weatherService;
 *     }
 *
 *     @Override
 *     public void execute(RequestContext context, AgentEmitter emitter) {
 *         // Initialize task if this is a new conversation
 *         if (context.getTask() == null) {
 *             emitter.submit();
 *         }
 *         emitter.startWork();
 *
 *         // Extract user input from the message
 *         String userMessage = context.getUserInput("\n");
 *
 *         // Process request (your business logic)
 *         String weatherData = weatherService.getWeather(userMessage);
 *
 *         // Return result as artifact
 *         emitter.addArtifact(List.of(new TextPart(weatherData, null)));
 *         emitter.complete();
 *     }
 *
 *     @Override
 *     public void cancel(RequestContext context, AgentEmitter emitter) {
 *         // Clean up resources and mark as canceled
 *         emitter.cancel();
 *     }
 * }
 * }</pre>
 *
 * <h2>Streaming Results</h2>
 * For long-running operations or LLM streaming, enqueue multiple artifacts:
 * <pre>{@code
 * emitter.startWork();
 * for (String chunk : llmService.stream(userInput)) {
 *     emitter.addArtifact(List.of(new TextPart(chunk, null)));
 * }
 * emitter.complete();  // Final event closes the queue
 * }</pre>
 *
 * @see RequestContext
 * @see AgentEmitter
 * @see org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler
 * @see org.a2aproject.sdk.spec.AgentCard
 */
public interface AgentExecutor {
    /**
     * Executes the agent's business logic for a message.
     * <p>
     * Called asynchronously by {@link org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler}
     * in a background thread when a client sends a message. Enqueue events to the queue as
     * processing progresses. The queue remains open until you enqueue a final event
     * (COMPLETED, FAILED, or CANCELED state).
     * </p>
     * <p>
     * <b>Important:</b> Don't throw exceptions for business logic errors. Instead, use
     * {@code emitter.fail(errorMessage)} to communicate failures to the client gracefully.
     * Only throw {@link A2AError} for truly exceptional conditions.
     * </p>
     *
     * @param context the request context containing the message, task state, and configuration
     * @param emitter the agent emitter for sending messages, updating status, and streaming artifacts
     * @throws A2AError if execution fails catastrophically (exception propagates to client)
     */
    void execute(RequestContext context, AgentEmitter emitter) throws A2AError;

    /**
     * Cancels an ongoing agent execution.
     * <p>
     * Called when a client requests task cancellation via the cancelTask operation.
     * You should:
     * <ul>
     *   <li>Stop any ongoing work (interrupt LLM calls, cancel API requests)</li>
     *   <li>Enqueue a CANCELED status event (typically via {@code emitter.cancel()})</li>
     *   <li>Clean up resources (close connections, release locks)</li>
     * </ul>
     * <p>
     * <b>Note:</b> The {@link #execute(RequestContext, AgentEmitter)} method may still be
     * running on another thread. Use appropriate synchronization or interruption mechanisms
     * if your agent maintains cancellable state.
     * <p>
     * <b>Error Handling:</b>
     * <ul>
     *   <li>Throw {@link org.a2aproject.sdk.spec.TaskNotCancelableError} if your agent does not support
     *       cancellation at all (e.g., fire-and-forget agents)</li>
     *   <li>Throw {@link A2AError} if cancellation is supported but failed to execute
     *       (e.g., unable to interrupt running operation)</li>
     *   <li>Return normally after enqueueing CANCELED event if cancellation succeeds</li>
     * </ul>
     *
     * @param context the request context for the task being canceled
     * @param emitter the agent emitter for sending the cancellation event
     * @throws org.a2aproject.sdk.spec.TaskNotCancelableError if this agent does not support cancellation
     * @throws A2AError if cancellation is supported but failed to execute
     */
    void cancel(RequestContext context, AgentEmitter emitter) throws A2AError;
}
