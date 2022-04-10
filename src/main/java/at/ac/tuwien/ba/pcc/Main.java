package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.search.dto.QueryParameter;
import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.GeoJsonObject;
import mil.nga.sf.wkt.GeometryReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.Arguments;
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

    private final static String EXAMPLE_WKT = "POLYGON((14.242 47.901,14.251 47.901,14.251 47.896,14.242 47.896,14.242 47.901))";
    private static final String DEFAULT_DIR = "./test_data/";
    private static final String EXAMPLE_DATE = "2022-03-26T10:00:31.024000Z";

    public static void main(String[] args) throws IOException, ParseException, FactoryException, TransformException, URISyntaxException, InterruptedException {

        // read args from input
        Arguments processedArgs = new Arguments(args);
        Optional<String> wktPolygon = Optional.ofNullable(
                processedArgs.getOptionalString("-a")
        );

        if (wktPolygon.isEmpty()){
            printUsage();
            System.exit(1);
        }

        // optional args
        String dir = processedArgs.getOptionalString("-o");
        final String collection = "sentinel-2-l2a";
        Optional<String> datetime = Optional.ofNullable(
                processedArgs.getOptionalString("-d")
        );

        // set default if not present
        if (dir == null) dir = DEFAULT_DIR;

        // time for performance measurement
        long start, stop;

        PlanetaryComputer pcClient = new PlanetaryComputerImpl();
        StacClient stacClient = pcClient.getStacClient();

        // search for items
        QueryParameter parameter = new QueryParameter();
        parameter.addCollection(collection);
        parameter.setIntersects(wktToGeoJson(wktPolygon.get()));
        parameter.setLimit(1);
        datetime.ifPresent(parameter::setDatetime);

        start = System.currentTimeMillis();
        var itemCollection = stacClient.search(parameter);
        stop = System.currentTimeMillis();
        System.out.printf("stacClient.search() took %dms%n", stop - start);

        if (itemCollection.getItems().size() == 0) {
            return;
        }

        var item = itemCollection.getItems().get(0);

        // get relevant bands
        Optional<Asset> b04Asset = item.getAsset("B04");
        Optional<Asset> b08Asset = item.getAsset("B08");
        if(b04Asset.isEmpty() || b08Asset.isEmpty()) {
            System.out.println("could not finde assets B04 or B08, terminating");
            return;
        }

        var aoiGeom = wktToJtsGeometry(wktPolygon.get());
        aoiGeom.setSRID(4326);

        // get geotiff from planetary computer
        start = System.currentTimeMillis();
        GridCoverage2D coverageB04 = pcClient.getCroppedCoverage(b04Asset.get(), aoiGeom);
        GridCoverage2D coverageB08 = pcClient.getCroppedCoverage(b08Asset.get(), aoiGeom);
        stop = System.currentTimeMillis();
        System.out.printf("pcClient.getCroppedCoverage() took %dms%n", stop - start);

        // calculate ndvi from b04 and b08
        GridCoverage2D coverageNdvi = calcCoverageNdvi(coverageB08, coverageB04);


        // output files
        String filename = item.getId() + "_" + "ndvi";

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
        writer.write(coverageNdvi, null);
        writer.dispose();

        File fileGeoJson = new File(dir + filename +".geojson");
        String geoJsonContent = FeatureConverter.toStringValue(wktToGeoJson(wktPolygon.get()));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileGeoJson));
        bufferedWriter.write(geoJsonContent);
        bufferedWriter.close();

        System.out.println("please be patient, GeoTiffWriter needs some time to free up the resources.");

    }

    private static void printUsage() {
        System.out.println(
                "Usage: -a areaOfInterest [-d datetime -o outputDirectory] ");
        System.out.printf("%n" +
                "-a     area of interest as well know text (required), coordinate reference system: WGS84 (EPSG:4326)%n" +
                "-d     datetime of the datetime of the desired entry in the following format: 2018-02-12T23:20:50Z%n" +
                "-o     output directory for the GeoTif and GeoJson file%n"
        );

        System.out.printf("%nExample: -a \"%s\" -d \"%s\" -o \"%s\"%n",
                EXAMPLE_WKT,
                EXAMPLE_DATE,
                DEFAULT_DIR
                );

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

    private static GridCoverage2D calcCoverageNdvi(GridCoverage2D nir, GridCoverage2D red){

        var rasterNIR = nir.getRenderedImage().getData();
        var rasterRed = red.getRenderedImage().getData();

        if (
                rasterNIR.getMinX() != rasterRed.getMinX()
                        || rasterNIR.getMinY() != rasterRed.getMinY()
                        || rasterNIR.getNumBands() != rasterRed.getNumBands()
                        || rasterNIR.getHeight() != rasterRed.getHeight()
                        || rasterNIR.getWidth() != rasterRed.getWidth()
        ) {
            throw new IllegalArgumentException("given not computable");
        }

        int numBands = rasterNIR.getNumBands();
        int height = rasterNIR.getHeight();
        int width = rasterNIR.getWidth();

        int[] pixelRowNIR = new int[width * numBands];
        int[] pixelRowRed = new int[width * numBands];
        float[][] matrixNdvi = new float[height][width];
        for (int i = 0; i < height; i++) {
            rasterNIR.getPixels(rasterNIR.getMinX(), rasterNIR.getMinY() + i, width, 1, pixelRowNIR);
            rasterRed.getPixels(rasterRed.getMinX(), rasterRed.getMinY() + i, width, 1, pixelRowRed);

            for (int k=0; k<pixelRowNIR.length; k++){
                float valNIR, valRed;
                valNIR = pixelRowNIR[k];
                valRed = pixelRowRed[k];
                matrixNdvi[i][k] = ( valNIR - valRed ) / ( valNIR + valRed );

            }
        }

        var factory = new GridCoverageFactory();
        var envelop = nir.getEnvelope();

        return factory.create(
                "ndvi",
                matrixNdvi,
                envelop
        );
    }
}
