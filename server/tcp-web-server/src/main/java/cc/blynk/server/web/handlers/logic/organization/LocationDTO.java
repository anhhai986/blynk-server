package cc.blynk.server.web.handlers.logic.organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class LocationDTO {

    public final String siteName;

    public final int deviceId;

    @JsonCreator
    public LocationDTO(@JsonProperty("siteName") String siteName,
                       @JsonProperty("deviceId") int deviceId) {
        this.siteName = siteName;
        this.deviceId = deviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocationDTO that = (LocationDTO) o;
        return Objects.equals(siteName, that.siteName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteName);
    }
}
