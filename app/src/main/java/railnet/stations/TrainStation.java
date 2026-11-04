package railnet.stations;

import railnet.utils.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import railnet.load.Load;
import railnet.trains.Train;

public abstract class TrainStation {
    private final String stationId;
    private final String type;
    private final Position position;
    private final int maxTrains;
    private final List<Load> currentLoads = new ArrayList<>();

    public TrainStation(String stationId, String type, double x, double y, int maxTrains) {
        this.stationId = stationId;
        this.type = type;
        this.position = new Position(x, y);
        this.maxTrains = maxTrains;
    }

    public String getStationId() {
        return this.stationId;
    }

    public String getStationType() {
        return this.type;
    }

    public Position getStationPosition() {
        return this.position;
    }

    public int getMaxTrains() {
        return this.maxTrains;
    }

    public void addLoad(Load load) {
        currentLoads.add(load);
    }

    public List<Load> getLoads() {
        return currentLoads;
    }

    public void embarkLoads(Train train) {
        List<Load> sortedLoads = new ArrayList<>(currentLoads);
        Collections.sort(sortedLoads);
        for (Load load : sortedLoads) {
            if (train.canCarry(load)) {
                train.addLoad(load);
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
}
