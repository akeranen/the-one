package test;

import core.DTNHost;
import core.Group;
import core.ModuleCommunicationBus;
import core.MulticastMessage;
import core.NetworkInterface;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import routing.ActiveRouter;
import routing.EpidemicRouter;
import routing.util.EnergyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains tests for the {@link routing.ActiveRouter} class.
 *
 * Created by Britta Heymann on 23.06.2017.
 */
public class ActiveRouterTest {
    private static final double SOME_ENERGY_LEVEL = 0.1;

    /**
     * Acceptable delta when comparing doubles for equality.
     */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    @After
    public void tearDown() {
        Group.clearGroups();
        DTNHost.reset();
    }

    /**
     * Checks that {@link ActiveRouter#remainingEnergyRatio()} returns 1 if energy is not modelled.
     */
    @Test
    public void testRemainingEnergyRatioWithoutEnergyModelling() {
        ActiveRouter router = new EpidemicRouter(new TestSettings());
        Assert.assertEquals("Expected 100% energy.", 1, router.remainingEnergyRatio(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Checks that {@link ActiveRouter#remainingEnergyRatio()} returns the correct ratio if energy is modelled.
     */
    @Test
    public void testRemainingEnergyRatioWithEnergyModelling() {
        // Prepare settings.
        TestSettings settings = new TestSettings();
        EnergyModelTest.addAllEnergySettings(settings);
        EnergyModelTest.addNecessaryInterfaceSettings(settings);

        // Initialize router with energy model knowing about a certain communication bus.
        ActiveRouter router = new EpidemicRouter(settings);
        ModuleCommunicationBus communicationBus = new ModuleCommunicationBus();
        List<NetworkInterface> interfaces = new ArrayList<>(1);
        interfaces.add(new TestInterface(settings));
        DTNHost host = new TestDTNHost(interfaces, communicationBus, settings);
        router.init(host, new ArrayList<>());
        router.update();

        // Decrease energy.
        communicationBus.updateProperty(EnergyModel.ENERGY_VALUE_ID, SOME_ENERGY_LEVEL);

        // Test remaining energy ratio.
        Assert.assertEquals("Expected different energy ratio.",
                SOME_ENERGY_LEVEL / EnergyModelTest.MAX_ENERGY, router.remainingEnergyRatio(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Checks that the successful transfer of a {@link core.MulticastMessage} to one of its recipients removes that one
     * from remaining recipients.
     */
    @Test
    public void successfulMulticastTransferChangesRemainingRecipients() {
        // Create three hosts.
        TestUtils utils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
        utils.setMessageRouterProto(new EpidemicRouter(new TestSettings()));
        DTNHost sender = utils.createHost();
        DTNHost receiver = utils.createHost();
        DTNHost remainingRecipient = utils.createHost();

        // Create a multicast between them.
        Group multicastGroup = Group.createGroup(0);
        multicastGroup.addHost(sender);
        multicastGroup.addHost(receiver);
        multicastGroup.addHost(remainingRecipient);
        MulticastMessage message = new MulticastMessage(sender, multicastGroup, "M1", 0);
        sender.createNewMessage(message);

        // Send multicast.
        sender.connect(receiver);
        sender.update(true);
        receiver.update(true);
        sender.update(true);

        // Message should have been transferred.
        Assert.assertTrue("Message should have been transferred.", !receiver.getMessageCollection().isEmpty());
        Assert.assertEquals("Only one recipient should be remaining.", 1, message.getRemainingRecipients().size());
        Assert.assertTrue("Other recipient should be remaining.",
                message.getRemainingRecipients().contains(remainingRecipient.getAddress()));
    }
}
