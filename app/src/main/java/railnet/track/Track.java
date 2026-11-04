package railnet.track;

import railnet.utils.TrackType;

public class Track {
    public static final int MAX_DURABILITY = 10;

    private final String trackId;
    private final String fromStationId;
    private final String toStationId;
    private int durability;
    private TrackType type;

    public Track(String trackId, String fromStationId, String toStationId, int durability, TrackType type) {
        this.trackId = trackId;
        this.fromStationId = fromStationId;
        this.toStationId = toStationId;
        this.durability = durability;
        this.type = type;
    }

    public String getFromStationId() {
        return this.fromStationId;
    }

    public String getToStationId() {
        return this.toStationId;
    }

    public String getTrackId() {
        return this.trackId;
    }

    public boolean connects(String a, String b) {
        return (fromStationId.equals(a) && toStationId.equals(b))
                || (fromStationId.equals(b) && toStationId.equals(a));
    }

    public int getDurability() {
        return this.durability;
    }

    public TrackType getType() {
        return this.type;
    }

    private void setType(TrackType type) {
        this.type = type;
    }

    public void decreaseDur(int amount) {
        if (this.type.equals(TrackType.NORMAL)) {
            return;
        }

        this.durability = Math.max(0, this.durability - amount);
    }

    public boolean isBroken() {
        if (this.type == TrackType.BROKEN) {
            return true;
        }
        return false;
    }

    public void repair(int amount) {
        if (this.type != TrackType.BROKEN) {
            return;
        }
        this.durability = Math.min(MAX_DURABILITY, this.durability + amount);
    }

    public void updateStatus() {
        if (this.type.equals(TrackType.NORMAL)) {
            return;
        }

        if (this.type.equals(TrackType.UNBROKEN) && this.durability == 0) {
            setType(TrackType.BROKEN);
        } else if (this.type.equals(TrackType.BROKEN) && this.durability >= MAX_DURABILITY) {
            setType(TrackType.UNBROKEN);
        }
    }
}
