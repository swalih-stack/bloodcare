package com.bloodcare.model;

/** EmergencyRequest - a BloodRequest that is always EMERGENCY and uses a stronger alert text (overrides). */
public class EmergencyRequest extends BloodRequest {

    public EmergencyRequest(int id, int requesterId, String patientName, String bloodGroup, int units,
                            Location location, String status) {
        super(id, requesterId, patientName, bloodGroup, units, location, EMERGENCY, status);
    }

    @Override
    public String alertMessage() {
        return "EMERGENCY: " + getBloodGroup() + " blood needed immediately at " + getLocation()
                + ". Open Help Requests to respond.";
    }
}
