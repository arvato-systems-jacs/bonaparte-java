package de.jpaw.bonaparte.jpa.api;

import de.jpaw.bonaparte.pojos.api.AsciiFilter;
import de.jpaw.bonaparte.pojos.api.BooleanFilter;
import de.jpaw.bonaparte.pojos.api.ByteArrayFilter;
import de.jpaw.bonaparte.pojos.api.ByteFilter;
import de.jpaw.bonaparte.pojos.api.BytesFilter;
import de.jpaw.bonaparte.pojos.api.DayFilter;
import de.jpaw.bonaparte.pojos.api.DecimalFilter;
import de.jpaw.bonaparte.pojos.api.DoubleFilter;
import de.jpaw.bonaparte.pojos.api.FieldFilter;
import de.jpaw.bonaparte.pojos.api.FloatFilter;
import de.jpaw.bonaparte.pojos.api.InstantFilter;
import de.jpaw.bonaparte.pojos.api.IntFilter;
import de.jpaw.bonaparte.pojos.api.LongFilter;
import de.jpaw.bonaparte.pojos.api.NullFilter;
import de.jpaw.bonaparte.pojos.api.ShortFilter;
import de.jpaw.bonaparte.pojos.api.TimeFilter;
import de.jpaw.bonaparte.pojos.api.TimestampFilter;
import de.jpaw.bonaparte.pojos.api.UnicodeFilter;
import de.jpaw.bonaparte.pojos.api.UuidFilter;
import de.jpaw.dp.Singleton;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

@Singleton
public class JpaFilterImpl implements JpaFilter {

    @Override
    @SuppressWarnings("unchecked")
    public Predicate applyFilter(CriteriaBuilder cb, Path<?> path, FieldFilter filter) {
        return switch (filter) {
            case NullFilter f -> cb.isNull(path);
            case BooleanFilter f -> cb.equal(path, Boolean.valueOf(f.getBooleanValue()));
            case AsciiFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLikeValue() != null)
                    yield cb.like((Path<String>) path, f.getLikeValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<String>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<String>) path, f.getLowerBound());
                else
                    yield cb.between((Path<String>) path, f.getLowerBound(), f.getUpperBound());
            }
            case UnicodeFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLikeValue() != null)
                    yield cb.like((Path<String>) path, f.getLikeValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<String>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<String>) path, f.getLowerBound());
                else
                    yield cb.between((Path<String>) path, f.getLowerBound(), f.getUpperBound());
            }
            case IntFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Integer>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Integer>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Integer>) path, f.getLowerBound(), f.getUpperBound());
            }
            case LongFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Long>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Long>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Long>) path, f.getLowerBound(), f.getUpperBound());
            }
            case DecimalFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<BigDecimal>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<BigDecimal>) path, f.getLowerBound());
                else
                    yield cb.between((Path<BigDecimal>) path, f.getLowerBound(), f.getUpperBound());
            }
            case DayFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<LocalDate>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<LocalDate>) path, f.getLowerBound());
                else
                    yield cb.between((Path<LocalDate>) path, f.getLowerBound(), f.getUpperBound());
            }
            case TimestampFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<LocalDateTime>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<LocalDateTime>) path, f.getLowerBound());
                else
                    yield cb.between((Path<LocalDateTime>) path, f.getLowerBound(), f.getUpperBound());
            }
            case InstantFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Instant>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Instant>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Instant>) path, f.getLowerBound(), f.getUpperBound());
            }
            case TimeFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<LocalTime>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<LocalTime>) path, f.getLowerBound());
                else
                    yield cb.between((Path<LocalTime>) path, f.getLowerBound(), f.getUpperBound());
            }
            case ByteFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Byte>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Byte>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Byte>) path, f.getLowerBound(), f.getUpperBound());
            }
            case ShortFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Short>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Short>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Short>) path, f.getLowerBound(), f.getUpperBound());
            }
            case DoubleFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Double>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Double>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Double>) path, f.getLowerBound(), f.getUpperBound());
            }
            case FloatFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else if (f.getLowerBound() == null)
                    yield cb.lessThanOrEqualTo((Path<Float>) path, f.getUpperBound());
                else if (f.getUpperBound() == null)
                    yield cb.greaterThanOrEqualTo((Path<Float>) path, f.getLowerBound());
                else
                    yield cb.between((Path<Float>) path, f.getLowerBound(), f.getUpperBound());
            }
            case UuidFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else
                    yield null;
            }
            case ByteArrayFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else
                    yield null;
            }
            case BytesFilter f -> {
                if (f.getValueList() != null)
                    yield path.in(f.getValueList());
                else if (f.getEqualsValue() != null)
                    yield cb.equal(path, f.getEqualsValue());
                else
                    yield null;
            }
            default -> throw new RuntimeException("Unrecognized field filter type: " + filter.ret$PQON());
        };
    }
}
