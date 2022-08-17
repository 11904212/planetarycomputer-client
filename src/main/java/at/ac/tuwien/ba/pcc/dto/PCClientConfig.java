package at.ac.tuwien.ba.pcc.dto;

import at.ac.tuwien.ba.pcc.exceptions.MalformedConfigurationException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * contains all important settings for a PlanetaryComputerClient.
 */
public class PCClientConfig {

    private final URL stacEndpoint;
    private final URL sasEndpoint;
    private final String subscriptionKey;

    /**
     * creat a configuration for a {@link at.ac.tuwien.ba.pcc.PlanetaryComputerClient}
     * @param stacEndpoint the {@link URL} of the planetary computer api.
     * @param sasEndpoint the {@link URL} of planetary computer sas api, for signing the resources.
     * @param subscriptionKey an optional @see <a href="https://planetarycomputer.microsoft.com/account/request">subscription key</a> for the sasEndpoint.
     *                        can be null.
     */
    public PCClientConfig(
            URL stacEndpoint,
            URL sasEndpoint,
            String subscriptionKey
    ) {
        this.stacEndpoint = stacEndpoint;
        this.sasEndpoint = sasEndpoint;
        this.subscriptionKey = subscriptionKey;
    }

    /**
     * creat a configuration for a {@link at.ac.tuwien.ba.pcc.PlanetaryComputerClient}
     * @param stacEndpoint the {@link URL} of the planetary computer api.
     * @param sasEndpoint the {@link URL} of planetary computer sas api, for signing the resources.
     */
    public PCClientConfig(
            URL stacEndpoint,
            URL sasEndpoint
    ) {
        this(stacEndpoint, sasEndpoint, null);
    }

    /**
     * the URL of the stac-api.
     * @return the {@link URL} of the api.
     */
    public URL getStacEndpoint() {
        return stacEndpoint;
    }

    /**
     * the URL of the sas-api.
     * @return the {@link URL} of the api.
     */
    public URL getSasEndpoint() {
        return sasEndpoint;
    }

    /**
     * the subscription key, can be null.
     * @return the subscription key.
     */
    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    /**
     * a default config of the planetarycomputer.
     * @return the default {@link PCClientConfig}.
     */
    public static PCClientConfig defaultConfig(){
        try {
            return new PCClientConfig(
                    new URL("https://planetarycomputer.microsoft.com/api/stac/v1/"),
                    new URL("https://planetarycomputer.microsoft.com/api/sas/v1/")
            );
        } catch (MalformedURLException e) {
            throw new MalformedConfigurationException(
                    "predefined URLs are malformed, pleas inform the maintainer of this project. " +
                            "You can supply a custom configuration as workaround.", e
            );
        }
    }
}
