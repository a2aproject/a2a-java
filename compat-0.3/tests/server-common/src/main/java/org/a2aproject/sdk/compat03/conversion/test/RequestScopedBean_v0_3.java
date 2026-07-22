package org.a2aproject.sdk.compat03.conversion.test;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedBean_v0_3 {

    public String getValue() {
        return "request-scoped-value";
    }
}
