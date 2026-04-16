package org.a2aproject.sdk.compat03.grpc.utils;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCardSignature_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentExtension_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentProvider_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.AuthorizationCodeOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.ClientCredentialsOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.FileContent_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.ImplicitOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MutualTLSSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuthFlows_v0_3;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.PasswordOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.jspecify.annotations.Nullable;

/**
 * Utility class to convert between GRPC and Spec objects.
 */
public class ProtoUtils_v0_3 {

    public static class ToProto {

        public static org.a2aproject.sdk.compat03.grpc.AgentCard agentCard(AgentCard_v0_3 agentCard) {
            org.a2aproject.sdk.compat03.grpc.AgentCard.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentCard.newBuilder();
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
                    org.a2aproject.sdk.compat03.grpc.Security.Builder securityBuilder = org.a2aproject.sdk.compat03.grpc.Security.newBuilder();
                    s.forEach((key, value) -> {
                        org.a2aproject.sdk.compat03.grpc.StringList.Builder stringListBuilder = org.a2aproject.sdk.compat03.grpc.StringList.newBuilder();
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
            if (agentCard.signatures() != null) {
                builder.addAllSignatures(agentCard.signatures().stream().map(ToProto::agentCardSignature).collect(Collectors.toList()));
            }
            return builder.build();
        }

        public static org.a2aproject.sdk.compat03.grpc.Task task(Task_v0_3 task) {
            org.a2aproject.sdk.compat03.grpc.Task.Builder builder = org.a2aproject.sdk.compat03.grpc.Task.newBuilder();
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

        public static org.a2aproject.sdk.compat03.grpc.Message message(Message_v0_3 message) {
            org.a2aproject.sdk.compat03.grpc.Message.Builder builder = org.a2aproject.sdk.compat03.grpc.Message.newBuilder();
            builder.setMessageId(message.getMessageId());
            if (message.getContextId() != null) {
                builder.setContextId(message.getContextId());
            }
            if (message.getTaskId() != null) {
                builder.setTaskId(message.getTaskId());
            }
            builder.setRole(role(message.getRole()));
            if (message.getParts() != null) {
                builder.addAllContent(message.getParts().stream().map(ToProto::part).collect(Collectors.toList()));
            }
            builder.setMetadata(struct(message.getMetadata()));
            return builder.build();
        }

        public static org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig taskPushNotificationConfig(TaskPushNotificationConfig_v0_3 config) {
            String id = config.pushNotificationConfig().id();
            org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.Builder builder = org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder();
            builder.setName("tasks/" + config.taskId() + "/pushNotificationConfigs" + (id == null ? "" : ('/' + id)));
            builder.setPushNotificationConfig(pushNotificationConfig(config.pushNotificationConfig()));
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.PushNotificationConfig pushNotificationConfig(PushNotificationConfig_v0_3 config) {
            org.a2aproject.sdk.compat03.grpc.PushNotificationConfig.Builder builder = org.a2aproject.sdk.compat03.grpc.PushNotificationConfig.newBuilder();
            if (config.url() != null) {
                builder.setUrl(config.url());
            }
            if (config.token() != null) {
                builder.setToken(config.token());
            }
            if (config.authentication() != null) {
                builder.setAuthentication(authenticationInfo(config.authentication()));
            }
            if (config.id() != null) {
                builder.setId(config.id());
            }
            return builder.build();
        }

        public static org.a2aproject.sdk.compat03.grpc.TaskArtifactUpdateEvent taskArtifactUpdateEvent(TaskArtifactUpdateEvent_v0_3 event) {
            org.a2aproject.sdk.compat03.grpc.TaskArtifactUpdateEvent.Builder builder = org.a2aproject.sdk.compat03.grpc.TaskArtifactUpdateEvent.newBuilder();
            builder.setTaskId(event.getTaskId());
            builder.setContextId(event.getContextId());
            builder.setArtifact(artifact(event.getArtifact()));
            if (event.isAppend() != null) {
                builder.setAppend(event.isAppend());
            }
            if (event.isLastChunk() != null) {
                builder.setLastChunk(event.isLastChunk());
            }
            if (event.getMetadata() != null) {
                builder.setMetadata(struct(event.getMetadata()));
            }
            return builder.build();
        }

        public static org.a2aproject.sdk.compat03.grpc.TaskStatusUpdateEvent taskStatusUpdateEvent(TaskStatusUpdateEvent_v0_3 event) {
            org.a2aproject.sdk.compat03.grpc.TaskStatusUpdateEvent.Builder builder = org.a2aproject.sdk.compat03.grpc.TaskStatusUpdateEvent.newBuilder();
            builder.setTaskId(event.getTaskId());
            builder.setContextId(event.getContextId());
            builder.setStatus(taskStatus(event.getStatus()));
            builder.setFinal(event.isFinal());
            if (event.getMetadata() != null) {
                builder.setMetadata(struct(event.getMetadata()));
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.Artifact artifact(Artifact_v0_3 artifact) {
            org.a2aproject.sdk.compat03.grpc.Artifact.Builder builder = org.a2aproject.sdk.compat03.grpc.Artifact.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.Part part(Part_v0_3<?> part) {
            org.a2aproject.sdk.compat03.grpc.Part.Builder builder = org.a2aproject.sdk.compat03.grpc.Part.newBuilder();
            if (part instanceof TextPart_v0_3) {
                builder.setText(((TextPart_v0_3) part).getText());
            } else if (part instanceof FilePart_v0_3) {
                builder.setFile(filePart((FilePart_v0_3) part));
            } else if (part instanceof DataPart_v0_3) {
                builder.setData(dataPart((DataPart_v0_3) part));
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.FilePart filePart(FilePart_v0_3 filePart) {
            org.a2aproject.sdk.compat03.grpc.FilePart.Builder builder = org.a2aproject.sdk.compat03.grpc.FilePart.newBuilder();
            FileContent_v0_3 fileContent = filePart.getFile();
            if (fileContent instanceof FileWithBytes_v0_3) {
                builder.setFileWithBytes(ByteString.copyFrom(((FileWithBytes_v0_3) fileContent).bytes(), StandardCharsets.UTF_8));
            } else if (fileContent instanceof FileWithUri_v0_3) {
                builder.setFileWithUri(((FileWithUri_v0_3) fileContent).uri());
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.DataPart dataPart(DataPart_v0_3 dataPart) {
            org.a2aproject.sdk.compat03.grpc.DataPart.Builder builder = org.a2aproject.sdk.compat03.grpc.DataPart.newBuilder();
            if (dataPart.getData() != null) {
                builder.setData(struct(dataPart.getData()));
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.Role role(Message_v0_3.Role role) {
            if (role == null) {
                return org.a2aproject.sdk.compat03.grpc.Role.ROLE_UNSPECIFIED;
            }
            return switch (role) {
                case USER ->
                    org.a2aproject.sdk.compat03.grpc.Role.ROLE_USER;
                case AGENT ->
                    org.a2aproject.sdk.compat03.grpc.Role.ROLE_AGENT;
            };
        }

        private static org.a2aproject.sdk.compat03.grpc.TaskStatus taskStatus(TaskStatus_v0_3 taskStatus) {
            org.a2aproject.sdk.compat03.grpc.TaskStatus.Builder builder = org.a2aproject.sdk.compat03.grpc.TaskStatus.newBuilder();
            if (taskStatus.state() != null) {
                builder.setState(taskState(taskStatus.state()));
            }
            if (taskStatus.message() != null) {
                builder.setUpdate(message(taskStatus.message()));
            }
            if (taskStatus.timestamp() != null) {
                Instant instant = taskStatus.timestamp().toInstant();
                builder.setTimestamp(com.google.protobuf.Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build());
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.TaskState taskState(TaskState_v0_3 taskState) {
            if (taskState == null) {
                return org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_UNSPECIFIED;
            }
            return switch (taskState) {
                case SUBMITTED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_SUBMITTED;
                case WORKING ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_WORKING;
                case INPUT_REQUIRED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_INPUT_REQUIRED;
                case AUTH_REQUIRED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_AUTH_REQUIRED;
                case COMPLETED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_COMPLETED;
                case CANCELED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_CANCELLED;
                case FAILED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_FAILED;
                case REJECTED ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_REJECTED;
                default ->
                    org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_UNSPECIFIED;
            };
        }

        private static org.a2aproject.sdk.compat03.grpc.AuthenticationInfo authenticationInfo(PushNotificationAuthenticationInfo_v0_3 pushNotificationAuthenticationInfo) {
            org.a2aproject.sdk.compat03.grpc.AuthenticationInfo.Builder builder = org.a2aproject.sdk.compat03.grpc.AuthenticationInfo.newBuilder();
            if (pushNotificationAuthenticationInfo.schemes() != null) {
                builder.addAllSchemes(pushNotificationAuthenticationInfo.schemes());
            }
            if (pushNotificationAuthenticationInfo.credentials() != null) {
                builder.setCredentials(pushNotificationAuthenticationInfo.credentials());
            }
            return builder.build();
        }

        public static org.a2aproject.sdk.compat03.grpc.SendMessageConfiguration messageSendConfiguration(MessageSendConfiguration_v0_3 messageSendConfiguration) {
            org.a2aproject.sdk.compat03.grpc.SendMessageConfiguration.Builder builder = org.a2aproject.sdk.compat03.grpc.SendMessageConfiguration.newBuilder();
            if (messageSendConfiguration.acceptedOutputModes() != null) {
                builder.addAllAcceptedOutputModes(messageSendConfiguration.acceptedOutputModes());
            }
            if (messageSendConfiguration.historyLength() != null) {
                builder.setHistoryLength(messageSendConfiguration.historyLength());
            }
            if (messageSendConfiguration.pushNotificationConfig() != null) {
                builder.setPushNotification(pushNotificationConfig(messageSendConfiguration.pushNotificationConfig()));
            }
            builder.setBlocking(messageSendConfiguration.blocking());
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.AgentProvider agentProvider(AgentProvider_v0_3 agentProvider) {
            org.a2aproject.sdk.compat03.grpc.AgentProvider.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentProvider.newBuilder();
            builder.setOrganization(agentProvider.organization());
            builder.setUrl(agentProvider.url());
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.AgentCapabilities agentCapabilities(AgentCapabilities_v0_3 agentCapabilities) {
            org.a2aproject.sdk.compat03.grpc.AgentCapabilities.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentCapabilities.newBuilder();
            builder.setStreaming(agentCapabilities.streaming());
            builder.setPushNotifications(agentCapabilities.pushNotifications());
            if (agentCapabilities.extensions() != null) {
                builder.addAllExtensions(agentCapabilities.extensions().stream().map(ToProto::agentExtension).collect(Collectors.toList()));
            }
            return builder.build();
        }

        public static org.a2aproject.sdk.compat03.grpc.SendMessageRequest sendMessageRequest(MessageSendParams_v0_3 request) {
            org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder builder =  org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder();
            builder.setRequest(message(request.message()));
            if (request.configuration() != null) {
                builder.setConfiguration(messageSendConfiguration(request.configuration()));
            }
            if (request.metadata() != null && ! request.metadata().isEmpty()) {
                builder.setMetadata(struct(request.metadata()));
            }
            return builder.build();
        }
        private static org.a2aproject.sdk.compat03.grpc.AgentExtension agentExtension(AgentExtension_v0_3 agentExtension) {
            org.a2aproject.sdk.compat03.grpc.AgentExtension.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentExtension.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.AgentSkill agentSkill(AgentSkill_v0_3 agentSkill) {
            org.a2aproject.sdk.compat03.grpc.AgentSkill.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentSkill.newBuilder();
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
            if (agentSkill.security() != null) {
                builder.addAllSecurity(agentSkill.security().stream().map(s -> {
                    org.a2aproject.sdk.compat03.grpc.Security.Builder securityBuilder = org.a2aproject.sdk.compat03.grpc.Security.newBuilder();
                    s.forEach((key, value) -> {
                        org.a2aproject.sdk.compat03.grpc.StringList.Builder stringListBuilder = org.a2aproject.sdk.compat03.grpc.StringList.newBuilder();
                        stringListBuilder.addAllList(value);
                        securityBuilder.putSchemes(key, stringListBuilder.build());
                    });
                    return securityBuilder.build();
                }).collect(Collectors.toList()));
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.AgentCardSignature agentCardSignature(AgentCardSignature_v0_3 agentCardSignature) {
            org.a2aproject.sdk.compat03.grpc.AgentCardSignature.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentCardSignature.newBuilder();
            builder.setProtected(agentCardSignature.protectedHeader());
            builder.setSignature(agentCardSignature.signature());
            if (agentCardSignature.header() != null) {
                builder.setHeader(struct(agentCardSignature.header()));
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.SecurityScheme securityScheme(SecurityScheme_v0_3 securityScheme) {
            org.a2aproject.sdk.compat03.grpc.SecurityScheme.Builder builder = org.a2aproject.sdk.compat03.grpc.SecurityScheme.newBuilder();
            if (securityScheme instanceof APIKeySecurityScheme_v0_3) {
                builder.setApiKeySecurityScheme(apiKeySecurityScheme((APIKeySecurityScheme_v0_3) securityScheme));
            } else if (securityScheme instanceof HTTPAuthSecurityScheme_v0_3) {
                builder.setHttpAuthSecurityScheme(httpAuthSecurityScheme((HTTPAuthSecurityScheme_v0_3) securityScheme));
            } else if (securityScheme instanceof OAuth2SecurityScheme_v0_3) {
                builder.setOauth2SecurityScheme(oauthSecurityScheme((OAuth2SecurityScheme_v0_3) securityScheme));
            } else if (securityScheme instanceof OpenIdConnectSecurityScheme_v0_3) {
                builder.setOpenIdConnectSecurityScheme(openIdConnectSecurityScheme((OpenIdConnectSecurityScheme_v0_3) securityScheme));
            } else if (securityScheme instanceof MutualTLSSecurityScheme_v0_3) {
                builder.setMtlsSecurityScheme(mutualTlsSecurityScheme((MutualTLSSecurityScheme_v0_3) securityScheme));
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.APIKeySecurityScheme apiKeySecurityScheme(APIKeySecurityScheme_v0_3 apiKeySecurityScheme) {
            org.a2aproject.sdk.compat03.grpc.APIKeySecurityScheme.Builder builder = org.a2aproject.sdk.compat03.grpc.APIKeySecurityScheme.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.HTTPAuthSecurityScheme httpAuthSecurityScheme(HTTPAuthSecurityScheme_v0_3 httpAuthSecurityScheme) {
            org.a2aproject.sdk.compat03.grpc.HTTPAuthSecurityScheme.Builder builder = org.a2aproject.sdk.compat03.grpc.HTTPAuthSecurityScheme.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.OAuth2SecurityScheme oauthSecurityScheme(OAuth2SecurityScheme_v0_3 oauth2SecurityScheme) {
            org.a2aproject.sdk.compat03.grpc.OAuth2SecurityScheme.Builder builder = org.a2aproject.sdk.compat03.grpc.OAuth2SecurityScheme.newBuilder();
            if (oauth2SecurityScheme.getDescription() != null) {
                builder.setDescription(oauth2SecurityScheme.getDescription());
            }
            if (oauth2SecurityScheme.getFlows() != null) {
                builder.setFlows(oauthFlows(oauth2SecurityScheme.getFlows()));
            }
            if (oauth2SecurityScheme.getOauth2MetadataUrl() != null) {
                builder.setOauth2MetadataUrl(oauth2SecurityScheme.getOauth2MetadataUrl());
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.OAuthFlows oauthFlows(OAuthFlows_v0_3 oAuthFlows) {
            org.a2aproject.sdk.compat03.grpc.OAuthFlows.Builder builder = org.a2aproject.sdk.compat03.grpc.OAuthFlows.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.AuthorizationCodeOAuthFlow authorizationCodeOAuthFlow(AuthorizationCodeOAuthFlow_v0_3 authorizationCodeOAuthFlow) {
            org.a2aproject.sdk.compat03.grpc.AuthorizationCodeOAuthFlow.Builder builder = org.a2aproject.sdk.compat03.grpc.AuthorizationCodeOAuthFlow.newBuilder();
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

        public static org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfigResponse(List<TaskPushNotificationConfig_v0_3> configs) {
            List<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> confs = new ArrayList<>(configs.size());
            for(TaskPushNotificationConfig_v0_3 config: configs) {
                confs.add(taskPushNotificationConfig(config));
            }
            return org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.newBuilder().addAllConfigs(confs).build();
        }

        private static org.a2aproject.sdk.compat03.grpc.ClientCredentialsOAuthFlow clientCredentialsOAuthFlow(ClientCredentialsOAuthFlow_v0_3 clientCredentialsOAuthFlow) {
            org.a2aproject.sdk.compat03.grpc.ClientCredentialsOAuthFlow.Builder builder = org.a2aproject.sdk.compat03.grpc.ClientCredentialsOAuthFlow.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.ImplicitOAuthFlow implicitOAuthFlow(ImplicitOAuthFlow_v0_3 implicitOAuthFlow) {
            org.a2aproject.sdk.compat03.grpc.ImplicitOAuthFlow.Builder builder = org.a2aproject.sdk.compat03.grpc.ImplicitOAuthFlow.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.PasswordOAuthFlow passwordOAuthFlow(PasswordOAuthFlow_v0_3 passwordOAuthFlow) {
            org.a2aproject.sdk.compat03.grpc.PasswordOAuthFlow.Builder builder = org.a2aproject.sdk.compat03.grpc.PasswordOAuthFlow.newBuilder();
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

        private static org.a2aproject.sdk.compat03.grpc.OpenIdConnectSecurityScheme openIdConnectSecurityScheme(OpenIdConnectSecurityScheme_v0_3 openIdConnectSecurityScheme) {
            org.a2aproject.sdk.compat03.grpc.OpenIdConnectSecurityScheme.Builder builder = org.a2aproject.sdk.compat03.grpc.OpenIdConnectSecurityScheme.newBuilder();
            if (openIdConnectSecurityScheme.getDescription() != null) {
                builder.setDescription(openIdConnectSecurityScheme.getDescription());
            }
            if (openIdConnectSecurityScheme.getOpenIdConnectUrl() != null) {
                builder.setOpenIdConnectUrl(openIdConnectSecurityScheme.getOpenIdConnectUrl());
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.MutualTlsSecurityScheme mutualTlsSecurityScheme(MutualTLSSecurityScheme_v0_3 mutualTlsSecurityScheme) {
            org.a2aproject.sdk.compat03.grpc.MutualTlsSecurityScheme.Builder builder = org.a2aproject.sdk.compat03.grpc.MutualTlsSecurityScheme.newBuilder();
            if (mutualTlsSecurityScheme.getDescription() != null) {
                builder.setDescription(mutualTlsSecurityScheme.getDescription());
            }
            return builder.build();
        }

        private static org.a2aproject.sdk.compat03.grpc.AgentInterface agentInterface(AgentInterface_v0_3 agentInterface) {
            org.a2aproject.sdk.compat03.grpc.AgentInterface.Builder builder = org.a2aproject.sdk.compat03.grpc.AgentInterface.newBuilder();
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

        public static StreamResponse streamResponse(StreamingEventKind_v0_3 streamingEventKind) {
            if (streamingEventKind instanceof TaskStatusUpdateEvent_v0_3) {
                return StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent((TaskStatusUpdateEvent_v0_3) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof TaskArtifactUpdateEvent_v0_3) {
                return StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent((TaskArtifactUpdateEvent_v0_3) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof Message_v0_3) {
                return StreamResponse.newBuilder()
                        .setMsg(message((Message_v0_3) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof Task_v0_3) {
                return StreamResponse.newBuilder()
                        .setTask(task((Task_v0_3) streamingEventKind))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + streamingEventKind);
            }
        }

        public static org.a2aproject.sdk.compat03.grpc.SendMessageResponse taskOrMessage(EventKind_v0_3 eventKind) {
            if (eventKind instanceof Task_v0_3) {
                return org.a2aproject.sdk.compat03.grpc.SendMessageResponse.newBuilder()
                        .setTask(task((Task_v0_3) eventKind))
                        .build();
            } else if (eventKind instanceof Message_v0_3) {
                return org.a2aproject.sdk.compat03.grpc.SendMessageResponse.newBuilder()
                        .setMsg(message((Message_v0_3) eventKind))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            }
        }

        public static org.a2aproject.sdk.compat03.grpc.StreamResponse taskOrMessageStream(StreamingEventKind_v0_3 eventKind) {
            if (eventKind instanceof Task_v0_3 task) {
                return org.a2aproject.sdk.compat03.grpc.StreamResponse.newBuilder()
                        .setTask(task(task))
                        .build();
            } else if (eventKind instanceof Message_v0_3 msg) {
                return org.a2aproject.sdk.compat03.grpc.StreamResponse.newBuilder()
                        .setMsg(message(msg))
                        .build();
            } else if (eventKind instanceof TaskArtifactUpdateEvent_v0_3 update) {
                return org.a2aproject.sdk.compat03.grpc.StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent(update))
                        .build();
            } else if (eventKind instanceof TaskStatusUpdateEvent_v0_3 update) {
                return org.a2aproject.sdk.compat03.grpc.StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent(update))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            }
        }

    }

    public static class FromProto {

        public static TaskQueryParams_v0_3 taskQueryParams(org.a2aproject.sdk.compat03.grpc.GetTaskRequestOrBuilder request) {
            String name = request.getName();
            String id = name.substring(name.lastIndexOf('/') + 1);
            return new TaskQueryParams_v0_3(id, request.getHistoryLength());
        }

        public static TaskIdParams_v0_3 taskIdParams(org.a2aproject.sdk.compat03.grpc.CancelTaskRequestOrBuilder request) {
            String name = request.getName();
            String id = name.substring(name.lastIndexOf('/') + 1);
            return new TaskIdParams_v0_3(id);
        }

        public static MessageSendParams_v0_3 messageSendParams(org.a2aproject.sdk.compat03.grpc.SendMessageRequestOrBuilder request) {
            MessageSendParams_v0_3.Builder builder = new MessageSendParams_v0_3.Builder();
            builder.message(message(request.getRequest()));
            if (request.hasConfiguration()) {
                builder.configuration(messageSendConfiguration(request.getConfiguration()));
            }
            if (request.hasMetadata()) {
                builder.metadata(struct(request.getMetadata()));
            }
            return builder.build();
        }

        public static TaskPushNotificationConfig_v0_3 taskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequestOrBuilder request) {
            return taskPushNotificationConfig(request.getConfig(), true);
        }

        public static TaskPushNotificationConfig_v0_3 taskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfigOrBuilder config) {
            return taskPushNotificationConfig(config, false);
        }

        private static TaskPushNotificationConfig_v0_3 taskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfigOrBuilder config, boolean create) {
            String name = config.getName(); // "tasks/{id}/pushNotificationConfigs/{push_id}"
            String[] parts = name.split("/");
            String taskId = parts[1];
            String configId = "";
            if (create) {
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Invalid name format for TaskPushNotificationConfig: " + name);
                }
                if (parts.length == 4) {
                    configId = parts[3];
                } else {
                    configId = taskId;
                }
            } else {
                if (parts.length < 4) {
                    throw new IllegalArgumentException("Invalid name format for TaskPushNotificationConfig: " + name);
                }
                configId = parts[3];
            }
            PushNotificationConfig_v0_3 pnc = pushNotification(config.getPushNotificationConfig(), configId);
            return new TaskPushNotificationConfig_v0_3(taskId, pnc);
        }

        public static GetTaskPushNotificationConfigParams_v0_3 getTaskPushNotificationConfigParams(org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequestOrBuilder request) {
            String name = request.getName(); // "tasks/{id}/pushNotificationConfigs/{push_id}"
            String[] parts = name.split("/");
            String taskId = parts[1];
            String configId;
            if (parts.length == 2) {
                configId = taskId;
            } else if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid name format for GetTaskPushNotificationConfigRequest: " + name);
            } else {
                configId = parts[3];
            }
            return new GetTaskPushNotificationConfigParams_v0_3(taskId, configId);
        }

        public static TaskIdParams_v0_3 taskIdParams(org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequestOrBuilder request) {
            String name = request.getName();
            String id = name.substring(name.lastIndexOf('/') + 1);
            return new TaskIdParams_v0_3(id);
        }

        public static List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigParams(org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponseOrBuilder response) {
            List<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> configs = response.getConfigsList();
            List<TaskPushNotificationConfig_v0_3> result = new ArrayList<>(configs.size());
            for(org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig config : configs) {
                result.add(taskPushNotificationConfig(config, false));
            }
            return result;
        }

        public static ListTaskPushNotificationConfigParams_v0_3 listTaskPushNotificationConfigParams(org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequestOrBuilder request) {
            String parent = request.getParent();
            String id = parent.substring(parent.lastIndexOf('/') + 1);
            return new ListTaskPushNotificationConfigParams_v0_3(id);
        }

        public static DeleteTaskPushNotificationConfigParams_v0_3 deleteTaskPushNotificationConfigParams(org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder request) {
            String name = request.getName(); // "tasks/{id}/pushNotificationConfigs/{push_id}"
            String[] parts = name.split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid name format for DeleteTaskPushNotificationConfigRequest: " + name);
            }
            String taskId = parts[1];
            String configId = parts[3];
            return new DeleteTaskPushNotificationConfigParams_v0_3(taskId, configId);
        }

        private static AgentExtension_v0_3 agentExtension(org.a2aproject.sdk.compat03.grpc.AgentExtensionOrBuilder agentExtension) {
            return new AgentExtension_v0_3(
                    agentExtension.getDescription(),
                    struct(agentExtension.getParams()),
                    agentExtension.getRequired(),
                    agentExtension.getUri()
            );
        }

        private static MessageSendConfiguration_v0_3 messageSendConfiguration(org.a2aproject.sdk.compat03.grpc.SendMessageConfigurationOrBuilder sendMessageConfiguration) {
            return new MessageSendConfiguration_v0_3(
                    sendMessageConfiguration.getAcceptedOutputModesList().isEmpty() ? null :
                            new ArrayList<>(sendMessageConfiguration.getAcceptedOutputModesList()),
                    sendMessageConfiguration.getHistoryLength(),
                    pushNotification(sendMessageConfiguration.getPushNotification()),
                    sendMessageConfiguration.getBlocking()
            );
        }

        private static @Nullable PushNotificationConfig_v0_3 pushNotification(org.a2aproject.sdk.compat03.grpc.PushNotificationConfigOrBuilder pushNotification, String configId) {
            if(pushNotification == null || pushNotification.getDefaultInstanceForType().equals(pushNotification)) {
                return null;
            }
            return new PushNotificationConfig_v0_3(
                    pushNotification.getUrl(),
                    pushNotification.getToken().isEmpty() ? null : pushNotification.getToken(),
                    pushNotification.hasAuthentication() ? authenticationInfo(pushNotification.getAuthentication()) : null,
                    pushNotification.getId().isEmpty() ? configId : pushNotification.getId()
            );
        }

        private static @Nullable PushNotificationConfig_v0_3 pushNotification(org.a2aproject.sdk.compat03.grpc.PushNotificationConfigOrBuilder pushNotification) {
            return pushNotification(pushNotification, pushNotification.getId());
        }

        private static PushNotificationAuthenticationInfo_v0_3 authenticationInfo(org.a2aproject.sdk.compat03.grpc.AuthenticationInfoOrBuilder authenticationInfo) {
            return new PushNotificationAuthenticationInfo_v0_3(
                    new ArrayList<>(authenticationInfo.getSchemesList()),
                    authenticationInfo.getCredentials()
            );
        }

        public static Task_v0_3 task(org.a2aproject.sdk.compat03.grpc.TaskOrBuilder task) {
            return new Task_v0_3(
                    task.getId(),
                    task.getContextId(),
                    taskStatus(task.getStatus()),
                    task.getArtifactsList().stream().map(item -> artifact(item)).collect(Collectors.toList()),
                    task.getHistoryList().stream().map(item -> message(item)).collect(Collectors.toList()),
                    struct(task.getMetadata())
            );
        }

        public static Message_v0_3 message(org.a2aproject.sdk.compat03.grpc.MessageOrBuilder message) {
            if (message.getMessageId().isEmpty()) {
                throw new InvalidParamsError_v0_3();
            }

            return new Message_v0_3(
                    role(message.getRole()),
                    message.getContentList().stream().map(item -> part(item)).collect(Collectors.toList()),
                    message.getMessageId().isEmpty() ? null :  message.getMessageId(),
                    message.getContextId().isEmpty() ? null :  message.getContextId(),
                    message.getTaskId().isEmpty() ? null :  message.getTaskId(),
                    null, // referenceTaskIds is not in grpc message
                    struct(message.getMetadata()),
                    message.getExtensionsList().isEmpty() ? null : message.getExtensionsList()
            );
        }

        public static TaskStatusUpdateEvent_v0_3 taskStatusUpdateEvent(org.a2aproject.sdk.compat03.grpc.TaskStatusUpdateEventOrBuilder taskStatusUpdateEvent) {
            return new TaskStatusUpdateEvent_v0_3.Builder()
                    .taskId(taskStatusUpdateEvent.getTaskId())
                    .status(taskStatus(taskStatusUpdateEvent.getStatus()))
                    .contextId(taskStatusUpdateEvent.getContextId())
                    .isFinal(taskStatusUpdateEvent.getFinal())
                    .metadata(struct(taskStatusUpdateEvent.getMetadata()))
                    .build();
        }

        public static TaskArtifactUpdateEvent_v0_3 taskArtifactUpdateEvent(org.a2aproject.sdk.compat03.grpc.TaskArtifactUpdateEventOrBuilder taskArtifactUpdateEvent) {
            return new TaskArtifactUpdateEvent_v0_3.Builder()
                    .taskId(taskArtifactUpdateEvent.getTaskId())
                    .append(taskArtifactUpdateEvent.getAppend())
                    .lastChunk(taskArtifactUpdateEvent.getLastChunk())
                    .artifact(artifact(taskArtifactUpdateEvent.getArtifact()))
                    .contextId(taskArtifactUpdateEvent.getContextId())
                    .metadata(struct(taskArtifactUpdateEvent.getMetadata()))
                    .build();
        }

        private static Artifact_v0_3 artifact(org.a2aproject.sdk.compat03.grpc.ArtifactOrBuilder artifact) {
            return new Artifact_v0_3(
                    artifact.getArtifactId(),
                    artifact.getName(),
                    artifact.getDescription(),
                    artifact.getPartsList().stream().map(item -> part(item)).collect(Collectors.toList()),
                    struct(artifact.getMetadata()),
                    artifact.getExtensionsList().isEmpty() ? null : artifact.getExtensionsList()
            );
        }

        private static Part_v0_3<?> part(org.a2aproject.sdk.compat03.grpc.PartOrBuilder part) {
            if (part.hasText()) {
                return textPart(part.getText());
            } else if (part.hasFile()) {
                return filePart(part.getFile());
            } else if (part.hasData()) {
                return dataPart(part.getData());
            }
            throw new InvalidRequestError_v0_3();
        }

        private static TextPart_v0_3 textPart(String text) {
            return new TextPart_v0_3(text);
        }

        private static FilePart_v0_3 filePart(org.a2aproject.sdk.compat03.grpc.FilePartOrBuilder filePart) {
            if (filePart.hasFileWithBytes()) {
                return new FilePart_v0_3(new FileWithBytes_v0_3(filePart.getMimeType(), null, filePart.getFileWithBytes().toStringUtf8()));
            } else if (filePart.hasFileWithUri()) {
                return new FilePart_v0_3(new FileWithUri_v0_3(filePart.getMimeType(), null, filePart.getFileWithUri()));
            }
            throw new InvalidRequestError_v0_3();
        }

        private static DataPart_v0_3 dataPart(org.a2aproject.sdk.compat03.grpc.DataPartOrBuilder dataPart) {
            return new DataPart_v0_3(struct(dataPart.getData()));
        }

        private static @Nullable TaskStatus_v0_3 taskStatus(org.a2aproject.sdk.compat03.grpc.TaskStatusOrBuilder taskStatus) {
            TaskState_v0_3 state = taskState(taskStatus.getState());
            if (state == null) {
                return null;
            }
            return new TaskStatus_v0_3(
                    taskState(taskStatus.getState()),
                    taskStatus.hasUpdate() ? message(taskStatus.getUpdateOrBuilder()) : null,
                    OffsetDateTime.ofInstant(Instant.ofEpochSecond(taskStatus.getTimestamp().getSeconds(), taskStatus.getTimestamp().getNanos()), ZoneOffset.UTC)
            );
        }

        private static Message_v0_3.@Nullable Role role(org.a2aproject.sdk.compat03.grpc.Role role) {
            if (role == null) {
                return null;
            }
            return switch (role) {
                case ROLE_USER ->
                    Message_v0_3.Role.USER;
                case ROLE_AGENT ->
                    Message_v0_3.Role.AGENT;
                default ->
                    throw new InvalidRequestError_v0_3();
            };
        }

        private static @Nullable TaskState_v0_3 taskState(org.a2aproject.sdk.compat03.grpc.TaskState taskState) {
            if (taskState == null) {
                return null;
            }
            return switch (taskState) {
                case TASK_STATE_SUBMITTED ->
                    TaskState_v0_3.SUBMITTED;
                case TASK_STATE_WORKING ->
                    TaskState_v0_3.WORKING;
                case TASK_STATE_INPUT_REQUIRED ->
                    TaskState_v0_3.INPUT_REQUIRED;
                case TASK_STATE_AUTH_REQUIRED ->
                    TaskState_v0_3.AUTH_REQUIRED;
                case TASK_STATE_COMPLETED ->
                    TaskState_v0_3.COMPLETED;
                case TASK_STATE_CANCELLED ->
                    TaskState_v0_3.CANCELED;
                case TASK_STATE_FAILED ->
                    TaskState_v0_3.FAILED;
                case TASK_STATE_REJECTED ->
                    TaskState_v0_3.REJECTED;
                case TASK_STATE_UNSPECIFIED ->
                    null;
                case UNRECOGNIZED ->
                    null;
            };
        }

        private static @Nullable Map<String, Object> struct(Struct struct) {
            if (struct == null || struct.getFieldsCount() == 0) {
                return null;
            }
            return struct.getFieldsMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> value(e.getValue())));
        }

        private static @Nullable Object value(Value value) {
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
                    throw new InvalidRequestError_v0_3();
            }
        }
    }

}
