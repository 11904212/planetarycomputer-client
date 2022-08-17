package io.github11904212.pcc;

import io.github11904212.java.stac.client.core.Asset;
import io.github11904212.pcc.dto.SasToken;

import java.time.ZonedDateTime;

/**
 * A STAC-asset that has be signed with a SAS-token.
 */
public interface SignedAsset extends Asset {

    /**
     * get the expiry datetime of the used {@link SasToken}.
     * @return the expiry datetime.
     */
    ZonedDateTime getExpiry();
}
