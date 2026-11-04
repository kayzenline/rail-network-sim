package railnet.response.models;

import java.util.Objects;

/**
 * Immutable load snapshot returned by a query.
 * Build it when answering a request. Domain objects do not keep this type as a field.
 */
public final class LoadInfoResponse {
    /**
     * The unique ID of the load.
     */
    private final String loadId;
    /**
     * The type of the load i.e. exactly "Passenger" or "Freight" or "RefrigeratedFreight" or "Technician"
     */
    private final String type;

    public LoadInfoResponse(String loadId, String type) {
        this.loadId = loadId;
        this.type = type;
    }

    public String getLoadId() {
        return loadId;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof LoadInfoResponse other) {
            return Objects.equals(this.type, other.getType()) && Objects.equals(this.loadId, other.getLoadId());
        }
        return false;
    }

    @Override
    public String toString() {
        return "LoadInfoResponse [loadId=" + loadId + ", type=" + type + "]";
    }
}
