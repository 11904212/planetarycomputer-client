package at.ac.tuwien.ba.pcc.impl;

import at.ac.tuwien.ba.pcc.PlanetaryComputerClient;
import at.ac.tuwien.ba.pcc.dto.PCClientConfig;
import io.github11904212.java.stac.client.search.dto.QueryParameter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PCClientIntegrationTest {

    private static final String TOKEN_TEMPLATE = "{" +
            "\"msft:expiry\": \"%s\"," +
            "\"token\": \"%s\"" +
            "}";

    private static final String DUMMY_TOKEN = "token1";

    private PlanetaryComputerClient pcClient;

    private MockWebServer mockStacApi;
    private MockWebServer mockSasApi;

    @BeforeEach
    void initialize() throws IOException {
        mockStacApi = new MockWebServer();
        mockSasApi = new MockWebServer();
        mockStacApi.start();
        mockSasApi.start();

        var config = new PCClientConfig(
                mockStacApi.url("").url(),
                mockSasApi.url("").url()
        );

        pcClient = new PCClientImpl(config);

    }

    @AfterEach
    void cleanUp() throws IOException {
        mockStacApi.shutdown();
        mockSasApi.shutdown();
    }

    @Test
    @Timeout(1) //prevents blocking if the MockWebServer is not correctly set up
    void getCatalog_whenStacApiResponseValid_expectValidCatalog() throws IOException {
        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/catalog.json"))
                .addHeader("Content-Type", "application/json"));

        var catalog = pcClient.getCatalog();

        assertThat(catalog).isNotNull();

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isZero();
    }

    @Test
    @Timeout(1)
    void getCatalog_whenStacApiResponseInvalid_expectValidCatalog() {
        mockStacApi.enqueue(new MockResponse()
                        .setResponseCode(500)
        );

        assertThatThrownBy(() -> pcClient.getCatalog())
                .isInstanceOf(IOException.class);

    }

    @Test
    @Timeout(1)
    void getCollection_whenStacApiResponseValid_expectValidCatalog() throws Exception {
        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/collection.json"))
                .addHeader("Content-Type", "application/json"));

        var collection = pcClient.getCollection("1234");

        assertThat(collection).isNotNull();

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        var request = mockStacApi.takeRequest();
        assertThat(request.getPath()).contains("1234");
        assertThat(mockSasApi.getRequestCount()).isZero();

    }

    @Test
    @Timeout(1)
    void getCollection_whenStacApiResponseInvalid_expectValidCatalog() {
        mockStacApi.enqueue(new MockResponse()
                .setResponseCode(500)
        );

        assertThatThrownBy(() -> pcClient.getCollection("1234"))
                .isInstanceOf(IOException.class);

    }

    @Test
    @Timeout(1)
    void getItem_whenBlobStorageItem_expectSignedItem() throws IOException {

        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/item_blob-storage.json"))
                .addHeader("Content-Type", "application/json"));

        mockTokenResponse(DUMMY_TOKEN, ZonedDateTime.now().plusMinutes(30));

        var item = pcClient.getItem("1234", "5678");
        assertThat(item).isNotEmpty();

        var optAsset1 = item.get().getAsset("visual");
        assertThat(optAsset1).isNotEmpty();
        var asset1 = optAsset1.get();

        assertThat(asset1).isInstanceOf(SignedAssetImpl.class);
        assertThat(asset1.getHref()).contains(DUMMY_TOKEN);

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isEqualTo(1);

    }

    @Test
    @Timeout(1)
    void getItem_whenNonBlobStorageItem_expectNotSignedItem() throws IOException {

        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/item_non-blob-storage.json"))
                .addHeader("Content-Type", "application/json"));

        mockTokenResponse(DUMMY_TOKEN, ZonedDateTime.now().plusMinutes(30));

        var item = pcClient.getItem("1234", "5678");
        assertThat(item).isNotEmpty();

        var optAsset1 = item.get().getAsset("visual");
        assertThat(optAsset1).isNotEmpty();
        var asset1 = optAsset1.get();

        assertThat(asset1).isInstanceOf(SignedAssetImpl.class);
        assertThat(asset1.getHref()).doesNotContain(DUMMY_TOKEN);

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isZero();

    }

    @Test
    @Timeout(1)
    void getItem_whenItemNotFound_expectEmptyItem() throws IOException {
        mockStacApi.enqueue(new MockResponse()
                .setResponseCode(404)
        );


        var item = pcClient.getItem("1234", "5678");
        assertThat(item).isEmpty();

    }

    @Test
    @Timeout(1)
    void search_whenBlobStorageItemCollection_expectSignedIC() throws Exception {

        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/itemcollection_blob-storage.json"))
                .addHeader("Content-Type", "application/json"));

        mockTokenResponse(DUMMY_TOKEN, ZonedDateTime.now().plusMinutes(30));

        var itemCollection = pcClient.search(new QueryParameter());
        assertThat(itemCollection.getItems()).hasSize(1);

        var optAsset1 = itemCollection.getItems().get(0).getAsset("visual");
        assertThat(optAsset1).isNotEmpty();
        var asset1 = optAsset1.get();

        assertThat(asset1).isInstanceOf(SignedAssetImpl.class);
        assertThat(asset1.getHref()).contains(DUMMY_TOKEN);

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isEqualTo(1);

    }

    @Test
    @Timeout(1)
    void search_whenNonBlobStorageItemCollection_expectNotSignedIC() throws Exception {

        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/itemcollection_non-blob-storage.json"))
                .addHeader("Content-Type", "application/json"));

        mockTokenResponse(DUMMY_TOKEN, ZonedDateTime.now().plusMinutes(30));

        var itemCollection = pcClient.search(new QueryParameter());
        assertThat(itemCollection.getItems()).hasSize(1);

        var optAsset1 = itemCollection.getItems().get(0).getAsset("visual");
        assertThat(optAsset1).isNotEmpty();
        var asset1 = optAsset1.get();

        assertThat(asset1).isInstanceOf(SignedAssetImpl.class);
        assertThat(asset1.getHref()).doesNotContain(DUMMY_TOKEN);

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isZero();

    }


    @Test
    @Timeout(1)
    void signAsset_whenBlobStorageAsset_expectSignedAsset() throws Exception {

        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/item_blob-storage.json"))
                .addHeader("Content-Type", "application/json"));

        mockTokenResponse(DUMMY_TOKEN, ZonedDateTime.now().plusMinutes(30));

        var stacClient = pcClient.getStacClientInstance();

        var item = stacClient.getItem("1234", "5678");
        assertThat(item).isNotEmpty();

        var optAsset1 = item.get().getAsset("visual");
        assertThat(optAsset1).isNotEmpty();
        var asset1 = optAsset1.get();

        var signedAsset = pcClient.sign(asset1);

        assertThat(signedAsset).isInstanceOf(SignedAssetImpl.class);
        assertThat(signedAsset.getHref()).contains(DUMMY_TOKEN);

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isEqualTo(1);

    }

    @Test
    @Timeout(1)
    void signAsset_whenNonBlobStorageAsset_expectSignedAsset() throws Exception {

        mockStacApi.enqueue(new MockResponse()
                .setBody(readTextFromResource("stac_examples/item_non-blob-storage.json"))
                .addHeader("Content-Type", "application/json"));

        mockTokenResponse(DUMMY_TOKEN, ZonedDateTime.now().plusMinutes(30));

        var stacClient = pcClient.getStacClientInstance();

        var item = stacClient.getItem("1234", "5678");
        assertThat(item).isNotEmpty();

        var optAsset1 = item.get().getAsset("visual");
        assertThat(optAsset1).isNotEmpty();
        var asset1 = optAsset1.get();

        var signedAsset = pcClient.sign(asset1);

        assertThat(signedAsset.getHref()).doesNotContain(DUMMY_TOKEN);

        assertThat(mockStacApi.getRequestCount()).isEqualTo(1);
        assertThat(mockSasApi.getRequestCount()).isZero();

    }


    @Test
    void getStacClientInstance_whenNoConfigProvided_expectValidDefaultConfig() {
        var pcClient = new PCClientImpl();
        var stacClient = pcClient.getStacClientInstance();

        assertThat(stacClient).isNotNull();

    }

    private String readTextFromResource(String resource) throws IOException {
        File file = new File(ClassLoader.getSystemResource(resource).getFile());
        return Files.readString(file.toPath());
    }

    private void mockTokenResponse(String token, ZonedDateTime date){

        String body = String.format(TOKEN_TEMPLATE,
                date.plusMinutes(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                token
        );
        mockSasApi.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

    }

}
