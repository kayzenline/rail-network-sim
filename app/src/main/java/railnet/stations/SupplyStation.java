package railnet.stations;

public class SupplyStation extends TrainStation {
    private static final int MAX_TRAINS = 6;

    public SupplyStation(String stationId, String type, double x, double y) {
        super(stationId, type, x, y, MAX_TRAINS);
    }
}
