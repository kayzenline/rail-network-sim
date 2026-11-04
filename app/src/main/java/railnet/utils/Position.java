package railnet.utils;

/**
 * A point in the network plane, with helpers for distance and one-tick movement.
 */
public class Position {
    private double x;
    private double y;

    // train directions are indicated by "up" and "down"
    // here's a wiki article if you are interested: https://en.wikipedia.org/wiki/Rail_directions

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public static Position calculateMidPoint(Position position1, Position position2) {
        return new Position((position1.getX() + position2.getX()) / 2, (position1.getY() + position2.getY()) / 2);
    }

    public Position calculateNewPosition(Position finalPosition, double speed) {
        double distanceToTravel = Math
                .sqrt(Math.pow(finalPosition.getX() - x, 2) + Math.pow(finalPosition.getY() - y, 2));
        double newX = x + (finalPosition.getX() - x) * speed / distanceToTravel;
        double newY = y + (finalPosition.getY() - y) * speed / distanceToTravel;
        return new Position(newX, newY);
    }

    @Override
    public String toString() {
        return "Position [x=" + x + ", y=" + y + "]";
    }

    /**
     * @return true if, at given the speed, the train will arrive at the station
     * before or at the end of the tick. Prevents the train overshooting the station.
     */
    public boolean isInBound(Position destination, double speed) {
        double distance = Math
                .sqrt(Math.pow(destination.getX() - this.x, 2) + Math.pow(destination.getY() - this.y, 2));
        return distance <= speed;
    }

    public double distance(Position destination) {
        return Math.sqrt(Math.pow(destination.getX() - this.x, 2) + Math.pow(destination.getY() - this.y, 2));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Position other = (Position) obj;
        if (Math.abs(x - other.x) > 0.01)
            return false;
        if (Math.abs(y - other.y) > 0.01)
            return false;
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(other.x) || Double.isNaN(other.y))
            return false;
        return true;
    }
}
