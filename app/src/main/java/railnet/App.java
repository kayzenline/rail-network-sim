package railnet;

import spark.Request;
import static spark.Spark.*;

import railnet.exceptions.TrainsControllerException;
import railnet.trains.TrainsController;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

// import java.lang.reflect.Type;
import scintilla.Scintilla;
import spark.Response;

public class App {
    private static TrainsController tc = new TrainsController();

    public static void main(String[] args) {
        Scintilla.initialize();
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Content-Type", "application/json");
        });

        stationRoutes(gson);
        trainRoutes(gson);
        trackRoutes(gson);
        freightRoutes(gson);
        passengerRoutes(gson);
        miscRoutes(gson);

        Scintilla.start();
    }

    private static JsonObject createError(Exception e, Response response) {
        JsonObject error = new JsonObject();
        if (e instanceof JsonSyntaxException) {
            response.status(400);
            error.addProperty("type", "unknown");
            error.addProperty("message", String.format("Invalid JSON format: %s", e.getMessage()));
        } else if (e instanceof TrainsControllerException err) {
            response.status(err.getStatusCode());
            error.addProperty("type", err.getType());
            error.addProperty("message", err.getMessage());
        } else {
            response.status(500);
            error.addProperty("type", "unknown");
            error.addProperty("message", e.getMessage());
        }
        return error;
    }

    private static JsonObject handleError(Exception err, Response response) {
        System.err.println(err);
        return App.createError(err, response);
    }

    private static void validateWantedKeys(JsonObject jsonBody, List<String> wantedKeys) throws Exception {
        if (wantedKeys.stream().filter(x -> !jsonBody.has(x)).findAny().isPresent()) {
            String message = String.format("Body missing: %s",
                    wantedKeys.stream().filter(x -> !jsonBody.has(x)).collect(Collectors.joining(", ")));
            throw new Exception(message);
        }
    }

    private static synchronized TrainsController getTrainsController(Request request) {
        return tc;
    }

    private static void stationRoutes(Gson gson) {
        /**
         * Get all stations ids
         */
        get("/api/stations", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                return tc.listStationIds();
            }
        }, gson::toJson);

        /**
         * Get all StationInfoResponses
         */
        get("/api/stations/all", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                return tc.listStationIds().stream().map(x -> tc.getStationInfo(x)).toList();
            }
        }, gson::toJson);

        /**
        * Create a new station
        */
        post("/api/station/create", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                List<String> wantedKeys = Arrays.asList("stationId", "type", "x", "y");
                try {
                    JsonObject jsonBody = JsonParser.parseString(request.body()).getAsJsonObject();
                    App.validateWantedKeys(jsonBody, wantedKeys);

                    String stationId = jsonBody.get("stationId").getAsString();
                    String type = jsonBody.get("type").getAsString();
                    Double posX = jsonBody.get("x").getAsDouble();
                    Double posY = jsonBody.get("y").getAsDouble();

                    tc.createStation(stationId, type, posX, posY);
                    return new JsonObject();
                } catch (Exception err) {
                    return App.handleError(err, response);
                }
            }
        }, gson::toJson);
    }

    private static void trainRoutes(Gson gson) {
        /**
         * Get all trainIds
         */
        get("/api/trains", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                return tc.listTrainIds();
            }
        }, gson::toJson);

        /**
         * Get all Responses
         */
        get("/api/trains/all", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                return tc.listTrainIds().stream().map(x -> tc.getTrainInfo(x)).toList();
            }
        }, gson::toJson);

        post("/api/train/create", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                List<String> wantedKeys = Arrays.asList("trainId", "type", "stationId", "route");
                try {
                    JsonObject jsonBody = JsonParser.parseString(request.body()).getAsJsonObject();
                    App.validateWantedKeys(jsonBody, wantedKeys);

                    String trainId = jsonBody.get("trainId").getAsString();
                    String type = jsonBody.get("type").getAsString();
                    String stationId = jsonBody.get("stationId").getAsString();
                    List<String> route = StreamSupport.stream(jsonBody.getAsJsonArray("route").spliterator(), false)
                            .map(JsonElement::getAsString).collect(Collectors.toList());

                    tc.createTrain(trainId, type, stationId, route);
                    return new JsonObject();
                } catch (Exception err) {
                    return App.handleError(err, response);
                }
            }
        }, gson::toJson);
    }

    private static void trackRoutes(Gson gson) {
        /**
         * Get all trackIds
         */
        get("/api/tracks", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                return tc.listTrackIds();
            }
        }, gson::toJson);

        /**
         * Get all TrackInfoResponses
         */
        get("/api/tracks/all", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                return tc.listTrackIds().stream().map(x -> tc.getTrackInfo(x)).toList();
            }
        }, gson::toJson);

        post("/api/track/create", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            synchronized (tc) {
                List<String> wantedKeys = Arrays.asList("trackId", "fromStationId", "toStationId", "type");
                try {
                    JsonObject jsonBody = JsonParser.parseString(request.body()).getAsJsonObject();
                    App.validateWantedKeys(jsonBody, wantedKeys);

                    String trackId = jsonBody.get("trackId").getAsString();
                    String fromStationId = jsonBody.get("fromStationId").getAsString();
                    String toStationId = jsonBody.get("toStationId").getAsString();
                    String type = jsonBody.get("type").getAsString();

                    if (type.equals("NORMAL")) {
                        tc.createTrack(trackId, fromStationId, toStationId);
                    } else if (type.equals("UNBROKEN")) {
                        tc.createTrack(trackId, fromStationId, toStationId, true);
                    } else {
                        throw new TrainsControllerException("Invalid type: " + type, "unknown", 400);
                    }
                    return new JsonObject();
                } catch (Exception err) {
                    return App.handleError(err, response);
                }
            }
        }, gson::toJson);
    };

    private static void freightRoutes(Gson gson) {
        post("/api/freight/create", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            List<String> wantedKeys = Arrays.asList("startStationId", "destStationId", "freightId", "weight");
            synchronized (tc) {
                try {
                    JsonObject jsonBody = JsonParser.parseString(request.body()).getAsJsonObject();
                    App.validateWantedKeys(jsonBody, wantedKeys);

                    Optional<Integer> minsTillSpoil = Optional.empty();
                    if (jsonBody.has("minsTillSpoil") && !jsonBody.get("minsTillSpoil").isJsonNull()) {
                        minsTillSpoil = Optional.of(jsonBody.get("minsTillSpoil").getAsInt());
                    }
                    String startStationId = jsonBody.get("startStationId").getAsString();
                    String destStationId = jsonBody.get("destStationId").getAsString();
                    String freightId = jsonBody.get("freightId").getAsString();
                    int weight = jsonBody.get("weight").getAsInt();

                    if (minsTillSpoil.isPresent()) {
                        tc.createRefrigeratedFreight(startStationId, destStationId, freightId, weight,
                                minsTillSpoil.get());
                    } else {
                        tc.createFreight(startStationId, destStationId, freightId, weight);
                    }
                    return new JsonObject();
                } catch (Exception err) {
                    return App.handleError(err, response);
                }
            }
        }, gson::toJson);
    }

    private static void passengerRoutes(Gson gson) {
        post("/api/passenger/create", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            List<String> wantedKeys = Arrays.asList("startStationId", "destStationId", "passengerId", "type");
            synchronized (tc) {
                try {
                    JsonObject jsonBody = JsonParser.parseString(request.body()).getAsJsonObject();
                    App.validateWantedKeys(jsonBody, wantedKeys);

                    String startStationId = jsonBody.get("startStationId").getAsString();
                    String destStationId = jsonBody.get("destStationId").getAsString();
                    String passengerId = jsonBody.get("passengerId").getAsString();
                    String type = jsonBody.get("type").getAsString();

                    if (type.equals("Passenger")) {
                        tc.createPassenger(startStationId, destStationId, passengerId);
                    } else if (type.equals("Technician")) {
                        tc.createPassenger(startStationId, destStationId, passengerId, true);
                    } else {
                        throw new TrainsControllerException("Invalid type: " + type, "unknown", 400);
                    }
                    return new JsonObject();
                } catch (Exception err) {
                    return App.handleError(err, response);
                }
            }
        }, gson::toJson);
    }

    private static void miscRoutes(Gson gson) {
        post("/api/simulate", "application/json", (request, response) -> {
            TrainsController tc = getTrainsController(request);
            List<String> wantedKeys = Arrays.asList("length");
            synchronized (tc) {
                try {
                    JsonObject jsonBody = JsonParser.parseString(request.body()).getAsJsonObject();
                    App.validateWantedKeys(jsonBody, wantedKeys);

                    int length = jsonBody.get("length").getAsInt();
                    tc.simulate(length);
                    return new JsonObject();
                } catch (Exception err) {
                    return App.handleError(err, response);
                }
            }
        }, gson::toJson);

        post("/api/reset", "application/json", (request, response) -> {
            App.tc = new TrainsController();
            return new JsonObject();
        }, gson::toJson);
    }
}
