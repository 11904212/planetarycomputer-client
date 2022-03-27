import com.fasterxml.jackson.annotation.JsonSetter;

public class SignedLink {

    private String href;
    private String msftExpiry;

    public String getHref() {
        return href;
    }

    @JsonSetter("href")
    public void setHref(String href) {
        this.href = href;
    }

    public String getMsftExpiry() {
        return msftExpiry;
    }

    @JsonSetter("msft:expiry")
    public void setMsftExpiry(String msftExpiry) {
        this.msftExpiry = msftExpiry;
    }
}
