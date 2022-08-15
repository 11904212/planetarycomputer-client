package at.ac.tuwien.ba.pcc;

import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;

import java.io.IOException;


public interface ResourceSigner {

    Item signInPlace(Item item) throws IOException;

    SignedAsset sign(Asset asset) throws IOException;

}
