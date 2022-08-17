package at.ac.tuwien.ba.pcc.impl;

import at.ac.tuwien.ba.pcc.SignedAsset;
import at.ac.tuwien.ba.pcc.dto.SasToken;
import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.java.stac.client.core.Item;
import io.github11904212.java.stac.client.core.impl.AssetImpl;
import io.github11904212.java.stac.client.core.impl.ItemImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ResourceSignerTest {

    private ResourceSigner resourceSigner;

    @Mock
    private TokenManager mockTokenManager;

    private static final String DEFAULT_STORAGE = "storage1";
    private static final String DEFAULT_CONTAINER = "container1";

    private final Asset dummyAsset1 = new AssetImpl(
            String.format("https://%s%s/%s/asset1.tif",
                    DEFAULT_STORAGE,
                    ResourceSigner.BLOB_STORAGE_DOMAIN,
                    DEFAULT_CONTAINER
            ),
            "asset1",
            "asset1",
            "image",
            Collections.emptyList()
    );

    private final Asset dummyAsset2 = new AssetImpl(
            String.format("https://%s%s/%s/asset2.tif",
                    DEFAULT_STORAGE,
                    ResourceSigner.BLOB_STORAGE_DOMAIN,
                    DEFAULT_CONTAINER
            ),
            "asset2",
            "asset2",
            "image",
            Collections.emptyList()
    );


    @BeforeEach
    void initialize() {
        resourceSigner = new ResourceSigner(mockTokenManager);

    }

    @AfterEach
    void cleanUp() {
    }

    @Test
    void signInPlace_whenItemUnsigned_expectSignedItem() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        setupTokenMock(DEFAULT_STORAGE, DEFAULT_CONTAINER, token1, date1);

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

        verify(mockTokenManager, times(2)).getToken(DEFAULT_STORAGE, DEFAULT_CONTAINER);

    }

    @Test
    void sign_whenSignedAssetValid_expectNoDoubleSigning() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().plusMinutes(30);

        setupTokenMock(DEFAULT_STORAGE, DEFAULT_CONTAINER, token1, date1);

        var signedAsset1 = resourceSigner.sign(dummyAsset1);
        var signedAsset2 = resourceSigner.sign(signedAsset1);

        assertThat(signedAsset2.getHref()).containsOnlyOnce(token1);
    }

    @Test
    void sign_whenSignedAssetIsExpired_expectNewSigning() throws Exception {
        String token1 = "token1";
        var date1 = ZonedDateTime.now().minusMinutes(30);
        setupTokenMock(DEFAULT_STORAGE, DEFAULT_CONTAINER, token1, date1);

        String token2 = "token2";
        var date2 = ZonedDateTime.now().plusMinutes(30);
        setupTokenMock(DEFAULT_STORAGE, DEFAULT_CONTAINER, token2, date2);

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

        var notBlobAsset = new AssetImpl(
                String.format("https://storage1%s/container1/asset1.tif", ".not.a.blob.storage"),
                "notBlobAsset",
                "notBlobAsset",
                "image",
                Collections.emptyList()
        );

        var signedAsset = resourceSigner.sign(notBlobAsset);

        assertThat(signedAsset.getHref()).isEqualTo(notBlobAsset.getHref());

        verify(mockTokenManager, never()).getToken(any(), any());

    }

    @Test
    void sign_whenAssetHrefHasNoStorage_expectException() throws IOException {

        var malformedHrefAsset = new AssetImpl(
                String.format("https://%s/container1/asset1.tif", ResourceSigner.BLOB_STORAGE_DOMAIN),
                "malformedHrefAsset",
                "malformedHrefAsset",
                "image",
                Collections.emptyList()
        );

        verify(mockTokenManager, times(0)).getToken(any(), any());

        assertThatThrownBy(() -> resourceSigner.sign(malformedHrefAsset))
                .isInstanceOf(MalformedURLException.class)
                .hasMessageContaining("storage")
        ;

    }

    @Test
    void sign_whenAssetHrefHasNoContainer_expectException() throws IOException {

        var malformedHrefAsset = new AssetImpl(
                String.format("https://storage1%s/asset1.tif", ResourceSigner.BLOB_STORAGE_DOMAIN),
                "malformedHrefAsset",
                "malformedHrefAsset",
                "image",
                Collections.emptyList()
        );

        verify(mockTokenManager, times(0)).getToken(any(), any());

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

    private void setupTokenMock(String storage, String container, String token, ZonedDateTime date) throws IOException {
        when(mockTokenManager.getToken(storage, container))
                .thenReturn(new SasToken(token, date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
    }

}
