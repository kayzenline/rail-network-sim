package railnet.trains;

import java.util.List;



public class CommuterTrain extends Train {
    private static final int MAX_LOAD = 3500;
    private static final double SPEED = 2.0;

    public CommuterTrain(String trainId, String type, String stationId, List<String> route, double x, double y) {
        super(trainId, type, stationId, route, x, y, MAX_LOAD, SPEED);
    }

    @Override
    public boolean carryPassenger() {
        return true;
    }

    @Override
    public boolean carryFreight() {
        return false;
    }


    @Override
    public boolean cyclicalRoute() {
        return false;
    }
}
