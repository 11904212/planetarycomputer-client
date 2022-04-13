package at.ac.tuwien.ba.pcc.signing;

import at.ac.tuwien.ba.stac.client.core.Asset;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;

public class TokenManagerImpl implements TokenManager{

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1";
    private final ObjectMapper mapper;
    private String subscriptionKey;

    public TokenManagerImpl(){

        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public TokenManagerImpl(String subscriptionKey) {
        this();
        this.subscriptionKey = subscriptionKey;
    }


    @Override
    public SignedLink signHref(String href) throws IOException {
        return this.directSignHref(href);
    }

    @Override
    public SignedLink signAsset(Asset asset) throws IOException {

        return this.directSignHref(asset.getHref());
    }

    private SignedLink directSignHref(String href) throws IOException {
        StringBuilder query = new StringBuilder(SAS_ENDPOINT);
        query.append("/sign?href=" );
        query.append(href);

        if (this.subscriptionKey != null) {
            query.append("&subscription-key=");
            query.append(this.subscriptionKey);
        }

        URL url = new URL(query.toString());

        return mapper.readValue(url, SignedLink.class);

    }

    private SasToken requestToken(String collectionId) throws IOException  {

        StringBuilder query = new StringBuilder(SAS_ENDPOINT);
        query.append("/token/" );
        query.append(collectionId);

        if (this.subscriptionKey != null) {
            query.append("?subscription-key=");
            query.append(this.subscriptionKey);
        }

        URL url = new URL(query.toString());
        return mapper.readValue(url, SasToken.class);
    }

}
