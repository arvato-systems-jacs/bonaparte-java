package de.jpaw.bonaparte.adapters.jts;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class Jts {
    private Jts() {
    }

    public static final int SRID_WGS84              =  4326;  // the GPS coordinate system (longitude / latitude)
    public static final int SRID_WEB_MERCATOR       =  3857;  // Google Maps, OpenStreetMap
    public static final int SRID_LAMBERT93          =  2154;  // France
    public static final int SRID_NAD83              =  5070;  // USA (continental)
    public static final int SRID_BRITISH_NATIONAL   = 27700;  // Great Britain
    public static final int SRID_ETRS89_ITM32N      = 25832;  // Germany, parts of Central Europe
    public static final int SRID_ETRS89_ITM33N      = 25833;  // Eastern Germany, Scandinavia
    public static final int SRID_WGS84_ITM32N       = 32632;  // GPS-based UTM coordinates
    public static final int SRID_WGS84_ITM33N       = 32633;  // GPS-based UTM coordinates

    public static final PrecisionModel DOUBLE_MODEL = new PrecisionModel(PrecisionModel.FLOATING);
    public static final GeometryFactory FACTORY     = new GeometryFactory(DOUBLE_MODEL, SRID_WGS84);
}
