package railnet.stations;

public class CentralStation extends TrainStation {
    private static final int MAX_TRAINS = 8;

    public CentralStation(String stationId, String type, double x, double y) {
        super(stationId, type, x, y, MAX_TRAINS);
    }
}
