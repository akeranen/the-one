package core;

/**
 * This is an interface for all classes that want to be notified of a specific node's battery becoming completely empty.
 * Created by Ansgar Mährlein on 24.02.2017.
 * @author Ansgar Mährlein
 */
@FunctionalInterface
public interface EnergyListener {
    /**
     * This Method is called when the battery of the node ran empty.
     */
    void batteryDied();
}
