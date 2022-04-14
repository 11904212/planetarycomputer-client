package at.ac.tuwien.ba.pcc.signing;

import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.core.Item;

import java.io.IOException;


public interface TokenManager {

    // TODO: add clone based methode
    Item signInPlace(Item item) throws IOException;

    SignedAsset sign(Asset asset, String collectionId) throws IOException;

    SignedAsset sign(Asset asset) throws IOException;

}
