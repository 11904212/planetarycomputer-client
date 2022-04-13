package at.ac.tuwien.ba.pcc.signing;

import at.ac.tuwien.ba.stac.client.core.Asset;

import java.io.IOException;

public interface TokenManager {

    SignedLink signHref(String href) throws IOException;

    SignedLink signAsset(Asset asset) throws IOException;
}
