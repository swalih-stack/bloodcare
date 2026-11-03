package com.bloodcare.model;

import java.util.Arrays;

/**
 * User - abstract base class of every account (INHERITANCE: Donor, BloodSeeker and Admin extend it).
 *
 * ENCAPSULATION: all fields are private. The id and email never change; the profile fields can only be
 * changed through updateProfile(), which validates them first, so a User can never hold invalid data.
 * POLYMORPHISM: each subclass answers getRole() and getDisplayRole() in its own way.
 * Used by: UserDao (creates objects), MainFrame (chooses the dashboard), every dashboard page.
 */
public abstract class User {
    private final int id;
    private final String email;
    private String name;
    private String phone;
    private String bloodGroup;
    private String district;

    protected User(int id, String name, String email, String phone, String bloodGroup, String district) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.district = district;
    }

    /** Factory: the role stored in the database decides which subclass is created. */
    public static User create(int id, String role, String name, String email, String phone,String bloodGroup, String district) {
        if ("ADMIN".equals(role)) return new Admin(id, name, email, phone, bloodGroup, district);
        return new Donor(id, name, email, phone, bloodGroup, district);
    }

    /** The rules for the editable profile fields. Returns a message, or null when everything is valid. */
    public static String profileProblem(String name, String phone, String bloodGroup, String district) {
        if (name == null || name.isBlank()) return "Name is required.";
        if (phone == null || !phone.matches("\\d{10}")) return "Phone must be exactly 10 digits.";
        if (!BloodGroups.isValid(bloodGroup)) return "Choose a blood group.";
        if (!Districts.isValid(district)) return "Choose a Kerala district.";
        return null;
    }

    /** Changes the editable profile fields, or throws IllegalArgumentException if any value is invalid. */
    public void updateProfile(String name, String phone, String bloodGroup, String district) {
        String problem = profileProblem(name, phone, bloodGroup, district);
        if (problem != null) throw new IllegalArgumentException(problem);
        this.name = name.trim();
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.district = district;
    }

    /** "USER" or "ADMIN" - the value stored in the users.role column. */
    public abstract String getRole();

    /** Text shown under the name in the sidebar. */
    public abstract String getDisplayRole();

    public boolean isAdmin() {
        return false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return name.split(" ")[0];
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public String getDistrict() {
        return district;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + ", " + name + "]";
    }
}
