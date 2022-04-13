package at.ac.tuwien.ba.pcc.signing;

import at.ac.tuwien.ba.stac.client.core.Item;

import java.io.IOException;


public interface TokenManager {

    void signInPlace(Item item) throws IOException;

}
