package at.ac.tuwien.ba.pcc;

import io.github11904212.java.stac.client.core.Asset;

import java.time.ZonedDateTime;

/**
 * A STAC-asset that has be signed with a SAS-token.
 */
public interface SignedAsset extends Asset {

    /**
     * get the expiry datetime of the used {@link at.ac.tuwien.ba.pcc.dto.SasToken}.
     * @return the expiry datetime.
     */
    ZonedDateTime getExpiry();
}
