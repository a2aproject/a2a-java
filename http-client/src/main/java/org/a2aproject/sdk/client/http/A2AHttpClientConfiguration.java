package org.a2aproject.sdk.client.http;

import java.util.List;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Optional settings used by {@link A2AHttpClientFactory} when selecting and creating a client.
 *
 * <p>A provider is eligible only when it supports every configured setting. This prevents the
 * factory from silently ignoring security or SSE parsing configuration.
 */
public record A2AHttpClientConfiguration(
        List<String> excludedProviderNames,
        @Nullable SSEParserConfig sseParserConfig,
        @Nullable RedirectHeaderPolicy redirectHeaderPolicy) {

    /** The default configuration, which selects the highest-priority available provider. */
    public static final A2AHttpClientConfiguration DEFAULT = new A2AHttpClientConfiguration(List.of(), null, null);

    public A2AHttpClientConfiguration {
        Assert.checkNotNullParam("excludedProviderNames", excludedProviderNames);
        for (String providerName : excludedProviderNames) {
            Assert.checkNotNullParam("excludedProviderName", providerName);
        }
        excludedProviderNames = List.copyOf(excludedProviderNames);
    }

    /** Returns a builder for a client configuration. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns the configured SSE parser limits, or the SDK defaults when none were supplied. */
    public SSEParserConfig sseParserConfigOrDefault() {
        return sseParserConfig == null ? SSEParserConfig.DEFAULT : sseParserConfig;
    }

    /** Returns the configured redirect policy, or the SDK default when none was supplied. */
    public RedirectHeaderPolicy redirectHeaderPolicyOrDefault() {
        return redirectHeaderPolicy == null ? RedirectHeaderPolicy.DEFAULT : redirectHeaderPolicy;
    }

    /** Builder for {@link A2AHttpClientConfiguration}. */
    public static final class Builder {
        private List<String> excludedProviderNames = List.of();
        private @Nullable SSEParserConfig sseParserConfig;
        private @Nullable RedirectHeaderPolicy redirectHeaderPolicy;

        private Builder() {}

        /** Sets provider names that must not be considered by the factory. */
        public Builder excludedProviderNames(List<String> excludedProviderNames) {
            this.excludedProviderNames = List.copyOf(Assert.checkNotNullParam(
                    "excludedProviderNames", excludedProviderNames));
            return this;
        }

        /** Sets optional SSE parser limits. */
        public Builder sseParserConfig(@Nullable SSEParserConfig sseParserConfig) {
            this.sseParserConfig = sseParserConfig;
            return this;
        }

        /** Sets the redirect-header policy for explicitly enabled POST redirects. */
        public Builder redirectHeaderPolicy(@Nullable RedirectHeaderPolicy redirectHeaderPolicy) {
            this.redirectHeaderPolicy = redirectHeaderPolicy;
            return this;
        }

        /** Creates the immutable configuration. */
        public A2AHttpClientConfiguration build() {
            return new A2AHttpClientConfiguration(excludedProviderNames, sseParserConfig, redirectHeaderPolicy);
        }
    }
}
