package at.ac.tuwien.ba.pcc.impl;

import java.time.ZonedDateTime;

class SignedLink {

    private final String href;
    private final ZonedDateTime msftExpiry;

    public SignedLink(
            String href,
            ZonedDateTime msftExpiry
    ) {
        this.href = href;
        this.msftExpiry = msftExpiry;
    }

    public String getHref() {
        return href;
    }

    public ZonedDateTime getMsftExpiry() {
        return msftExpiry;
    }

}
