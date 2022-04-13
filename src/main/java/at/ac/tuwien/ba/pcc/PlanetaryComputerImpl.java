package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.pcc.signing.SignedLink;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import at.ac.tuwien.ba.pcc.signing.TokenManagerImpl;
import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.impl.StacClientImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class PlanetaryComputerImpl implements PlanetaryComputer {

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1/sign";
    private final static String PC_ENDPOINT = "https://planetarycomputer.microsoft.com/api/stac/v1/";

    private final StacClient stacClient;
    private final TokenManager tokenManager;
    private final URL urlPcEndpoint;

    public PlanetaryComputerImpl() throws MalformedURLException {

        this.urlPcEndpoint = new URL(PC_ENDPOINT);

        this.stacClient = new StacClientImpl(this.urlPcEndpoint);

        this.tokenManager = new TokenManagerImpl();

        //TODO find a other solution
        System.setProperty("org.geotools.referencing.forceXY", "true");

    }


    @Override
    public GridCoverage2D getCoverage(Asset asset) throws IOException {
        SignedLink signedLink = this.tokenManager.signAsset(asset);

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

    @Override
    public GridCoverage2D getCroppedCoverage(Asset asset, Geometry geometryAoi) throws IOException, FactoryException, TransformException {
        var coverage = this.getCoverage(asset);

        CoordinateReferenceSystem sourceCRS;
        if (geometryAoi.getSRID() != 0) {
            sourceCRS = CRS.decode("EPSG:" + geometryAoi.getSRID());
        } else {
            sourceCRS = CRS.decode("EPSG:4326");
        }

        CoordinateReferenceSystem targetCRS = coverage.getCoordinateReferenceSystem();
        MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS);

        var geomTargetCRS = JTS.transform(geometryAoi, mathTransform);
        ReferencedEnvelope envelope = new ReferencedEnvelope(geomTargetCRS.getEnvelopeInternal(), targetCRS);

        Operations ops = new Operations(null);
        return (GridCoverage2D) ops.crop(coverage, envelope);
    }

    @Override
    public StacClient getStacClient() {
        return stacClient;
    }


}
