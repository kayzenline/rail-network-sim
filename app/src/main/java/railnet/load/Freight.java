package railnet.load;

import railnet.trains.Train;

public class Freight extends Load {

    public Freight(String freightId, String destinationStationId, int weight) {
        super(freightId, destinationStationId, weight);
    }

    @Override
    public String getLoadType() {
        return "Freight";
    }

    @Override
    public boolean canBeCarriedBy(Train train) {
        return train.carryFreight();
    }

    @Override
    public int getFreightWeight() {
        return getWeight();
    }
}
