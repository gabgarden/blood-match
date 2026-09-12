package bloodmatch.domain.roles.person.donor;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.person.PersonRole;
import bloodmatch.domain.shared.policy.DonationPolicy;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;

public class Donor extends PersonRole {

    public static final double DEFAULT_MAX_RECOMMENDATION_DISTANCE_KM = 30.0;

    private BloodType bloodType;
    private LocalDate lastDonationDate;
    private double weight;
    private LocalDate weightUpdatedAt;
    private double maxRecommendationDistanceKm;

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
        this.weightUpdatedAt = LocalDate.now();
        this.maxRecommendationDistanceKm = DEFAULT_MAX_RECOMMENDATION_DISTANCE_KM;
    }

  protected Donor(Person person, BloodType bloodType, double weight, DomainID id) {
    super(person, id);

    if (bloodType == null) {
      throw new IllegalArgumentException("Blood type cannot be null");
    }
    if (weight < 50) {
      throw new IllegalArgumentException("Minimum weight is 50kg");
    }

    this.bloodType = bloodType;
    this.weight = weight;
    this.weightUpdatedAt = LocalDate.now();
    this.maxRecommendationDistanceKm = DEFAULT_MAX_RECOMMENDATION_DISTANCE_KM;
  }

  public static Donor reconstitute(Person person, BloodType bloodType, double weight, LocalDate lastDonationDate,
      Double maxRecommendationDistanceKm, DomainID id, LocalDate weightUpdatedAt) {
    return reconstitute(person, bloodType, weight, lastDonationDate, maxRecommendationDistanceKm, id, weightUpdatedAt,
        null);
  }

  public static Donor reconstitute(Person person, BloodType bloodType, double weight, LocalDate lastDonationDate,
      Double maxRecommendationDistanceKm, DomainID id, LocalDate weightUpdatedAt, Long version) {
    Donor donor = new Donor(person, bloodType, weight, id);
    if (lastDonationDate != null) {
      donor.registerDonation(lastDonationDate, lastDonationDate);
    }
    if (maxRecommendationDistanceKm != null) {
      donor.updateMaxRecommendationDistanceKm(maxRecommendationDistanceKm);
    }
    if (weightUpdatedAt != null) {
      donor.weightUpdatedAt = weightUpdatedAt;
    }
    donor.setVersion(version);
    return donor;
  }

  public boolean canDonateTo(BloodType requestedType) {

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
            .plusMonths(DonationPolicy.getDonationIntervalInMonths())
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

        if (Double.compare(this.weight, weight) != 0) {
            this.weightUpdatedAt = LocalDate.now();
        }

        this.bloodType = bloodType;
        this.weight = weight;
    }

    public void updateBloodType(BloodType bloodType) {
        if (bloodType == null) {
            throw new IllegalArgumentException("Blood type cannot be null");
        }
        this.bloodType = bloodType;
    }

    public void updateWeight(double weight) {
        if (weight < 50) {
            throw new IllegalArgumentException("Minimum weight is 50kg");
        }
        if (Double.compare(this.weight, weight) != 0) {
            this.weight = weight;
            this.weightUpdatedAt = LocalDate.now();
        }
    }

    public void updateMaxRecommendationDistanceKm(double maxRecommendationDistanceKm) {
        if (maxRecommendationDistanceKm <= 0) {
            throw new IllegalArgumentException("Maximum recommendation distance must be greater than zero");
        }

        this.maxRecommendationDistanceKm = maxRecommendationDistanceKm;
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

    public LocalDate getWeightUpdatedAt() {
        return weightUpdatedAt;
    }

    public double getMaxRecommendationDistanceKm() {
        return maxRecommendationDistanceKm;
    }
}
