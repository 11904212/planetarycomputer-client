package at.ac.tuwien.ba.pcc;

import at.ac.tuwien.ba.stac.client.StacClient;
import at.ac.tuwien.ba.stac.client.core.Catalog;
import at.ac.tuwien.ba.stac.client.core.Collection;
import at.ac.tuwien.ba.stac.client.core.Item;
import at.ac.tuwien.ba.stac.client.impl.StacClientImpl;
import at.ac.tuwien.ba.stac.client.search.ItemCollection;
import at.ac.tuwien.ba.stac.client.search.dto.QueryParameter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class PlanetaryComputerImpl implements PlanetaryComputer {

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1/sign";
    private final static String PC_ENDPOINT = "https://planetarycomputer.microsoft.com/api/stac/v1/";

    private final ObjectMapper mapper;
    private final StacClient stacClient;
    private final TokenManager tokenManager;
    private final URL urlPcEndpoint;

    public PlanetaryComputerImpl() throws MalformedURLException {

        this.urlPcEndpoint = new URL(PC_ENDPOINT);

        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.stacClient = new StacClientImpl(this.urlPcEndpoint);

        this.tokenManager = new TokenManager();

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
        for (var item : itemCollection.getItems()){
            tokenManager.signInPlace(item);
        }
        return itemCollection;
    }
}
