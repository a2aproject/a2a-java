package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenTelemetryA2AHttpClientTest {

    @Mock
    private A2AHttpClient delegate;

    @Mock
    private OpenTelemetry openTelemetry;

    @Mock
    private ContextPropagators propagators;

    @Mock
    private TextMapPropagator textMapPropagator;

    @Mock
    private A2AHttpClient.PostBuilder delegatePostBuilder;

    @Mock
    private A2AHttpClient.GetBuilder delegateGetBuilder;

    @Mock
    private A2AHttpClient.DeleteBuilder delegateDeleteBuilder;

    @Mock
    private A2AHttpResponse response;

    private OpenTelemetryA2AHttpClient client;

    @BeforeEach
    void setUp() {
        when(openTelemetry.getPropagators()).thenReturn(propagators);
        when(propagators.getTextMapPropagator()).thenReturn(textMapPropagator);

        when(delegate.createPost()).thenReturn(delegatePostBuilder);
        when(delegate.createGet()).thenReturn(delegateGetBuilder);
        when(delegate.createDelete()).thenReturn(delegateDeleteBuilder);

        stubBuilderDefaults(delegatePostBuilder);
        when(delegatePostBuilder.body(any())).thenReturn(delegatePostBuilder);
        when(delegatePostBuilder.followRedirects(any(boolean.class))).thenReturn(delegatePostBuilder);

        stubBuilderDefaults(delegateGetBuilder);
        stubBuilderDefaults(delegateDeleteBuilder);

        client = new OpenTelemetryA2AHttpClient(delegate, openTelemetry);
    }

    private static <B extends A2AHttpClient.Builder<B>> void stubBuilderDefaults(B builder) {
        when(builder.url(any())).thenReturn(builder);
        when(builder.addHeader(any(), any())).thenReturn(builder);
        when(builder.addHeaders(any())).thenReturn(builder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void post_injectsContextBeforeDelegating() throws IOException, InterruptedException {
        when(delegatePostBuilder.post()).thenReturn(response);

        A2AHttpClient.PostBuilder builder = client.createPost();
        A2AHttpResponse result = builder
                .url("http://example.com/push")
                .addHeader("Content-Type", "application/json")
                .body("{}")
                .post();

        assertSame(response, result);

        InOrder order = inOrder(textMapPropagator, delegatePostBuilder);
        order.verify(textMapPropagator).inject(any(Context.class), any(), any(TextMapSetter.class));
        order.verify(delegatePostBuilder).post();
    }

    @Test
    @SuppressWarnings("unchecked")
    void post_propagatesCurrentSpanContext() throws IOException, InterruptedException {
        when(delegatePostBuilder.post()).thenReturn(response);

        Context testContext = Context.current().with(io.opentelemetry.context.ContextKey.named("test"), "value");
        try (Scope ignored = testContext.makeCurrent()) {
            client.createPost()
                    .url("http://example.com/push")
                    .body("{}")
                    .post();
        }

        verify(textMapPropagator).inject(any(Context.class), any(), any(TextMapSetter.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_injectsContextBeforeDelegating() throws IOException, InterruptedException {
        when(delegateGetBuilder.get()).thenReturn(response);

        A2AHttpClient.GetBuilder builder = client.createGet();
        A2AHttpResponse result = builder
                .url("http://example.com/api")
                .get();

        assertSame(response, result);

        InOrder order = inOrder(textMapPropagator, delegateGetBuilder);
        order.verify(textMapPropagator).inject(any(Context.class), any(), any(TextMapSetter.class));
        order.verify(delegateGetBuilder).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void delete_injectsContextBeforeDelegating() throws IOException, InterruptedException {
        when(delegateDeleteBuilder.delete()).thenReturn(response);

        A2AHttpClient.DeleteBuilder builder = client.createDelete();
        A2AHttpResponse result = builder
                .url("http://example.com/resource")
                .delete();

        assertSame(response, result);

        InOrder order = inOrder(textMapPropagator, delegateDeleteBuilder);
        order.verify(textMapPropagator).inject(any(Context.class), any(), any(TextMapSetter.class));
        order.verify(delegateDeleteBuilder).delete();
    }

    @Test
    void postBuilder_chainsCorrectly() {
        A2AHttpClient.PostBuilder builder = client.createPost();

        A2AHttpClient.PostBuilder chained = builder
                .url("http://example.com")
                .addHeader("X-Custom", "value")
                .body("{}")
                .followRedirects(false);

        assertNotNull(chained);
    }

    @Test
    @SuppressWarnings("unchecked")
    void contextInjection_propagatesHeadersViaAddHeaders() throws IOException, InterruptedException {
        when(delegatePostBuilder.post()).thenReturn(response);

        doAnswer(invocation -> {
            Map<String, String> carrier = invocation.getArgument(1);
            TextMapSetter<Map<String, String>> setter = invocation.getArgument(2);
            setter.set(carrier, "traceparent", "00-abc-def-01");
            return null;
        }).when(textMapPropagator).inject(any(Context.class), any(), any(TextMapSetter.class));

        client.createPost()
                .url("http://example.com")
                .body("{}")
                .post();

        verify(delegatePostBuilder).addHeaders(Map.of("traceparent", "00-abc-def-01"));
    }
}
