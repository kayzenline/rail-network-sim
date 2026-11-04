package railnet.trains;

import java.util.List;

public class MaglevTrain extends Train {
    private static final int MAX_LOAD = 6000;
    private static final double SPEED = 6.0;

    public MaglevTrain(String trainId, String type, String stationId, List<String> route, double x, double y) {
        super(trainId, type, stationId, route, x, y, MAX_LOAD, SPEED);
    }

    @Override
    public boolean carryPassenger() {
        return true;
    }

    @Override
    public boolean carryFreight() {
        return true;
    }


    @Override
    public boolean cyclicalRoute() {
        return true;
    }
}
