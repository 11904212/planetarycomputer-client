package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import at.ac.tuwien.ba.pcc.signing.impl.TokenManagerImpl;
import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.core.Catalog;
import at.ac.tuwien.ba.stac.client.core.Collection;
import at.ac.tuwien.ba.stac.client.core.Item;
import at.ac.tuwien.ba.stac.client.impl.StacClientImpl;
import at.ac.tuwien.ba.stac.client.search.ItemCollection;
import at.ac.tuwien.ba.stac.client.search.dto.QueryParameter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class PlanetaryComputerImpl implements PlanetaryComputer {

    private final static String PC_ENDPOINT = "https://planetarycomputer.microsoft.com/api/stac/v1/";


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
    public Collection getCollection(String id) throws IOException {
        return stacClient.getCollection(id);
    }

    @Override
    public Item getItem(String collectionId, String itemId) throws IOException {
        var item = stacClient.getItem(collectionId, itemId);
        tokenManager.signInPlace(item);
        return item;
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
