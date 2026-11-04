package trains;

import org.junit.jupiter.api.Test;

import railnet.response.models.LoadInfoResponse;
import railnet.trains.TrainsController;
import railnet.utils.Position;
import static trains.TestHelpers.assertListAreEqualIgnoringOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTests {
    @Test
    public void testSimpleMovement() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "PassengerStation", 10.0, 10.0);
        controller.createTrack("t1-2", "s1", "s2");
        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "CommuterTrain", "s1", List.of("s1", "s2"));
            controller.createTrain("train2", "FreightTrain", "s1", List.of("s1", "s2"));
            controller.createTrain("train3", "MaglevTrain", "s1", List.of("s1", "s2"));
        });

        // Check that the trains were created.
        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("train1").getPosition());
        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("train2").getPosition());
        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("train3").getPosition());

        controller.simulate();

        // Check that each train type has moved the correct amount.
        assertEquals(new Position(1.41, 1.41), controller.getTrainInfo("train1").getPosition());
        assertEquals(new Position(2.82, 2.82), controller.getTrainInfo("train2").getPosition());
        assertEquals(new Position(4.24, 4.24), controller.getTrainInfo("train3").getPosition());
    }

    @Test
    public void testCreateDifferentPassengersAndFreight() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "PassengerStation", 0, 0);
        controller.createStation("s2", "FreightStation", 0, 10);
        controller.createStation("s3", "CentralStation", 10, 10);

        // Passengers should be able to be created at passenger stations,
        // and freight should be able to be created at freight stations.
        controller.createPassenger("s1", "s3", "p1");
        controller.createFreight("s2", "s3", "c1", 500);

        assertEquals(List.of(new LoadInfoResponse("p1", "Passenger")), controller.getStationInfo("s1").getLoads());
        assertEquals(List.of(new LoadInfoResponse("c1", "Freight")), controller.getStationInfo("s2").getLoads());

        // Both passengers and freight should be able to be created at central stations.
        controller.createPassenger("s3", "s1", "p2");
        controller.createFreight("s3", "s2", "c2", 1500);

        List<LoadInfoResponse> expectedCentralStationLoads = List.of(new LoadInfoResponse("p2", "Passenger"),
                new LoadInfoResponse("c2", "Freight"));
        assertListAreEqualIgnoringOrder(expectedCentralStationLoads, controller.getStationInfo("s3").getLoads());
    }

    @Test
    public void testTrainPicksUpPassenger() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "PassengerStation", 0, 0);
        controller.createStation("s2", "PassengerStation", 0, 10);
        controller.createTrack("t1-2", "s1", "s2");
        controller.createPassenger("s1", "s2", "p1");
        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "CommuterTrain", "s1", List.of("s1", "s2"));
        });

        // Check that the load was created at the station.
        assertEquals(List.of(new LoadInfoResponse("p1", "Passenger")), controller.getStationInfo("s1").getLoads());

        // After the first tick, the train with id t1 should have picked up the passenger.
        controller.simulate();
        assertEquals(0, controller.getStationInfo("s1").getLoads().size());
        assertEquals(List.of(new LoadInfoResponse("p1", "Passenger")), controller.getTrainInfo("train1").getLoads());
    }
}
