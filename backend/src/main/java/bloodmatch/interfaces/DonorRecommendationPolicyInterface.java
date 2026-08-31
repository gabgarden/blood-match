package bloodmatch.interfaces;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.person.donor.Donor;

public interface DonorRecommendationPolicyInterface {

  boolean isSatisfiedBy(
      Donor donor,
      DonationRequest request);

}