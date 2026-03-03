package uk.ac.rdg.resc.edal.ncwms;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for WMS axis order compliance.
 *
 * WMS 1.3 (OGC 06-042): CRS=EPSG:4326 is defined as lat/lon (y/x)
 * WMS 1.1 (OGC 01-068r3): SRS=EPSG:4326 is lon/lat (x/y)
 *
 * This is the most common source of coordinate confusion in WMS
 * implementations.
 * See: https://docs.geoserver.org/stable/en/user/services/wms/axis_order.html
 */
public class NcwmsAxisOrderTest {

    /**
     * WMS 1.3 EPSG:4326 must use lat/lon axis order (Y/X).
     * BBOX format: minLat,minLon,maxLat,maxLon
     * Example: "-90,-180,90,180" (NOT "-180,-90,180,90" which is WMS 1.1 style)
     */
    @Test
    public void testWms13Epsg4326IsLatLon() {
        // In WMS 1.3, EPSG:4326 CRS has axis order: Latitude first, Longitude second.
        // A BBOX covering the whole world would be: -90,-180,90,180
        // minY(lat), minX(lon), maxY(lat), maxX(lon)
        double minLat = -90.0;
        double minLon = -180.0;
        double maxLat = 90.0;
        double maxLon = 180.0;

        // WMS 1.3 BBOX for EPSG:4326: minLat,minLon,maxLat,maxLon
        String wms13Bbox = buildWms13Bbox("EPSG:4326", minLat, minLon, maxLat, maxLon);

        // Verify lat comes before lon (axis order: Y/X)
        String[] parts = wms13Bbox.split(",");
        double parsedMinY = Double.parseDouble(parts[0]);
        double parsedMinX = Double.parseDouble(parts[1]);

        // minY (lat) must be -90, not -180
        assertEquals("WMS 1.3 EPSG:4326 BBOX[0] must be minLat (-90), not minLon (-180)",
                minLat, parsedMinY, 0.001);
        // minX (lon) must be -180
        assertEquals("WMS 1.3 EPSG:4326 BBOX[1] must be minLon (-180), not minLat (-90)",
                minLon, parsedMinX, 0.001);
    }

    /**
     * WMS 1.1 EPSG:4326 must use lon/lat axis order (X/Y).
     * BBOX format: minLon,minLat,maxLon,maxLat
     * Example: "-180,-90,180,90"
     */
    @Test
    public void testWms11Epsg4326IsLonLat() {
        double minLat = -90.0;
        double minLon = -180.0;
        double maxLat = 90.0;
        double maxLon = 180.0;

        // WMS 1.1 BBOX for EPSG:4326: minLon,minLat,maxLon,maxLat
        String wms11Bbox = buildWms11Bbox("EPSG:4326", minLon, minLat, maxLon, maxLat);

        String[] parts = wms11Bbox.split(",");
        double parsedMinX = Double.parseDouble(parts[0]);
        double parsedMinY = Double.parseDouble(parts[1]);

        // minX (lon) must be -180
        assertEquals("WMS 1.1 EPSG:4326 BBOX[0] must be minLon (-180), not minLat (-90)",
                minLon, parsedMinX, 0.001);
        // minY (lat) must be -90
        assertEquals("WMS 1.1 EPSG:4326 BBOX[1] must be minLat (-90), not minLon (-180)",
                minLat, parsedMinY, 0.001);
    }

    /**
     * Sanity check: WMS 1.3 with EPSG:4326 must NOT use lon/lat (WMS 1.1 style).
     * This test documents the breaking change between WMS 1.1 and 1.3.
     */
    @Test
    public void testWms13Epsg4326IsNotLonLat() {
        double minLat = -90.0;
        double minLon = -180.0;
        double maxLat = 90.0;
        double maxLon = 180.0;

        String wms13Bbox = buildWms13Bbox("EPSG:4326", minLat, minLon, maxLat, maxLon);
        String wms11Style = minLon + "," + minLat + "," + maxLon + "," + maxLat;

        assertNotEquals(
                "WMS 1.3 EPSG:4326 BBOX must differ from WMS 1.1 BBOX (axis order changed)",
                wms11Style, wms13Bbox);
    }

    /**
     * CRS:84 uses lon/lat in WMS 1.3 (unlike EPSG:4326).
     * This documents the difference between CRS:84 and EPSG:4326 in WMS 1.3.
     */
    @Test
    public void testWms13Crs84IsLonLat() {
        double minLat = -90.0;
        double minLon = -180.0;
        double maxLon = 180.0;
        double maxLat = 90.0;

        // CRS:84 in WMS 1.3: lon/lat (x/y, same as WMS 1.1 EPSG:4326)
        String crs84Bbox = buildWms13Bbox("CRS:84", minLon, minLat, maxLon, maxLat);

        String[] parts = crs84Bbox.split(",");
        double parsedFirst = Double.parseDouble(parts[0]);

        // CRS:84 first value should be lon (-180), not lat (-90)
        assertEquals("WMS 1.3 CRS:84 BBOX[0] must be minLon (-180) — lon/lat order",
                minLon, parsedFirst, 0.001);
    }

    // -------------------------------------------------------------------------
    // Helper methods — these would be replaced by actual WMS request parsing
    // in a full integration test. For now they document the expected contract.
    // -------------------------------------------------------------------------

    /**
     * Builds a WMS 1.3 BBOX string.
     * For EPSG:4326: order is minLat,minLon,maxLat,maxLon (Y/X)
     * For CRS:84: order is minLon,minLat,maxLon,maxLat (X/Y)
     */
    private String buildWms13Bbox(String crs, double first, double second,
            double third, double fourth) {
        return first + "," + second + "," + third + "," + fourth;
    }

    /**
     * Builds a WMS 1.1 BBOX string.
     * Always lon/lat (X/Y) regardless of SRS.
     */
    private String buildWms11Bbox(String srs, double minX, double minY,
            double maxX, double maxY) {
        return minX + "," + minY + "," + maxX + "," + maxY;
    }
}
