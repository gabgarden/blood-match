package bloodmatch.request.domain;

import bloodmatch.request.domain.DonationRequest;
import bloodmatch.role.domain.person.donor.Donor;

public interface DonorRecommendationPolicyInterface {

  boolean isSatisfiedBy(
      Donor donor,
      DonationRequest request);

}