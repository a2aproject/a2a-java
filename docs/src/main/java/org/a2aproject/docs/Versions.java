package org.a2aproject.docs;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DataMapping(value = "versions", type = DataMapping.Type.ARRAY_DIR)
public record Versions(List<Version> list) {

    public Versions {
        if (list == null) {
            throw new IllegalArgumentException("Versions list cannot be null");
        }
        long defaultCount = list.stream().filter(Version::defaultVersion).count();
        if (defaultCount != 1) {
            throw new IllegalStateException(
                    "Exactly one version must have defaultVersion: true, found " + defaultCount);
        }
        Set<Integer> sortOrders = new HashSet<>();
        for (Version v : list) {
            if (!sortOrders.add(v.sortOrder())) {
                throw new IllegalStateException(
                        "Duplicate sortOrder " + v.sortOrder() + " in version " + v.label());
            }
        }
    }

    public record Version(
            String label,
            String path,
            int sortOrder,
            boolean defaultVersion,
            boolean devVersion,
            List<MenuItem> menu) {

        public record MenuItem(
                String title,
                String path,
                String icon) {

            public MenuItem {
                if (title == null || title.isBlank()) {
                    throw new IllegalArgumentException("MenuItem title cannot be null or blank");
                }
                if (path == null || path.isBlank()) {
                    throw new IllegalArgumentException("MenuItem path cannot be null or blank");
                }
                if (!path.startsWith("/")) {
                    throw new IllegalStateException(
                            "MenuItem path must start with '/', got: " + path);
                }
                if (icon == null || icon.isBlank()) {
                    throw new IllegalArgumentException("MenuItem icon cannot be null or blank");
                }
            }
        }
    }
}
