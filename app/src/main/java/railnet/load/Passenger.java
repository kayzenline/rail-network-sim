package railnet.load;

import railnet.trains.Train;

public class Passenger extends Load {
    private static final int PASSENGER_WEIGHT = 70;

    public Passenger(String passengerId, String destinationStationId) {
        super(passengerId, destinationStationId, PASSENGER_WEIGHT);
    }


    @Override
    public String getLoadType() {
        return "Passenger";
    }

    @Override
    public boolean canBeCarriedBy(Train train) {
        return train.carryPassenger();
    }
}
