package org.a2aproject.sdk.extras.taskstore.database.jpa;

import org.a2aproject.sdk.util.Assert;

/** Complete per-task read policy, suitable for database-side filtering and counting. */
public sealed interface JpaTaskReadScope {
    record OwnedBy(String subjectKey) implements JpaTaskReadScope {
        public OwnedBy {
            Assert.checkNotNullParam("subjectKey", subjectKey);
            JpaSubjectKey.encode(subjectKey);
        }
    }

    record All() implements JpaTaskReadScope {
    }

    record None() implements JpaTaskReadScope {
    }
}
