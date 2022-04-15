package at.ac.tuwien.ba.pcc.signing;

import at.ac.tuwien.ba.stac.client.core.Asset;

import java.time.ZonedDateTime;

public interface SignedAsset extends Asset {

    ZonedDateTime getExpiry();
}
