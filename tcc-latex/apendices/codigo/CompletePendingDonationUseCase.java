public Output execute(Input input, LocalDate currentDate) {
    if (input == null) {
        throw new ValidationException("Request body cannot be null");
    }
    if (currentDate == null) {
        throw new ValidationException("Current date cannot be null");
    }

    DomainID donationId = DomainIdParser.parse(input.donationId(), "donationId");
    if (input.completionDate() == null) {
        throw new ValidationException("completionDate cannot be null");
    }

    Donation donation = donationRepository.findById(donationId)
        .orElseThrow(() -> new NotFoundException("Donation not found"));

    PartyOwnership.requireSameParty(
        donation.getDonor().getPerson().getId(), input.actorPartyId());

    donation.complete(input.completionDate(), currentDate);
    donation.getDonor().registerDonation(input.completionDate(), currentDate);

    donorRepository.save(donation.getDonor());
    donationRepository.save(donation);

    return Output.from(donation);
}
