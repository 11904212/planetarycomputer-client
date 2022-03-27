import at.ac.tuwien.ba.stac.client.Item;
import at.ac.tuwien.ba.stac.client.ItemCollection;
import at.ac.tuwien.ba.stac.client.QueryParameter;
import at.ac.tuwien.ba.stac.client.StacClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReader;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;
import mil.nga.sf.Geometry;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.Polygon;
import mil.nga.sf.geojson.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlanetaryComputer {

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1/sign";

    private final ObjectMapper mapper;

    public PlanetaryComputer() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {

        var pcClient = new PlanetaryComputer();

        URL stacEndpoint = new URL("https://planetarycomputer.microsoft.com/api/stac/v1/");
        StacClient client = new StacClient(stacEndpoint);

        QueryParameter queryParameter = new QueryParameter();
        queryParameter.addCollection("sentinel-2-l2a");
        queryParameter.setDatetime("2022-02-13/2022-04-15");
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

        queryParameter.setIntersects(
                featureCollection.getFeature(0).getGeometry()
        );

        ItemCollection searchRes = client.search(queryParameter);

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

        SignedLink signedLink = pcClient.signHref(bestItem.getAssets().get("visual").getHref());

        System.out.println(signedLink.getHref());

        URL assetURL = URI.create(signedLink.getHref()).toURL();
        ImageInputStream cogStream = new CogImageInputStreamSpi().createInputStreamInstance(assetURL, false, null);

        CogImageReader reader = new CogImageReader(new CogImageReaderSpi());
        reader.setInput(cogStream);

        CogImageReadParam param = new CogImageReadParam();

        param.setSourceRegion(new Rectangle(500, 500, 100, 100));
        param.setRangeReaderClass(HttpRangeReader.class);
        long start = System.currentTimeMillis();
        BufferedImage cogImage = reader.read(0, param);
        long stop = System.currentTimeMillis();

        System.out.printf("took %dms", stop-start);


    }

    private SignedLink signHref(String href) throws IOException {

        String uriStr = SAS_ENDPOINT + "?href=" + href;

        return mapper.readValue(URI.create(uriStr).toURL(), SignedLink.class);

    }

    private double[] calcBbox(Polygon polygon) {
        double[] bbox = new double[4];
        polygon.getCoordinates();

        return bbox;
    }
}
