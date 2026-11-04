package trains;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import railnet.response.models.LoadInfoResponse;
import railnet.trains.TrainsController;
import railnet.utils.Position;
import railnet.utils.TrackType;

public class ScenarioTests {

    @Test
    public void testFreightSlowsDownTrain() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 100.0);
        controller.createTrack("t1-2", "s1", "s2");

        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("train1").getPosition());

        // Add 1000kg freight, 10% reduce speed to 3.6
        controller.createFreight("s1", "s2", "f1", 1000);
        controller.simulate();

        // Train embarked the freight and moved at speed 3.6
        assertEquals(new Position(0.0, 3.6), controller.getTrainInfo("train1").getPosition());
    }

    @Test
    public void testRefrigeratedFreightDeterioratesAndSpoils() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 100.0);
        controller.createTrack("t1-2", "s1", "s2");

        // Create refrigerated freight with 2 mins till spoil
        controller.createRefrigeratedFreight("s1", "s2", "rf1", 500, 2);
        assertEquals(1, controller.getStationInfo("s1").getLoads().size());

        // 1 tick passed: deteriorates to 1 min left
        controller.simulate();
        assertEquals(1, controller.getStationInfo("s1").getLoads().size());

        // 2nd tick passed: deteriorates to 0 mins left
        controller.simulate();
        assertEquals(0, controller.getStationInfo("s1").getLoads().size());
    }

    @Test
    public void testRouteIndexUpdatesAndBouncesOnLinearRoute() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 4.0);
        controller.createTrack("t1-2", "s1", "s2");

        assertDoesNotThrow(() -> {
            // FreightTrain has speed 4.0, distance is 4.0
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        assertEquals("s1", controller.getTrainInfo("train1").getLocation());

        // Tick 1: arrives at s2
        controller.simulate();
        assertEquals(new Position(0.0, 4.0), controller.getTrainInfo("train1").getPosition());
        assertEquals("s2", controller.getTrainInfo("train1").getLocation());

        // Tick 2: arrives at s1
        controller.simulate();
        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("train1").getPosition());
        assertEquals("s1", controller.getTrainInfo("train1").getLocation());

        // Tick 3: arrives back at s2
        controller.simulate();
        assertEquals(new Position(0.0, 4.0), controller.getTrainInfo("train1").getPosition());
        assertEquals("s2", controller.getTrainInfo("train1").getLocation());
    }

    @Test
    public void testCyclicalRouteMovement() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 6.0);
        controller.createStation("s3", "CentralStation", 6.0, 6.0);
        controller.createStation("s4", "CentralStation", 6.0, 0.0);
        controller.createTrack("t1-2", "s1", "s2");
        controller.createTrack("t2-3", "s2", "s3");
        controller.createTrack("t3-4", "s3", "s4");
        controller.createTrack("t4-1", "s4", "s1");

        assertDoesNotThrow(() -> {
            // MaglevTrain has speed 6.0, can travel on cyclical route
            controller.createTrain("train1", "MaglevTrain", "s1", List.of("s1", "s2", "s3", "s4"));
        });

        assertEquals("s1", controller.getTrainInfo("train1").getLocation());

        // Tick 1: arrives at s2
        controller.simulate();
        assertEquals("s2", controller.getTrainInfo("train1").getLocation());

        // Tick 2: arrives at s3
        controller.simulate();
        assertEquals("s3", controller.getTrainInfo("train1").getLocation());

        // Tick 3: arrives at s4
        controller.simulate();
        assertEquals("s4", controller.getTrainInfo("train1").getLocation());

        // Tick 4: arrives back at s1
        controller.simulate();
        assertEquals("s1", controller.getTrainInfo("train1").getLocation());

        // Tick 5: cyclical loop continues to s2
        controller.simulate();
        assertEquals("s2", controller.getTrainInfo("train1").getLocation());
    }

    @Test
    public void testTrainBlockedAtStationDoesNotDecreaseDurability() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        // FreightStation has maxTrains = 3
        controller.createStation("s2", "FreightStation", 0.0, 2.0);
        controller.createStation("s3", "CentralStation", 0.0, 100.0);
        controller.createTrack("t1-2", "s1", "s2", true);
        controller.createTrack("t2-3", "s2", "s3");

        assertDoesNotThrow(() -> {
            // trainA is at s1, moves first in lexicographical order
            controller.createTrain("trainA", "FreightTrain", "s1", List.of("s1", "s2"));
            // 3 trains already docked at s2, so s2 is full
            controller.createTrain("trainZ1", "FreightTrain", "s2", List.of("s2", "s3"));
            controller.createTrain("trainZ2", "FreightTrain", "s2", List.of("s2", "s3"));
            controller.createTrain("trainZ3", "FreightTrain", "s2", List.of("s2", "s3"));
        });

        // Track starts with durability 10
        assertEquals(10, controller.getTrackInfo("t1-2").getDurability());

        // Simulate 1 tick: trainA cannot enter s2 because s2 is full, so it remains at
        // s1
        controller.simulate();

        assertEquals("s1", controller.getTrainInfo("trainA").getLocation());
        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("trainA").getPosition());

        // Durability remain 10 because trainA never arrived at s2 (did not transfer off
        // the track)
        assertEquals(10, controller.getTrackInfo("t1-2").getDurability());
    }

    @Test
    public void testTrackStatusUpdatesWhenDurabilityReachesZero() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 4.0);
        controller.createTrack("t1-2", "s1", "s2", true);

        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        assertEquals(TrackType.UNBROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(10, controller.getTrackInfo("t1-2").getDurability());

        controller.simulate(9);
        assertEquals(TrackType.UNBROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(1, controller.getTrackInfo("t1-2").getDurability());

        controller.simulate();
        assertEquals(TrackType.BROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(0, controller.getTrackInfo("t1-2").getDurability());
    }

    @Test
    public void testTrackIsBrokenThenRepair() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 4.0);
        controller.createTrack("t1-2", "s1", "s2", true);

        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        assertEquals(TrackType.UNBROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(10, controller.getTrackInfo("t1-2").getDurability());

        // Track is broken
        controller.simulate(10);
        assertEquals(TrackType.BROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(0, controller.getTrackInfo("t1-2").getDurability());

        // Create repair train to repair
        assertDoesNotThrow(() -> {
            controller.createTrain("repair1", "RepairTrain", "s1", List.of("s1", "s2"));
        });

        // Create 2 techicians
        controller.createPassenger("s1", "s2", "tech1", true);
        controller.createPassenger("s1", "s2", "tech2", true);

        // RepairTrain speed is 2, distance is 4, auto repair 1 and 4 (2 techicians)
        controller.simulate();

        assertEquals(2, controller.getTrainInfo("repair1").getLoads().size());
        assertEquals("tech1", controller.getTrainInfo("repair1").getLoads().get(0).getLoadId());
        assertEquals("Technician", controller.getTrainInfo("repair1").getLoads().get(0).getType());
        assertEquals("tech2", controller.getTrainInfo("repair1").getLoads().get(1).getLoadId());
        assertEquals("Technician", controller.getTrainInfo("repair1").getLoads().get(1).getType());

        assertEquals(TrackType.BROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(5, controller.getTrackInfo("t1-2").getDurability());
        assertEquals(new Position(0.0, 2.0), controller.getTrainInfo("repair1").getPosition());

        // Other train block by broken track
        assertEquals("s1", controller.getTrainInfo("train1").getLocation());

        // Tick2: Repair train arrive, add dur after disembark, dur == 10
        controller.simulate();

        assertEquals(10, controller.getTrackInfo("t1-2").getDurability());
        assertEquals(TrackType.UNBROKEN, controller.getTrackInfo("t1-2").getType());
        assertEquals(0, controller.getTrainInfo("repair1").getLoads().size());

        assertEquals("s1", controller.getTrainInfo("train1").getLocation());

        // Track repaired
        controller.simulate();

        assertEquals("s2", controller.getTrainInfo("train1").getLocation());
        assertEquals(9, controller.getTrackInfo("t1-2").getDurability());
    }

    @Test
    public void testRefrigeratedFreightSpoilsOnTrainAndRestoresSpeed() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 6.0);
        controller.createTrack("t1-2", "s1", "s2", true);

        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        assertEquals(new Position(0.0, 0.0), controller.getTrainInfo("train1").getPosition());

        // Create normal freight (1000kg) and refrigerated freight (2000kg, 2 mins till
        // spoil)
        controller.createFreight("s1", "s2", "f1", 1000);
        controller.createRefrigeratedFreight("s1", "s2", "rf1", 2000, 2);

        // Tick 1:
        // Existing load f1 (1000kg) gives speed 3.6 km/min.
        // Distance is 6.0 km -> ceil(6.0 / 3.6) = 2 mins <= 2 minsTillSpoil, so rf1 is
        // embarked!
        // Both f1 and rf1 embarked: total freight = 3000kg -> speed = 2.8 km/min
        controller.simulate();

        assertEquals(2, controller.getTrainInfo("train1").getLoads().size());
        assertEquals(new Position(0.0, 2.8), controller.getTrainInfo("train1").getPosition());

        // Tick 2:
        // Train moves at speed 2.8 to position 5.6.
        // At end of tick 2, rf1 reaches 0 minsTillSpoil and spoils, immediately removed
        controller.simulate();

        // Only f1 remains on the train
        assertEquals(List.of(new LoadInfoResponse("f1", "Freight")), controller.getTrainInfo("train1").getLoads());
        // Speed is recalculated to exclude rf1's weight
        assertEquals(new Position(0.0, 5.6), controller.getTrainInfo("train1").getPosition());

        // Tick 3: Train continues moving at speed 3.6 and arrives at s2 (distance 6.0)
        controller.simulate();
        assertEquals("s2", controller.getTrainInfo("train1").getLocation());

        // Track damage only includes remaining 1000kg freight: 1 + ceil(1000 / 1000) =
        // 2
        // Initial durability was 10, so 10 - 2 = 8
        assertEquals(8, controller.getTrackInfo("t1-2").getDurability());
    }

    @Test
    public void testRefrigeratedFreightWillNotEmbarkIfCannotReachDestinationBeforeSpoiling() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 20.0);
        controller.createTrack("t1-2", "s1", "s2");

        assertDoesNotThrow(() -> {
            // FreightTrain has speed 4.0 km/min.
            // Distance s1 -> s2 is 20.0 km.
            // Time to reach destination without new freight is ceil(20.0 / 4.0) = 5 mins.
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        // rf_too_slow expires in 4 mins (< 5 mins required) -> must not embark
        controller.createRefrigeratedFreight("s1", "s2", "rf_too_slow", 500, 4);
        // rf_ok expires in 5 mins (>= 5 mins required) -> should embark
        controller.createRefrigeratedFreight("s1", "s2", "rf_ok", 500, 5);

        controller.simulate();

        // train1 should only have embarked rf_ok, not rf_too_slow
        assertEquals(List.of(new LoadInfoResponse("rf_ok", "RefrigeratedFreight")),
                controller.getTrainInfo("train1").getLoads());
    }

    @Test
    public void testTrainBlockedFromDepartingDoesNotEmbarkLoads() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "PassengerStation", 0.0, 0.0);
        // PassengerStation has maxTrains = 4
        controller.createStation("s2", "PassengerStation", 0.0, 2.0);
        controller.createStation("s3", "PassengerStation", 0.0, 100.0);
        controller.createTrack("t1-2", "s1", "s2");
        controller.createTrack("t2-3", "s2", "s3");

        assertDoesNotThrow(() -> {
            // 4 trains docked at s2 makes s2 full
            controller.createTrain("trainZ1", "CommuterTrain", "s2", List.of("s2", "s3"));
            controller.createTrain("trainZ2", "CommuterTrain", "s2", List.of("s2", "s3"));
            controller.createTrain("trainZ3", "CommuterTrain", "s2", List.of("s2", "s3"));
            controller.createTrain("trainZ4", "CommuterTrain", "s2", List.of("s2", "s3"));

            // trainA is at s1, route [s1, s2]. Speed is 2.0, distance is 2.0 should depart
            // s1 and arrive at s2 in 1 tick
            controller.createTrain("trainA", "CommuterTrain", "s1", List.of("s1", "s2"));
        });

        // Passenger waiting at s1
        controller.createPassenger("s1", "s2", "p1");

        // Simulate 1 tick: trainA is inbound to s2, but s2 is full, so trainA cannot
        // depart s1
        controller.simulate();

        // trainA must remain at s1
        assertEquals("s1", controller.getTrainInfo("trainA").getLocation());
        // Because trainA did not depart, passenger p1 should remain at s1 and not be
        // boarded on trainA
        assertEquals(1, controller.getStationInfo("s1").getLoads().size());
        assertEquals(0, controller.getTrainInfo("trainA").getLoads().size());
    }

    @Test
    public void testRefrigeratedFreightDeterioratesAtEndOfTick() {
        TrainsController controller = new TrainsController();
        controller.createStation("s1", "CentralStation", 0.0, 0.0);
        controller.createStation("s2", "CentralStation", 0.0, 2.0);
        controller.createTrack("t1-2", "s1", "s2", true);

        assertDoesNotThrow(() -> {
            // FreightTrain speed = 4.0, distance = 2.0
            controller.createTrain("train1", "FreightTrain", "s1", List.of("s1", "s2"));
        });

        // rf1 has 1 min till spoil. Takes 1 tick to reach s2.
        // With 1000kg load, track damage upon arrival at s2 is 1 + ceil(1000/1000) = 2
        // (durability 10 -> 8).
        // If rf1 spoiled at start of tick before departure, train travels empty (damage
        // = 1, durability 10 -> 9).
        controller.createRefrigeratedFreight("s1", "s2", "rf1", 1000, 1);

        controller.simulate();

        // Train arrived at s2
        assertEquals("s2", controller.getTrainInfo("train1").getLocation());
        // Track durability should be 8, proving rf1 was successfully carried and
        // delivered in tick 1
        assertEquals(8, controller.getTrackInfo("t1-2").getDurability());
    }

    @Test
    public void testRefrigeratedFreightMultiHopETA() {
        TrainsController controller = new TrainsController();
        controller.createStation("sA", "CentralStation", 0.0, 0.0);
        controller.createStation("sB", "CentralStation", 0.0, 4.0);
        controller.createStation("sC", "CentralStation", 0.0, 8.0);
        controller.createTrack("tA-B", "sA", "sB");
        controller.createTrack("tB-C", "sB", "sC");

        assertDoesNotThrow(() -> {
            controller.createTrain("train1", "FreightTrain", "sA", List.of("sA", "sB", "sC"));
        });

        // rfFail has minsTillSpoil = 1 -> cannot survive 2 ticks, should NOT embark
        controller.createRefrigeratedFreight("sA", "sC", "rfFail", 1000, 1);
        // rfPass has minsTillSpoil = 2 -> can survive 2 ticks, should embark
        controller.createRefrigeratedFreight("sA", "sC", "rfPass", 1000, 2);

        controller.simulate();

        // Only rfPass is embarked
        assertEquals(List.of(new LoadInfoResponse("rfPass", "RefrigeratedFreight")),
                controller.getTrainInfo("train1").getLoads());
    }

    @Test
    public void testRefrigeratedFreightLinearReverseETA() {
        TrainsController controller = new TrainsController();
        controller.createStation("sA", "CentralStation", 0.0, 0.0);
        controller.createStation("sB", "CentralStation", 0.0, 4.0);
        controller.createStation("sC", "CentralStation", 0.0, 8.0);
        controller.createTrack("tA-B", "sA", "sB");
        controller.createTrack("tB-C", "sB", "sC");

        assertDoesNotThrow(() -> {
            // Starts at sB on route [sA, sB, sC], initially moving forward toward sC
            controller.createTrain("train1", "FreightTrain", "sB", List.of("sA", "sB", "sC"));
        });


        // rfFail (2 mins) rejected; rfPass (3 mins) embarked
        controller.createRefrigeratedFreight("sB", "sA", "rfFail", 1000, 2);
        controller.createRefrigeratedFreight("sB", "sA", "rfPass", 1000, 3);

        controller.simulate();

        assertEquals(List.of(new LoadInfoResponse("rfPass", "RefrigeratedFreight")),
                controller.getTrainInfo("train1").getLoads());
    }

    @Test
    public void testRefrigeratedFreightCyclicalRouteETA() {
        TrainsController controller = new TrainsController();
        controller.createStation("sA", "CentralStation", 0.0, 0.0);
        controller.createStation("sB", "CentralStation", 0.0, 6.0);
        controller.createStation("sC", "CentralStation", 6.0, 6.0);
        controller.createStation("sD", "CentralStation", 6.0, 0.0);
        controller.createTrack("tA-B", "sA", "sB");
        controller.createTrack("tB-C", "sB", "sC");
        controller.createTrack("tC-D", "sC", "sD");
        controller.createTrack("tD-A", "sD", "sA");

        assertDoesNotThrow(() -> {
            // Maglev starts at sD on cyclical route [sA, sB, sC, sD]
            controller.createTrain("train1", "MaglevTrain", "sD", List.of("sA", "sB", "sC", "sD"));
        });

        // Destination is sB: cyclical path is sD -> sA (1 tick) -> sB (1 tick) = 2
        // ticks total.
        // rfFail (1 min) rejected; rfPass (2 mins) embarked
        controller.createRefrigeratedFreight("sD", "sB", "rfFail", 1000, 1);
        controller.createRefrigeratedFreight("sD", "sB", "rfPass", 1000, 2);

        controller.simulate();

        assertEquals(List.of(new LoadInfoResponse("rfPass", "RefrigeratedFreight")),
                controller.getTrainInfo("train1").getLoads());
    }
}
