package com.bloodcare.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * DonorEligibilityChecker - the standard rules: age 18-65, weight at least 50 kg, and at least
 * 90 days between two donations. Implements EligibilityChecker. The limits are private final fields.
 */
public class DonorEligibilityChecker implements EligibilityChecker {
    /** The checker the application uses. */
    public static final DonorEligibilityChecker DEFAULT = new DonorEligibilityChecker(18, 65, 50, 90);

    private final int minAge;
    private final int maxAge;
    private final int minWeightKg;
    private final int waitDays;

    public DonorEligibilityChecker(int minAge, int maxAge, int minWeightKg, int waitDays) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.minWeightKg = minWeightKg;
        this.waitDays = waitDays;
    }

    @Override
    public boolean isEligible(Donation donation, LocalDate lastApproved) {
        return getIneligibilityReason(donation, lastApproved) == null;
    }

    @Override
    public String getIneligibilityReason(Donation donation, LocalDate lastApproved) {
        if (donation.getDonorAge() < minAge || donation.getDonorAge() > maxAge) {
            return "Age must be between " + minAge + " and " + maxAge;
        }
        if (donation.getWeightKg() < minWeightKg) return "Weight is below " + minWeightKg + " kg";
        if (lastApproved != null && Math.abs(ChronoUnit.DAYS.between(lastApproved, donation.getDate())) < waitDays) {
            return "Gave blood less than " + waitDays + " days apart (last: " + lastApproved + ")";
        }
        return null;
    }

    @Override
    public LocalDate nextEligibleDate(LocalDate lastDonation) {
        return lastDonation.plusDays(waitDays);
    }

    public int getWaitDays() {
        return waitDays;
    }
}
