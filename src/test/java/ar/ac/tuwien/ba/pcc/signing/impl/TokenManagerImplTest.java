package ar.ac.tuwien.ba.pcc.signing.impl;

import at.ac.tuwien.ba.pcc.signing.SignedAsset;
import at.ac.tuwien.ba.pcc.signing.TokenManager;
import at.ac.tuwien.ba.pcc.signing.impl.TokenManagerImpl;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.core.impl.AssetImpl;
import io.github11904212.java.stac.client.core.impl.ItemImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenManagerImplTest {

    private MockWebServer mockApi;

    private TokenManager tokenManager;

    private final Asset dummyAsset1 = new AssetImpl(
            String.format("https://storage1%s/container1/asset1.tif", TokenManagerImpl.BLOB_STORAGE_DOMAIN),
            "asset1",
            "asset1",
            "image",
            Collections.emptyList()
    );

    private final Asset dummyAsset2 = new AssetImpl(
            String.format("https://storage1%s/container1/asset2.tif", TokenManagerImpl.BLOB_STORAGE_DOMAIN),
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

        tokenManager = new TokenManagerImpl(mockApi.url("").uri().toString(), null);

    }

    @AfterEach
    void cleanUp() throws IOException {
        mockApi.shutdown();
    }


    @Test
    void sign_whenAssetsOfSameContainer_expectOneApiCall() throws Exception {
        var token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        String body = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token1);

        mockApi.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));


        tokenManager.sign(dummyAsset1);
        tokenManager.sign(dummyAsset2);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("only one query should be performed")
                .isEqualTo(1);

        RecordedRequest request = mockApi.takeRequest();
        assertThat(request.getPath())
                .isEqualTo("/token/storage1/container1");

        assertThat(request.getMethod())
                .withFailMessage("client did not use a GET request")
                .isEqualTo("GET");
    }

    @Test
    void sign_whenAssetsOfDifferentContainers_expectMultipleApiCalls() throws Exception {
        var token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        String body1 = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token1);
        String body2 = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "token2");

        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));
        mockApi.enqueue(new MockResponse()
                .setBody(body2)
                .addHeader("Content-Type", "application/json"));

        var asset2 = new AssetImpl(
                dummyAsset1.getHref().replace("container1", "container2"),
                "asset2",
                "some text",
                "type2",
                Collections.emptyList()
        );

        tokenManager.sign(dummyAsset1);
        tokenManager.sign(asset2);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("two queries should be performed")
                .isEqualTo(2);

        RecordedRequest request1 = mockApi.takeRequest();
        assertThat(request1.getPath())
                .endsWith("container1");

        RecordedRequest request2 = mockApi.takeRequest();
        assertThat(request2.getPath())
                .endsWith("container2");

    }

    @Test
    void sign_whenTokenIsExpired_expectRequestNewToken() throws Exception {
        var token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        String body1 = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token1);
        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));

        var token2 = "token2";
        var date2 = ZonedDateTime.now().plusMinutes(30);
        String body2 = String.format(TOKEN_TEMPLATE, date2.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token2);
        mockApi.enqueue(new MockResponse()
                .setBody(body2)
                .addHeader("Content-Type", "application/json"));

        tokenManager.sign(dummyAsset1);
        var asset2 = tokenManager.sign(dummyAsset1);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("two queries should be performed")
                .isEqualTo(2);

        assertThat(asset2.getHref())
                .contains(token2)
                .doesNotContain(token1)
        ;

        assertThat(asset2.getExpiry()).isEqualTo(date2);

    }

    @Test
    void signInPlace_whenApiReturnsError_expectException() {

        mockApi.enqueue(new MockResponse()
                .setResponseCode(404));

        var item1 = creatItem();

        assertThatThrownBy(() -> tokenManager.signInPlace(item1))
                .isInstanceOf(IOException.class);
    }

    @Test
    void signInPlace_whenApiReturnsEmptyBody_expectException() {

        mockApi.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        var item1 = creatItem();

        assertThatThrownBy(() -> tokenManager.signInPlace(item1))
                .isInstanceOf(IOException.class);

    }

    @Test
    void signInPlace_whenApiReturnsInvalidDate_expectException() {

        String body1 = String.format(TOKEN_TEMPLATE, "2022-08-10T25:57:20Z", "token1");

        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));

        var item1 = creatItem();

        assertThatThrownBy(() -> tokenManager.signInPlace(item1))
                .isInstanceOf(IOException.class);

    }

    @Test
    void signInPlace_whenApiReturnsInvalidToken_expectException() {

        String body1 = String.format(TOKEN_TEMPLATE, "2022-08-10T12:57:20Z", "\"null\"");

        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));

        var item1 = creatItem();

        assertThatThrownBy(() -> tokenManager.signInPlace(item1))
                .isInstanceOf(IOException.class);

    }


    @Test
    void signInPlace_whenItemUnsigned_expectSignedItem() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        String body = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token1);

        mockApi.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        var item = tokenManager.signInPlace(creatItem());

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

        String body = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token1);

        mockApi.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        var signedAsset1 = tokenManager.sign(dummyAsset1);
        var signedAsset2 = tokenManager.sign(signedAsset1);

        assertThat(signedAsset2.getHref()).containsOnlyOnce(token1);
    }

    @Test
    void sign_whenSignedItemIsExpired_expectNewSigning() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        String body1 = String.format(TOKEN_TEMPLATE, date1.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token1);
        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));

        String token2 = "token2";
        var date2 = ZonedDateTime.now().plusMinutes(30);
        String body2 = String.format(TOKEN_TEMPLATE, date2.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token2);
        mockApi.enqueue(new MockResponse()
                .setBody(body2)
                .addHeader("Content-Type", "application/json"));

        var signedAsset1 = tokenManager.sign(dummyAsset1);
        var signedAsset2 = tokenManager.sign(signedAsset1);

        assertThat(signedAsset2.getHref())
                .doesNotContain(token1)
                .contains(token2)
        ;
        assertThat(signedAsset2.getExpiry()).isEqualTo(date2);

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
}
