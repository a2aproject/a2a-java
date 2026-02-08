package io.a2a.server.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import java.util.UUID;

/**
 * @author Sandeep Belgavi
 */
@ApplicationScoped
@Default
public class UUIDIdGenerator implements IdGenerator {
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
