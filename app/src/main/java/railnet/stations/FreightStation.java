package railnet.stations;

public class FreightStation extends TrainStation {
    private static final int MAX_TRAINS = 3;

    public FreightStation(String stationId, String type, double x, double y) {
        super(stationId, type, x, y, MAX_TRAINS);
    }
}
