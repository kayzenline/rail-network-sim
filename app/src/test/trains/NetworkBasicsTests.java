package trains;

import org.junit.jupiter.api.Test;

import railnet.response.models.StationInfoResponse;
import railnet.response.models.TrackInfoResponse;
import railnet.response.models.TrainInfoResponse;
import railnet.trains.TrainsController;
import railnet.utils.Position;
import railnet.utils.TrackType;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static trains.TestHelpers.assertListAreEqualIgnoringOrder;

public class NetworkBasicsTests {
    @Test
    public void testCreateStations() {
        TrainsController controller = new TrainsController();

        controller.createStation("station1", "SupplyStation", 1.0, 1.0);
        assertEquals(new StationInfoResponse("station1", "SupplyStation", new Position(1.0, 1.0)),
                controller.getStationInfo("station1"));

        controller.createStation("station2", "FreightStation", 10.0, 10.0);
        assertEquals(new StationInfoResponse("station2", "FreightStation", new Position(10.0, 10.0)),
                controller.getStationInfo("station2"));

        controller.createStation("station3", "PassengerStation", 19.6, 15);
        assertEquals(new StationInfoResponse("station3", "PassengerStation", new Position(19.6, 15)),
                controller.getStationInfo("station3"));

        assertListAreEqualIgnoringOrder(List.of("station1", "station2", "station3"), controller.listStationIds());
    }

    @Test
    public void testCreateTracks() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "SupplyStation", 1.0, 1.0);
        controller.createStation("s2", "FreightStation", 10.0, 10.0);
        controller.createTrack("t1-2", "s1", "s2");
        assertEquals(controller.getTrackInfo("t1-2"), new TrackInfoResponse("t1-2", "s1", "s2", TrackType.NORMAL, 10));
    }

    @Test
    public void testCreateTrains() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "SupplyStation", 1.0, 1.0);
        controller.createStation("s2", "FreightStation", 10.0, 10.0);
        controller.createStation("s3", "PassengerStation", 3.0, 19.0);
        controller.createTrack("t1-2", "s1", "s2");
        controller.createTrack("t2-3", "s2", "s3");

        // Make and check the passenger train was created.
        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "CommuterTrain", "s2", List.of("s1", "s2", "s3"));
        });

        assertEquals(new TrainInfoResponse("train1", "s2", "CommuterTrain", new Position(10.0, 10.0),
                Collections.emptyList()), controller.getTrainInfo("train1"));

        // Make and check the freight train was created.
        assertDoesNotThrow(() -> {
            controller.createTrain("train2", "FreightTrain", "s1", List.of("s1", "s2", "s3"));
        });

        assertEquals(
                new TrainInfoResponse("train2", "s1", "FreightTrain", new Position(1.0, 1.0), Collections.emptyList()),
                controller.getTrainInfo("train2"));

        // Make and check the maglev train was created.
        assertDoesNotThrow(() -> {
            controller.createTrain("train3", "MaglevTrain", "s3", List.of("s1", "s2", "s3"));

        });

        assertEquals(
                new TrainInfoResponse("train3", "s3", "MaglevTrain", new Position(3.0, 19.0), Collections.emptyList()),
                controller.getTrainInfo("train3"));

        assertListAreEqualIgnoringOrder(List.of("train1", "train2", "train3"), controller.listTrainIds());
    }

    @Test
    public void testMultipleControllers() {
        /**
         * Note: If you are failing this test, you are likely using static variables in controller
         * which means the variables are share across all instances of the controller (e.g., `controller`, `controller2)
         */
        TrainsController controller = new TrainsController();
        TrainsController controller2 = new TrainsController();

        controller.createStation("s1", "SupplyStation", 1.0, 1.0);
        controller.createStation("s2", "SupplyStation", 1.0, 2.0);
        controller.createTrack("t1-2", "s1", "s2");
        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "CommuterTrain", "s2", List.of("s1", "s2"));
        });

        assertEquals(controller.listStationIds(), List.of("s1", "s2"));
        assertEquals(controller.listTrackIds(), List.of("t1-2"));
        assertEquals(controller.listTrainIds(), List.of("train1"));

        // controller2 should not have these entities
        assertNotEquals(controller.listStationIds(), controller2.listStationIds());
        assertNotEquals(controller.listTrackIds(), controller2.listTrackIds());
        assertNotEquals(controller.listTrainIds(), controller2.listTrainIds());
        assertEquals(controller2.listStationIds(), List.of());
        assertEquals(controller2.listTrackIds(), List.of());
        assertEquals(controller2.listTrainIds(), List.of());
    }
}
