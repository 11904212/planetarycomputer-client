package at.ac.tuwien.ba.pcc;


import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import io.github11904212.java.stac.client.StacClient;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.search.ItemCollection;

import java.io.IOException;

/**
 * signs the resources of the planetary computer with a sas token to make them accessible.
 * for more information @see <a href="https://planetarycomputer.microsoft.com/docs/concepts/sas/</a>
 */
public interface PlanetaryComputer extends StacClient {


    /**
     * gets the instance of {@link StacClient} which refers to the planetary computer.
     * @return the instance
     */
    StacClient getStacClientInstance();

    /**
     * signs an {@link Item} with a sas token, all {@link Asset}s of the item will by replaces by {@link SignedAsset}
     * @param item to sign
     * @return the same item but with signed assets
     * @throws IOException
     */
    Item sign(Item item) throws IOException;

    /**
     * signs all {@link Item}s of an {@link ItemCollection} with a sas token,
     * all {@link Asset}s of each {@link Item} the item will by replaces by {@link SignedAsset}
     * @param itemCollection to sign
     * @return the same itemCollection but with signed assets
     * @throws IOException
     */
    ItemCollection sign(ItemCollection itemCollection) throws IOException;

    /**
     * signs a single {@link Asset}
     * @param asset to sign
     * @param collectionId the collection of the asset
     * @return a {@link SignedAsset}
     * @throws IOException
     */
    SignedAsset sign(Asset asset, String collectionId) throws IOException;

}
