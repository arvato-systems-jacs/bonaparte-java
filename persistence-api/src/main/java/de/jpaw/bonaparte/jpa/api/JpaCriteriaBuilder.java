package de.jpaw.bonaparte.jpa.api;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

import de.jpaw.bonaparte.pojos.api.AndFilter;
import de.jpaw.bonaparte.pojos.api.FieldFilter;
import de.jpaw.bonaparte.pojos.api.NotFilter;
import de.jpaw.bonaparte.pojos.api.OrFilter;
import de.jpaw.bonaparte.pojos.api.SearchFilter;
import de.jpaw.dp.Jdp;

public class JpaCriteriaBuilder {
    private final JpaFilter jpaFilter = Jdp.getRequired(JpaFilter.class);

    private final JpaPathResolver pathResolver;
    private final CriteriaBuilder cb;

    public JpaCriteriaBuilder(@Nonnull final JpaPathResolver pathResolver, @Nonnull final CriteriaBuilder cb) {
        this.pathResolver = pathResolver;
        this.cb = cb;
    }

    public Predicate buildPredicate(@Nullable final SearchFilter filter) {
        if (filter == null) {
            return null;
        }
        return switch (filter) {
            case FieldFilter f -> jpaFilter.applyFilter(cb, pathResolver.getPath(f.getFieldName()), f);
            case AndFilter f -> cb.and(buildPredicate(f.getFilter1()), buildPredicate(f.getFilter2()));
            case OrFilter f -> cb.or(buildPredicate(f.getFilter1()), buildPredicate(f.getFilter2()));
            case NotFilter f -> buildPredicate(f.getFilter()).not();
            default -> throw new RuntimeException("Unrecognized filter type: " + filter.ret$PQON());
        };
    }
}
