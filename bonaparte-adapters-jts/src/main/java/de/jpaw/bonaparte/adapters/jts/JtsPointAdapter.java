package de.jpaw.bonaparte.adapters.jts;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.core.ExceptionConverter;
import de.jpaw.bonaparte.pojos.adapters.jts.BPoint2dim;


public class JtsPointAdapter {
	private static final int SRID_WGS84 = 4326;  // the GPS coordinate system
	private static final PrecisionModel DOUBLE_MODEL = new PrecisionModel(PrecisionModel.FLOATING);
	private static final GeometryFactory FACTORY = new GeometryFactory(DOUBLE_MODEL, SRID_WGS84);

    /** Convert the custom type into a serializable BonaPortable. */
    public static BPoint2dim marshal(final Point point) {
        return new BPoint2dim(point.getX(), point.getY());
    }

    /** Convert a parsed adapter type into the custom type. */
    public static <E extends Exception> Point unmarshal(BonaPortable obj, ExceptionConverter<E> p) throws E {
        if (obj instanceof BPoint2dim point) {
        	return FACTORY.createPoint(new Coordinate(point.getX(), point.getY()));
        } else {
            throw new IllegalArgumentException("Incorrect type returned");
        }
    }
}
