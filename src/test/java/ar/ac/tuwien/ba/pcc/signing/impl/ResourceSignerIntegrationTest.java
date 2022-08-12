package ar.ac.tuwien.ba.pcc.signing.impl;

import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.ResourceSigner;
import at.ac.tuwien.ba.pcc.signing.impl.ResourceSignerImpl;
import at.ac.tuwien.ba.pcc.signing.impl.TokenManagerImpl;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.core.impl.AssetImpl;
import io.github11904212.java.stac.client.core.impl.ItemImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceSignerIntegrationTest {

    private MockWebServer mockApi;

    private ResourceSigner resourceSigner;

    private final Asset dummyAsset1 = new AssetImpl(
            String.format("https://storage1%s/container1/asset1.tif", ResourceSignerImpl.BLOB_STORAGE_DOMAIN),
            "asset1",
            "asset1",
            "image",
            Collections.emptyList()
    );

    private final Asset dummyAsset2 = new AssetImpl(
            String.format("https://storage1%s/container1/asset2.tif", ResourceSignerImpl.BLOB_STORAGE_DOMAIN),
            "asset2",
            "asset2",
            "image",
            Collections.emptyList()
    );

    private static final String TOKEN_TEMPLATE = "{" +
            "\"msft:expiry\": \"%s\"," +
            "\"token\": \"%s\"" +
            "}";


    @BeforeEach
    void initialize() throws IOException {
        mockApi = new MockWebServer();
        mockApi.start();

        var tokenManager = new TokenManagerImpl(mockApi.url("").uri().toURL(), null);

        resourceSigner = new ResourceSignerImpl(tokenManager);

    }

    @AfterEach
    void cleanUp() throws IOException {
        mockApi.shutdown();
    }

    @Test
    void signInPlace_whenItemUnsigned_expectSignedItem() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        mockTokenResponse(token1, date1);

        var item = resourceSigner.signInPlace(creatItem());

        assertThat(item.getAssets()).hasSize(2);

        var optAsset1 = item.getAsset("asset1");
        assertThat(optAsset1).isNotEmpty();

        var asset1 = optAsset1.get();
        assertThat(asset1)
                .isInstanceOf(SignedAsset.class);

        var signedAsset1 = (SignedAsset) asset1;
        assertThat(signedAsset1.getHref()).contains(token1);
        assertThat(signedAsset1.getExpiry()).isEqualTo(date1);

    }

    @Test
    void sign_whenSignedAssetValid_expectNoDoubleSigning() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        mockTokenResponse(token1, date1);

        var signedAsset1 = resourceSigner.sign(dummyAsset1);
        var signedAsset2 = resourceSigner.sign(signedAsset1);

        assertThat(signedAsset2.getHref()).containsOnlyOnce(token1);
    }

    @Test
    void sign_whenSignedAssetIsExpired_expectNewSigning() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        mockTokenResponse(token1, date1);

        String token2 = "token2";
        var date2 = ZonedDateTime.now().plusMinutes(30);
        mockTokenResponse(token2, date2);

        var signedAsset1 = resourceSigner.sign(dummyAsset1);
        var signedAsset2 = resourceSigner.sign(signedAsset1);

        assertThat(signedAsset2.getHref())
                .doesNotContain(token1)
                .contains(token2)
        ;
        assertThat(signedAsset2.getExpiry()).isEqualTo(date2);

    }

    @Test
    void sign_whenAssetHrefIsNotABlobStorageUrl_expectSameHref() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        mockTokenResponse(token1, date1);

        var notBlobAsset = new AssetImpl(
                String.format("https://storage1%s/container1/asset1.tif", ".not.a.blob.storage"),
                "notBlobAsset",
                "notBlobAsset",
                "image",
                Collections.emptyList()
        );

        var signedAsset = resourceSigner.sign(notBlobAsset);

        assertThat(signedAsset.getHref()).isEqualTo(notBlobAsset.getHref());

        assertThat(mockApi.getRequestCount())
                .withFailMessage("no request should be performed")
                .isZero();

    }

    @Test
    void sign_whenAssetHrefHasNoStorage_expectException() {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        mockTokenResponse(token1, date1);

        var malformedHrefAsset = new AssetImpl(
                String.format("https://%s/container1/asset1.tif", ResourceSignerImpl.BLOB_STORAGE_DOMAIN),
                "malformedHrefAsset",
                "malformedHrefAsset",
                "image",
                Collections.emptyList()
        );

        assertThatThrownBy(() -> resourceSigner.sign(malformedHrefAsset))
                .isInstanceOf(MalformedURLException.class)
                .hasMessageContaining("storage")
        ;

    }

    @Test
    void sign_whenAssetHrefHasNoContainer_expectException() {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        mockTokenResponse(token1, date1);

        var malformedHrefAsset = new AssetImpl(
                String.format("https://storage1%s/asset1.tif", ResourceSignerImpl.BLOB_STORAGE_DOMAIN),
                "malformedHrefAsset",
                "malformedHrefAsset",
                "image",
                Collections.emptyList()
        );

        assertThatThrownBy(() -> resourceSigner.sign(malformedHrefAsset))
                .isInstanceOf(MalformedURLException.class)
                .hasMessageContaining("container")
        ;

    }



    private Item creatItem(){
        Map<String, Asset> assets = new HashMap<>();
        assets.put(
                "asset1",
                dummyAsset1
        );

        assets.put(
                "asset2",
                dummyAsset2
        );

        return new ItemImpl(
                "1.0.0",
                Collections.emptyList(),
                Collections.emptyList(),
                assets,
                "collection1"
        );
    }

    private void mockTokenResponse(String token, ZonedDateTime date){

        String body = String.format(TOKEN_TEMPLATE, date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token);
        mockApi.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

    }
}
