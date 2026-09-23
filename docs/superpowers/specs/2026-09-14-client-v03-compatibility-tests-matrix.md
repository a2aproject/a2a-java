# v1 Client to v0.3 Compatibility Test Matrix

The original v0.3 suites remain unchanged in `compat-0.3/server-conversion`. The
compatibility suites use the public v1 `Client` API against the standalone
`compat-0.3/reference/{jsonrpc,rest,grpc}` servers. HTTP and JSON-RPC tests
discover the standalone legacy card with `A2A.getAgentCard(url, Set.of("0.3"))`;
gRPC uses the equivalent v1 card fixture because gRPC has no card endpoint.

| Legacy source | Compatibility replacement | Coverage |
|---|---|---|
| `testTaskStoreMethodsSanityTest` | None; server utility plumbing only | Intentionally excluded: task-store sanity is not client behavior |
| `testGetTaskSuccess` | `testGetTaskSuccess` | Included in `AbstractA2AServerCompatibilityTest_v0_3` |
| `testGetTaskNotFound` | `testGetTaskNotFound` | Included |
| `testCancelTaskSuccess` | `testCancelTaskSuccess` | Included |
| `testCancelTaskNotSupported` | `testCancelTaskNotSupported` | Included |
| `testCancelTaskNotFound` | `testCancelTaskNotFound` | Included |
| `testSendMessageNewMessageSuccess` | `testSendMessageNewMessageSuccess` | Included |
| `testRequestScopedBeanAvailableOnAgentExecutorThread` | Same-named v1 test | Included |
| `testSendMessageExistingTaskSuccess` | `testSendMessageExistingTaskSuccess` | Included |
| `testSetPushNotificationSuccess` | `testSetPushNotificationSuccess` | Included |
| `testGetPushNotificationSuccess` | `testGetPushNotificationSuccess` | Included |
| `testError` | `testError` | Included |
| `testGetAgentCard` | `getAgentCard()` resolver path plus parser tests | Replaced: v1 card projection is asserted by `AgentCardMapper_v0_3_Test` and `Compat03AgentCardCompatibilityParserTest` |
| `testSendMessageStreamNewMessageSuccess` | Same-named v1 test | Included |
| `testSendMessageStreamExistingTaskSuccess` | Same-named v1 test | Included |
| `testResubscribeExistingTaskSuccess` | Same-named v1 test | Included |
| `testResubscribeNoExistingTaskError` | Same-named v1 test | Included |
| `testMainQueueReferenceCountingWithMultipleConsumers` | Same-named v1 test | Included |
| `testNonBlockingWithMultipleMessages` | Same-named v1 test | Included |
| `testMainQueueStaysOpenForNonFinalTasks` | Same-named v1 test | Included |
| `testMainQueueClosesForFinalizedTasks` | Same-named v1 test | Included |
| `testListPushNotificationConfigWithConfigId` | `testListPushNotificationConfigsWithConfigId` | Included with v1 result/params |
| `testListPushNotificationConfigWithoutConfigId` | `testListPushNotificationConfigsWithoutConfigId` | Included |
| `testListPushNotificationConfigTaskNotFound` | `testListPushNotificationConfigsTaskNotFound` | Included |
| `testListPushNotificationConfigEmptyList` | `testListPushNotificationConfigsEmptyList` | Included |
| `testDeletePushNotificationConfigWithValidConfigId` | `testDeletePushNotificationConfigWithValidConfigId` | Included |
| `testDeletePushNotificationConfigWithNonExistingConfigId` | Same-named v1 test | Included |
| `testDeletePushNotificationConfigTaskNotFound` | Same-named v1 test | Included |
| `testDeletePushNotificationConfigSetWithoutConfigId` | Same-named v1 test | Included |
| `testMalformedJSONRPCRequest` | None | Intentionally excluded: direct wire coverage remains in frozen legacy suite |
| `testInvalidParamsJSONRPCRequest` | None | Intentionally excluded: direct wire coverage remains in frozen legacy suite |
| `testInvalidJSONRPCRequestMissingJsonrpc` | None | Intentionally excluded |
| `testInvalidJSONRPCRequestMissingMethod` | None | Intentionally excluded |
| `testInvalidJSONRPCRequestInvalidId` | None | Intentionally excluded |
| `testInvalidJSONRPCRequestNonExistentMethod` | None | Intentionally excluded |
| `testNonStreamingMethodWithAcceptHeader` | None | Intentionally excluded: transport wire behavior |
| `testStreamingMethodWithAcceptHeader` | None | Intentionally excluded: transport wire behavior |
| `testStreamingMethodWithoutAcceptHeader` | None | Intentionally excluded: transport wire behavior |
| `testSendStreamingMessage(boolean)` | `testSendMessageStream*` | Replaced by public v1 streaming calls |
| `testInputRequiredWorkflow`, `testAuthRequiredWorkflow` | None | Intentionally excluded: not part of the non-authorization compatibility slice |
| `testAgentToAgentDelegation`, `testAgentToAgentLocalHandling` | None | Intentionally excluded: reference-server-specific workflows |
| `testSendMessageWithHistoryLengthZero`, `testSendStreamingMessageWithHistoryLengthZero` | None | Intentionally excluded: no legacy-client compatibility requirement |
| `testGetTaskRequiresAuthenticationUnauthenticated` | Auth base same-named v1 test | Included using v1 `AuthInterceptor` |
| `testGetTaskWithAuthentication` | Auth base same-named v1 test | Included using v1 `AuthInterceptor` |
| `testGetAgentCardIsPublic` | Auth base public-card assertion | Included through v1 card discovery |
| `testBasicAuthWorksViaHttp` | None | Intentionally excluded: raw HTTP request remains in frozen legacy suite |
| adapter unsupported-operation resolver cases | `testUnsupportedOperationsAreRejectedLocally` plus `Compat03ClientTransportSupportTest.rejectsUnsupportedOperationsBeforeDelegateUse` | Included at public-client and adapter-unit levels |
| task authorization variants | None | Intentionally excluded: compatibility target is the non-task-authorization standalone variants |

Runnable compatibility commands:

```bash
mvn -pl compat-0.3/reference/jsonrpc -am -Dtest='*Compatibility*' -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl compat-0.3/reference/rest -am -Dtest='*Compatibility*' -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl compat-0.3/reference/grpc -am -Dtest='*Compatibility*' -Dsurefire.failIfNoSpecifiedTests=false test
```

The reference test classpaths must contain the v1 client and native transport,
the root server-common test-jar, and the matching compatibility adapter plus
binding adapter. Adapter JAR service descriptors provide parser and versioned
transport discovery; no test resource copies are required. The original v0.3
tests remain present and are run separately without the `*Compatibility*`
selector.
