package de.jpaw.bonaparte.jpa.api;

import jakarta.persistence.criteria.Path;

public interface JpaPathResolver {
    Path<?> getPath(String fieldName);
}
