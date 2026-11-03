package com.bloodcare.model;

import java.time.LocalDate;

/**
 * Donor - a registered member who can give blood (the account type behind every normal sign-in).
 * Remembers the date of the last approved donation and, with an EligibilityChecker, answers
 * "am I eligible now?" and "when can I donate next?". A donor can also ask for blood: toSeeker()
 * returns the same person acting as a BloodSeeker.
 */
public class Donor extends User {
    private LocalDate lastDonationDate;

    public Donor(int id, String name, String email, String phone, String bloodGroup, String district) {
        super(id, name, email, phone, bloodGroup, district);
    }

    @Override
    public String getRole() {
        return "USER";
    }

    @Override
    public String getDisplayRole() {
        return "Donor";
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    /** Remembers a donation date; only a later date than the one already known replaces it. */
    public void recordDonation(LocalDate date) {
        if (date != null && (lastDonationDate == null || date.isAfter(lastDonationDate))) {
            lastDonationDate = date;
        }
    }

    public boolean isEligible(EligibilityChecker checker) {
        return lastDonationDate == null || !checker.nextEligibleDate(lastDonationDate).isAfter(LocalDate.now());
    }

    /** Today if already eligible, otherwise the first day on which the donor may give blood again. */
    public LocalDate getNextEligibleDate(EligibilityChecker checker) {
        return lastDonationDate == null ? LocalDate.now() : checker.nextEligibleDate(lastDonationDate);
    }

    /** The same person, acting as someone who needs blood. */
    public BloodSeeker toSeeker() {
        return new BloodSeeker(getId(), getName(), getEmail(), getPhone(), getBloodGroup(), getDistrict());
    }
}
