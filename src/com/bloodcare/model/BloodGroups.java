package com.bloodcare.model;

import java.util.Arrays;

/** The eight blood groups, in the order they are shown everywhere. */
public final class BloodGroups {
    public static final String[] ALL = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    private BloodGroups() {}

    public static boolean isValid(String group) {
        return group != null && Arrays.asList(ALL).contains(group);
    }

    /**
     * Red-cell donor groups compatible with a recipient group. For example, an A+ recipient
     * can receive O-, O+, A- or A+ blood. The returned array is a copy, so callers cannot
     * alter the compatibility rules.
     */
    public static String[] compatibleDonorGroups(String recipientGroup) {
        return switch (recipientGroup) {
            case "O-" -> new String[]{"O-"};
            case "O+" -> new String[]{"O-", "O+"};
            case "A-" -> new String[]{"O-", "A-"};
            case "A+" -> new String[]{"O-", "O+", "A-", "A+"};
            case "B-" -> new String[]{"O-", "B-"};
            case "B+" -> new String[]{"O-", "O+", "B-", "B+"};
            case "AB-" -> new String[]{"O-", "A-", "B-", "AB-"};
            case "AB+" -> ALL.clone();
            default -> new String[0];
        };
    }
}
