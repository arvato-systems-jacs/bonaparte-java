package de.jpaw.bonaparte.adapters.jts;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.core.ExceptionConverter;
import de.jpaw.bonaparte.pojos.adapters.jts.BPoint2dimWithSrid;


public class JtsPointWithSridAdapter {
	private static final PrecisionModel DOUBLE_MODEL = new PrecisionModel(PrecisionModel.FLOATING);

    /** Convert the custom type into a serializable BonaPortable. */
    public static BPoint2dimWithSrid marshal(final Point point) {
        return new BPoint2dimWithSrid(point.getSRID(), point.getX(), point.getY());
    }

    /** Convert a parsed adapter type into the custom type. */
    public static <E extends Exception> Point unmarshal(BonaPortable obj, ExceptionConverter<E> p) throws E {
        if (obj instanceof BPoint2dimWithSrid point) {
        	GeometryFactory factory = new GeometryFactory(DOUBLE_MODEL, point.getSrid());
        	return factory.createPoint(new Coordinate(point.getX(), point.getY()));
        } else {
            throw new IllegalArgumentException("Incorrect type returned");
        }
    }
}
