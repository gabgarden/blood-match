package bloodmatch.domain.shared.policy;

public class DonationPolicy {
    private static int donationIntervalInMonths = 3;

    public static int getDonationIntervalInMonths() {
        return donationIntervalInMonths;
    }

    public static void setDonationIntervalInMonths(int months) {
       if (months <= 0) {
            throw new IllegalArgumentException("The interval must be greater than zero.");
        }
        donationIntervalInMonths = months;
    }
}
