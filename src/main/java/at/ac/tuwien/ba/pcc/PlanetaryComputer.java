package at.ac.tuwien.ba.pcc;


import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Asset;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

public interface PlanetaryComputer {

    GridCoverage2D getCoverage(Asset asset) throws IOException;

    GridCoverage2D getCroppedCoverage(Asset asset, Geometry geometryAoi) throws IOException, FactoryException, TransformException;

    StacClient getStacClient();

}
