package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.stac.client.Asset;
import at.ac.tuwien.ba.stac.client.Item;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface PlanetaryComputer {

    List<Item> getItems(List<String> collectionIds ,String wktAoi, int page, int size) throws IOException;

    List<Item> getItems(List<String> collectionIds, String wktAoi) throws IOException;

    Item getItem(String wktAoi, Date date);

    GridCoverage2D getCoverage(Asset asset) throws IOException;

}
