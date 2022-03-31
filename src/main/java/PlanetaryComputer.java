import at.ac.tuwien.ba.stac.client.Item;
import at.ac.tuwien.ba.stac.client.ItemCollection;
import at.ac.tuwien.ba.stac.client.QueryParameter;
import at.ac.tuwien.ba.stac.client.StacClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;
import mil.nga.sf.geojson.FeatureCollection;
//import mil.nga.sf.geojson.Polygon;
import mil.nga.sf.geojson.Geometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class PlanetaryComputer {

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1/sign";

    private final ObjectMapper mapper;

    public PlanetaryComputer() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, ParseException, FactoryException, TransformException {

        System.setProperty("org.geotools.referencing.forceXY", "true");

        var pcClient = new PlanetaryComputer();

        long start, stop;

        var geom = pcClient.getJtsGeometry();

        URL stacEndpoint = new URL("https://planetarycomputer.microsoft.com/api/stac/v1/");
        StacClient client = new StacClient(stacEndpoint);

        QueryParameter queryParameter = new QueryParameter();
        queryParameter.addCollection("sentinel-2-l2a");
        queryParameter.setDatetime("2022-02-13/2022-04-15");
        queryParameter.setIntersects(pcClient.getGeometry());

        start = System.currentTimeMillis();
        ItemCollection searchRes = client.search(queryParameter);
        stop = System.currentTimeMillis();
        System.out.printf("searchRes took %dms%n", stop-start);

        double bestCC = Double.MAX_VALUE;
        Item bestItem = searchRes.getItems().get(0);

        for(Item item: searchRes.getItems()) {
            double cloudCover = (double) item.getProperties().get("eo:cloud_cover");
            if (cloudCover < bestCC) {
                bestItem = item;
                bestCC = cloudCover;
            }

        }

        System.out.println(bestItem);

        start = System.currentTimeMillis();
        SignedLink signedLink = pcClient.signHref(bestItem.getAssets().get("visual").getHref());
        stop = System.currentTimeMillis();
        System.out.printf("signedLink took %dms%n", stop-start);

        start = System.currentTimeMillis();
        BasicAuthURI cogUri = new BasicAuthURI(signedLink.getHref(), false);
        HttpRangeReader rangeReader =
                new HttpRangeReader(cogUri.getUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH);
        CogSourceSPIProvider input =
                new CogSourceSPIProvider(
                        cogUri,
                        new CogImageReaderSpi(),
                        new CogImageInputStreamSpi(),
                        rangeReader.getClass().getName());

        GeoTiffReader reader = new GeoTiffReader(input);

        GridCoverage2D coverage = reader.read(null);
        Operations ops = new Operations(null);


        var cropedCoverage =  (GridCoverage2D) ops.crop(coverage, geom);

        File file = new File("/home/martin/Dokumente/Ausbildung/TU_Wien/ba/test_data/test_crop/test3.tif");
        GeoTiffWriter writer = new GeoTiffWriter(file);
        writer.write(cropedCoverage, null);
        writer.dispose();


        stop = System.currentTimeMillis();

        System.out.printf("took %dms", stop-start);
    }

    private SignedLink signHref(String href) throws IOException {

        String uriStr = SAS_ENDPOINT + "?href=" + href;

        return mapper.readValue(URI.create(uriStr).toURL(), SignedLink.class);

    }

    private Geometry getGeometry() throws JsonProcessingException {
        String polygonStr = "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {},\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Polygon\",\n" +
                "        \"coordinates\": [\n" +
                "          [\n" +
                "            [\n" +
                "              15.447721481323242,\n" +
                "              48.2544340495978\n" +
                "            ],\n" +
                "            [\n" +
                "              15.447839498519897,\n" +
                "              48.25395900359276\n" +
                "            ],\n" +
                "            [\n" +
                "              15.448188185691832,\n" +
                "              48.25306604802908\n" +
                "            ],\n" +
                "            [\n" +
                "              15.448397397994995,\n" +
                "              48.252240943226624\n" +
                "            ],\n" +
                "            [\n" +
                "              15.448821187019348,\n" +
                "              48.25243739795865\n" +
                "            ],\n" +
                "            [\n" +
                "              15.449244976043701,\n" +
                "              48.252480260708985\n" +
                "            ],\n" +
                "            [\n" +
                "              15.449266433715819,\n" +
                "              48.25255527043557\n" +
                "            ],\n" +
                "            [\n" +
                "              15.4490464925766,\n" +
                "              48.252991039051764\n" +
                "            ],\n" +
                "            [\n" +
                "              15.448815822601318,\n" +
                "              48.25367325950906\n" +
                "            ],\n" +
                "            [\n" +
                "              15.44850468635559,\n" +
                "              48.25474836332529\n" +
                "            ],\n" +
                "            [\n" +
                "              15.447909235954285,\n" +
                "              48.25445190838409\n" +
                "            ],\n" +
                "            [\n" +
                "              15.447721481323242,\n" +
                "              48.2544340495978\n" +
                "            ]\n" +
                "          ]\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        FeatureCollection featureCollection = new ObjectMapper().readValue(polygonStr, FeatureCollection.class);


        return featureCollection.getFeature(0).getGeometry();
    }

    private Polygon getJtsGeometry() throws ParseException, FactoryException, TransformException {
        var wktPolygon = "POLYGON((15.14474246290308 48.125505654482964, 15.144740310902085 48.12568573568895, 15.144738298763158 48.12580142263035, 15.144717465597664 48.125953315247074, 15.144707779052617 48.126070165590036, 15.146010149034772 48.12623761981437, 15.147465978725792 48.126666765538296, 15.147096999632566 48.127126292374115, 15.147353624798829 48.12710164514268, 15.147549790959433 48.12704527712296, 15.147667582390431 48.12700699514024, 15.147917353797382 48.126865484239126, 15.148143481729862 48.12671663938023, 15.14825752943781 48.12663921846345, 15.148275270278493 48.12657708683634, 15.148260583963674 48.12654673081544, 15.148291524724753 48.12647923700685, 15.14830842463369 48.12640647346418, 15.148302252227976 48.1263010643032, 15.148226589489626 48.12628506800544, 15.147793095541541 48.126202158960375, 15.147416532429377 48.12611989843675, 15.14474246290308 48.125505654482964))";
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);

        var polygon = (Polygon) reader.read(wktPolygon);

        CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32633");
        MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS);
        Polygon p1 = (Polygon) JTS.transform(polygon, mathTransform);

        return p1;
    }
}
