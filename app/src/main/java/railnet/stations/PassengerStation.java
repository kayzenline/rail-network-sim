package railnet.stations;

public class PassengerStation extends TrainStation {
    private static final int MAX_TRAINS = 4;

    public PassengerStation(String stationId, String type, double x, double y) {
        super(stationId, type, x, y, MAX_TRAINS);
    }
}
