package railnet.load;

public class RefrigeratedFreight extends Freight {
    private int minsTillSpoil;

    public RefrigeratedFreight(String freightId, String destinationStationId, int weight, int minsTillSpoil) {
        super(freightId, destinationStationId, weight);
        this.minsTillSpoil = minsTillSpoil;
    }

    @Override
    public String getLoadType() {
        return "RefrigeratedFreight";
    }

    @Override
    public void deteriorate() {
        this.minsTillSpoil--;
    }

    @Override
    public boolean isSpoiled() {
        return this.minsTillSpoil <= 0;
    }

    @Override
    public boolean canSurviveTrip(int travelTime) {
        return travelTime <= this.minsTillSpoil;
    }
}
