package at.ac.tuwien.ba.pcc.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SignedLink {

    private String href;
    private ZonedDateTime msftExpiry;

    public String getHref() {
        return href;
    }

    @JsonSetter("href")
    public void setHref(String href) {
        this.href = href;
    }

    public ZonedDateTime getMsftExpiry() {
        return msftExpiry;
    }

    @JsonSetter("msft:expiry")
    public void setMsftExpiry(String msftExpiry) {
        this.msftExpiry = ZonedDateTime
                .parse(msftExpiry, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withZoneSameInstant(ZoneId.of("UTC"));
    }

    public void setMsftExpiry(ZonedDateTime expiry) {
        this.msftExpiry = expiry;
    }
}
