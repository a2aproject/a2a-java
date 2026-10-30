package org.a2aproject.sdk.extras.taskstore.database.jpa;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.jspecify.annotations.Nullable;

/**
 * Invoke through a CDI proxy: each method uses a fresh transaction-scoped persistence
 * context. Direct construction is unsupported; use the repository factory outside CDI.
 */
@ApplicationScoped
public class JpaTaskOwnershipTransactions {
    @PersistenceContext(unitName = "a2a-java")
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void insert(String taskId, byte[] subjectKey) {
        insert(em, taskId, subjectKey);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public byte @Nullable [] owner(String taskId) {
        return owner(em, taskId);
    }

    static void insert(EntityManager em, String taskId, byte[] subjectKey) {
        em.persist(new JpaTaskOwner(taskId, subjectKey));
        em.flush();
    }

    static byte @Nullable [] owner(EntityManager em, String taskId) {
        return em.createQuery("SELECT o.subjectKey FROM JpaTaskOwner o WHERE o.taskId = :taskId", byte[].class)
                .setParameter("taskId", taskId)
                .setHint("jakarta.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS)
                .setHint("org.hibernate.cacheable", false).getResultStream().findFirst().orElse(null);
    }
}
