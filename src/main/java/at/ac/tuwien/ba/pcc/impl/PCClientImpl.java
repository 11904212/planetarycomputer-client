package at.ac.tuwien.ba.pcc.impl;

import at.ac.tuwien.ba.pcc.PlanetaryComputerClient;
import at.ac.tuwien.ba.pcc.dto.PCClientConfig;
import at.ac.tuwien.ba.pcc.SignedAsset;
import io.github11904212.java.stac.client.StacClient;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Catalog;
import io.github11904212.java.stac.client.core.Collection;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.impl.StacClientImpl;
import io.github11904212.java.stac.client.search.ItemCollection;
import io.github11904212.java.stac.client.search.dto.QueryParameter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public class PCClientImpl implements PlanetaryComputerClient {

    private final StacClient stacClient;
    private final ResourceSigner resourceSigner;

    public PCClientImpl(PCClientConfig config) {

        this.stacClient = new StacClientImpl(config.getStacEndpoint());

        var tokenManager = new TokenManager(config.getSasEndpoint(), config.getSubscriptionKey());

        this.resourceSigner = new ResourceSigner(tokenManager);

    }

    public PCClientImpl() {
        this(PCClientConfig.defaultConfig());
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
            return Optional.of(sign(item.get()));
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
        return resourceSigner.signInPlace(item);
    }

    @Override
    public ItemCollection sign(ItemCollection itemCollection) throws IOException {
        for (var item : itemCollection.getItems()){
            resourceSigner.signInPlace(item);
        }
        return itemCollection;
    }

    @Override
    public SignedAsset sign(Asset asset) throws IOException {
        return resourceSigner.sign(asset);
    }
}
