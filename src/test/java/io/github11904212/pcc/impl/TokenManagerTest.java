package io.github11904212.pcc.impl;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenManagerTest {

    private MockWebServer mockApi;

    private TokenManager tokenManager;

    private static final String TOKEN_TEMPLATE = "{" +
            "\"msft:expiry\": \"%s\"," +
            "\"token\": \"%s\"" +
            "}";

    private final String dummyAccount = "storage1";
    private final String dummyContainer = "container1";
    private final String dummyToken = "token1";


    @BeforeEach
    void initialize() throws IOException {
        mockApi = new MockWebServer();
        mockApi.start();

        tokenManager = new TokenManager(mockApi.url("").uri().toURL(), null);

    }

    @AfterEach
    void cleanUp() throws IOException {
        mockApi.shutdown();
    }


    @Test
    @Timeout(1)
    void getToken_whenSameAccountAndContainer_expectOneApiCall() throws Exception {
        var date1 = ZonedDateTime.now().plusMinutes(30);

        mockTokenResponse(dummyToken, date1);

        tokenManager.getToken(dummyAccount, dummyContainer);
        tokenManager.getToken(dummyAccount, dummyContainer);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("only one query should be performed")
                .isEqualTo(1);

        RecordedRequest request = mockApi.takeRequest();
        assertThat(request.getPath())
                .isEqualTo(String.format("/token/%s/%s", dummyAccount, dummyContainer));

        assertThat(request.getMethod())
                .withFailMessage("client did not use a GET request")
                .isEqualTo("GET");
    }

    @Test
    @Timeout(1)
    void sign_whenAssetsOfDifferentContainers_expectMultipleApiCalls() throws Exception {
        var date1 = ZonedDateTime.now().plusMinutes(30);
        var newContainer = "container2";

        mockTokenResponse(dummyToken, date1);
        mockTokenResponse("token2", date1);

        tokenManager.getToken(dummyAccount, dummyContainer);
        tokenManager.getToken(dummyAccount, newContainer);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("two queries should be performed")
                .isEqualTo(2);

        RecordedRequest request1 = mockApi.takeRequest();
        assertThat(request1.getPath())
                .endsWith(dummyContainer);

        RecordedRequest request2 = mockApi.takeRequest();
        assertThat(request2.getPath())
                .endsWith(newContainer);

    }

    @Test
    @Timeout(1)
    void sign_whenTokenIsExpired_expectRequestNewToken() throws Exception {

        var date1 = ZonedDateTime.now().minusMinutes(30);
        mockTokenResponse(dummyToken, date1);

        var token2 = "token2";
        var date2 = ZonedDateTime.now().plusMinutes(30);
        mockTokenResponse(token2, date2);

        var sasToken1 =tokenManager.getToken(dummyAccount, dummyContainer);
        var sasToken2 = tokenManager.getToken(dummyAccount, dummyContainer);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("two queries should be performed")
                .isEqualTo(2);

        assertThat(sasToken1.getToken()).isEqualTo(dummyToken);

        assertThat(sasToken2.getToken()).isEqualTo(token2);

    }

    @Test
    @Timeout(1)
    void sign_whenSubscriptionKeyProvided_expectUsageOfKey() throws Exception {

        var date1 = ZonedDateTime.now().minusMinutes(30);
        mockTokenResponse(dummyToken, date1);

        var subscriptionKey = "key1234";

        var subTokenManager = new TokenManager(mockApi.url("").url(), subscriptionKey);

        subTokenManager.getToken(dummyAccount, dummyContainer);

        assertThat(mockApi.getRequestCount())
                .withFailMessage("only one should be performed")
                .isEqualTo(1);

        RecordedRequest request1 = mockApi.takeRequest();
        assertThat(request1.getPath())
                .contains("subscription-key=" + subscriptionKey);

    }

    @Test
    @Timeout(1)
    void signInPlace_whenApiReturnsError_expectException() {

        mockApi.enqueue(new MockResponse()
                .setResponseCode(404));

        assertThatThrownBy(() -> tokenManager.getToken(dummyAccount, dummyContainer))
                .isInstanceOf(IOException.class);
    }

    @Test
    @Timeout(1)
    void signInPlace_whenApiReturnsEmptyBody_expectException() {

        mockApi.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> tokenManager.getToken(dummyAccount, dummyContainer))
                .isInstanceOf(IOException.class);

    }

    @Test
    @Timeout(1)
    void signInPlace_whenApiReturnsInvalidDate_expectException() {

        String body1 = String.format(TOKEN_TEMPLATE, "2022-08-10T25:57:20Z", "token1");

        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> tokenManager.getToken(dummyAccount, dummyContainer))
                .isInstanceOf(IOException.class);

    }

    @Test
    @Timeout(1)
    void signInPlace_whenApiReturnsInvalidToken_expectException() {

        String body1 = String.format(TOKEN_TEMPLATE, "2022-08-10T12:57:20Z", "\"null\"");

        mockApi.enqueue(new MockResponse()
                .setBody(body1)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> tokenManager.getToken(dummyAccount, dummyContainer))
                .isInstanceOf(IOException.class);

    }

    private void mockTokenResponse(String token, ZonedDateTime date){

        String body = String.format(TOKEN_TEMPLATE, date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), token);
        mockApi.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

    }

}
