package core;

/**
 * Classes implementing this interface have an address which can be used for identification
 *
 * Created by Marius Meyer on 10.03.17.
 */
@FunctionalInterface
public interface Addressable {
    /**
     * Returns the address of the object
     *
     * @return address of the object
     */
    int getAddress();

}
