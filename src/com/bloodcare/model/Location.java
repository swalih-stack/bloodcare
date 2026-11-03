package com.bloodcare.model;

/**
 * Location - a place in Kerala: the district plus a specific place (a hospital or a blood bank address).
 * Immutable value object: its fields are private and final, and the constructor refuses a district
 * that is not one of the 14 Kerala districts.
 * Used by: BloodRequest (where the blood is needed) and BloodBank (where it is located).
 */
public final class Location {
    private final String district;
    private final String place;

    /** Creates a location, checking the district (use this for new data typed by a user). */
    public Location(String district, String place) {
        if (!Districts.isValid(district)) throw new IllegalArgumentException("Choose a Kerala district.");
        this.district = district;
        this.place = place == null ? "" : place.trim();
    }
    private Location(String district, String place, boolean unchecked) {
        this.district = district;
        this.place = place == null ? "" : place.trim();
    }
    /** Rebuilds a location read from the database without re-checking old rows. */
    public static Location restore(String district, String place) {
        return new Location(district, place, true);
    }
    public String getDistrict() {
        return district;
    }
    public String getPlace() {
        return place;
    }
    @Override
    public String toString() {
        return place.isEmpty() ? district : place + ", " + district;
    }
}