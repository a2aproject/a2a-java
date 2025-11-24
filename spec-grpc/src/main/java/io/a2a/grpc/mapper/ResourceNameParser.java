package io.a2a.grpc.mapper;

/**
 * Utility class for parsing gRPC resource names.
 * <p>
 * Provides methods to extract IDs and components from resource name strings
 * following the pattern: "tasks/{taskId}/pushNotificationConfigs/{configId}"
 */
public class ResourceNameParser {

    /**
     * Extracts the task ID from a simple resource name like "tasks/{taskId}".
     *
     * @param resourceName the resource name (e.g., "tasks/abc123")
     * @return the extracted task ID
     */
    public static String extractTaskId(String resourceName) {
        return resourceName.substring(resourceName.lastIndexOf('/') + 1);
    }

    /**
     * Parses a task push notification config resource name and extracts taskId and configId.
     * <p>
     * Expected format: "tasks/{taskId}/pushNotificationConfigs/{configId}"
     *
     * @param resourceName the resource name to parse
     * @return array with [taskId, configId]
     * @throws IllegalArgumentException if the format is invalid
     */
    public static String[] parseTaskPushNotificationConfigName(String resourceName) {
        String[] parts = resourceName.split("/");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid name format for TaskPushNotificationConfig: " + resourceName);
        }
        return new String[]{parts[1], parts[3]};
    }

    /**
     * Parses a get task push notification config request name and extracts taskId and configId.
     * <p>
     * Expected formats:
     * - "tasks/{taskId}" (returns taskId for both values)
     * - "tasks/{taskId}/pushNotificationConfigs/{configId}"
     *
     * @param resourceName the resource name to parse
     * @return array with [taskId, configId]
     * @throws IllegalArgumentException if the format is invalid
     */
    public static String[] parseGetTaskPushNotificationConfigName(String resourceName) {
        String[] parts = resourceName.split("/");
        String taskId = parts[1];
        String configId;

        if (parts.length == 2) {
            // "tasks/{taskId}" - use taskId as configId
            configId = taskId;
        } else if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid name format for GetTaskPushNotificationConfigRequest: " + resourceName);
        } else {
            // "tasks/{taskId}/pushNotificationConfigs/{configId}"
            configId = parts[3];
        }

        return new String[]{taskId, configId};
    }

    /**
     * Extracts the parent ID (task ID) from a parent resource name like "tasks/{taskId}".
     *
     * @param parentName the parent resource name
     * @return the extracted parent ID
     */
    public static String extractParentId(String parentName) {
        return parentName.substring(parentName.lastIndexOf('/') + 1);
    }
}
