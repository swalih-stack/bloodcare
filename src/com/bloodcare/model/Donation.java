package com.bloodcare.model;

import java.time.LocalDate;

/**
 * Donation - one offer to donate blood. It starts PENDING; the admin approves or rejects it.
 * ENCAPSULATION: private fields, no setters; the status changes only through approve() / reject(),
 * and only from PENDING. Whether the donor is *eligible* is decided by an EligibilityChecker, not here.
 * Used by: DonationDao, DonationHistory, DonatePage, the eligibility queue.
 */
public class Donation {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final int MAX_UNITS = 2;

    private final int id;
    private final int donorId;
    private final String bloodGroup;
    private final int units;
    private final LocalDate date;
    private final int donorAge;
    private final int weightKg;
    private String status;

    private Donation(int id, int donorId, String bloodGroup, int units, LocalDate date,
                     int donorAge, int weightKg, String status) {
        this.id = id;
        this.donorId = donorId;
        this.bloodGroup = bloodGroup;
        this.units = units;
        this.date = date;
        this.donorAge = donorAge;
        this.weightKg = weightKg;
        this.status = status;
    }

    /** A new offer typed by a donor. Throws IllegalArgumentException with a readable message if invalid. */
    public static Donation offer(int donorId, String bloodGroup, int units, LocalDate date, int age, int weightKg) {
        if (!BloodGroups.isValid(bloodGroup)) throw new IllegalArgumentException("Choose a blood group.");
        if (units < 1 || units > MAX_UNITS) throw new IllegalArgumentException("Units must be 1 or " + MAX_UNITS + ".");
        if (date == null) throw new IllegalArgumentException("Enter the date as yyyy-mm-dd, for example 2026-12-01.");
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("The donation date cannot be in the past.");
        return new Donation(0, donorId, bloodGroup, units, date, age, weightKg, PENDING);
    }

    /** Rebuilds a donation read from the database. */
    public static Donation restore(int id, int donorId, String bloodGroup, int units, LocalDate date,
                                   int age, int weightKg, String status) {
        return new Donation(id, donorId, bloodGroup, units, date, age, weightKg, status);
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public boolean isApproved() {
        return APPROVED.equals(status);
    }

    /** PENDING -> APPROVED. Returns false if it was not pending. */
    public boolean approve() {
        if (!isPending()) return false;
        status = APPROVED;
        return true;
    }

    /** PENDING -> REJECTED. Returns false if it was not pending. */
    public boolean reject() {
        if (!isPending()) return false;
        status = REJECTED;
        return true;
    }

    public int getId() {
        return id;
    }

    public int getDonorId() {
        return donorId;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public int getUnits() {
        return units;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDonorAge() {
        return donorAge;
    }

    public int getWeightKg() {
        return weightKg;
    }

    public String getStatus() {
        return status;
    }
}
