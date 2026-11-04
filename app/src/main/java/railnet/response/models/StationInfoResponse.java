package railnet.response.models;

import railnet.utils.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable station snapshot returned by a query.
 * Build it when answering a request. Domain objects do not keep this type as a field.
 */
public final class StationInfoResponse {
    /**
     * The unique ID of the station.
     */
    private final String stationId;
    /**
     * The type of the station, i.e.: exactly "CentralStation", "PassengerStation", "FreightStation", or "SupplyStation"
     */
    private final String type;
    /**
     * The position of the station on the map (x and y-coords, see Position.java).
     */
    private final Position position;
    /**
     * A list of info responses of the loads currently present at the station.
     */
    private final List<LoadInfoResponse> loads;
    /**
     * A list of info responses of the trains currently docked at the station.
     */
    private List<TrainInfoResponse> trains;

    /**
     * Constructor for a stationInfoResponse representing a station with no loads or trains present
     */
    public StationInfoResponse(String stationId, String type, Position position) {
        this(stationId, type, position, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Constructor for a stationInfoResponse representing a station with no loads but some trains present
     */
    public StationInfoResponse(String stationId, String type, Position position, List<TrainInfoResponse> trains) {
        this(stationId, type, position, new ArrayList<>(), trains);
    }

    /**
     * Full constructor for a stationInfoResponse
     */
    public StationInfoResponse(String stationId, String type, Position position, List<LoadInfoResponse> loads,
            List<TrainInfoResponse> trains) {
        this.stationId = stationId;
        this.type = type;
        this.position = position;
        this.loads = loads;
        this.trains = trains;
    }

    public String getStationId() {
        return stationId;
    }

    public String getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public List<LoadInfoResponse> getLoads() {
        return loads;
    }

    public List<TrainInfoResponse> getTrains() {
        return trains;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (o instanceof StationInfoResponse other) {
            return Objects.equals(stationId, other.stationId) && Objects.equals(type, other.type)
                    && Objects.equals(position, other.position) && Objects.equals(loads, other.loads)
                    && Objects.equals(trains, other.trains);
        }
        return false;
    }

    @Override
    public String toString() {
        return "StationInfoResponse [stationId=" + stationId + ", type=" + type + ", position=" + position + ", trains="
                + trains + ", loads=" + loads + "]";
    }
}
