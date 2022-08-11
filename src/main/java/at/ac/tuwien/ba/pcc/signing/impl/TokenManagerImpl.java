package at.ac.tuwien.ba.pcc.signing.impl;

import at.ac.tuwien.ba.pcc.dto.SasToken;
import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import at.ac.tuwien.ba.pcc.dto.SignedLink;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class TokenManagerImpl implements TokenManager {

    public static final String BLOB_STORAGE_DOMAIN = ".blob.core.windows.net";

    private final String sasEndpoint;
    private final ObjectMapper mapper;
    private final String subscriptionKey;
    private final Map<String, SasToken> tokenCache = new HashMap<>();

    public TokenManagerImpl(String sasEndpoint, String subscriptionKey) {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.sasEndpoint = sasEndpoint;
        this.subscriptionKey = subscriptionKey;
    }

    @Override
    public Item signInPlace(Item item) throws IOException {

        var assets = item.getAssets();
        for (var entry : assets.entrySet()){
            entry.setValue(sign(entry.getValue()));
        }

        return item;
    }


    @Override
    public SignedAsset sign(Asset asset) throws IOException {
        var signedLink = signHref(asset.getHref());
        return new SignedAssetImpl(
                asset,
                signedLink
        );
    }


    private SignedLink signHref(String href) throws IOException {

        var url = new URL(href);

        if (!isBlobStorageDomain(url)) {
            return new SignedLink(
                    url.toString(),
                    ZonedDateTime.now().plusYears(1)
            );
        }

        String account = extractAccount(url);
        String container = extractContainer(url);

        var sasToken = getToken(account, container);

        var urlStr = url.toString();

        if (url.getQuery() != null){
            var query = "?" + url.getQuery();
            urlStr = urlStr.replace(query, "");
        }

        return new SignedLink(
                urlStr + "?" + sasToken.getToken(),
                sasToken.getMsftExpiry()
        );
    }

    private SasToken getToken(String account, String container) throws IOException  {

        String tokenKey = account + "/" + container;

        if (
                tokenCache.containsKey(tokenKey)   // has token
                        && tokenCache.get(tokenKey)
                        .getMsftExpiry().isAfter(ZonedDateTime.now().plusMinutes(1)) // token still valid
        ) {
            return tokenCache.get(tokenKey);
        }


        StringBuilder query = new StringBuilder(sasEndpoint);
        query.append("token/" );
        query.append(account);
        query.append("/");
        query.append(container);

        if (this.subscriptionKey != null) {
            query.append("?subscription-key=");
            query.append(this.subscriptionKey);
        }

        URL url = new URL(query.toString());
        var newToken  = mapper.readValue(url, SasToken.class);
        tokenCache.put(tokenKey, newToken);
        return newToken;
    }

    private boolean isBlobStorageDomain(URL url){
        return url.getHost().endsWith(BLOB_STORAGE_DOMAIN);
    }

    private String extractAccount(URL url) throws MalformedURLException {
        var parts = url.getHost().split(BLOB_STORAGE_DOMAIN);
        if (parts.length < 1) {
            throw new MalformedURLException(String.format("the given asset url did not contain a storage account. %s", url));
        }
        return parts[0];
    }

    private String extractContainer(URL url) throws MalformedURLException {
        var parts = url.getPath().split("/");
        if (parts.length < 2) {
            throw new MalformedURLException(String.format("the given asset url did not contain a container. %s", url));
        }
        return parts[1];
    }

}
