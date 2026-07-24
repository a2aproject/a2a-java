package org.a2aproject.sdk.server.apps.common;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedBean {

    public String getValue() {
        return "request-scoped-value";
    }
}
