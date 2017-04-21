package core;

import java.util.Random;

/**
 * Wrapper class around Random, that logs all accesses to a subset of the methods in Random,
 * that is used in the mobility models. Useful for reproducibility testing and debugging.
 *
 * Created by Ansgar Mährlein on 21.04.2017.
 */
public class DebugRandom extends Random {

    private DebugPrinter debugPrinter;
    private Random rng;
    private int counter;

    public DebugRandom(long seed) {
        debugPrinter = new DebugPrinter("DebugRandom");
        this.rng = new Random(seed);
        counter = 0;
        debugPrinter.println("DebugRandom initialized with seed " + seed);
    }

    @Override
    public double nextDouble() {
        counter++;
        double d = rng.nextDouble();
        debugPrinter.println("draw nr. " + counter + " is the double " + d);
        return d;
    }

    @Override
    public int nextInt() {
        counter++;
        int i = rng.nextInt();
        debugPrinter.println("draw nr. " + counter + " is the int " + i);
        return i;
    }

    @Override
    public int nextInt(int bound) {
        counter++;
        int b = rng.nextInt(bound);
        debugPrinter.println("draw nr. " + counter + " is the bounded int " + b + " bound " + bound);
        return b;
    }

    @Override
    synchronized public double nextGaussian() {
        counter++;
        double g = rng.nextGaussian();
        debugPrinter.println("draw nr. " + counter + " is the gaussian " + g);
        return g;
    }
}
