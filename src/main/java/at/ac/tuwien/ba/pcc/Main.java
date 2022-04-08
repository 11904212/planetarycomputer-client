package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.search.dto.QueryParameter;
import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.GeoJsonObject;
import mil.nga.sf.wkt.GeometryReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public class Main {

    public static void main(String[] args) throws IOException, ParseException, FactoryException, TransformException, URISyntaxException, InterruptedException {

        final String wktPolygon = "POLYGON((14.242 47.901,14.251 47.901,14.251 47.896,14.242 47.896,14.242 47.901))";
        final String dir = "./test_data/";
        final String assetType = "visual";
        final String collection = "sentinel-2-l2a";
        final String datetime = "2022-03-26T10:00:31.024000Z";

        long start, stop;

        PlanetaryComputer pcClient = new PlanetaryComputerImpl();
        StacClient stacClient = pcClient.getStacClient();

        QueryParameter parameter = new QueryParameter();
        parameter.addCollection(collection);
        parameter.setIntersects(wktToGeoJson(wktPolygon));
        parameter.setLimit(1);
        parameter.setDatetime(datetime);

        start = System.currentTimeMillis();
        var itemCollection = stacClient.search(parameter);
        stop = System.currentTimeMillis();
        System.out.printf("stacClient.search() took %dms%n", stop - start);

        if (itemCollection.getItems().size() == 0) {
            return;
        }

        var item = itemCollection.getItems().get(0);

        Optional<Asset> visualAsset = item.getAsset(assetType);
        if(visualAsset.isEmpty()) return;

        var aoiGeom = wktToJtsGeometry(wktPolygon);
        aoiGeom.setSRID(4326);

        start = System.currentTimeMillis();
        GridCoverage2D coverage = pcClient.getCroppedCoverage(visualAsset.get(), aoiGeom);
        stop = System.currentTimeMillis();
        System.out.printf("pcClient.getCroppedCoverage() took %dms%n", stop - start);

        String filename = item.getId() + "_" + assetType;

        File directory = new File(dir);
        if (! directory.exists()){
            var dirCreated = directory.mkdir();
            if (!dirCreated){
                System.out.println("no permission to creat directory, program will terminate");
                return;
            }
        }

        File file = new File(dir + filename +".tif");
        GeoTiffWriter writer = new GeoTiffWriter(file);
        writer.write(coverage, null);
        writer.dispose();

        File fileGeoJson = new File(dir + filename +".geojson");
        String geoJsonContent = FeatureConverter.toStringValue(wktToGeoJson(wktPolygon));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileGeoJson));
        bufferedWriter.write(geoJsonContent);
        bufferedWriter.close();

        System.out.println("please be patient, GeoTiffWriter needs some time to free up the resources.");

    }

    private static GeoJsonObject wktToGeoJson(String wkt) throws IOException {
        var geom = GeometryReader.readGeometry(wkt);
        return FeatureConverter.toGeometry(geom);
    }

    private static org.locationtech.jts.geom.Geometry wktToJtsGeometry(String wkt) throws ParseException {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTReader reader = new WKTReader(geometryFactory);

        return reader.read(wkt);
    }
}
