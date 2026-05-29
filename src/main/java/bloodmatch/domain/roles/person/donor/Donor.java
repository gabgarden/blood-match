package bloodmatch.domain.roles.person.donor;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.person.PersonRole;
import bloodmatch.domain.shared.valueObjects.BloodType;

import java.time.LocalDate;

public class Donor extends PersonRole {

    private BloodType bloodType;
    private LocalDate lastDonationDate;
    private double weight;

    public Donor(Person person, BloodType bloodType, double weight) {
        super(person);

        if (bloodType == null) {
            throw new IllegalArgumentException("Blood type cannot be null");
        }
        if (weight < 50) {
            throw new IllegalArgumentException("Minimum weight is 50kg");
        }

        this.bloodType = bloodType;
        this.weight = weight;
    }

    public boolean canDonateTo(BloodType requestedType) {

        if (requestedType == null) {
            throw new IllegalArgumentException("Requested blood type cannot be null");
        }

        return bloodType.canDonateTo(requestedType);
    }

    protected boolean hasValidAge(LocalDate currentDate) {

        int age = getPerson().getAge(currentDate);

        return age >= 16 && age <= 69;
    }

    public boolean isEligibleToDonate(LocalDate currentDate) {

        if (!hasValidAge(currentDate)) {
            return false;
        }
        if (lastDonationDate == null) {
            return true;
        }
        return !lastDonationDate
            .plusMonths(3)
            .isAfter(currentDate);
    }

    public void registerDonation(LocalDate donationDate) {
        registerDonation(donationDate, LocalDate.now());
    }

    public void registerDonation(LocalDate donationDate, LocalDate currentDate) {

        if (donationDate == null) {
            throw new IllegalArgumentException("Donation date cannot be null");
        }
        if (currentDate == null) {
            throw new IllegalArgumentException("Current date cannot be null");
        }
        if (donationDate.isAfter(currentDate)) {
            throw new IllegalArgumentException("Donation date cannot be in the future");
        }

        this.lastDonationDate = donationDate;
    }

    public void updateProfile(BloodType bloodType, double weight) {

        if (bloodType == null) {
            throw new IllegalArgumentException("Blood type cannot be null");
        }
        if (weight < 50) {
            throw new IllegalArgumentException("Minimum weight is 50kg");
        }

        this.bloodType = bloodType;
        this.weight = weight;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public double getWeight() {
        return weight;
    }

}
