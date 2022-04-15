package at.ac.tuwien.ba.pcc.signing.impl;

import at.ac.tuwien.ba.pcc.dto.SasToken;
import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import at.ac.tuwien.ba.pcc.dto.SignedLink;
import at.ac.tuwien.ba.stac.client.core.Asset;
import at.ac.tuwien.ba.stac.client.core.Item;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class TokenManagerImpl implements TokenManager {

    private final static String SAS_ENDPOINT = "https://planetarycomputer.microsoft.com/api/sas/v1";
    private final ObjectMapper mapper;
    private String subscriptionKey;
    private final Map<String, SasToken> tokenCache = new HashMap<>();

    public TokenManagerImpl(){

        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public TokenManagerImpl(String subscriptionKey) {
        this();
        this.subscriptionKey = subscriptionKey;
    }

    @Override
    public Item signInPlace(Item item) throws IOException {
        if (item.getCollection().isPresent()){
            var assets = item.getAssets();
            for (var key : assets.keySet()){
                Asset oldAsset = assets.get(key);

                SignedAsset newAsset = sign(
                        oldAsset,
                        item.getCollection().get()
                );
                assets.put(key, newAsset);
            }
        } else {
            // TODO: could sign by href, but should we? (the performance would be terrible)
        }

        return item;
    }

    @Override
    public SignedAsset sign(Asset asset, String collectionId) throws IOException {
        var signedLink = signWithToken(asset.getHref(), collectionId);
        return new SignedAssetImpl(
                asset,
                signedLink
        );
    }

    @Override
    public SignedAsset sign(Asset asset) throws IOException {
        var signedLink = directSignHref(asset.getHref());
        return new SignedAssetImpl(
                asset,
                signedLink
        );
    }

    private SignedLink signWithToken(String href, String tokenKey) throws IOException {

        if (
                !tokenCache.containsKey(tokenKey)   // no token
                || tokenCache.get(tokenKey)
                        .getMsftExpiry().isBefore(ZonedDateTime.now().plusMinutes(5)) // token expired
        ) {
            var newToken = requestToken(tokenKey);
            tokenCache.put(tokenKey, newToken);
        }

        var sasToken = tokenCache.get(tokenKey);

        var link = new SignedLink();
        link.setHref(href + "?" + sasToken.getToken());
        link.setMsftExpiry(sasToken.getMsftExpiry());
        return link;

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
