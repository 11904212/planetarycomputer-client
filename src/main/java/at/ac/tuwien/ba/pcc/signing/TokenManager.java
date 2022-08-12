package at.ac.tuwien.ba.pcc.signing;

import at.ac.tuwien.ba.pcc.dto.SasToken;

import java.io.IOException;

public interface TokenManager {

    SasToken getToken(String account, String container) throws IOException;

}
