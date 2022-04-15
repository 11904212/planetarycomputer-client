package at.ac.tuwien.ba.pcc.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SasToken {
    private String token;
    private ZonedDateTime msftExpiry;

    public String getToken() {
        return token;
    }

    @JsonSetter("token")
    public void setToken(String token) {
        this.token = token;
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
}
