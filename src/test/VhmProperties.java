package test;

import core.Settings;
import input.VhmEvent;
import movement.MovementModel;
import movement.VoluntaryHelperMovement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to extract properties of the {@link VoluntaryHelperMovement} for test purposes.
 *
 * Created by Marius Meyer on 13.04.17.
 */
public class VhmProperties extends VoluntaryHelperMovement {

    public VhmProperties(Settings s){
        super(s);
    }

    private VhmProperties(VoluntaryHelperMovement prototype){
        super(prototype);
    }

    public double getHelpTime(){
        return helpTime;
    }

    public double getHospitalWaitTime(){
        return hospitalWaitTime;
    }

    public movementMode getMode() {
        return mode;
    }

    public boolean isLocalHelper() {
        return isLocalHelper;
    }

    public void setLocalHelper(boolean localHelper) {
        isLocalHelper = localHelper;
    }

    public double getInjuryProbability() {
        return injuryProbability;
    }

    public void setInjuryProbability(double injuryProbability) {
        this.injuryProbability = injuryProbability;
    }

    public double getWaitProbability() {
        return waitProbability;
    }

    public void setWaitProbability(double waitProbability) {
        this.waitProbability = waitProbability;
    }

    public double getIntensityWeight() {
        return intensityWeight;
    }

    public void setIntensityWeight(double intensityWeight) {
        this.intensityWeight = intensityWeight;
    }

    public VhmEvent getChosenDisaster() {
        return chosenDisaster;
    }

    public VhmEvent getChosenHospital() {
        return chosenHospital;
    }

    public List<VhmEvent> getDisasters() {
        return Collections.synchronizedList(new ArrayList<>(disasters));
    }

    public List<VhmEvent> getHospitals() {
        return Collections.synchronizedList(new ArrayList<>(hospitals));
    }

    @Override
    public MovementModel replicate(){
        return new VhmProperties((VoluntaryHelperMovement) super.replicate());
    }
}
