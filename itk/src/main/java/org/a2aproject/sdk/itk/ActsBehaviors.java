package org.a2aproject.sdk.itk;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ACTS SUT behaviour contract (ACTS spec §11).
 *
 * <p>ACTS tests are declarative — they say what to send and what to expect — so the agent under
 * test has to produce a deterministic reply for each case. §11 does that with a message-prefix
 * convention rather than a side-channel API: the text of the first user message names the
 * behaviour. {@code ItkAgentExecutor} routes here when it sees a {@code tck-} prefix and otherwise
 * runs the ITK instruction path, so one agent serves both suites.
 *
 * <p>{@code acts/sut-behaviors.yaml} is what this SDK claims; this class is what it does. Nothing
 * here restates the list of names — the prefix is read out of the message, and one that reaches
 * {@link #dispatch} without a branch fails the task rather than completing it, so a gap shows up
 * in the conformance report instead of passing quietly.
 */
final class ActsBehaviors {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActsBehaviors.class);

    /**
     * Greedy to the word boundary, which gives longest-match for free: {@code tck-artifact-file-url}
     * beats {@code tck-artifact-file} with no ordered table.
     */
    private static final Pattern NAME = Pattern.compile("^(tck-[a-z0-9]+(?:-[a-z0-9]+)*)");

    /**
     * Behaviours whose whole contract is the state they leave the task in. Named for
     * {@code sut-behaviors.yaml}'s {@code terminal_state} key, though two of them are interrupted
     * states rather than terminal ones.
     */
    private static final Map<String, TaskState> TERMINAL_STATES = Map.of(
            "tck-complete-task", TaskState.TASK_STATE_COMPLETED,
            "tck-task-failure", TaskState.TASK_STATE_FAILED,
            "tck-reject-task", TaskState.TASK_STATE_REJECTED,
            "tck-input-required", TaskState.TASK_STATE_INPUT_REQUIRED,
            "tck-auth-required", TaskState.TASK_STATE_AUTH_REQUIRED);

    private static final String MULTI_TURN_DONE = "done";

    /**
     * Short enough not to dominate a run, long enough that a test polling for a non-terminal state
     * sees one: the corpus polls every 2s, 15 times.
     */
    private static final long LONG_RUNNING_DELAY_MS = 1000;

    /** Safety bound on a parked {@code tck-cancel} task, so agent-pool threads are never leaked. */
    private static final long CANCEL_PARK_TIMEOUT_MINUTES = 5;

    /**
     * Wakes a parked {@code tck-cancel} execution.
     *
     * <p>The SDK delivers no cancellation signal into a running {@code execute()} — the
     * {@code CompletableFuture.cancel(true)} it issues never interrupts — and {@code cancel()} runs
     * on the request thread with its own {@code RequestContext} and {@code AgentEmitter}. A latch
     * shared by task id is the only channel between the two.
     */
    private final ConcurrentMap<String, CountDownLatch> parked = new ConcurrentHashMap<>();

    /**
     * The behaviour each live task was opened with.
     *
     * <p>A continuation names the behaviour only in its first message, so the other SDKs' agents
     * recover it from the task's history. a2a-java stores no history — a task created from an
     * agent event gets {@code initialMessage = null}, so the user's message never lands in it —
     * which leaves the executor no record of its own contract. This note is that record. It hides
     * nothing: CORE-HIST-005 and CORE-HIST-006 assert on the history itself and still fail.
     */
    private final ConcurrentMap<String, String> opened = new ConcurrentHashMap<>();

    private final ActsClientParse clientParse = new ActsClientParse();

    /**
     * Whether the card should advertise a diminished capability set.
     *
     * <p>Four ACTS tests assert that an agent <em>without</em> a capability answers
     * {@code UnsupportedOperationError}, so their preconditions require the card not to advertise
     * it and they can never run against a fully capable agent. The runner starts a second SUT with
     * this variable set to reach them. The card alone is enough here: every a2a-java transport
     * handler gates streaming, push-notification and extended-card operations on the resolved
     * card's capabilities.
     */
    static boolean reducedCapabilities() {
        String value = System.getenv("ITK_ACTS_REDUCED_CAPABILITIES");
        return value != null && !value.isEmpty();
    }

    /**
     * Resolves the behaviour from the incoming message, falling back to the task it belongs to.
     *
     * <p>A multi-turn test opens with the prefix and then sends plain "here is more input" and
     * "done", so a continuation has to recover the contract from where it was declared. History is
     * the right place to look and is tried first; {@link #opened} covers the case where this SDK
     * kept none.
     *
     * <p>The name is an asserted behaviour, not necessarily an implemented one: an unknown
     * {@code tck-} still routes to ACTS and is reported as unimplemented, which beats handing a
     * message plainly meant for ACTS to the traversal decoder.
     */
    String behaviorFor(RequestContext context) {
        String named = behaviorIn(firstText(context.getMessage()));
        if (named != null) {
            opened.put(context.getTaskId(), named);
            return named;
        }
        Task task = context.getTask();
        if (task == null) {
            return null;
        }
        if (task.history() != null) {
            for (Message historical : task.history()) {
                String found = behaviorIn(firstText(historical));
                if (found != null) {
                    return found;
                }
            }
        }
        return opened.get(context.getTaskId());
    }

    private static String behaviorIn(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = NAME.matcher(text.strip());
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String firstText(Message message) {
        if (message == null || message.parts() == null) {
            return null;
        }
        for (Part<?> part : message.parts()) {
            if (part instanceof TextPart textPart && !textPart.text().isEmpty()) {
                return textPart.text();
            }
        }
        return null;
    }

    void run(RequestContext context, AgentEmitter emitter, String behavior) {
        LOGGER.info("Serving ACTS behaviour {} for task {}", behavior, emitter.getTaskId());

        // This one must open no task at all: A2A lets an agent answer with a bare Message, and a
        // server that created a task first would turn the reply into a task update, which is what
        // CORE-SEND-003 checks.
        if ("tck-message-response".equals(behavior)) {
            emitter.sendMessage("tck message response");
            return;
        }

        // Persists WORKING before anything blocks, so a concurrent cancel_task finds a non-final
        // task in the store and a blocking send_message is released by the first event.
        emitter.startWork();
        dispatch(context, emitter, behavior);
    }

    private void dispatch(RequestContext context, AgentEmitter emitter, String behavior) {
        if (ActsClientParse.BEHAVIOR.equals(behavior)) {
            clientParse.run(context, emitter);
            return;
        }

        if (behavior.startsWith("tck-artifact-")) {
            List<Part<?>> parts = artifactParts(behavior);
            if (parts == null) {
                unimplemented(emitter, behavior);
                return;
            }
            emitter.addArtifact(parts, null, behavior, null);
            complete(emitter, TaskState.TASK_STATE_COMPLETED, behavior + " ok");
            return;
        }

        switch (behavior) {
            case "tck-multi-turn" -> multiTurn(context, emitter);
            case "tck-cancel" -> waitForCancel(emitter);
            case "tck-long-running" -> longRunning(emitter);
            case "tck-stream-basic", "tck-stream-chunked" -> stream(emitter, behavior);
            default -> {
                TaskState state = TERMINAL_STATES.get(behavior);
                if (state == null) {
                    unimplemented(emitter, behavior);
                } else {
                    complete(emitter, state, behavior + " ok");
                }
            }
        }
    }

    /** Releases a task parked by {@code tck-cancel}; a no-op for every other behaviour. */
    void released(String taskId) {
        opened.remove(taskId);
        CountDownLatch latch = parked.get(taskId);
        if (latch != null) {
            latch.countDown();
        }
    }

    /** Fails the task rather than completing it: a silent success would report conformance the agent never demonstrated. */
    private void unimplemented(AgentEmitter emitter, String behavior) {
        complete(emitter, TaskState.TASK_STATE_FAILED, "unimplemented ACTS behaviour \"" + behavior + "\"");
    }

    private void complete(AgentEmitter emitter, TaskState state, String text) {
        if (state.isFinal()) {
            opened.remove(emitter.getTaskId());
        }
        emitter.updateStatus(state, emitter.newAgentMessage(List.of(new TextPart(text)), null));
    }

    private void multiTurn(RequestContext context, AgentEmitter emitter) {
        String said = context.getUserInput(" ").strip().toLowerCase();
        if (said.startsWith(MULTI_TURN_DONE)) {
            complete(emitter, TaskState.TASK_STATE_COMPLETED, "multi-turn complete");
            return;
        }
        complete(emitter, TaskState.TASK_STATE_INPUT_REQUIRED, "more input please");
    }

    /**
     * Holds the task in WORKING until {@code cancel_task} arrives.
     *
     * <p>Emits nothing while parked. {@code doCancelTask} breaks on the first event it sees on the
     * tapped queue and rejects anything that is not a CANCELED task, so a heartbeat here would make
     * the cancel RPC fail with {@code TaskNotCancelableError}. Returning emits nothing either — the
     * cancel side has already published CANCELED on its own emitter.
     */
    private void waitForCancel(AgentEmitter emitter) {
        String taskId = emitter.getTaskId();
        CountDownLatch latch = parked.computeIfAbsent(taskId, id -> new CountDownLatch(1));
        try {
            if (!latch.await(CANCEL_PARK_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                complete(emitter, TaskState.TASK_STATE_FAILED, "tck-cancel was never canceled");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            parked.remove(taskId, latch);
        }
    }

    private void longRunning(AgentEmitter emitter) {
        try {
            Thread.sleep(LONG_RUNNING_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // CORE-EXEC-001 polls to completion and then asserts the finished task carries at least one
        // artifact, so the work has to leave one behind even though §11.2 describes this behaviour
        // only as delayed completion.
        emitter.addArtifact(List.of(new TextPart("long running result")), null, "long-running", null);
        complete(emitter, TaskState.TASK_STATE_COMPLETED, "long running work finished");
    }

    private List<Part<?>> artifactParts(String behavior) {
        return switch (behavior) {
            case "tck-artifact-text" -> List.of(new TextPart("generated text content"));
            case "tck-artifact-data" -> List.of(new DataPart(dataArtifact()));
            case "tck-artifact-file" -> List.of(new FilePart(new FileWithBytes(
                    "text/plain", "document.txt", "file bytes".getBytes(StandardCharsets.UTF_8))));
            case "tck-artifact-file-url" -> List.of(new FilePart(new FileWithUri(
                    "text/plain", "document.txt", "https://example.com/document.txt")));
            default -> null;
        };
    }

    private Map<String, Object> dataArtifact() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", "value");
        data.put("count", 1);
        return data;
    }

    /**
     * Emits working -> artifact(s) -> completed as separate events, so each becomes its own SSE
     * frame; a single combined update would satisfy {@code min_count} only by accident.
     */
    private void stream(AgentEmitter emitter, String behavior) {
        emitter.startWork(emitter.newAgentMessage(List.of(new TextPart("streaming started")), null));

        if ("tck-stream-chunked".equals(behavior)) {
            List<String> chunks = List.of("chunk one ", "chunk two ", "chunk three");
            String artifactId = UUID.randomUUID().toString();
            for (int i = 0; i < chunks.size(); i++) {
                // The first chunk goes out with append=false; an update naming an artifact the task
                // has not seen yet has nothing to append to.
                emitter.addArtifact(List.of(new TextPart(chunks.get(i))), artifactId, "chunked", null,
                        i > 0, i == chunks.size() - 1);
            }
        } else {
            emitter.addArtifact(List.of(new TextPart("streamed content")), null, "streamed", null, false, true);
        }

        complete(emitter, TaskState.TASK_STATE_COMPLETED, behavior + " ok");
    }
}
