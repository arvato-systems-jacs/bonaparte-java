package de.jpaw.bonaparte.adapters.jts;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.core.ExceptionConverter;
import de.jpaw.bonaparte.pojos.adapters.jts.BPoint;
import de.jpaw.bonaparte.pojos.adapters.jts.BPolygon;


public class JtsPolygonAdapter {

    /** Convert the custom type into a serializable BonaPortable. */
    public static BPolygon marshal(final Polygon polygon) {
        final Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        final List<BPoint> result = new ArrayList<>(coordinates.length);

        for (final Coordinate c : coordinates) {
            result.add(new BPoint(c.getX(), c.getY()));
        }
        return new BPolygon(result);
    }

    /** Convert a parsed adapter type into the custom type. */
    public static <E extends Exception> Polygon unmarshal(final BonaPortable obj, final ExceptionConverter<E> p) throws E {
        if (obj instanceof BPolygon polygon) {
            final int len = polygon.getCoordinates().size();
            final Coordinate[] coordinates = new Coordinate[len];
            for (int i = 0; i < len; i++) {
                final BPoint point = polygon.getCoordinates().get(i);
                coordinates[i] = new Coordinate(point.getX(), point.getY());
            }
            return Jts.FACTORY.createPolygon(coordinates);
        } else {
            throw new IllegalArgumentException("Incorrect type returned");
        }
    }
}
