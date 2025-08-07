package io.a2a.server.agentexecution;

import io.a2a.server.events.EventQueue;
import io.a2a.spec.JSONRPCError;

public interface AgentExecutor {
    void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError;

    void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError;
}
