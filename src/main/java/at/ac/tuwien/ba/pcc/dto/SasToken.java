package at.ac.tuwien.ba.pcc.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SasToken {
    private final String token;
    private final ZonedDateTime msftExpiry;

    @JsonCreator
    public SasToken(
            @JsonProperty(value = "token", required = true) String token,
            @JsonProperty(value = "msft:expiry", required = true) String msftExpiry
    ) {
        this.token = token;
        this.msftExpiry = ZonedDateTime
                .parse(msftExpiry, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withZoneSameInstant(ZoneId.of("UTC"));
    }

    public String getToken() {
        return token;
    }

    public ZonedDateTime getMsftExpiry() {
        return msftExpiry;
    }
}
