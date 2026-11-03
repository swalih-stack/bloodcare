package com.bloodcare.model;

import java.time.LocalDate;

/**
 * EligibilityChecker - INTERFACE for "may this person donate?". It says WHAT is asked, not HOW it is decided,
 * so the rules can change (or a stricter checker can be plugged in) without touching the code that uses it.
 * Implemented by DonorEligibilityChecker; used by DonationDao and the donor dashboard.
 */
public interface EligibilityChecker {

    /** Is the donor of this offer eligible, given the date of their last approved donation (null if none)? */
    boolean isEligible(Donation donation, LocalDate lastApproved);

    /** Why the donor is NOT eligible, or null if they are. */
    String getIneligibilityReason(Donation donation, LocalDate lastApproved);

    /** The first day a person who last donated on the given date may donate again. */
    LocalDate nextEligibleDate(LocalDate lastDonation);
}
