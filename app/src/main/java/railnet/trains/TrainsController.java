package railnet.trains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import railnet.load.Freight;
import railnet.load.Load;
import railnet.load.Passenger;
import railnet.load.RefrigeratedFreight;
import railnet.load.Technician;
import railnet.exceptions.InvalidRouteException;
import railnet.response.models.*;
import railnet.stations.CentralStation;
import railnet.stations.FreightStation;
import railnet.stations.PassengerStation;
import railnet.stations.SupplyStation;
import railnet.stations.TrainStation;
import railnet.track.Track;
import railnet.utils.TrackType;

/**
 * Owns the stations, tracks, and trains, and advances the network one tick at a time.
 */
public class TrainsController {
    private List<TrainStation> stations = new ArrayList<>();
    private List<Track> tracks = new ArrayList<>();
    private List<Train> trains = new ArrayList<>();

    public void createStation(String stationId, String type, double x, double y) {
        TrainStation station = null;

        switch (type) {
            case "PassengerStation":
                station = new PassengerStation(stationId, type, x, y);
                break;
            case "FreightStation":
                station = new FreightStation(stationId, type, x, y);
                break;
            case "CentralStation":
                station = new CentralStation(stationId, type, x, y);
                break;
            case "SupplyStation":
                station = new SupplyStation(stationId, type, x, y);
                break;
            default:
                break;
        }

        if (station != null) {
            stations.add(station);
        }
    }

    public void createTrack(String trackId, String fromStationId, String toStationId) {
        createTrack(trackId, fromStationId, toStationId, false);
    }

    public void createTrain(String trainId, String type, String stationId, List<String> route)
            throws InvalidRouteException {
        Train train = null;
        TrainStation startStation = getStation(stationId);
        double x = startStation.getStationPosition().getX();
        double y = startStation.getStationPosition().getY();

        switch (type) {
            case "CommuterTrain":
                train = new CommuterTrain(trainId, type, stationId, route, x, y);
                break;
            case "FreightTrain":
                train = new FreightTrain(trainId, type, stationId, route, x, y);
                break;
            case "MaglevTrain":
                train = new MaglevTrain(trainId, type, stationId, route, x, y);
                break;
            case "RepairTrain":
                train = new RepairTrain(trainId, type, stationId, route, x, y);
                break;
            default:
                break;
        }

        if (train != null) {
            boolean cyclical = isCyclical(route);
            if (cyclical && !train.cyclicalRoute()) {
                throw new InvalidRouteException("Train cannot travel on cyclical route");
            }
            train.setCyclical(cyclical);
            List<TrainStation> routeStations = new ArrayList<>();
            for (String stId : route) {
                TrainStation st = getStation(stId);
                if (st != null) {
                    routeStations.add(st);
                }
            }
            train.setRouteStations(routeStations);
            trains.add(train);
        }
    }

    public List<String> listStationIds() {
        List<String> stationIds = new ArrayList<>();

        for (TrainStation station : stations) {
            stationIds.add(station.getStationId());
        }
        return stationIds;
    }

    public List<String> listTrackIds() {
        List<String> trackIds = new ArrayList<>();
        for (Track track : tracks) {
            trackIds.add(track.getTrackId());
        }
        return trackIds;
    }

    public List<String> listTrainIds() {
        List<String> trainIds = new ArrayList<>();
        for (Train train : trains) {
            trainIds.add(train.getTrainId());
        }
        return trainIds;
    }

    // Helper to get train by Id
    private Train getTrain(String trainId) {
        for (Train train : trains) {
            if (train.getTrainId().equals(trainId)) {
                return train;
            }
        }
        return null;
    }

    public TrainInfoResponse getTrainInfo(String trainId) {
        Train train = getTrain(trainId);
        TrainInfoResponse trainInfo = new TrainInfoResponse(
                trainId, train.getLocation(), train.getType(),
                train.getPosition(), getLoadInfos(train.getCurrentLoads()));
        return trainInfo;
    }

    public StationInfoResponse getStationInfo(String stationId) {
        TrainStation station = getStation(stationId);
        StationInfoResponse stationInfo = new StationInfoResponse(stationId, station.getStationType(),
                station.getStationPosition(),
                getLoadInfos(station.getLoads()), getStationTrainInfos(stationId));
        return stationInfo;
    }

    public TrackInfoResponse getTrackInfo(String trackId) {
        Track track = getTrack(trackId);
        TrackInfoResponse trackInfo = new TrackInfoResponse(trackId, track.getFromStationId(),
                track.getToStationId(), track.getType(), track.getDurability());
        return trackInfo;
    }

    public void simulate() {
        List<Train> sortedTrains = new ArrayList<>(trains);
        Collections.sort(sortedTrains);

        for (Train train : sortedTrains) {

            String targetId = train.getTargetStationId();
            TrainStation targetStation = getStation(targetId);
            boolean isFull = isFull(targetStation);

            Track currTrack;
            if (train.getStationId() != null) {
                currTrack = getTrackBetween(train.getStationId(), targetId);
            } else {
                currTrack = getTrack(train.getLocation());
            }

            if (train.getStationId() != null) {
                if (train.canMove(targetStation, currTrack, isFull)) {
                    getStation(train.getStationId()).embarkLoads(train);
                }
            }

            boolean arrived = train.move(targetStation, currTrack, isFull);

            if (arrived) {
                currTrack.decreaseDur(train.trackDamage());
                train.disEmbarkLoads(train.getStationId());
            }
        }

        // 2. Repair broken tracks by default amount
        for (Track track : tracks) {
            track.repair(1);
        }

        // 3. Update track status to broken or unbroken based on durability
        for (Track track : tracks) {
            track.updateStatus();
        }

        // 4. Deteriorate refrigerated freight
        for (TrainStation station : stations) {
            station.deteriorateLoads();
        }

        for (Train train : trains) {
            train.deteriorateLoads();
        }
    }

    /**
     * Advance the simulation by the given number of ticks.
     */
    public void simulate(int numberOfMinutes) {
        for (int i = 0; i < numberOfMinutes; i++) {
            simulate();
        }
    }

    public void createPassenger(String startStationId, String destStationId, String passengerId) {
        createPassenger(startStationId, destStationId, passengerId, false);
    }

    public void createFreight(String startStationId, String destStationId, String freightId, int weight) {
        TrainStation station = getStation(startStationId);
        Freight freight = new Freight(freightId, destStationId, weight);
        station.addLoad(freight);
    }

    public void createRefrigeratedFreight(String startStationId, String destStationId, String freightId, int weight,
            int minsTillSpoil) {
        TrainStation station = getStation(startStationId);
        RefrigeratedFreight refrigeratedFreight = new RefrigeratedFreight(freightId, destStationId, weight,
                minsTillSpoil);
        station.addLoad(refrigeratedFreight);
    }

    public void createTrack(String trackId, String fromStationId, String toStationId, boolean isBreakable) {
        TrackType type = isBreakable ? TrackType.UNBROKEN : TrackType.NORMAL;
        tracks.add(new Track(trackId, fromStationId, toStationId, Track.MAX_DURABILITY, type));
    }

    public void createPassenger(String startStationId, String destStationId, String passengerId, boolean isTechnician) {
        TrainStation station = getStation(startStationId);
        Passenger passenger = isTechnician
                ? new Technician(passengerId, destStationId)
                : new Passenger(passengerId, destStationId);
        station.addLoad(passenger);
    }

    // Helper to get station by Id
    private TrainStation getStation(String stationId) {
        for (TrainStation station : stations) {
            if (station.getStationId().equals(stationId)) {
                return station;
            }
        }
        return null;
    }

    // Helper to Check existence of a Track between 2 stations
    public boolean hasTrack(String a, String b) {
        return getTrackBetween(a, b) != null;
    }

    // Helper to check is the route a cyclical route
    public boolean isCyclical(List<String> route) {
        if (route.size() >= 3 && hasTrack(route.get(0), route.get(route.size() - 1))) {
            return true;
        }
        return false;
    }

    // Helper to get Track by Id
    private Track getTrack(String trackId) {
        for (Track track : tracks) {
            if (track.getTrackId().equals(trackId)) {
                return track;
            }
        }
        return null;
    }

    // Helper to get bool of station full or not
    private boolean isFull(TrainStation targetStation) {
        int count = 0;

        for (Train train : trains) {
            if (targetStation.getStationId().equals(train.getStationId())) {
                count++;
            }
        }

        return count >= targetStation.getMaxTrains();
    }

    // Helper to get Track between two stations
    private Track getTrackBetween(String a, String b) {
        for (Track track : tracks) {
            if (track.connects(a, b)) {
                return track;
            }
        }
        return null;
    }

    // Helper to get list of TrainInfoResponse for a station
    private List<TrainInfoResponse> getStationTrainInfos(String stationId) {
        List<TrainInfoResponse> responses = new ArrayList<>();
        for (Train train : trains) {
            if (stationId.equals(train.getStationId())) {
                responses.add(getTrainInfo(train.getTrainId()));
            }
        }
        return responses;
    }

    // Helper to get list of LoadInfoResponse for a station
    private List<LoadInfoResponse> getLoadInfos(List<Load> loads) {
        List<LoadInfoResponse> responses = new ArrayList<>();
        for (Load load : loads) {
            responses.add(new LoadInfoResponse(
                    load.getLoadId(), load.getLoadType()));
        }
        return responses;
    }
}
