package com.bloodcare.model;

/** BloodSeeker - a member acting as someone who needs blood for a patient. Creates BloodRequest objects. */
public class BloodSeeker extends User {

    public BloodSeeker(int id, String name, String email, String phone, String bloodGroup, String district) {
        super(id, name, email, phone, bloodGroup, district);
    }

    @Override
    public String getRole() {
        return "USER";
    }

    @Override
    public String getDisplayRole() {
        return "Blood Seeker";
    }

    /**
     * Builds a new request for a patient. The kind of object depends on the urgency (an EMERGENCY request is an
     * EmergencyRequest). Throws IllegalArgumentException with a readable message if anything is missing or invalid.
     */
    public BloodRequest createRequest(String patient, String bloodGroup, int units, String hospital,
                                      String district, String urgency) {
        if (patient == null || patient.isBlank() || hospital == null || hospital.isBlank() || district == null) {
            throw new IllegalArgumentException("Patient name, hospital and district are required.");
        }
        return BloodRequest.create(getId(), patient, bloodGroup, units, new Location(district, hospital), urgency);
    }
}
