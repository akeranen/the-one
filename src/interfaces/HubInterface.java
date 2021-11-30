package interfaces;

import core.CBRConnection;
import core.Connection;
import core.NetworkInterface;
import core.Settings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An interface for creating communication hubs, e.g., Wi-Fi access points. With the {@code HubInterface}, node groups
 * can be assigned as hubs using the {@code hubGroups} setting (list of group names). The interface will only create
 * connections between node pairs where exactly one of the nodes is a hub. It is also possible to inter-connect the hubs
 * using the {@code connectHubs} setting (true/false).
 *
 * @author teemuk
 */
public class HubInterface
extends SimpleBroadcastInterface {

  //=============================================================================//
  // Settings
  //=============================================================================//
  public static final String HUB_GROUPS_SETTING = "hubGroups";
  public static final String CONNECT_HUBS_SETTING = "connectHubs";
  //=============================================================================//


  //=============================================================================//
  // Instance vars
  //=============================================================================//
  private final Set <String> hubGroups;
  private final boolean connectHubs;
  //=============================================================================//


  //=============================================================================//
  // NetworkInterface
  //=============================================================================//
  @Override
  public void connect( NetworkInterface anotherInterface) {
    if (super.isScanning()
        && anotherInterface.getHost().isRadioActive()
        && isWithinRange(anotherInterface)
        && !isConnected(anotherInterface)
        && (this != anotherInterface)
        && this.isHubConnection( anotherInterface ) ) {

      // new contact within range
      // connection speed is the lower one of the two speeds
      int conSpeed = anotherInterface.getTransmitSpeed(this);
      if (conSpeed > this.transmitSpeed) {
        conSpeed = this.transmitSpeed;
      }

      Connection con = new CBRConnection(this.host, this,
          anotherInterface.getHost(), anotherInterface, conSpeed);
      connect(con,anotherInterface);
    }
  }

  @Override
  public HubInterface replicate() {
    return new HubInterface( this );
  }

  public HubInterface( Settings s ) {
    super( s );
    final String[] hubs = s.getCsvSetting( HUB_GROUPS_SETTING );
    this.hubGroups = new HashSet<>();
    Collections.addAll( this.hubGroups, hubs );
    this.connectHubs = s.getBoolean( CONNECT_HUBS_SETTING, false );
  }

  public HubInterface( HubInterface copyFrom ) {
    super( copyFrom );
    this.hubGroups = copyFrom.hubGroups;
    this.connectHubs = copyFrom.connectHubs;
  }
  //=============================================================================//


  //=============================================================================//
  // Private
  //=============================================================================//
  private boolean isHubConnection( final NetworkInterface toInterface ) {
    final boolean otherIsHub = this.hubGroups.contains( toInterface.getHost().groupId );
    final boolean isHub = this.hubGroups.contains( super.getHost().groupId );
    if ( isHub && otherIsHub ) {
      return this.connectHubs;
    } else {
      return ( isHub || otherIsHub );
    }
  }
  //=============================================================================//
}
