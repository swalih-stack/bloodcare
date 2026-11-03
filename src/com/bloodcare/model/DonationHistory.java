package com.bloodcare.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DonationHistory - all donations of one donor, with the totals the dashboard shows.
 * ENCAPSULATION: the list is private; callers get a read-only view, never the list itself.
 */
public class DonationHistory {
    private final int donorId;
    private final List<Donation> donations = new ArrayList<>();

    public DonationHistory(int donorId) {
        this.donorId = donorId;
    }

    public void addDonation(Donation donation) {
        donations.add(donation);
    }

    public int getDonorId() {
        return donorId;
    }

    public List<Donation> getDonations() {
        return Collections.unmodifiableList(donations);
    }

    /** Number of APPROVED donations. */
    public int getTotalDonations() {
        int count = 0;
        for (Donation d : donations) if (d.isApproved()) count++;
        return count;
    }

    /** Units given in APPROVED donations. */
    public int getTotalUnits() {
        int units = 0;
        for (Donation d : donations) if (d.isApproved()) units += d.getUnits();
        return units;
    }

    public int getPendingCount() {
        int count = 0;
        for (Donation d : donations) if (d.isPending()) count++;
        return count;
    }

    /** Date of the most recent APPROVED donation, or null if the donor has none. */
    public LocalDate getLastApprovedDate() {
        LocalDate last = null;
        for (Donation d : donations) {
            if (d.isApproved() && (last == null || d.getDate().isAfter(last))) last = d.getDate();
        }
        return last;
    }
}
