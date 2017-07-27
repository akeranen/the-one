package routing.util;

import applications.DatabaseApplication;
import core.Application;
import core.Connection;
import core.DTNHost;
import core.DataMessage;
import core.Message;
import routing.MessageRouter;
import util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains useful methods for scenarios where {@link applications.DatabaseApplication} may be used with a router type.
 *
 * Created by Britta Heymann on 19.04.2017.
 */
public final class DatabaseApplicationUtil {
    /**
     * Private default constructor throwing an {@link IllegalAccessError} if called.
     * Reason: This is a utility class with static methods only that should never be initialized.
     */
    private DatabaseApplicationUtil() {
        throw new IllegalAccessError("Utility class.");
    }

    /**
     * Checks whether the router has no messages to send.
     *
     * @param router The router to check.
     * @return True iff the message collection is empty and the router does not have a {@link DatabaseApplication}.
     */
    public static boolean hasNoMessagesToSend(MessageRouter router) {
        return router.getMessageCollection().isEmpty()
                && DatabaseApplicationUtil.findDatabaseApplication(router) == null;
    }

    /**
     * Fetches database synchronization messages from the provided router that should be sent to neighboring hosts.
     *
     * @param router The router to do this for.
     * @param host The DTNHost the router is attached to.
     * @param connections Connections to find data messages for.
     * @return The created messages and the connection they should be sent over.
     */
    public static List<Tuple<Message, Connection>> wrapUsefulDataIntoMessages(
            MessageRouter router, DTNHost host, List<Connection> connections) {
        // First find out if we even have a database application.
        DatabaseApplication application = DatabaseApplicationUtil.findDatabaseApplication(router);
        if (application == null) {
            return new ArrayList<>(0);
        }

        // Then fetch prototypes of the important messages ...
        List<DataMessage> messagePrototypes = application.wrapUsefulDataIntoMessages(host);

        // ... and create real instances for each of them:
        List<Tuple<Message, Connection>> messages = new ArrayList<>(messagePrototypes.size() * connections.size());
        for (DataMessage dataMessage : messagePrototypes) {
            // For each receiver ...
            for (Connection connection : connections) {
                DTNHost receiver = connection.getOtherNode(host);

                // ... copy the message.
                DataMessage message = dataMessage.instantiateFor(receiver);
                messages.add(new Tuple<>(message, connection));
            }
        }

        // Finally return all messages.
        return messages;
    }

    /**
     * Returns the {@link DatabaseApplication} instance of the provided {@link MessageRouter}, if it has one.
     *
     * @param router The router to check for a {@link DatabaseApplication}.
     * @return The found {@link DatabaseApplication} or null if there is none.
     */
    public static DatabaseApplication findDatabaseApplication(MessageRouter router) {
        for (Application application : router.getApplications(DatabaseApplication.APP_ID)) {
            if (application instanceof DatabaseApplication) {
                return (DatabaseApplication)application;
            }
        }
        return null;
    }
}
