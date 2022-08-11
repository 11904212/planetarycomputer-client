package at.ac.tuwien.ba.pcc.signing;

import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;

import java.io.IOException;


public interface TokenManager {

    // TODO: add clone based methode
    Item signInPlace(Item item) throws IOException;

    SignedAsset sign(Asset asset) throws IOException;

}
