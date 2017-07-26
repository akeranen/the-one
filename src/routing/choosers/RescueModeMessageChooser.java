package routing.choosers;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SettingsError;
import routing.DisasterRouter;
import routing.MessageChoosingStrategy;
import routing.MessageRouter;
import routing.util.DatabaseApplicationUtil;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A host with very low power enters a "RESCUE"-mode: To prevent data-loss it will try to get rid of all messages it is
 * transporting. To save the messages they will be given to any other encountered node regardless of whether it is a
 * good or bad relay.
 *
 * Created by Britta Heymann on 23.07.2017.
 */
public class RescueModeMessageChooser implements MessageChoosingStrategy{
    /** Namespace for all rescue mode message chooser settings. */
    public static final String RESCUE_MODE_MESSAGE_CHOOSER_NS = "RescueModeMessageChooser";

    /**
     * If a neighbor's relative power level is below this threshold, no messages will be sent. -setting id ({@value}).
     */
    public static final String POWER_THRESHOLD = "powerThreshold";

    /**
     * The number of seconds a data item modification counts as recent. -setting id ({@value}).
     */
    public static final String SHORT_TIMESPAN_THRESHOLD = "shortTimespanThreshold";

    /** If a neighbor's relative power level is below this threshold, no messages will be sent. */
    private double powerThreshold;

    /** The number of seconds a data item modification counts as recent. */
    private int shortTimespanThreshold;

    /** Host choosing the messages. */
    private DTNHost attachedHost;

    /**
     * Initializes a new instance of the {@link RescueModeMessageChooser} class.
     */
    public RescueModeMessageChooser() {
        // Read settings:
        Settings s = new Settings(RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.powerThreshold = s.getDouble(POWER_THRESHOLD);
        this.shortTimespanThreshold = s.getInt(SHORT_TIMESPAN_THRESHOLD);
        s.restoreNameSpace();

        // Check thresholds are valid.
        if (this.powerThreshold < 0 || this.powerThreshold > 1) {
            throw new SettingsError("Power threshold must be in [0, 1], but is " + this.powerThreshold + "!");
        }
        if (this.shortTimespanThreshold < 0) {
            throw new SettingsError(
                    "Short timespan threshold must be natural, but is " + this.shortTimespanThreshold + "!");
        }
    }

    /** Copy constructor. */
    private RescueModeMessageChooser(RescueModeMessageChooser chooser) {
        this.powerThreshold = chooser.powerThreshold;
        this.shortTimespanThreshold = chooser.shortTimespanThreshold;
    }

    /**
     * Chooses non-direct messages to send.
     *
     * @param messages    All messages in buffer.
     * @param connections All connections the host has.
     * @return Which messages should be send to which neighbors.
     */
    @Override
    public Collection<Tuple<Message, Connection>> chooseNonDirectMessages(
            Collection<Message> messages, List<Connection> connections) {
        Collection<Tuple<Message, Connection>> chosenMessages = new ArrayList<>();
        List<Connection> availableConnections = new ArrayList<>();

        // Add ordinary messages: Send everything to all available connections.
        for (Connection con : connections) {
            DTNHost neighbor = con.getOtherNode(this.attachedHost);
            DisasterRouter.checkRouterIsDisasterRouter(neighbor.getRouter());
            DisasterRouter neighborRouter = (DisasterRouter)neighbor.getRouter();

            if (neighborRouter.isTransferring() || neighborRouter.remainingEnergyRatio() < this.powerThreshold) {
                continue;
            }

            availableConnections.add(con);
            for (Message m : messages) {
                if (!m.isFinalRecipient(neighbor) && !neighborRouter.hasMessage(m.getId())) {
                    chosenMessages.add(new Tuple<>(m, con));
                }
            }
        }

        // Wrap useful data stored at host which has been modified recently into data messages to available neighbors
        // and add them to the messages to sent.
        chosenMessages.addAll(DatabaseApplicationUtil.wrapRecentUsefulDataIntoMessages(
                this.attachedHost, availableConnections, this.shortTimespanThreshold));

        return chosenMessages;
    }

    /**
     * Creates a replicate of this message choosing strategy. The replicate has the same settings as this message
     * choosing strategy but is attached to the provided router and has no attached host.
     *
     * @param attachedRouter Router choosing the messages.
     * @return The replicate.
     */
    @Override
    public MessageChoosingStrategy replicate(MessageRouter attachedRouter) {
        return new RescueModeMessageChooser(this);
    }

    /**
     * Sets the attached host.
     *
     * @param host host choosing the messages.
     */
    @Override
    public void setAttachedHost(DTNHost host) {
        this.attachedHost = host;
    }

    /**
     * Gets the power threshold.
     * @return The power threshold.
     */
    public double getPowerThreshold() {
        return powerThreshold;
    }

    /**
     * Gets the short timespan threshold.
     * @return The short timespan threshold.
     */
    public int getShortTimespanThreshold() {
        return shortTimespanThreshold;
    }
}
