
package railnet.load;

import railnet.trains.Train;

public abstract class Load implements Comparable<Load> {
    private final String loadId; // The unique ID of the load
    private final String destinationStationId;
    private final int weight;

    public Load(String loadId, String destinationStationId, int weight) {
        this.loadId = loadId;
        this.destinationStationId = destinationStationId;
        this.weight = weight;
    }

    public String getLoadId() {
        return this.loadId;
    }

    public String getLoad() {
        return getLoadId();
    }

    public String getDestinationStationId() {
        return this.destinationStationId;
    }

    public int getWeight() {
        return this.weight;
    }

    public abstract String getLoadType();

    public abstract boolean canBeCarriedBy(Train train);

    public int getFreightWeight() {
        return 0;
    }

    public void deteriorate() {
    }

    public boolean isSpoiled() {
        return false;
    }

    public boolean canSurviveTrip(int travelTime) {
        return true;
    }

    public int getTechnicianCount() {
        return 0;
    }


    @Override
    public int compareTo(Load other) {
        return this.loadId.compareTo(other.loadId);
    }
}
