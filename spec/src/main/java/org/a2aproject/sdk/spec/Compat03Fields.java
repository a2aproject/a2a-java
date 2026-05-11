package org.a2aproject.sdk.spec;

import java.util.List;

public final class Compat03Fields {

    private static final boolean COMPAT_03_AVAILABLE;

    static {
        boolean available;
        try {
            Class.forName("org.a2aproject.sdk.compat03.spec.AgentCard_v0_3");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        COMPAT_03_AVAILABLE = available;
    }

    private Compat03Fields() {
    }

    public static void addCompat03FieldsIfAvailable(AgentCard.Builder builder,
            List<AgentInterface> supportedInterfaces, String url, String preferredTransport) {
        if (!COMPAT_03_AVAILABLE) {
            return;
        }
        builder.url(url)
                .preferredTransport(preferredTransport)
                .additionalInterfaces(
                        supportedInterfaces.stream()
                                .map(iface -> new Legacy_0_3_AgentInterface(iface.protocolBinding(), iface.url()))
                                .toList());
    }
}
