package de.jpaw.bonaparte.jpa.api;

import de.jpaw.bonaparte.pojos.api.FieldFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public interface JpaFilter {
    Predicate applyFilter(CriteriaBuilder cb, Path<?> from, FieldFilter f);
}
