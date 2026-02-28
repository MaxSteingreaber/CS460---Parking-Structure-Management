package controller;

import input.EmergencySignal;
import input.InductionLoopSensor;
import input.PowerFaultDetector;
import input.WeightSensor;

import java.util.List;

public class FacilitiesInputController {

    private final List<InductionLoopSensor> inductionLoops;
    private final List<WeightSensor>        weightSensors;
    private final EmergencySignal           emergencySignal;
    private final PowerFaultDetector        powerFaultDetector;

    public FacilitiesInputController(List<InductionLoopSensor> inductionLoops,
                                     List<WeightSensor> weightSensors,
                                     EmergencySignal emergencySignal,
                                     PowerFaultDetector powerFaultDetector) {
        this.inductionLoops     = inductionLoops;
        this.weightSensors      = weightSensors;
        this.emergencySignal    = emergencySignal;
        this.powerFaultDetector = powerFaultDetector;
    }

    /**
     * Iterates through all sensors, collects their current states,
     * and returns a formatted status report.
     */
    public String pollSensors() {
        StringBuilder report = new StringBuilder();
        for (InductionLoopSensor s : inductionLoops) {
            report.append(String.format("Loop[%s]: %s%n",
                    s.getSensorId(), s.detect() ? "DETECTED" : "CLEAR"));
        }
        for (WeightSensor s : weightSensors) {
            report.append(String.format("Weight[%s]: %s%n",
                    s.getSpaceId(), s.isOccupied() ? "OCCUPIED" : "EMPTY"));
        }
        return report.toString();
    }

    public boolean isEmergencySignalActive() {
        return emergencySignal.getState();
    }

    public boolean isPowerFaultDetected() {
        return !powerFaultDetector.checkPower();
    }

    public List<InductionLoopSensor> getInductionLoops()    { return inductionLoops; }
    public List<WeightSensor>        getWeightSensors()     { return weightSensors; }
    public EmergencySignal           getEmergencySignal()   { return emergencySignal; }
    public PowerFaultDetector        getPowerFaultDetector(){ return powerFaultDetector; }
}
