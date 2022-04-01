package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.stac.client.Asset;
import at.ac.tuwien.ba.stac.client.Item;
import at.ac.tuwien.ba.stac.client.ItemCollection;
import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.search.QueryParameter;
import at.ac.tuwien.ba.stac.client.search.SortDirection;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;
import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.GeoJsonObject;
import mil.nga.sf.wkt.GeometryReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class PlanetaryComputerImpl implements PlanetaryComputer{

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1/sign";
    private final static String PC_ENDPOINT = "https://planetarycomputer.microsoft.com/api/stac/v1/";

    private final ObjectMapper mapper;
    private final StacClient stacClient;
    private final URL urlPcEndpoint;

    public PlanetaryComputerImpl() throws MalformedURLException {

        this.urlPcEndpoint = new URL(PC_ENDPOINT);

        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.stacClient = new StacClient(this.urlPcEndpoint);

    }


    public static void main(String[] args) throws IOException, ParseException, FactoryException, TransformException {

        var start = System.currentTimeMillis();
        //TODO find a other solution
        System.setProperty("org.geotools.referencing.forceXY", "true");

        var pcClient = new PlanetaryComputerImpl();
        //var wktPolygon = "POLYGON((15.14474246290308 48.125505654482964, 15.144740310902085 48.12568573568895, 15.144738298763158 48.12580142263035, 15.144717465597664 48.125953315247074, 15.144707779052617 48.126070165590036, 15.146010149034772 48.12623761981437, 15.147465978725792 48.126666765538296, 15.147096999632566 48.127126292374115, 15.147353624798829 48.12710164514268, 15.147549790959433 48.12704527712296, 15.147667582390431 48.12700699514024, 15.147917353797382 48.126865484239126, 15.148143481729862 48.12671663938023, 15.14825752943781 48.12663921846345, 15.148275270278493 48.12657708683634, 15.148260583963674 48.12654673081544, 15.148291524724753 48.12647923700685, 15.14830842463369 48.12640647346418, 15.148302252227976 48.1263010643032, 15.148226589489626 48.12628506800544, 15.147793095541541 48.126202158960375, 15.147416532429377 48.12611989843675, 15.14474246290308 48.125505654482964))";
        var wktPolygon = "POLYGON((15.444348509999998 48.247984419999995,15.444368259999997 48.24799091000003,15.44473393 48.248146959999985,15.44496923 48.24826841000001,15.44526512 48.24850010999998,15.445482799999999 48.248664160000004,15.44564391 48.248776969999994,15.44564997 48.24878221,15.444984560000002 48.24903871999999,15.4435836 48.24957014,15.44293155 48.249794759999986,15.44272478 48.24982803000003,15.44258721 48.24980571999998,15.442390759999999 48.24971973999999,15.442028020000002 48.24952769999999,15.44183799 48.24942764999997,15.44170078 48.249375490000006,15.441494170000002 48.24931169999999,15.44122826 48.24926796,15.44111949 48.249262079999994,15.44097925 48.24925786,15.44068194 48.24926707,15.44036654 48.24931638999999,15.440163310000003 48.249353740000004,15.439943769999998 48.24941430999999,15.439533919999999 48.249540079999974,15.43882697 48.24978543999998,15.43830827 48.24992277999999,15.43835965 48.24985090999999,15.43845105 48.249710170000014,15.438675409999998 48.24951331,15.43883932 48.249392889999996,15.43895762 48.24931993000001,15.43912528 48.24926511000001,15.439362789999999 48.24920186,15.43992434 48.24902713,15.44203443 48.248572629999984,15.442952819999999 48.24834107000001,15.444348509999998 48.247984419999995))";
        var geom = pcClient.wktToJtsGeometry(wktPolygon);

        List<String> selectedCollections = new ArrayList<>();
        selectedCollections.add("sentinel-2-l2a");

/*        List<Item> items = pcClient.getItems(selectedCollections, wktPolygon, 0, 1);
        if(items.isEmpty()) return;
        Item item = items.get(0);*/

        Item item = pcClient.getCloudFreeItem(selectedCollections, wktPolygon);

        Optional<Asset> visualAsset = item.getAsset("visual");
        if(visualAsset.isEmpty()) return;

        GridCoverage2D coverage = pcClient.getCoverage(visualAsset.get());

        CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem targetCRS = coverage.getCoordinateReferenceSystem();
        MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS);

        var geomTargetCRS = JTS.transform(geom, mathTransform);
        Operations ops = new Operations(null);
        var cropedCoverage =  (GridCoverage2D) ops.crop(coverage, geomTargetCRS);

        File file = new File("/home/martin/Dokumente/Ausbildung/TU_Wien/ba/test_data/test_crop/test6.tif");
        GeoTiffWriter writer = new GeoTiffWriter(file);
        writer.write(cropedCoverage, null);
        writer.dispose();

        var stop = System.currentTimeMillis();
        System.out.printf("took %dms%n", stop - start);

    }

    private SignedLink signHref(String href) throws IOException {

        //TODO: add option for subscription-key and fetch token for collections on first request

        String uriStr = SAS_ENDPOINT + "?href=" + href;

        return mapper.readValue(URI.create(uriStr).toURL(), SignedLink.class);

    }


    @Override
    public List<Item> getItems(List<String> collectionIds, String wktAoi, int page, int size) throws IOException {
        // TODO: finde solution for page. CQL?
        QueryParameter parameter = new QueryParameter();
        parameter.setIntersects(this.wktToGeoJson(wktAoi));
        parameter.setCollections(collectionIds);
        parameter.setLimit(size);
        parameter.addSortBy("datetime", SortDirection.DESC);
        ItemCollection collection;
        try {
            collection = this.stacClient.search(parameter);
        } catch (URISyntaxException | InterruptedException e) {
            //TODO use StacClientException
            throw new IOException(e);
        }
        return collection.getItems();
    }

    @Override
    public List<Item> getItems(List<String> collectionIds, String wktAoi) throws IOException {
        return getItems(collectionIds, wktAoi, 0, 10);
    }

    @Override
    public Item getItem(String wktAoi, Date date) {
        // TODO: maks no sens in context of multi collections
        return null;
    }

    @Override
    public GridCoverage2D getCoverage(Asset asset) throws IOException{
        SignedLink signedLink = this.signHref(asset.getHref());

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

        return reader.read(null);
    }

    public Item getCloudFreeItem(List<String> collectionIds, String wktAoi) throws IOException {

        QueryParameter parameter = new QueryParameter();
        parameter.setIntersects(this.wktToGeoJson(wktAoi));
        parameter.setCollections(collectionIds);
        parameter.setLimit(10);
        ItemCollection collection;
        try {
            collection = this.stacClient.search(parameter);
        } catch (URISyntaxException | InterruptedException e) {
            //TODO use StacClientException
            throw new IOException(e);
        }
        return collection.getItems().get(collection.getItems().size() - 1);
    }

    private org.locationtech.jts.geom.Geometry wktToJtsGeometry(String wkt) throws ParseException {
        var hint = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(hint);
        WKTReader reader = new WKTReader(geometryFactory);

        return reader.read(wkt);
    }

    private GeoJsonObject wktToGeoJson(String wkt) throws IOException {
        var geom = GeometryReader.readGeometry(wkt);
        return FeatureConverter.toGeometry(geom);
    }
}
