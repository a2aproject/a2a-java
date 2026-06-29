package org.a2aproject.sdk.server.common.quarkus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VertxSecurityHelperTest {

    @Mock
    ArcContainer arcContainer;
    @Mock
    ManagedContext requestContext;
    @Mock
    InjectableContext.ContextState contextState;
    @Mock
    RoutingContext routingContext;
    @Mock
    HttpServerResponse response;
    @Mock
    Instance<io.quarkus.vertx.http.runtime.security.HttpAuthenticator> httpAuthenticator;
    @Mock
    Instance<io.quarkus.security.identity.CurrentIdentityAssociation> currentIdentityAssociation;

    MockedStatic<Arc> arcStatic;
    MockedStatic<Context> vertxContextStatic;

    VertxSecurityHelper helper;

    AtomicReference<Handler<Void>> capturedEndHandler;
    AtomicReference<Handler<Void>> capturedCloseHandler;

    @BeforeEach
    void setUp() {
        capturedEndHandler = new AtomicReference<>();
        capturedCloseHandler = new AtomicReference<>();

        arcStatic = mockStatic(Arc.class);
        vertxContextStatic = mockStatic(Context.class);

        arcStatic.when(Arc::container).thenReturn(arcContainer);
        vertxContextStatic.when(Context::isOnEventLoopThread).thenReturn(false);

        when(arcContainer.requestContext()).thenReturn(requestContext);
        when(routingContext.response()).thenReturn(response);
        when(httpAuthenticator.isUnsatisfied()).thenReturn(true);

        doAnswer(inv -> {
            capturedEndHandler.set(inv.getArgument(0));
            return response;
        }).when(response).endHandler(any());
        doAnswer(inv -> {
            capturedCloseHandler.set(inv.getArgument(0));
            return response;
        }).when(response).closeHandler(any());

        helper = new VertxSecurityHelper();
        helper.httpAuthenticator = httpAuthenticator;
        helper.currentIdentityAssociation = currentIdentityAssociation;
    }

    @AfterEach
    void tearDown() {
        arcStatic.close();
        vertxContextStatic.close();
    }

    @Nested
    class RunInRequestContextDeferredTests {

        @Test
        void freshContext_activatesAndRegistersCleanupHandlers() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            helper.runInRequestContextDeferred(routingContext, () -> {});

            verify(requestContext).activate();
            verify(requestContext, never()).getState();
            verify(response).endHandler(any());
            verify(response).closeHandler(any());
            verify(requestContext).deactivate();
        }

        @Test
        void preActiveContext_capturesStateInsteadOfActivating() {
            when(requestContext.isActive()).thenReturn(true);
            when(requestContext.getState()).thenReturn(contextState);

            helper.runInRequestContextDeferred(routingContext, () -> {});

            verify(requestContext).getState();
            verify(requestContext, never()).activate();
        }

        @Test
        void preActiveContext_registersCleanupHandlersAndDeactivates() {
            when(requestContext.isActive()).thenReturn(true);
            when(requestContext.getState()).thenReturn(contextState);

            helper.runInRequestContextDeferred(routingContext, () -> {});

            verify(response).endHandler(any());
            verify(response).closeHandler(any());
            verify(requestContext).deactivate();
        }

        @Test
        void deactivateCalledInFinally_evenOnSuccess() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            helper.runInRequestContextDeferred(routingContext, () -> {});

            verify(requestContext).deactivate();
            verify(requestContext, never()).destroy(any(InjectableContext.ContextState.class));
        }

        @Test
        void endHandler_destroysContextState() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            helper.runInRequestContextDeferred(routingContext, () -> {});

            capturedEndHandler.get().handle(null);

            verify(requestContext).destroy(contextState);
        }

        @Test
        void closeHandler_destroysContextState() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            helper.runInRequestContextDeferred(routingContext, () -> {});

            capturedCloseHandler.get().handle(null);

            verify(requestContext).destroy(contextState);
        }

        @Test
        void cleanupIsIdempotent_bothHandlersFire_destroysOnlyOnce() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            helper.runInRequestContextDeferred(routingContext, () -> {});

            capturedEndHandler.get().handle(null);
            capturedCloseHandler.get().handle(null);

            verify(requestContext, times(1)).destroy(contextState);
        }

        @Test
        void taskException_destroysImmediatelyAndDeactivates() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            assertThrows(RuntimeException.class, () ->
                    helper.runInRequestContextDeferred(routingContext, () -> {
                        throw new RuntimeException("task failed");
                    }));

            verify(requestContext).destroy(contextState);
            verify(requestContext).deactivate();
        }

        @Test
        void taskException_subsequentEndHandlerDoesNotDestroyAgain() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();

            assertThrows(RuntimeException.class, () ->
                    helper.runInRequestContextDeferred(routingContext, () -> {
                        throw new RuntimeException();
                    }));

            capturedEndHandler.get().handle(null);

            verify(requestContext, times(1)).destroy(contextState);
        }

        @Test
        void preActiveContext_endHandlerDestroysTheCapturedState() {
            when(requestContext.isActive()).thenReturn(true);
            when(requestContext.getState()).thenReturn(contextState);

            helper.runInRequestContextDeferred(routingContext, () -> {});

            capturedEndHandler.get().handle(null);

            verify(requestContext).destroy(contextState);
        }

        @Test
        void eventLoopThread_throwsIllegalStateException() {
            vertxContextStatic.when(Context::isOnEventLoopThread).thenReturn(true);

            assertThrows(IllegalStateException.class, () ->
                    helper.runInRequestContextDeferred(routingContext, () -> {}));
        }

        @Test
        void taskIsExecuted() {
            when(requestContext.isActive()).thenReturn(false);
            doAnswer(inv -> contextState).when(requestContext).activate();
            boolean[] taskRan = {false};

            helper.runInRequestContextDeferred(routingContext, () -> taskRan[0] = true);

            assertTrue(taskRan[0]);
        }
    }

    @Nested
    class RunInRequestContextTests {

        @Test
        void freshContext_activatesAndTerminates() {
            when(requestContext.isActive()).thenReturn(false);

            helper.runInRequestContext(routingContext, () -> {});

            verify(requestContext).activate();
            verify(requestContext).terminate();
        }

        @Test
        void preActiveContext_doesNotActivateOrTerminate() {
            when(requestContext.isActive()).thenReturn(true);

            helper.runInRequestContext(routingContext, () -> {});

            verify(requestContext, never()).activate();
            verify(requestContext, never()).terminate();
        }

        @Test
        void taskException_stillTerminatesWhenFreshContext() {
            when(requestContext.isActive()).thenReturn(false);

            assertThrows(RuntimeException.class, () ->
                    helper.runInRequestContext(routingContext, () -> {
                        throw new RuntimeException("task failed");
                    }));

            verify(requestContext).terminate();
        }

        @Test
        void eventLoopThread_throwsIllegalStateException() {
            vertxContextStatic.when(Context::isOnEventLoopThread).thenReturn(true);

            assertThrows(IllegalStateException.class, () ->
                    helper.runInRequestContext(routingContext, () -> {}));
        }
    }
}
