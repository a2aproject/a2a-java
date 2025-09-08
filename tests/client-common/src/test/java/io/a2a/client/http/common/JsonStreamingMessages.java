package io.a2a.client.http.common;

/**
 * Contains JSON strings for testing SSE streaming.
 */
public class JsonStreamingMessages {

    static final String SEND_MESSAGE_STREAMING_TEST_RESPONSE =
            "event: message\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"id\":\"2\",\"contextId\":\"context-1234\",\"status\":{\"state\":\"completed\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"kind\":\"text\",\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{},\"kind\":\"task\"}}\n\n";

    static final String TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE =
            "event: message\n" +
                    "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"id\":\"2\",\"contextId\":\"context-5678\",\"status\":{\"state\":\"completed\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"kind\":\"text\",\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{},\"kind\":\"task\"}}\n\n";
}