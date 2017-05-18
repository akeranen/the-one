package routing.util;

import core.Settings;
import core.SettingsError;
import core.SimClock;

/**
 * Abstract class for rating mechanisms updating after a certain interval.
 *
 * Created by Britta Heymann on 18.05.2017.
 */
public abstract class AbstractIntervalRatingMechanism {
    /**
     * Length of a time window in seconds -setting id ({@value}).
     * How many seconds a time window lasts. After each time window, the rating mechanism updates using the most
     * recent data.
     */
    public static final String WINDOW_LENGTH_S = "windowLength";

    private double windowLength;
    private double nextWindowEnd;

    protected AbstractIntervalRatingMechanism(Settings s) {
        this.windowLength = s.getDouble(WINDOW_LENGTH_S);
        if (this.windowLength <= 0) {
            throw new SettingsError("Window length must be positive!");
        }
        this.nextWindowEnd = windowLength;
    }

    /**
     * Copy constructor.
     */
    protected AbstractIntervalRatingMechanism(AbstractIntervalRatingMechanism ratingMechanism) {
        this.windowLength = ratingMechanism.windowLength;
        this.nextWindowEnd = ratingMechanism.nextWindowEnd;
    }

    /**
     * Updates the rating mechanism if a time window has ended.
     * Call this method in each simulation step.
     */
    public void update() {
        while (SimClock.getTime() >= this.nextWindowEnd) {
            this.updateRatingMechanism();
            this.nextWindowEnd += this.windowLength;
        }
    }

    /**
     * Updates the rating mechanism after a time window has ended.
     */
    protected abstract void updateRatingMechanism();
}
