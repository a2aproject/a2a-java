package io.a2a.grpc.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.a2a.grpc.StreamResponse;
import io.a2a.spec.APIKeySecurityScheme;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.Artifact;
import io.a2a.spec.AuthorizationCodeOAuthFlow;
import io.a2a.spec.ClientCredentialsOAuthFlow;
import io.a2a.spec.DataPart;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.FileContent;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.ImplicitOAuthFlow;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OAuthFlows;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.Part;
import io.a2a.spec.PasswordOAuthFlow;
import io.a2a.spec.PushNotificationAuthenticationInfo;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SecurityScheme;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

/**
 * Utility class to convert between GRPC and Spec objects.
 */
public class ProtoUtils {
    public static class ToProto {

        public static io.a2a.grpc.AgentCard agentCard(AgentCard agentCard) {
            io.a2a.grpc.AgentCard.Builder builder = io.a2a.grpc.AgentCard.newBuilder();
            if (agentCard.protocolVersion() != null) {
                builder.setProtocolVersion(agentCard.protocolVersion());
            }
            if (agentCard.name() != null) {
                builder.setName(agentCard.name());
            }
            if (agentCard.description() != null) {
                builder.setDescription(agentCard.description());
            }
            if (agentCard.url() != null) {
                builder.setUrl(agentCard.url());
            }
            if (agentCard.preferredTransport() != null) {
                builder.setPreferredTransport(agentCard.preferredTransport());
            }
            if (agentCard.additionalInterfaces() != null) {
                builder.addAllAdditionalInterfaces(agentCard.additionalInterfaces().stream().map(item -> agentInterface(item)).collect(Collectors.toList()));
            }
            if (agentCard.provider() != null) {
                builder.setProvider(agentProvider(agentCard.provider()));
            }
            if (agentCard.version() != null) {
                builder.setVersion(agentCard.version());
            }
            if (agentCard.documentationUrl() != null) {
                builder.setDocumentationUrl(agentCard.documentationUrl());
            }
            if (agentCard.capabilities() != null) {
                builder.setCapabilities(agentCapabilities(agentCard.capabilities()));
            }
            if (agentCard.securitySchemes() != null) {
                builder.putAllSecuritySchemes(
                        agentCard.securitySchemes().entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> securityScheme(e.getValue())))
                );
            }
            if (agentCard.security() != null) {
                builder.addAllSecurity(agentCard.security().stream().map(s -> {
                    io.a2a.grpc.Security.Builder securityBuilder = io.a2a.grpc.Security.newBuilder();
                    s.forEach((key, value) -> {
                        io.a2a.grpc.StringList.Builder stringListBuilder = io.a2a.grpc.StringList.newBuilder();
                        stringListBuilder.addAllList(value);
                        securityBuilder.putSchemes(key, stringListBuilder.build());
                    });
                    return securityBuilder.build();
                }).collect(Collectors.toList()));
            }
            if (agentCard.defaultInputModes() != null) {
                builder.addAllDefaultInputModes(agentCard.defaultInputModes());
            }
            if (agentCard.defaultOutputModes() != null) {
                builder.addAllDefaultOutputModes(agentCard.defaultOutputModes());
            }
            if (agentCard.skills() != null) {
                builder.addAllSkills(agentCard.skills().stream().map(ToProto::agentSkill).collect(Collectors.toList()));
            }
            builder.setSupportsAuthenticatedExtendedCard(agentCard.supportsAuthenticatedExtendedCard());
            return builder.build();
        }

        public static io.a2a.grpc.Task task(Task task) {
            io.a2a.grpc.Task.Builder builder = io.a2a.grpc.Task.newBuilder();
            builder.setId(task.getId());
            builder.setContextId(task.getContextId());
            builder.setStatus(taskStatus(task.getStatus()));
            if (task.getArtifacts() != null) {
                builder.addAllArtifacts(task.getArtifacts().stream().map(ToProto::artifact).collect(Collectors.toList()));
            }
            if (task.getHistory() != null) {
                builder.addAllHistory(task.getHistory().stream().map(ToProto::message).collect(Collectors.toList()));
            }
            builder.setMetadata(struct(task.getMetadata()));
            return builder.build();
        }

        public static io.a2a.grpc.Message message(Message message) {
            io.a2a.grpc.Message.Builder builder = io.a2a.grpc.Message.newBuilder();
            builder.setMessageId(message.getMessageId());
            builder.setContextId(message.getContextId());
            builder.setTaskId(message.getTaskId());
            builder.setRole(role(message.getRole()));
            if (message.getParts() != null) {
                builder.addAllContent(message.getParts().stream().map(ToProto::part).collect(Collectors.toList()));
            }
            builder.setMetadata(struct(message.getMetadata()));
            return builder.build();
        }

        public static io.a2a.grpc.TaskPushNotificationConfig taskPushNotificationConfig(TaskPushNotificationConfig config) {
            io.a2a.grpc.TaskPushNotificationConfig.Builder builder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
            builder.setName("tasks/" + config.taskId() + "/pushNotificationConfigs/" + config.pushNotificationConfig().id());
            builder.setPushNotificationConfig(pushNotificationConfig(config.pushNotificationConfig()));
            return builder.build();
        }

        private static io.a2a.grpc.PushNotificationConfig pushNotificationConfig(PushNotificationConfig config) {
            io.a2a.grpc.PushNotificationConfig.Builder builder = io.a2a.grpc.PushNotificationConfig.newBuilder();
            if (config.url() != null) {
                builder.setUrl(config.url());
            }
            if (config.token() != null) {
                builder.setToken(config.token());
            }
            if (config.authentication() != null) {
                builder.setAuthentication(authenticationInfo(config.authentication()));
            }
            if (config.id() !=  null) {
                builder.setId(config.id());
            }
            return builder.build();
        }

        public static io.a2a.grpc.TaskArtifactUpdateEvent taskArtifactUpdateEvent(TaskArtifactUpdateEvent event) {
            io.a2a.grpc.TaskArtifactUpdateEvent.Builder builder = io.a2a.grpc.TaskArtifactUpdateEvent.newBuilder();
            builder.setTaskId(event.getTaskId());
            builder.setContextId(event.getContextId());
            builder.setArtifact(artifact(event.getArtifact()));
            builder.setAppend(event.isAppend() == null ? false : event.isAppend());
            builder.setLastChunk(event.isLastChunk() == null ? false : event.isLastChunk());
            if (event.getMetadata() != null) {
                builder.setMetadata(struct(event.getMetadata()));
            }
            return builder.build();
        }

        public static io.a2a.grpc.TaskStatusUpdateEvent taskStatusUpdateEvent(TaskStatusUpdateEvent event) {
            io.a2a.grpc.TaskStatusUpdateEvent.Builder builder = io.a2a.grpc.TaskStatusUpdateEvent.newBuilder();
            builder.setTaskId(event.getTaskId());
            builder.setContextId(event.getContextId());
            builder.setStatus(taskStatus(event.getStatus()));
            builder.setFinal(event.isFinal());
            if (event.getMetadata() != null) {
                builder.setMetadata(struct(event.getMetadata()));
            }
            return builder.build();
        }

        private static io.a2a.grpc.Artifact artifact(Artifact artifact) {
            io.a2a.grpc.Artifact.Builder builder = io.a2a.grpc.Artifact.newBuilder();
            if (artifact.artifactId() != null) {
                builder.setArtifactId(artifact.artifactId());
            }
            if (artifact.name() != null) {
                builder.setName(artifact.name());
            }
            if (artifact.description() != null) {
                builder.setDescription(artifact.description());
            }
            if (artifact.parts() != null) {
                builder.addAllParts(artifact.parts().stream().map(ToProto::part).collect(Collectors.toList()));
            }
            if (artifact.metadata() != null) {
                builder.setMetadata(struct(artifact.metadata()));
            }
            return builder.build();
        }

        private static io.a2a.grpc.Part part(Part<?> part) {
            io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();
            if (part instanceof TextPart) {
                builder.setText(((TextPart) part).getText());
            } else if (part instanceof FilePart) {
                builder.setFile(filePart((FilePart) part));
            } else if (part instanceof DataPart) {
                builder.setData(dataPart((DataPart) part));
            }
            return builder.build();
        }

        private static io.a2a.grpc.FilePart filePart(FilePart filePart) {
            io.a2a.grpc.FilePart.Builder builder = io.a2a.grpc.FilePart.newBuilder();
            FileContent fileContent = filePart.getFile();
            if (fileContent instanceof FileWithBytes) {
                builder.setFileWithBytes(ByteString.copyFrom(((FileWithBytes) fileContent).bytes(), StandardCharsets.UTF_8));
            } else if (fileContent instanceof FileWithUri) {
                builder.setFileWithUri(((FileWithUri) fileContent).uri());
            }
            return builder.build();
        }

        private static io.a2a.grpc.DataPart dataPart(DataPart dataPart) {
            io.a2a.grpc.DataPart.Builder builder = io.a2a.grpc.DataPart.newBuilder();
            if (dataPart.getData() != null) {
                builder.setData(struct(dataPart.getData()));
            }
            return builder.build();
        }

        private static io.a2a.grpc.Role role(Message.Role role) {
            if (role == null) {
                return io.a2a.grpc.Role.ROLE_UNSPECIFIED;
            }
            return switch (role) {
                case USER -> io.a2a.grpc.Role.ROLE_USER;
                case AGENT -> io.a2a.grpc.Role.ROLE_AGENT;
            };
        }

        private static io.a2a.grpc.TaskStatus taskStatus(TaskStatus taskStatus) {
            io.a2a.grpc.TaskStatus.Builder builder = io.a2a.grpc.TaskStatus.newBuilder();
            if (taskStatus.state() != null) {
                builder.setState(taskState(taskStatus.state()));
            }
            if (taskStatus.message() != null) {
                builder.setUpdate(message(taskStatus.message()));
            }
            if (taskStatus.timestamp() != null) {
                Instant instant = taskStatus.timestamp().toInstant(ZoneOffset.UTC);
                builder.setTimestamp(com.google.protobuf.Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build());
            }
            return builder.build();
        }

        private static io.a2a.grpc.TaskState taskState(TaskState taskState) {
            if (taskState == null) {
                return io.a2a.grpc.TaskState.TASK_STATE_UNSPECIFIED;
            }
            return switch (taskState) {
                case SUBMITTED -> io.a2a.grpc.TaskState.TASK_STATE_SUBMITTED;
                case WORKING -> io.a2a.grpc.TaskState.TASK_STATE_WORKING;
                case INPUT_REQUIRED -> io.a2a.grpc.TaskState.TASK_STATE_INPUT_REQUIRED;
                case AUTH_REQUIRED -> io.a2a.grpc.TaskState.TASK_STATE_AUTH_REQUIRED;
                case COMPLETED -> io.a2a.grpc.TaskState.TASK_STATE_COMPLETED;
                case CANCELED -> io.a2a.grpc.TaskState.TASK_STATE_CANCELLED;
                case FAILED -> io.a2a.grpc.TaskState.TASK_STATE_FAILED;
                case REJECTED -> io.a2a.grpc.TaskState.TASK_STATE_REJECTED;
                default -> io.a2a.grpc.TaskState.TASK_STATE_UNSPECIFIED;
            };
        }

        private static io.a2a.grpc.AuthenticationInfo authenticationInfo(PushNotificationAuthenticationInfo pushNotificationAuthenticationInfo) {
            io.a2a.grpc.AuthenticationInfo.Builder builder = io.a2a.grpc.AuthenticationInfo.newBuilder();
            if (pushNotificationAuthenticationInfo.schemes() != null) {
                builder.addAllSchemes(pushNotificationAuthenticationInfo.schemes());
            }
            if (pushNotificationAuthenticationInfo.credentials() != null) {
                builder.setCredentials(pushNotificationAuthenticationInfo.credentials());
            }
            return builder.build();
        }

        public static io.a2a.grpc.SendMessageConfiguration messageSendConfiguration(MessageSendConfiguration messageSendConfiguration) {
            io.a2a.grpc.SendMessageConfiguration.Builder builder = io.a2a.grpc.SendMessageConfiguration.newBuilder();
            builder.addAllAcceptedOutputModes(messageSendConfiguration.acceptedOutputModes());
            if (messageSendConfiguration.historyLength() != null) {
                builder.setHistoryLength(messageSendConfiguration.historyLength());
            }
            if (messageSendConfiguration.pushNotification() != null) {
                builder.setPushNotification(pushNotificationConfig(messageSendConfiguration.pushNotification()));
            }
            builder.setBlocking(messageSendConfiguration.blocking());
            return builder.build();
        }

        private static io.a2a.grpc.AgentProvider agentProvider(AgentProvider agentProvider) {
            io.a2a.grpc.AgentProvider.Builder builder = io.a2a.grpc.AgentProvider.newBuilder();
            builder.setOrganization(agentProvider.organization());
            builder.setUrl(agentProvider.url());
            return builder.build();
        }

        private static io.a2a.grpc.AgentCapabilities agentCapabilities(AgentCapabilities agentCapabilities) {
            io.a2a.grpc.AgentCapabilities.Builder builder = io.a2a.grpc.AgentCapabilities.newBuilder();
            builder.setStreaming(agentCapabilities.streaming());
            builder.setPushNotifications(agentCapabilities.pushNotifications());
            if (agentCapabilities.extensions() != null) {
                builder.addAllExtensions(agentCapabilities.extensions().stream().map(ToProto::agentExtension).collect(Collectors.toList()));
            }
            return builder.build();
        }

        private static io.a2a.grpc.AgentExtension agentExtension(AgentExtension agentExtension) {
            io.a2a.grpc.AgentExtension.Builder builder = io.a2a.grpc.AgentExtension.newBuilder();
            if (agentExtension.description() != null) {
                builder.setDescription(agentExtension.description());
            }
            if (agentExtension.params() != null) {
                builder.setParams(struct(agentExtension.params()));
            }
            builder.setRequired(agentExtension.required());
            if (agentExtension.uri() != null) {
                builder.setUri(agentExtension.uri());
            }
            return builder.build();
        }

        private static io.a2a.grpc.AgentSkill agentSkill(AgentSkill agentSkill) {
            io.a2a.grpc.AgentSkill.Builder builder = io.a2a.grpc.AgentSkill.newBuilder();
            if (agentSkill.id() != null) {
                builder.setId(agentSkill.id());
            }
            if (agentSkill.name() != null) {
                builder.setName(agentSkill.name());
            }
            if (agentSkill.description() != null) {
                builder.setDescription(agentSkill.description());
            }
            if (agentSkill.tags() != null) {
                builder.addAllTags(agentSkill.tags());
            }
            if (agentSkill.examples() != null) {
                builder.addAllExamples(agentSkill.examples());
            }
            if (agentSkill.inputModes() != null) {
                builder.addAllInputModes(agentSkill.inputModes());
            }
            if (agentSkill.outputModes() != null) {
                builder.addAllOutputModes(agentSkill.outputModes());
            }
            return builder.build();
        }

        private static io.a2a.grpc.SecurityScheme securityScheme(SecurityScheme securityScheme) {
            io.a2a.grpc.SecurityScheme.Builder builder = io.a2a.grpc.SecurityScheme.newBuilder();
            if (securityScheme instanceof APIKeySecurityScheme) {
                builder.setApiKeySecurityScheme(apiKeySecurityScheme((APIKeySecurityScheme) securityScheme));
            } else if (securityScheme instanceof HTTPAuthSecurityScheme) {
                builder.setHttpAuthSecurityScheme(httpAuthSecurityScheme((HTTPAuthSecurityScheme) securityScheme));
            } else if (securityScheme instanceof OAuth2SecurityScheme) {
                builder.setOauth2SecurityScheme(oauthSecurityScheme((OAuth2SecurityScheme) securityScheme));
            } else if (securityScheme instanceof OpenIdConnectSecurityScheme) {
                builder.setOpenIdConnectSecurityScheme(openIdConnectSecurityScheme((OpenIdConnectSecurityScheme) securityScheme));
            }
            return builder.build();
        }

        private static io.a2a.grpc.APIKeySecurityScheme apiKeySecurityScheme(APIKeySecurityScheme apiKeySecurityScheme) {
            io.a2a.grpc.APIKeySecurityScheme.Builder builder = io.a2a.grpc.APIKeySecurityScheme.newBuilder();
            if (apiKeySecurityScheme.getDescription() != null) {
                builder.setDescription(apiKeySecurityScheme.getDescription());
            }
            if (apiKeySecurityScheme.getIn() != null) {
                builder.setLocation(apiKeySecurityScheme.getIn());
            }
            if (apiKeySecurityScheme.getName() != null) {
                builder.setName(apiKeySecurityScheme.getName());
            }
            return builder.build();
        }

        private static io.a2a.grpc.HTTPAuthSecurityScheme httpAuthSecurityScheme(HTTPAuthSecurityScheme httpAuthSecurityScheme) {
            io.a2a.grpc.HTTPAuthSecurityScheme.Builder builder = io.a2a.grpc.HTTPAuthSecurityScheme.newBuilder();
            if (httpAuthSecurityScheme.getBearerFormat() != null) {
                builder.setBearerFormat(httpAuthSecurityScheme.getBearerFormat());
            }
            if (httpAuthSecurityScheme.getDescription() != null) {
                builder.setDescription(httpAuthSecurityScheme.getDescription());
            }
            if (httpAuthSecurityScheme.getScheme() != null) {
                builder.setScheme(httpAuthSecurityScheme.getScheme());
            }
            return builder.build();
        }

        private static io.a2a.grpc.OAuth2SecurityScheme oauthSecurityScheme(OAuth2SecurityScheme oauth2SecurityScheme) {
            io.a2a.grpc.OAuth2SecurityScheme.Builder builder = io.a2a.grpc.OAuth2SecurityScheme.newBuilder();
            if (oauth2SecurityScheme.getDescription() != null) {
                builder.setDescription(oauth2SecurityScheme.getDescription());
            }
            if (oauth2SecurityScheme.getFlows() != null) {
                builder.setFlows(oauthFlows(oauth2SecurityScheme.getFlows()));
            }
            return builder.build();
        }

        private static io.a2a.grpc.OAuthFlows oauthFlows(OAuthFlows oAuthFlows) {
            io.a2a.grpc.OAuthFlows.Builder builder = io.a2a.grpc.OAuthFlows.newBuilder();
            if (oAuthFlows.authorizationCode() != null) {
                builder.setAuthorizationCode(authorizationCodeOAuthFlow(oAuthFlows.authorizationCode()));
            }
            if (oAuthFlows.clientCredentials() != null) {
                builder.setClientCredentials(clientCredentialsOAuthFlow(oAuthFlows.clientCredentials()));
            }
            if (oAuthFlows.implicit() != null) {
                builder.setImplicit(implicitOAuthFlow(oAuthFlows.implicit()));
            }
            if (oAuthFlows.password() != null) {
                builder.setPassword(passwordOAuthFlow(oAuthFlows.password()));
            }
            return builder.build();
        }

        private static io.a2a.grpc.AuthorizationCodeOAuthFlow authorizationCodeOAuthFlow(AuthorizationCodeOAuthFlow authorizationCodeOAuthFlow) {
            io.a2a.grpc.AuthorizationCodeOAuthFlow.Builder builder = io.a2a.grpc.AuthorizationCodeOAuthFlow.newBuilder();
            if (authorizationCodeOAuthFlow.authorizationUrl() != null) {
                builder.setAuthorizationUrl(authorizationCodeOAuthFlow.authorizationUrl());
            }
            if (authorizationCodeOAuthFlow.refreshUrl() != null) {
                builder.setRefreshUrl(authorizationCodeOAuthFlow.refreshUrl());
            }
            if (authorizationCodeOAuthFlow.scopes() != null) {
                builder.putAllScopes(authorizationCodeOAuthFlow.scopes());
            }
            if (authorizationCodeOAuthFlow.tokenUrl() != null) {
                builder.setTokenUrl(authorizationCodeOAuthFlow.tokenUrl());
            }
            return builder.build();
        }

        private static io.a2a.grpc.ClientCredentialsOAuthFlow clientCredentialsOAuthFlow(ClientCredentialsOAuthFlow clientCredentialsOAuthFlow) {
            io.a2a.grpc.ClientCredentialsOAuthFlow.Builder builder = io.a2a.grpc.ClientCredentialsOAuthFlow.newBuilder();
            if (clientCredentialsOAuthFlow.refreshUrl() != null) {
                builder.setRefreshUrl(clientCredentialsOAuthFlow.refreshUrl());
            }
            if (clientCredentialsOAuthFlow.scopes() != null) {
                builder.putAllScopes(clientCredentialsOAuthFlow.scopes());
            }
            if (clientCredentialsOAuthFlow.tokenUrl() != null) {
                builder.setTokenUrl(clientCredentialsOAuthFlow.tokenUrl());
            }
            return builder.build();
        }

        private static io.a2a.grpc.ImplicitOAuthFlow implicitOAuthFlow(ImplicitOAuthFlow implicitOAuthFlow) {
            io.a2a.grpc.ImplicitOAuthFlow.Builder builder = io.a2a.grpc.ImplicitOAuthFlow.newBuilder();
            if (implicitOAuthFlow.authorizationUrl() != null) {
                builder.setAuthorizationUrl(implicitOAuthFlow.authorizationUrl());
            }
            if (implicitOAuthFlow.refreshUrl() != null) {
                builder.setRefreshUrl(implicitOAuthFlow.refreshUrl());
            }
            if (implicitOAuthFlow.scopes() != null) {
                builder.putAllScopes(implicitOAuthFlow.scopes());
            }
            return builder.build();
        }

        private static io.a2a.grpc.PasswordOAuthFlow passwordOAuthFlow(PasswordOAuthFlow passwordOAuthFlow) {
            io.a2a.grpc.PasswordOAuthFlow.Builder builder = io.a2a.grpc.PasswordOAuthFlow.newBuilder();
            if (passwordOAuthFlow.refreshUrl() != null) {
                builder.setRefreshUrl(passwordOAuthFlow.refreshUrl());
            }
            if (passwordOAuthFlow.scopes() != null) {
                builder.putAllScopes(passwordOAuthFlow.scopes());
            }
            if (passwordOAuthFlow.tokenUrl() != null) {
                builder.setTokenUrl(passwordOAuthFlow.tokenUrl());
            }
            return builder.build();
        }

        private static io.a2a.grpc.OpenIdConnectSecurityScheme openIdConnectSecurityScheme(OpenIdConnectSecurityScheme openIdConnectSecurityScheme) {
            io.a2a.grpc.OpenIdConnectSecurityScheme.Builder builder = io.a2a.grpc.OpenIdConnectSecurityScheme.newBuilder();
            if (openIdConnectSecurityScheme.getDescription() != null) {
                builder.setDescription(openIdConnectSecurityScheme.getDescription());
            }
            if (openIdConnectSecurityScheme.getOpenIdConnectUrl() != null) {
                builder.setOpenIdConnectUrl(openIdConnectSecurityScheme.getOpenIdConnectUrl());
            }
            return builder.build();
        }

        private static io.a2a.grpc.AgentInterface agentInterface(AgentInterface agentInterface) {
            io.a2a.grpc.AgentInterface.Builder builder = io.a2a.grpc.AgentInterface.newBuilder();
            if (agentInterface.transport() != null) {
                builder.setTransport(agentInterface.transport());
            }
            if (agentInterface.url() != null) {
                builder.setUrl(agentInterface.url());
            }
            return builder.build();
        }

        public static Struct struct(Map<String, Object> map) {
            Struct.Builder structBuilder = Struct.newBuilder();
            if (map != null) {
                map.forEach((k, v) -> structBuilder.putFields(k, value(v)));
            }
            return structBuilder.build();
        }

        private static Value value(Object value) {
            Value.Builder valueBuilder = Value.newBuilder();
            if (value instanceof String) {
                valueBuilder.setStringValue((String) value);
            } else if (value instanceof Number) {
                valueBuilder.setNumberValue(((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                valueBuilder.setBoolValue((Boolean) value);
            } else if (value instanceof Map) {
                valueBuilder.setStructValue(struct((Map<String, Object>) value));
            } else if (value instanceof List) {
                valueBuilder.setListValue(listValue((List<Object>) value));
            }
            return valueBuilder.build();
        }

        private static com.google.protobuf.ListValue listValue(List<Object> list) {
            com.google.protobuf.ListValue.Builder listValueBuilder = com.google.protobuf.ListValue.newBuilder();
            if (list != null) {
                list.forEach(o -> listValueBuilder.addValues(value(o)));
            }
            return listValueBuilder.build();
        }

        public static StreamResponse streamResponse(StreamingEventKind streamingEventKind) {
            if (streamingEventKind instanceof TaskStatusUpdateEvent) {
                return StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent((TaskStatusUpdateEvent) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof TaskArtifactUpdateEvent) {
                return StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent((TaskArtifactUpdateEvent) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof Message) {
                return StreamResponse.newBuilder()
                        .setMsg(message((Message) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof Task) {
                return StreamResponse.newBuilder()
                        .setTask(task((Task) streamingEventKind))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + streamingEventKind);
            }
        }

        public static io.a2a.grpc.SendMessageResponse taskOrMessage(EventKind eventKind) {
            if (eventKind instanceof Task) {
                return io.a2a.grpc.SendMessageResponse.newBuilder()
                        .setTask(task((Task) eventKind))
                        .build();
            } else if (eventKind instanceof Message) {
                return io.a2a.grpc.SendMessageResponse.newBuilder()
                        .setMsg(message((Message) eventKind))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            }
        }


    }

    public static class FromProto {

        public static TaskQueryParams taskQueryParams(io.a2a.grpc.GetTaskRequest request) {
            String name = request.getName();
            String id = name.substring(name.lastIndexOf('/') + 1);
            return new TaskQueryParams(id, request.getHistoryLength());
        }

        public static TaskIdParams taskIdParams(io.a2a.grpc.CancelTaskRequest request) {
            String name = request.getName();
            String id = name.substring(name.lastIndexOf('/') + 1);
            return new TaskIdParams(id);
        }

        public static MessageSendParams messageSendParams(io.a2a.grpc.SendMessageRequest request) {
            MessageSendParams.Builder builder = new MessageSendParams.Builder();
            builder.message(message(request.getRequest()));
            if (request.hasConfiguration()) {
                builder.configuration(messageSendConfiguration(request.getConfiguration()));
            }
            if (request.hasMetadata()) {
                builder.metadata(struct(request.getMetadata()));
            }
            return builder.build();
        }

        public static TaskPushNotificationConfig taskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request) {
            return taskPushNotificationConfig(request.getConfig());
        }

        public static TaskPushNotificationConfig taskPushNotificationConfig(io.a2a.grpc.TaskPushNotificationConfig config) {
            String name = config.getName(); // "tasks/{id}/pushNotificationConfigs/{push_id}"
            String[] parts = name.split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid name format for TaskPushNotificationConfig: " + name);
            }
            String taskId = parts[1];
            String configId = parts[3];
            PushNotificationConfig pnc = pushNotification(config.getPushNotificationConfig(), configId);
            return new TaskPushNotificationConfig(taskId, pnc);
        }

        public static GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams(io.a2a.grpc.GetTaskPushNotificationConfigRequest request) {
            String name = request.getName(); // "tasks/{id}/pushNotificationConfigs/{push_id}"
            String[] parts = name.split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid name format for GetTaskPushNotificationConfigRequest: " + name);
            }
            String taskId = parts[1];
            String configId = parts[3];
            return new GetTaskPushNotificationConfigParams(taskId, configId);
        }

        public static TaskIdParams taskIdParams(io.a2a.grpc.TaskSubscriptionRequest request) {
            String name = request.getName();
            String id = name.substring(name.lastIndexOf('/') + 1);
            return new TaskIdParams(id);
        }

        public static ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams(io.a2a.grpc.ListTaskPushNotificationConfigRequest request) {
            String parent = request.getParent();
            String id = parent.substring(parent.lastIndexOf('/') + 1);
            return new ListTaskPushNotificationConfigParams(id);
        }

        public static DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request) {
            String name = request.getName(); // "tasks/{id}/pushNotificationConfigs/{push_id}"
            String[] parts = name.split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid name format for DeleteTaskPushNotificationConfigRequest: " + name);
            }
            String taskId = parts[1];
            String configId = parts[3];
            return new DeleteTaskPushNotificationConfigParams(taskId, configId);
        }

        private static AgentCapabilities agentCapabilities(io.a2a.grpc.AgentCapabilities agentCapabilities) {
            return new AgentCapabilities(agentCapabilities.getStreaming(), agentCapabilities.getPushNotifications(), false,
                    agentCapabilities.getExtensionsList().stream().map(item -> agentExtension(item)).collect(Collectors.toList())
            );
        }

        private static AgentExtension agentExtension(io.a2a.grpc.AgentExtension agentExtension) {
            return new AgentExtension(
                    agentExtension.getDescription(),
                    struct(agentExtension.getParams()),
                    agentExtension.getRequired(),
                    agentExtension.getUri()
            );
        }

        private static MessageSendConfiguration messageSendConfiguration(io.a2a.grpc.SendMessageConfiguration sendMessageConfiguration) {
            return new MessageSendConfiguration(
                    new ArrayList<>(sendMessageConfiguration.getAcceptedOutputModesList()),
                    sendMessageConfiguration.getHistoryLength(),
                    pushNotification(sendMessageConfiguration.getPushNotification()),
                    sendMessageConfiguration.getBlocking()
            );
        }

        private static PushNotificationConfig pushNotification(io.a2a.grpc.PushNotificationConfig pushNotification, String configId) {
            return new PushNotificationConfig(
                    pushNotification.getUrl(),
                    pushNotification.getToken(),
                    authenticationInfo(pushNotification.getAuthentication()),
                    pushNotification.getId().isEmpty() ? configId : pushNotification.getId()
            );
        }

        private static PushNotificationConfig pushNotification(io.a2a.grpc.PushNotificationConfig pushNotification) {
            return pushNotification(pushNotification, pushNotification.getId());
        }

        private static PushNotificationAuthenticationInfo authenticationInfo(io.a2a.grpc.AuthenticationInfo authenticationInfo) {
            return new PushNotificationAuthenticationInfo(
                    new ArrayList<>(authenticationInfo.getSchemesList()),
                    authenticationInfo.getCredentials()
            );
        }

        public static Task task(io.a2a.grpc.Task task) {
            return new Task(
                    task.getId(),
                    task.getContextId(),
                    taskStatus(task.getStatus()),
                    task.getArtifactsList().stream().map(item -> artifact(item)).collect(Collectors.toList()),
                    task.getHistoryList().stream().map(item -> message(item)).collect(Collectors.toList()),
                    struct(task.getMetadata())
            );
        }

        public static Message message(io.a2a.grpc.Message message) {
            return new Message(
                    role(message.getRole()),
                    message.getContentList().stream().map(item -> part(item)).collect(Collectors.toList()),
                    message.getMessageId(),
                    message.getContextId(),
                    message.getTaskId(),
                    null, // referenceTaskIds is not in grpc message
                    struct(message.getMetadata())
            );
        }

        public static TaskStatusUpdateEvent taskStatusUpdateEvent(io.a2a.grpc.TaskStatusUpdateEvent taskStatusUpdateEvent) {
            return new TaskStatusUpdateEvent.Builder()
                    .taskId(taskStatusUpdateEvent.getTaskId())
                    .status(taskStatus(taskStatusUpdateEvent.getStatus()))
                    .contextId(taskStatusUpdateEvent.getContextId())
                    .isFinal(taskStatusUpdateEvent.getFinal())
                    .metadata(struct(taskStatusUpdateEvent.getMetadata()))
                    .build();
        }

        public static TaskArtifactUpdateEvent taskArtifactUpdateEvent(io.a2a.grpc.TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            return new TaskArtifactUpdateEvent.Builder()
                    .taskId(taskArtifactUpdateEvent.getTaskId())
                    .append(taskArtifactUpdateEvent.getAppend())
                    .lastChunk(taskArtifactUpdateEvent.getLastChunk())
                    .artifact(artifact(taskArtifactUpdateEvent.getArtifact()))
                    .contextId(taskArtifactUpdateEvent.getContextId())
                    .metadata(struct(taskArtifactUpdateEvent.getMetadata()))
                    .build();
        }

        private static Artifact artifact(io.a2a.grpc.Artifact artifact) {
            return new Artifact(
                    artifact.getArtifactId(),
                    artifact.getName(),
                    artifact.getDescription(),
                    artifact.getPartsList().stream().map(item -> part(item)).collect(Collectors.toList()),
                    struct(artifact.getMetadata())
            );
        }

        private static Part<?> part(io.a2a.grpc.Part part) {
            if (part.hasText()) {
                return textPart(part.getText());
            } else if (part.hasFile()) {
                return filePart(part.getFile());
            } else if (part.hasData()) {
                return dataPart(part.getData());
            }
            return null;
        }

        private static TextPart textPart(String text) {
            return new TextPart(text);
        }

        private static FilePart filePart(io.a2a.grpc.FilePart filePart) {
            if (filePart.hasFileWithBytes()) {
                return new FilePart(new FileWithBytes(filePart.getMimeType(), null, filePart.getFileWithBytes().toStringUtf8()));
            } else if (filePart.hasFileWithUri()) {
                return new FilePart(new FileWithUri(filePart.getMimeType(), null, filePart.getFileWithUri()));
            }
            return null;
        }

        private static DataPart dataPart(io.a2a.grpc.DataPart dataPart) {
            return new DataPart(struct(dataPart.getData()));
        }

        private static TaskStatus taskStatus(io.a2a.grpc.TaskStatus taskStatus) {
            return new TaskStatus(
                    taskState(taskStatus.getState()),
                    message(taskStatus.getUpdate()),
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(taskStatus.getTimestamp().getSeconds(), taskStatus.getTimestamp().getNanos()), ZoneOffset.UTC)
            );
        }

        private static Message.Role role(io.a2a.grpc.Role role) {
            if (role == null) {
                return null;
            }
            return switch (role) {
                case ROLE_USER -> Message.Role.USER;
                case ROLE_AGENT -> Message.Role.AGENT;
                default -> null;
            };
        }

        private static TaskState taskState(io.a2a.grpc.TaskState taskState) {
            if (taskState == null) {
                return null;
            }
            return switch (taskState) {
                case TASK_STATE_SUBMITTED -> TaskState.SUBMITTED;
                case TASK_STATE_WORKING -> TaskState.WORKING;
                case TASK_STATE_INPUT_REQUIRED -> TaskState.INPUT_REQUIRED;
                case TASK_STATE_AUTH_REQUIRED -> TaskState.AUTH_REQUIRED;
                case TASK_STATE_COMPLETED -> TaskState.COMPLETED;
                case TASK_STATE_CANCELLED -> TaskState.CANCELED;
                case TASK_STATE_FAILED -> TaskState.FAILED;
                case TASK_STATE_REJECTED -> TaskState.REJECTED;
                case TASK_STATE_UNSPECIFIED -> null;
                case UNRECOGNIZED -> null;
            };
        }

        private static Map<String, Object> struct(Struct struct) {
            if (struct == null || struct.getFieldsCount() == 0) {
                return null;
            }
            return struct.getFieldsMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> value(e.getValue())));
        }

        private static Object value(Value value) {
            switch (value.getKindCase()) {
                case STRUCT_VALUE:
                    return struct(value.getStructValue());
                case LIST_VALUE:
                    return value.getListValue().getValuesList().stream()
                            .map(FromProto::value)
                            .collect(Collectors.toList());
                case BOOL_VALUE:
                    return value.getBoolValue();
                case NUMBER_VALUE:
                    return value.getNumberValue();
                case STRING_VALUE:
                    return value.getStringValue();
                case NULL_VALUE:
                default:
                    return null;
            }
        }
    }


}