package at.ac.tuwien.ba.pcc.signing.impl;

import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.ResourceSigner;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;

public class ResourceSignerImpl implements ResourceSigner {

    public static final String BLOB_STORAGE_DOMAIN = ".blob.core.windows.net";

    private final TokenManager tokenManager;


    public ResourceSignerImpl(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
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

        var sasToken = tokenManager.getToken(account, container);

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
        if (parts.length < 3) {
            throw new MalformedURLException(String.format("the given asset url did not contain a container. %s", url));
        }
        return parts[1];
    }

}
