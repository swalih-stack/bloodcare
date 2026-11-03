package com.bloodcare.model;

import java.util.Arrays;

/** The 14 districts of Kerala. A user's "city" and a request's "city" always hold one of these names. */
public final class Districts {
    public static final String[] ALL = {
        "Thiruvananthapuram", "Kollam", "Pathanamthitta", "Alappuzha", "Kottayam", "Idukki", "Ernakulam",
        "Thrissur", "Palakkad", "Malappuram", "Kozhikode", "Wayanad", "Kannur", "Kasaragod"
    };

    private Districts() {}

    public static boolean isValid(String name) {
        return name != null && Arrays.asList(ALL).contains(name);
    }
}
