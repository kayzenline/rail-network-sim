package railnet.trains;

import java.util.List;
import railnet.track.Track;

public class RepairTrain extends CommuterTrain {

    public RepairTrain(String trainId, String type, String stationId, List<String> route, double x, double y) {
        super(trainId, type, stationId, route, x, y);
    }

    @Override
    protected void onMoved(Track track) {
        track.repair(repairRate());
    }

    private static final int REPAIR_BONUS_PER_TECH = 2;

    @Override
    public int trackDamage() {
        return 0;
    }

    @Override
    public int repairRate() {
        return REPAIR_BONUS_PER_TECH * getTechnicianCount();
    }

    @Override
    public boolean canMoveOnBrokenTrack() {
        return true;
    }
}
