package at.ac.tuwien.ba.pcc;


import io.github11904212.java.stac.client.StacClient;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.search.ItemCollection;

import java.io.IOException;

/**
 * signs the resources of the planetary computer with a sas token to make them retrievable.
 * for more information @see <a href="https://planetarycomputer.microsoft.com/docs/concepts/sas/">SAS-token</a>
 */
public interface PlanetaryComputerClient extends StacClient {


    /**
     * gets the instance of {@link StacClient} which refers to the planetary computer.
     * @return the instance
     */
    StacClient getStacClientInstance();

    /**
     * signs an {@link Item} with a sas token, all {@link Asset}s of the item will be replaced by {@link SignedAsset}
     * @param item to sign
     * @return the same item but with signed assets
     * @throws IOException if an error occurs.
     */
    Item sign(Item item) throws IOException;

    /**
     * signs all {@link Item}s of an {@link ItemCollection} with a sas token,
     * all {@link Asset}s of each {@link Item} the item will be replaced by {@link SignedAsset}
     * @param itemCollection to sign
     * @return the same itemCollection but with signed assets
     * @throws IOException if an error occurs.
     */
    ItemCollection sign(ItemCollection itemCollection) throws IOException;

    /**
     * signs a single {@link Asset}
     * @param asset to sign
     * @return a {@link SignedAsset}
     * @throws IOException if an error occurs.
     */
    SignedAsset sign(Asset asset) throws IOException;

}
