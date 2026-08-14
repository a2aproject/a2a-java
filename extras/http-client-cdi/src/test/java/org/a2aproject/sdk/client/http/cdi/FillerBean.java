package org.a2aproject.sdk.client.http.cdi;

import jakarta.enterprise.context.ApplicationScoped;

// Weld SE requires at least one bean class when disableDiscovery() is active.
// Used in test scenarios that start a CDI container without registering an A2AHttpClient bean.
@ApplicationScoped
class FillerBean {}
