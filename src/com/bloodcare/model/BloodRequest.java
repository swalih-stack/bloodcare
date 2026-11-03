package com.bloodcare.model;

/**
 * BloodRequest - a request for blood for one patient (NORMAL or EMERGENCY; EmergencyRequest extends it).
 *
 * ENCAPSULATION: fields are private, the only thing that changes after creation is the status, and it
 * changes only through moveTo(), which enforces the workflow  PENDING -> APPROVED -> FULFILLED  (or REJECTED).
 * POLYMORPHISM: alertMessage() and getUrgency() are overridden by EmergencyRequest.
 * Used by: BloodSeeker (creates), RequestDao (saves, applies status changes), RequestPage.
 */
public class BloodRequest {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String FULFILLED = "FULFILLED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    public static final String NORMAL = "NORMAL";
    public static final String EMERGENCY = "EMERGENCY";
    public static final String[] URGENCY_LEVELS = {NORMAL, EMERGENCY};

    public static final int MAX_UNITS = 10;

    private final int id;
    private final int requesterId;
    private final String patientName;
    private final String bloodGroup;
    private final int units;
    private final Location location;
    private final String urgency;
    private String status;

    protected BloodRequest(int id, int requesterId, String patientName, String bloodGroup, int units,
                           Location location, String urgency, String status) {
        this.id = id;
        this.requesterId = requesterId;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.units = units;
        this.location = location;
        this.urgency = urgency;
        this.status = status;
    }

    /** Creates a NEW request (status PENDING), validating every value. */
    public static BloodRequest create(int requesterId, String patientName, String bloodGroup, int units,
                                      Location location, String urgency) {
        if (patientName == null || patientName.isBlank()) throw new IllegalArgumentException("Patient name is required.");
        if (location == null || location.getPlace().isEmpty()) throw new IllegalArgumentException("Hospital is required.");
        if (!BloodGroups.isValid(bloodGroup)) throw new IllegalArgumentException("Choose a blood group.");
        if (units < 1 || units > MAX_UNITS) throw new IllegalArgumentException("Units must be between 1 and " + MAX_UNITS + ".");
        if (!isUrgency(urgency)) throw new IllegalArgumentException("Choose an urgency level.");
        return build(0, requesterId, patientName.trim(), bloodGroup, units, location, urgency, PENDING);
    }

    /** Rebuilds a request read from the database (no validation, the row was valid when saved). */
    public static BloodRequest restore(int id, int requesterId, String patientName, String bloodGroup, int units,
                                       Location location, String urgency, String status) {
        return build(id, requesterId, patientName, bloodGroup, units, location, urgency, status);
    }

    private static BloodRequest build(int id, int requesterId, String patient, String group, int units,
                                      Location location, String urgency, String status) {
        return EMERGENCY.equals(urgency)
                ? new EmergencyRequest(id, requesterId, patient, group, units, location, status)
                : new BloodRequest(id, requesterId, patient, group, units, location, urgency, status);
    }

    private static boolean isUrgency(String value) {
        for (String level : URGENCY_LEVELS) {
            if (level.equals(value)) return true;
        }
        return false;
    }

    /**
     * Moves the request to a new status if the workflow allows it. Returns null on success, otherwise the
     * reason it was refused (the status is left unchanged).
     */
    public String moveTo(String next) {
        if (!isOpen()) return "This request is already " + status + " and cannot be changed.";
        if (status.equals(next)) return "This request is already " + status + ".";
        if (!next.equals(APPROVED) && !next.equals(FULFILLED) && !next.equals(REJECTED)) {
            return "A request cannot be moved to " + next + " here.";
        }
        status = next;
        return null;
    }

    /** Still waiting for blood: PENDING or APPROVED. */
    public boolean isOpen() {
        return PENDING.equals(status) || APPROVED.equals(status);
    }

    /** Text sent to the donors of the same district as soon as the request is saved. */
    public String alertMessage() {
        return bloodGroup + " blood needed at " + location + ". Open Help Requests to respond.";
    }

    /** Text for the admin inbox. */
    public String adminSummary() {
        return "New " + getUrgency() + " blood request: " + units + " unit(s) of " + bloodGroup + " at " + location + ".";
    }

    public int getId() {
        return id;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public int getUnits() {
        return units;
    }

    public Location getLocation() {
        return location;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getStatus() {
        return status;
    }
}
