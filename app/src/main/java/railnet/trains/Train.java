package railnet.trains;

import java.util.ArrayList;
import java.util.List;

import railnet.load.Load;
import railnet.stations.TrainStation;
import railnet.track.Track;
import railnet.utils.Position;

public abstract class Train implements Comparable<Train> {
    private static final double FREIGHT_SPEED_REDUCTION_FACTOR = 0.0001;

    private final String trainId;
    private final String type;
    private String stationId;
    private String location; // The ID of the station the train is docked at, or the track the train is on.
    private final List<String> route; // The list of stations.
    private Position position;
    private final int maximumLoad;
    private final double baseSpeed;
    private int direction = 1; // 1 means forward, -1 means backward
    private int routeIndex;
    private boolean isCyclical;
    private final List<Load> currentLoads = new ArrayList<>();
    private List<TrainStation> routeStations = new ArrayList<>();

    public Train(String trainId, String type, String stationId, List<String> route,
            double x, double y, int maximumLoad, double speed) {
        this.trainId = trainId;
        this.type = type;
        this.stationId = stationId;
        this.location = stationId;
        this.route = new ArrayList<>(route);
        this.position = new Position(x, y);
        this.maximumLoad = maximumLoad;
        this.baseSpeed = speed;
        this.routeIndex = route.indexOf(stationId);
        if (this.routeIndex == route.size() - 1) {
            this.direction = -1;
        }
    }

    public String getTrainId() {
        return this.trainId;
    }

    public String getType() {
        return this.type;
    }

    public String getStationId() {
        return this.stationId;
    }

    public String getLocation() {
        return this.location;
    }

    public List<String> getRoute() {
        return this.route;
    }

    public Position getPosition() {
        return this.position;
    }

    public int getMaximumLoad() {
        return this.maximumLoad;
    }

    public double getSpeed() {
        int freightWeight = 0;
        for (Load load : currentLoads) {
            freightWeight += load.getFreightWeight();
        }
        return this.baseSpeed * (1.0 - freightWeight * FREIGHT_SPEED_REDUCTION_FACTOR);
    }

    public void setCyclical(boolean isCyclical) {
        this.isCyclical = isCyclical;
    }

    public boolean isCyclical() {
        return this.isCyclical;
    }

    public String getTargetStationId() {
        if (isCyclical) {
            return route.get((routeIndex + 1) % route.size());
        }

        int nextDirection = direction;
        int nextIndex = routeIndex + nextDirection;
        if (nextIndex < 0 || nextIndex >= route.size()) {
            nextDirection = -nextDirection;
            nextIndex = routeIndex + nextDirection;
        }
        return route.get(nextIndex);
    }

    public boolean canMove(TrainStation targetStation, Track currTrack, boolean isTargetStationFull) {
        if (currTrack.isBroken() && !canMoveOnBrokenTrack()) {
            return false;
        }

        double currentSpeed = getSpeed();
        Position targetPosition = targetStation.getStationPosition();

        if (this.position.isInBound(targetPosition, currentSpeed)) {
            if (isTargetStationFull) {
                return false;
            }
        }

        return true;
    }

    // Return true if arrive target station, return false if can not arrive target station
    public boolean move(TrainStation targetStation, Track currTrack, boolean isTargetStationFull) {
        if (!canMove(targetStation, currTrack, isTargetStationFull)) {
            return false;
        }

        double currentSpeed = getSpeed();
        Position targetPosition = targetStation.getStationPosition();

        if (this.position.isInBound(targetPosition, currentSpeed)) {
            this.position = targetPosition;
            this.location = targetStation.getStationId();
            this.stationId = targetStation.getStationId();
            this.routeIndex = this.route.indexOf(targetStation.getStationId());

            if (!this.isCyclical) {
                if (this.routeIndex == this.route.size() - 1) {
                    this.direction = -1;
                } else if (this.routeIndex == 0) {
                    this.direction = 1;
                }
            }

            onMoved(currTrack);

            return true;

        } else {

            Position newPosition = this.position.calculateNewPosition(targetPosition, currentSpeed);
            this.position = newPosition;
            this.location = currTrack.getTrackId();
            this.stationId = null; // on track

            onMoved(currTrack);

            return false;
        }
    }

    protected void onMoved(Track track) {
    }

    public void addLoad(Load load) {
        currentLoads.add(load);
    }

    public void removeLoad(Load load) {
        currentLoads.remove(load);
    }

    public List<Load> getCurrentLoads() {
        return this.currentLoads;
    }

    public int getCurrentLoadWeight() {
        int totalWeight = 0;
        for (Load load : currentLoads) {
            totalWeight += load.getWeight();
        }
        return totalWeight;
    }

    public void setRouteStations(List<TrainStation> routeStations) {
        this.routeStations = new ArrayList<>(routeStations);
    }

    private Position getRouteStationPosition(String stationId) {
        for (TrainStation station : routeStations) {
            if (station.getStationId().equals(stationId)) {
                return station.getStationPosition();
            }
        }
        return null;
    }

    public boolean canCarry(Load load) {
        if (!this.route.contains(load.getDestinationStationId())) {
            return false;
        }

        if (this.getCurrentLoadWeight() + load.getWeight() > this.maximumLoad) {
            return false;
        }

        if (!load.canBeCarriedBy(this)) {
            return false;
        }

        int timeToDest = calculateTimeToDestination(load.getDestinationStationId());
        if (!load.canSurviveTrip(timeToDest)) {
            return false;
        }

        return true;
    }

    public int calculateTimeToDestination(String destStationId) {
        if (this.routeStations == null || this.routeStations.isEmpty()) {
            return 0;
        }

        int currentIndex = this.route.indexOf(this.stationId);
        if (currentIndex < 0) {
            currentIndex = this.routeIndex;
        }
        int currentDir = this.direction;
        double speed = this.getSpeed();
        int totalMinutes = 0;

        int maxHops = this.route.size() * 4 + 10;
        int hops = 0;

        while (hops++ < maxHops) {
            int nextIndex;
            if (this.isCyclical) {
                nextIndex = (currentIndex + 1) % route.size();
            } else {
                nextIndex = currentIndex + currentDir;
                if (nextIndex >= route.size() || nextIndex < 0) {
                    currentDir = -currentDir;
                    nextIndex = currentIndex + currentDir;
                }
            }

            String fromStationId = route.get(currentIndex);
            String toStationId = route.get(nextIndex);

            Position fromPos = getRouteStationPosition(fromStationId);
            Position toPos = getRouteStationPosition(toStationId);

            if (fromPos != null && toPos != null && speed > 0) {
                double dist = fromPos.distance(toPos);
                int ticksForHop = (int) Math.ceil(dist / speed);
                totalMinutes += Math.max(1, ticksForHop);
            } else {
                totalMinutes += 1;
            }

            if (toStationId.equals(destStationId)) {
                return totalMinutes;
            }

            currentIndex = nextIndex;
        }

        return totalMinutes;
    }

    public void disEmbarkLoads(String stationId) {
        for (Load load : new ArrayList<>(currentLoads)) {

            if (load.getDestinationStationId().equals(stationId)) {
                currentLoads.remove(load);
            }

        }
    }

    public void deteriorateLoads() {
        for (Load load : new ArrayList<>(currentLoads)) {
            load.deteriorate();

            if (load.isSpoiled()) {
                currentLoads.remove(load);
            }

        }
    }

    public int trackDamage() {
        int currLoadsWeight = this.getCurrentLoadWeight();
        return 1 + (int) Math.ceil(currLoadsWeight / 1000.0);
    }

    public int repairRate() {
        return 0;
    }

    public boolean canMoveOnBrokenTrack() {
        return false;
    }

    public int getTechnicianCount() {
        int count = 0;
        for (Load load : currentLoads) {
            count += load.getTechnicianCount();
        }
        return count;
    }

    @Override
    public int compareTo(Train other) {
        return this.trainId.compareTo(other.trainId);
    }

    public abstract boolean carryPassenger();

    public abstract boolean carryFreight();

    public abstract boolean cyclicalRoute();
}
