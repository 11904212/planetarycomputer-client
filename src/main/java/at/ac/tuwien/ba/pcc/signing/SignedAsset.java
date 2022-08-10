package at.ac.tuwien.ba.pcc.signing;

import io.github11904212.java.stac.client.core.Asset;

import java.time.ZonedDateTime;

public interface SignedAsset extends Asset {

    ZonedDateTime getExpiry();
}
