package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import at.ac.tuwien.ba.pcc.signing.impl.TokenManagerImpl;
import io.github11904212.java.stac.client.StacClient;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Catalog;
import io.github11904212.java.stac.client.core.Collection;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.impl.StacClientImpl;
import io.github11904212.java.stac.client.search.ItemCollection;
import io.github11904212.java.stac.client.search.dto.QueryParameter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

public class PlanetaryComputerImpl implements PlanetaryComputer {

    private static final String PC_ENDPOINT = "https://planetarycomputer.microsoft.com/api/stac/v1/";


    private final StacClient stacClient;
    private final TokenManager tokenManager;

    public PlanetaryComputerImpl() throws MalformedURLException {

        this.stacClient = new StacClientImpl(new URL(PC_ENDPOINT));

        this.tokenManager = new TokenManagerImpl();

    }

    @Override
    public Catalog getCatalog() throws IOException {
        return stacClient.getCatalog();
    }

    @Override
    public Optional<Collection> getCollection(String id) throws IOException {
        return stacClient.getCollection(id);
    }

    @Override
    public Optional<Item> getItem(String collectionId, String itemId) throws IOException {
        var item = stacClient.getItem(collectionId, itemId);
        if (item.isPresent()) {
            return Optional.of(tokenManager.signInPlace(item.get()));
        }
        return Optional.empty();
    }

    @Override
    public ItemCollection search(QueryParameter queryParameter) throws IOException, URISyntaxException, InterruptedException {
        var itemCollection = stacClient.search(queryParameter);
        return sign(itemCollection);
    }

    @Override
    public StacClient getStacClientInstance() {
        return stacClient;
    }

    @Override
    public Item sign(Item item) throws IOException {
        return tokenManager.signInPlace(item);
    }

    @Override
    public ItemCollection sign(ItemCollection itemCollection) throws IOException {
        for (var item : itemCollection.getItems()){
            tokenManager.signInPlace(item);
        }
        return itemCollection;
    }

    @Override
    public SignedAsset sign(Asset asset, String collectionId) throws IOException {
        return tokenManager.sign(asset, collectionId);
    }
}
