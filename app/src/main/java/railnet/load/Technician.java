package railnet.load;

public class Technician extends Passenger {
    public Technician(String passengerId, String destinationStationId) {
        super(passengerId, destinationStationId);
    }

    @Override
    public int getTechnicianCount() {
        return 1;
    }


    @Override
    public String getLoadType() {
        return "Technician";
    }
}
