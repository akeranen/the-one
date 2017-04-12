package core;

/**
 * An object representing a data item in a database.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class DisasterData {
    /**
     * Types of data used in {@link applications.DatabaseApplication}.
     */
    public enum DataType {
        /**
         * Data is part of a map.
         */
        MAP,
        /**
         * Data represents a marker on the map.
         */
        MARKER,
        /**
         * Data represents a person's skill.
         */
        SKILL,
        /**
         * Data represents a person's resource.
         */
        RESOURCE
    }

    /** Type of the data object. */
    private DataType type;
    /** Size of the data object. */
    private int size;
    /** Time of creation in simulator time. */
    private double creation;
    /** Location of the object represented by the data. */
    private Coord location;

    /**
     * Initializes a new instance of the {@link DisasterData} class.
     * @param type Type of the data object.
     * @param size Size of the data object.
     * @param creation Time of creation in simulator time.
     * @param location Location of the object represented by the data.
     */
    public DisasterData(DataType type, int size, double creation, Coord location) {
        this.type = type;
        this.size = size;
        this.creation = creation;
        this.location = location;
    }

    /**
     * Gets the data's type.
     * @return The data's type.
     */
    public DataType getType() {
        return this.type;
    }

    /**
     * Gets the data's size.
     * @return The data's size.
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Gets the creation time of the data.
     * @return The creation time of the data in simulator time.
     */
    public double getCreation() {
        return this.creation;
    }

    /**
     * Gets the location of the object presented by the data.
     * @return Location of the object presented by the data.
     */
    public Coord getLocation() {
        return this.location;
    }
}
