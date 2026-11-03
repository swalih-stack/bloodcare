package com.bloodcare.model;

/** Admin - the system administrator. Reviews requests and donations, manages stock and banks. */
public class Admin extends User {

    public Admin(int id, String name, String email, String phone, String bloodGroup, String district) {
        super(id, name, email, phone, bloodGroup, district);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getDisplayRole() {
        return "System Administrator";
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
