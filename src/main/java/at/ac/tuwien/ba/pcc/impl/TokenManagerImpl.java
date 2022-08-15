package at.ac.tuwien.ba.pcc.impl;

import at.ac.tuwien.ba.pcc.dto.SasToken;
import at.ac.tuwien.ba.pcc.TokenManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

class TokenManagerImpl implements TokenManager {

    private final URL sasEndpoint;
    private final ObjectMapper mapper;
    private final String subscriptionKey;
    private final Map<String, SasToken> tokenCache = new HashMap<>();


    public TokenManagerImpl(URL sasEndpoint, String subscriptionKey) {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.sasEndpoint = sasEndpoint;
        this.subscriptionKey = subscriptionKey;
    }

    public SasToken getToken(String account, String container) throws IOException {

        String tokenKey = account + "/" + container;

        if (
                tokenCache.containsKey(tokenKey)   // has token
                        && tokenCache.get(tokenKey)
                        .getMsftExpiry().isAfter(ZonedDateTime.now().plusMinutes(1)) // token still valid
        ) {
            return tokenCache.get(tokenKey);
        }


        StringBuilder query = new StringBuilder(sasEndpoint.toString());
        String tokenPath = String.format("token/%s/%s", account, container);
        query.append(tokenPath);

        if (this.subscriptionKey != null) {
            query.append("?subscription-key=");
            query.append(this.subscriptionKey);
        }

        URL url = new URL(query.toString());
        var newToken  = mapper.readValue(url, SasToken.class);
        tokenCache.put(tokenKey, newToken);
        return newToken;
    }
}
