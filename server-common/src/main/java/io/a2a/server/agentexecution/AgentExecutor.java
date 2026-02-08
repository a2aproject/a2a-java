package io.a2a.server.agentexecution;

import io.a2a.server.events.EventQueue;
import io.a2a.spec.A2AError;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Core business logic interface for implementing A2A agent functionality.
 * <p>
 * This is the primary extension point where agent developers implement their agent's behavior -
 * LLM interactions, data processing, external API calls, or any custom logic. Along with an
 * {@link io.a2a.spec.AgentCard}, implementing this interface is the minimum requirement to
 * create a functioning A2A agent.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * The {@link io.a2a.server.requesthandlers.DefaultRequestHandler} executes AgentExecutor methods
 * asynchronously in a background thread pool when requests arrive from transport layers.
 * Your implementation should:
 * <ul>
 *   <li>Use the {@link EventQueue} to enqueue task status updates and artifacts</li>
 *   <li>Use {@link io.a2a.server.tasks.TaskUpdater} helper for common lifecycle operations</li>
 *   <li>Handle cancellation via the {@link #cancel(RequestContext, EventQueue)} method</li>
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
 *     public void execute(RequestContext context, EventQueue eventQueue) {
 *         TaskUpdater updater = new TaskUpdater(context, eventQueue);
 *
 *         // Initialize task if this is a new conversation
 *         if (context.getTask() == null) {
 *             updater.submit();
 *         }
 *         updater.startWork();
 *
 *         // Extract user input from the message
 *         String userMessage = context.getUserInput("\n");
 *
 *         // Process request (your business logic)
 *         String weatherData = weatherService.getWeather(userMessage);
 *
 *         // Return result as artifact
 *         updater.addArtifact(List.of(new TextPart(weatherData, null)));
 *         updater.complete();
 *     }
 *
 *     @Override
 *     public void cancel(RequestContext context, EventQueue eventQueue) {
 *         // Clean up resources and mark as canceled
 *         new TaskUpdater(context, eventQueue).cancel();
 *     }
 * }
 * }</pre>
 *
 * <h2>Streaming Results</h2>
 * For long-running operations or LLM streaming, enqueue multiple artifacts:
 * <pre>{@code
 * updater.startWork();
 * for (String chunk : llmService.stream(userInput)) {
 *     updater.addArtifact(List.of(new TextPart(chunk, null)));
 * }
 * updater.complete();  // Final event closes the queue
 * }</pre>
 *
 * @see RequestContext
 * @see io.a2a.server.tasks.TaskUpdater
 * @see io.a2a.server.events.EventQueue
 * @see io.a2a.server.requesthandlers.DefaultRequestHandler
 * @see io.a2a.spec.AgentCard
 */
public interface AgentExecutor {
    /**
     * Executes the agent's business logic for a message.
     * <p>
     * Called asynchronously by {@link io.a2a.server.requesthandlers.DefaultRequestHandler}
     * in a background thread when a client sends a message. Enqueue events to the queue as
     * processing progresses. The queue remains open until you enqueue a final event
     * (COMPLETED, FAILED, or CANCELED state).
     * </p>
     * <p>
     * <b>Important:</b> Don't throw exceptions for business logic errors. Instead, use
     * {@code updater.fail(errorMessage)} to communicate failures to the client gracefully.
     * Only throw {@link A2AError} for truly exceptional conditions.
     * </p>
     *
     * @param context the request context containing the message, task state, and configuration
     * @param eventQueue the queue for enqueueing status updates and artifacts
     * @throws A2AError if execution fails catastrophically (exception propagates to client)
     */
    @WithSpan("AgentExecutor.execute")
    void execute(RequestContext context, EventQueue eventQueue) throws A2AError;

    /**
     * Cancels an ongoing agent execution.
     * <p>
     * Called when a client requests task cancellation via the cancelTask operation.
     * You should:
     * <ul>
     *   <li>Stop any ongoing work (interrupt LLM calls, cancel API requests)</li>
     *   <li>Enqueue a CANCELED status event (typically via {@code TaskUpdater.cancel()})</li>
     *   <li>Clean up resources (close connections, release locks)</li>
     * </ul>
     * <p>
     * <b>Note:</b> The {@link #execute(RequestContext, EventQueue)} method may still be
     * running on another thread. Use appropriate synchronization or interruption mechanisms
     * if your agent maintains cancellable state.
     * <p>
     * <b>Error Handling:</b>
     * <ul>
     *   <li>Throw {@link io.a2a.spec.TaskNotCancelableError} if your agent does not support
     *       cancellation at all (e.g., fire-and-forget agents)</li>
     *   <li>Throw {@link A2AError} if cancellation is supported but failed to execute
     *       (e.g., unable to interrupt running operation)</li>
     *   <li>Return normally after enqueueing CANCELED event if cancellation succeeds</li>
     * </ul>
     *
     * @param context the request context for the task being canceled
     * @param eventQueue the queue for enqueueing the cancellation event
     * @throws io.a2a.spec.TaskNotCancelableError if this agent does not support cancellation
     * @throws A2AError if cancellation is supported but failed to execute
     */
    @WithSpan("AgentExecutor.cancel")
    void cancel(RequestContext context, EventQueue eventQueue) throws A2AError;
}
