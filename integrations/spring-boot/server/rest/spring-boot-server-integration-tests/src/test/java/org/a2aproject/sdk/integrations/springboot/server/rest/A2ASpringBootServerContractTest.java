package org.a2aproject.sdk.integrations.springboot.server.rest;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.server.apps.common.AgentToAgentClientFactory;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;
import static org.a2aproject.sdk.spec.TransportProtocol.HTTP_JSON;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
		classes = A2ASpringBootServerContractTest.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class A2ASpringBootServerContractTest extends AbstractA2AServerTest {

	private static final int SERVER_PORT = findAvailablePort();

	@Autowired
	private ApplicationContext applicationContext;

	A2ASpringBootServerContractTest() {
		super(SERVER_PORT);
	}

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> SERVER_PORT);
	}

	@Override
	protected String getTransportProtocol() {
		return TransportProtocol.HTTP_JSON.asString();
	}

	@Override
	protected String getTransportUrl() {
		return "http://localhost:" + SERVER_PORT;
	}

	@Override
	protected void configureTransport(ClientBuilder builder) {
		builder.withTransport(RestTransport.class, new RestTransportConfigBuilder());
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TestSupportController.class)
	static class TestApplication {

		@Bean
		AgentCard agentCard() {
			return testAgentCard();
		}

		@Bean("extendedAgentCard")
		AgentCard extendedAgentCard() {
			return testAgentCard();
		}

		@Bean
		AtomicInteger streamingSubscribedCount() {
			return new AtomicInteger();
		}

		@Bean
		StreamingSubscriptionObserver streamingSubscriptionObserver(AtomicInteger streamingSubscribedCount) {
			return streamingSubscribedCount::incrementAndGet;
		}

		@Bean
		@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS)
		RequestScopedBean requestScopedBean() {
			return new RequestScopedBean();
		}

		@Bean
		AgentExecutor agentExecutor(AgentCard agentCard, RequestScopedBean requestScopedBean) {
			return new TestAgentExecutor(agentCard, requestScopedBean);
		}

		@Bean
		RequestHandler requestHandler(AgentExecutor agentExecutor,
									  TaskStore taskStore,
									  QueueManager queueManager,
									  PushNotificationConfigStore pushNotificationConfigStore,
									  MainEventBusProcessor mainEventBusProcessor,
									  @org.springframework.beans.factory.annotation.Qualifier("a2aInternalExecutor") java.util.concurrent.Executor internalExecutor,
									  @org.springframework.beans.factory.annotation.Qualifier("a2aEventConsumerExecutor") java.util.concurrent.Executor eventConsumerExecutor) {
			return new DefaultRequestHandler(
					agentExecutor,
					taskStore,
					queueManager,
					pushNotificationConfigStore,
					mainEventBusProcessor,
					internalExecutor,
					eventConsumerExecutor);
		}

		private AgentCard testAgentCard() {
			return AgentCard.builder()
					.name("test-card")
					.description("A test agent card")
					.version("1.0")
					.documentationUrl("http://example.com/docs")
					.capabilities(AgentCapabilities.builder()
							.streaming(true)
							.pushNotifications(true)
							.extendedAgentCard(true)
							.build())
					.defaultInputModes(List.of("text"))
					.defaultOutputModes(List.of("text"))
					.skills(List.of())
					.supportedInterfaces(List.of(new AgentInterface(HTTP_JSON.asString(), "http://localhost:" + SERVER_PORT)))
					.build();
		}
	}

	private static int findAvailablePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to allocate a free port for Spring Boot integration tests", e);
		}
	}

	@RestController
	static class TestSupportController {

		private final TaskStore taskStore;
		private final QueueManager queueManager;
		private final PushNotificationConfigStore pushNotificationConfigStore;
		private final AtomicInteger streamingSubscribedCount;

		TestSupportController(TaskStore taskStore,
							  QueueManager queueManager,
							  PushNotificationConfigStore pushNotificationConfigStore,
							  AtomicInteger streamingSubscribedCount) {
			this.taskStore = taskStore;
			this.queueManager = queueManager;
			this.pushNotificationConfigStore = pushNotificationConfigStore;
			this.streamingSubscribedCount = streamingSubscribedCount;
		}

		@PostMapping(value = "/test/task", consumes = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<Void> saveTask(@RequestBody String body) throws Exception {
			taskStore.save(JsonUtil.fromJson(body, Task.class), false);
			return ResponseEntity.ok().build();
		}

		@GetMapping(value = "/test/task/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<String> getTask(@PathVariable String taskId) throws Exception {
			Task task = taskStore.get(taskId);
			if (task == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(JsonUtil.toJson(task));
		}

		@DeleteMapping("/test/task/{taskId}")
		ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
			taskStore.delete(taskId);
			return ResponseEntity.ok().build();
		}

		@PostMapping("/test/queue/ensure/{taskId}")
		ResponseEntity<Void> ensureQueue(@PathVariable String taskId) {
			queueManager.createOrTap(taskId);
			return ResponseEntity.ok().build();
		}

		@PostMapping(value = "/test/queue/enqueueTaskStatusUpdateEvent/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<Void> enqueueStatus(@PathVariable String taskId, @RequestBody String body) throws Exception {
			TaskStatusUpdateEvent event = JsonUtil.fromJson(body, TaskStatusUpdateEvent.class);
			queueManager.get(taskId).enqueueEvent(event);
			return ResponseEntity.ok().build();
		}

		@PostMapping(value = "/test/queue/enqueueTaskArtifactUpdateEvent/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<Void> enqueueArtifact(@PathVariable String taskId, @RequestBody String body) throws Exception {
			TaskArtifactUpdateEvent event = JsonUtil.fromJson(body, TaskArtifactUpdateEvent.class);
			queueManager.get(taskId).enqueueEvent(event);
			return ResponseEntity.ok().build();
		}

		@GetMapping("/test/streamingSubscribedCount")
		ResponseEntity<String> getStreamingSubscribedCount() {
			return ResponseEntity.ok(String.valueOf(streamingSubscribedCount.get()));
		}

		@GetMapping("/test/queue/childCount/{taskId}")
		ResponseEntity<String> getChildQueueCount(@PathVariable String taskId) {
			return ResponseEntity.ok(String.valueOf(queueManager.getActiveChildQueueCount(taskId)));
		}

		@DeleteMapping("/test/task/{taskId}/config/{configId}")
		ResponseEntity<Void> deletePushNotificationConfig(@PathVariable String taskId, @PathVariable String configId) {
			pushNotificationConfigStore.deleteInfo(taskId, configId);
			return ResponseEntity.ok().build();
		}

		@PostMapping(value = "/test/task/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<Void> savePushNotificationConfig(@PathVariable String taskId, @RequestBody String body) throws Exception {
			TaskPushNotificationConfig notificationConfig = JsonUtil.fromJson(body, TaskPushNotificationConfig.class);
			pushNotificationConfigStore.setInfo(TaskPushNotificationConfig.builder(notificationConfig).taskId(taskId).build());
			return ResponseEntity.ok().build();
		}

		@PostMapping("/test/queue/awaitChildCountStable/{taskId}/{expectedCount}/{timeoutMs}")
		ResponseEntity<String> awaitChildCountStable(@PathVariable String taskId,
													 @PathVariable String expectedCount,
													 @PathVariable String timeoutMs) throws Exception {
			int expected = Integer.parseInt(expectedCount);
			long timeout = Long.parseLong(timeoutMs);
			long end = System.currentTimeMillis() + timeout;
			int consecutive = 0;
			while (System.currentTimeMillis() < end) {
				if (queueManager.getActiveChildQueueCount(taskId) == expected) {
					consecutive++;
					if (consecutive >= 3) {
						return ResponseEntity.ok("true");
					}
				} else {
					consecutive = 0;
				}
				Thread.sleep(50);
			}
			return ResponseEntity.ok("false");
		}
	}

	static final class TestAgentExecutor implements AgentExecutor {

		private final AgentCard agentCard;
		private final RequestScopedBean requestScopedBean;

		TestAgentExecutor(AgentCard agentCard, RequestScopedBean requestScopedBean) {
			this.agentCard = agentCard;
			this.requestScopedBean = requestScopedBean;
		}

		@Override
		public void execute(org.a2aproject.sdk.server.agentexecution.RequestContext context,
							org.a2aproject.sdk.server.tasks.AgentEmitter agentEmitter) throws A2AError {
			String taskId = context.getTaskId();
			String input = context.getMessage() != null ? extractTextFromMessage(context.getMessage()) : "";

			if (input.startsWith("request-scoped:")) {
				agentEmitter.startWork();
				agentEmitter.addArtifact(List.of(new TextPart("request-scoped:" + requestScopedBean.getValue())));
				agentEmitter.complete();
				return;
			}
			if (input.startsWith("delegate:") || input.startsWith("a2a-local:")) {
				handleAgentToAgentTest(context, agentEmitter);
				return;
			}
			if (input.startsWith("multi-event:first")) {
				agentEmitter.startWork();
				return;
			}
			if (input.startsWith("multi-event:second")) {
				agentEmitter.addArtifact(List.of(new TextPart("Second message artifact")), "artifact-2", "Second Artifact", null);
				agentEmitter.complete();
				return;
			}
			if (input.startsWith("input-required:")) {
				String payload = input.substring("input-required:".length());
				if ("User input".equals(payload)) {
					agentEmitter.complete();
					return;
				}
				agentEmitter.requiresInput(agentEmitter.newAgentMessage(
						List.of(new TextPart("Please provide additional information")),
						context.getMessage().metadata()));
				return;
			}
			if (input.startsWith("auth-required:")) {
				agentEmitter.requiresAuth(agentEmitter.newAgentMessage(
						List.of(new TextPart("Please authenticate with OAuth provider")),
						context.getMessage().metadata()));
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new InternalError("Auth simulation interrupted: " + e.getMessage());
				}
				agentEmitter.complete();
				return;
			}
			if ("task-not-supported-123".equals(taskId)) {
				throw new UnsupportedOperationError();
			}
			if (input.startsWith("#a2a-delegated#")) {
				String actualContent = input.substring("#a2a-delegated#".length());
				agentEmitter.startWork();
				agentEmitter.addArtifact(List.of(new TextPart("Handled locally: " + actualContent)));
				agentEmitter.complete();
				return;
			}
			if (context.getMessage() != null) {
				agentEmitter.sendMessage(context.getMessage());
			} else {
				agentEmitter.addTask(context.getTask());
			}
		}

		@Override
		public void cancel(org.a2aproject.sdk.server.agentexecution.RequestContext context,
						   org.a2aproject.sdk.server.tasks.AgentEmitter agentEmitter) throws A2AError {
			if ("cancel-task-123".equals(context.getTask().id())) {
				agentEmitter.cancel();
			} else if ("cancel-task-not-supported-123".equals(context.getTask().id())) {
				throw new UnsupportedOperationError();
			}
		}

		private void handleAgentToAgentTest(org.a2aproject.sdk.server.agentexecution.RequestContext context,
											org.a2aproject.sdk.server.tasks.AgentEmitter agentEmitter) throws A2AError {
			try {
				ServerCallContext callContext = context.getCallContext();
				if (callContext == null) {
					agentEmitter.fail(new InternalError("No call context available for agent-to-agent test"));
					return;
				}

				TransportProtocol transportProtocol = (TransportProtocol) callContext.getState().get(TRANSPORT_KEY);
				if (transportProtocol == null) {
					agentEmitter.fail(new InternalError("Transport type not set in call context"));
					return;
				}

				String userInput = context.getUserInput("\n");
				if (userInput == null || userInput.isEmpty()) {
					agentEmitter.fail(new InternalError("No user input received"));
					return;
				}

				if (userInput.startsWith("delegate:")) {
					handleDelegation(userInput, transportProtocol, agentEmitter);
				} else {
					handleLocally(userInput.substring("a2a-local:".length()), agentEmitter);
				}
			} catch (Exception e) {
				e.printStackTrace();
				agentEmitter.fail(new InternalError("Agent-to-agent test failed: " + e.getMessage()));
			}
		}

		private void handleDelegation(String userInput, TransportProtocol transportProtocol,
									  org.a2aproject.sdk.server.tasks.AgentEmitter agentEmitter) {
			String delegatedContent = userInput.substring("delegate:".length()).trim();

			try (org.a2aproject.sdk.client.Client client = AgentToAgentClientFactory.createClient(agentCard, transportProtocol)) {
				agentEmitter.startWork();
				java.util.concurrent.atomic.AtomicReference<Task> taskRef = new java.util.concurrent.atomic.AtomicReference<>();
				Message delegatedMessage = Message.builder()
						.role(Message.Role.ROLE_USER)
						.parts(new TextPart("#a2a-delegated#" + delegatedContent))
						.build();
				client.sendMessage(delegatedMessage, List.of((event, card) -> {
					if (event instanceof org.a2aproject.sdk.client.TaskEvent te) {
						taskRef.set(te.getTask());
					} else if (event instanceof org.a2aproject.sdk.client.TaskUpdateEvent tue) {
						taskRef.set(tue.getTask());
					}
				}), null);
				Task delegatedResult = taskRef.get();
				assertNotNull(delegatedResult);
				if (!delegatedResult.status().state().isFinal()) {
					String diagnostic = String.format(
							"RACE CONDITION DETECTED: Blocking call returned non-final task! State: %s, TaskId: %s, Artifacts: %d.",
							delegatedResult.status().state(),
							delegatedResult.id(),
							delegatedResult.artifacts() != null ? delegatedResult.artifacts().size() : 0);
					System.err.println(diagnostic);
					agentEmitter.fail(new InternalError(diagnostic));
					return;
				}
				if (delegatedResult.artifacts() != null) {
					for (Artifact artifact : delegatedResult.artifacts()) {
						agentEmitter.addArtifact(artifact.parts());
					}
				}
				agentEmitter.complete();
			} catch (A2AClientException e) {
				agentEmitter.fail(new InternalError("Failed to create client: " + e.getMessage()));
			}
		}

		private void handleLocally(String userInput,
								   org.a2aproject.sdk.server.tasks.AgentEmitter agentEmitter) {
			try {
				agentEmitter.startWork();
				agentEmitter.addArtifact(List.of(new TextPart("Handled locally: " + userInput)));
				agentEmitter.complete();
			} catch (Exception e) {
				agentEmitter.fail(new InternalError("Local handling failed: " + e.getMessage()));
			}
		}

		private String extractTextFromMessage(Message message) {
			StringBuilder textBuilder = new StringBuilder();
			if (message.parts() != null) {
				for (var part : message.parts()) {
					if (part instanceof TextPart textPart) {
						textBuilder.append(textPart.text());
					}
				}
			}
			return textBuilder.toString();
		}
	}

	static class RequestScopedBean {

		String getValue() {
			return "request-scoped-value";
		}
	}
}
