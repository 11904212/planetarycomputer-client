package at.ac.tuwien.ba.pcc;


import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.core.Item;
import at.ac.tuwien.ba.stac.client.search.ItemCollection;

import java.io.IOException;
import java.util.Optional;

public interface PlanetaryComputer extends StacClient {


    StacClient getStacClientInstance();

    Item sign(Item item) throws IOException;

    ItemCollection sign(ItemCollection itemCollection) throws IOException;

    SignedAsset sign(Asset asset, String collectionId) throws IOException;

}
