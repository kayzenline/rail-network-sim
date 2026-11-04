package railnet.trains;

import java.util.List;


public class FreightTrain extends Train {
    private static final int MAX_LOAD = 7000;
    private static final double SPEED = 4.0;

    public FreightTrain(String trainId, String type, String stationId, List<String> route, double x, double y) {
        super(trainId, type, stationId, route, x, y, MAX_LOAD, SPEED);
    }

    @Override
    public boolean carryPassenger() {
        return false;
    }

    @Override
    public boolean carryFreight() {
        return true;
    }


    @Override
    public boolean cyclicalRoute() {
        return false;
    }
}
