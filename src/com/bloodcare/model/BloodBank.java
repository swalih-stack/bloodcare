package com.bloodcare.model;

/**
 * BloodBank - a partner blood bank. Immutable and encapsulated; toString() is the name so it can be
 * put straight into a drop-down. BloodBank.NONE is the required-choice placeholder.
 */
public class BloodBank {
    /** Placeholder (id 0) used until an administrator chooses a blood bank. */
    public static final BloodBank NONE = new BloodBank(0, "Choose a blood bank", null, "");

    private final int id;
    private final String name;
    private final Location location;
    private final String phone;

    public BloodBank(int id, String name, Location location, String phone) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.phone = phone == null ? "" : phone;
    }

    /** A new bank typed by the admin; throws IllegalArgumentException if the name or district is missing. */
    public static BloodBank create(String name, String district, String address, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Bank name is required.");
        return new BloodBank(0, name.trim(), new Location(district, address), phone);
    }

    public boolean isNone() {
        return id == 0;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return name;
    }
}
