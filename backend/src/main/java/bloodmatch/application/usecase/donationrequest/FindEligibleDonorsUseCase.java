package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.matching.DonorMatchingService;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.interfaces.DonorRecommendationPolicyInterface;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FindEligibleDonorsUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonorMatchingService donorMatchingService;
  private final DonorRecommendationPolicyInterface recommendationPolicy;

  public FindEligibleDonorsUseCase(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonorRepositoryInterface donorRepository,
      DonorMatchingService donorMatchingService) {
    this(donationRequestRepository, donorRepository, donorMatchingService, null);
  }

  public FindEligibleDonorsUseCase(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonorRepositoryInterface donorRepository,
      DonorMatchingService donorMatchingService,
      DonorRecommendationPolicyInterface recommendationPolicy) {

    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    if (donorMatchingService == null)
      throw new IllegalArgumentException("DonorMatchingService cannot be null");

    this.donationRequestRepository = donationRequestRepository;
    this.donorRepository = donorRepository;
    this.donorMatchingService = donorMatchingService;
    this.recommendationPolicy = recommendationPolicy;
  }

  public List<Donor> execute(
      DomainID requestId,
      List<DomainID> donorIds,
      LocalDate currentDate) {

    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");
    if (donorIds == null)
      throw new IllegalArgumentException("Donor ids cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

    List<Donor> donors = new ArrayList<>();
    for (DomainID donorId : donorIds) {
      if (donorId == null)
        throw new IllegalArgumentException("Donor id cannot be null");

        Donor donor = donorRepository.findByPartyId(donorId)
          .orElseThrow(() -> new IllegalArgumentException("Donor role not found: " + donorId.getValue()));

      donors.add(donor);
    }

    List<Donor> eligible = donorMatchingService.findEligibleDonors(request, donors, currentDate);

    if (recommendationPolicy == null)
      return eligible;

    List<Donor> recommended = new ArrayList<>();
    for (Donor donor : eligible) {
      if (recommendationPolicy.isSatisfiedBy(donor, request)) {
        recommended.add(donor);
      }
    }

    return recommended;
  }
}