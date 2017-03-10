package core;

/**
 * Classes implementing this interface have an address and
 * a certain type, which can be used for identification
 *
 * Created by Marius Meyer on 10.03.17.
 */
public interface Addressable {

    /**
     * Defines different types of addresses.
     */
    enum AddressType {
        /**
         * An address of a single node
         */
        HOST,
        /**
         * An address of a group of nodes
         */
        GROUP
    }

    /**
     * Returns the address of the object
     *
     * @return address of the object
     */
    int getAddress();

    /**
     * Returns the type of the address returned by the object
     * @return type of the address
     */
    AddressType getAddressableType();
}
