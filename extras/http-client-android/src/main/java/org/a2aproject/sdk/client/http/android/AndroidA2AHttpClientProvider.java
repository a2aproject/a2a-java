package org.a2aproject.sdk.client.http.android;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientProvider;

/**
 * Service provider for {@link AndroidA2AHttpClient}.
 */
public final class AndroidA2AHttpClientProvider implements A2AHttpClientProvider {

    private static final boolean ANDROID_AVAILABLE = isAndroidAvailable();

    private static boolean isAndroidAvailable() {
        String runtimeName = System.getProperty("java.runtime.name");
        return runtimeName != null && runtimeName.toLowerCase(java.util.Locale.ENGLISH).contains("android");
    }

    @Override
    public A2AHttpClient create() {
        if (!ANDROID_AVAILABLE) {
            throw new IllegalStateException(
                    "Android classes are not available. This provider is only supported on Android.");
        }
        return new AndroidA2AHttpClient();
    }

    @Override
    public int priority() {
        return ANDROID_AVAILABLE ? 110 : -1; // Higher priority than Vert.x on Android
    }

    @Override
    public String name() {
        return "android";
    }
}
