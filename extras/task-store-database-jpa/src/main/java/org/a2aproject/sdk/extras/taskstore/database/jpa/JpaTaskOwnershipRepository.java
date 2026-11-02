package org.a2aproject.sdk.extras.taskstore.database.jpa;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** Persistent first-writer-wins ownership with independent insert and confirmation transactions. */
@Dependent
public class JpaTaskOwnershipRepository {
    private final JpaTaskOwnershipTransactions transactions;

    /** The argument must be the CDI-intercepted bean, never a directly constructed helper. */
    @Inject
    JpaTaskOwnershipRepository(JpaTaskOwnershipTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
    }

    /**
     * Programmatic JTA path. The factory must create JTA entity managers for the same
     * database as the task store; the transaction manager must own their transactions.
     * Each operation suspends the caller and creates/closes a separate persistence context.
     */
    public static JpaTaskOwnershipRepository create(
            EntityManagerFactory factory, TransactionManager manager) {
        return new JpaTaskOwnershipRepository(
                new JpaTaskOwnershipTransactions() {
                    @Override
                    public void insert(String id, byte[] subject) {
                        isolated(
                                factory,
                                manager,
                                em -> {
                                    JpaTaskOwnershipTransactions.insert(em, id, subject);
                                    return null;
                                });
                    }

                    @Override
                    public byte @Nullable [] owner(String id) {
                        return isolated(
                                factory, manager, em -> JpaTaskOwnershipTransactions.owner(em, id));
                    }
                });
    }

    private static <T> T isolated(
            EntityManagerFactory factory,
            TransactionManager manager,
            Function<EntityManager, T> action) {
        Transaction suspended = null;
        try {
            suspended = manager.suspend();
            manager.begin();
            try (EntityManager em = factory.createEntityManager()) {
                em.joinTransaction();
                T result = action.apply(em);
                manager.commit();
                return result;
            } catch (Exception failure) {
                if (manager.getTransaction() != null) {
                    manager.rollback();
                }
                throw failure;
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Isolated ownership transaction failed", failure);
        } finally {
            if (suspended != null) {
                try {
                    manager.resume(suspended);
                } catch (Exception failure) {
                    throw new IllegalStateException("Could not resume caller transaction", failure);
                }
            }
        }
    }

    public byte @Nullable [] owner(String taskId) {
        return transactions.owner(taskId);
    }

    public void record(String taskId, byte[] subjectKey) {
        try {
            transactions.insert(taskId, subjectKey);
        } catch (RuntimeException insertionFailure) {
            // The interceptor has rolled back before this catch executes. Confirmation
            // must suspend the caller again, including its possible repeatable-read snapshot.
            try {
                if (transactions.owner(taskId) != null) {
                    return;
                }
            } catch (RuntimeException confirmationFailure) {
                insertionFailure.addSuppressed(confirmationFailure);
            }
            throw insertionFailure;
        }
    }
}
