package at.ac.tuwien.ba.pcc.impl;

import at.ac.tuwien.ba.pcc.SignedAsset;
import io.github11904212.java.stac.client.core.Asset;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

class SignedAssetImpl implements SignedAsset {

    private final String href;
    private final String title;
    private final String description;
    private final String type;
    private final List<String> roles;
    private final ZonedDateTime expiry;

    public SignedAssetImpl(Asset asset, SignedLink signedLink){
        this.href = signedLink.getHref();
        this.expiry = signedLink.getMsftExpiry();
        this.title = asset.getTitle().orElse(null);
        this.description = asset.getDescription().orElse(null);
        this.type = asset.getType().orElse(null);
        this.roles = asset.getRoles();
    }

    @Override
    public String getHref() {
        return href;
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public ZonedDateTime getExpiry() {
        return expiry;
    }

    @Override
    public String toString() {
        return String.format("Asset{href: %s}", this.getHref());
    }

}
