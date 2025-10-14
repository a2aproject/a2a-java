package io.a2a.client.http.jdk.sse;

import io.a2a.client.http.sse.CommentEvent;
import io.a2a.client.http.sse.DataEvent;
import io.a2a.client.http.sse.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SSEHandler extends SubmissionPublisher<Event>
  implements Flow.Processor<String, Event> {

    public static final String EVENT_STREAM_MEDIA_TYPE = "text/event-stream";

    private static final Logger LOG = LoggerFactory.getLogger(SSEHandler.class);

    private static final String UTF8_BOM = "\uFEFF";

    private static final String DEFAULT_EVENT_NAME = "message";

    private Flow.Subscription subscription;

    private String currentEventName = DEFAULT_EVENT_NAME;
    private final StringBuilder dataBuffer = new StringBuilder();

    private String lastEventId = "";

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String input) {
        LOG.debug("got line `{}`", input);
        String line = removeTrailingNewline(removeLeadingBom(input));

        if (line.startsWith(":")) {
            submit(new CommentEvent(line.substring(1).trim()));
        } else if (line.isBlank()) {
            LOG.debug(
                    "broadcasting new event named {} lastEventId is {}",
                    currentEventName,
                    lastEventId
            );

            String dataString = dataBuffer.toString();
            if (!dataString.isEmpty()) {
                submit(new DataEvent(currentEventName, dataBuffer.toString(), lastEventId));
            }
            //reset things
            dataBuffer.setLength(0);
            currentEventName = DEFAULT_EVENT_NAME;
        } else if (line.contains(":")) {
            List<String> lineParts = List.of(line.split(":", 2));
            if (lineParts.size() == 2) {
                handleFieldValue(lineParts.get(0), stripLeadingSpaceIfPresent(lineParts.get(1)));
            }
        } else {
            handleFieldValue(line, "");
        }
        subscription.request(1);
    }

    private void handleFieldValue(String fieldName, String value) {
        switch (fieldName) {
            case "event":
                currentEventName = value;
                break;
            case "data":
                dataBuffer.append(value).append("\n");
                break;
            case "id":
                if (!value.contains("\0")) {
                    lastEventId = value;
                }
                break;
            case "retry":
                // ignored
                break;
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.debug("Error in SSE handler {}", throwable.getMessage());
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        LOG.debug("SSE handler complete");
        close();
    }

    private String stripLeadingSpaceIfPresent(String field) {
        if (field.charAt(0) == ' ') {
            return field.substring(1);
        }
        return field;
    }

    private String removeLeadingBom(String input) {
        if (input.startsWith(UTF8_BOM)) {
            return input.substring(UTF8_BOM.length());
        }
        return input;
    }

    private String removeTrailingNewline(String input) {
        if (input.endsWith("\n")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }
}
