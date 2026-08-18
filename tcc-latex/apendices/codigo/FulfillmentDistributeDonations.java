public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
        BloodCenter bloodCenter,
        LocalDate startDate,
        LocalDate endDate) {

    List<DonationRequest> requests =
            requestRepository.findByOrganizationId(bloodCenter.getOrganization().getId());
    List<Donation> donations =
            donationRepository.findCompletedDonationsByOrganizationIdAndDateRange(
                    bloodCenter.getOrganization().getId(),
                    startOfPool(startDate, requests),
                    endDate);

    return allocateFifo(requests, donations, endDate);
}

private Map<DomainID, DonationRequestFulfillmentStatusRecord> allocateFifo(
        List<DonationRequest> requests,
        List<Donation> donations,
        LocalDate asOfDate) {

    requests.sort(OLDEST_REQUEST_FIRST);
    donations.sort(OLDEST_DONATION_FIRST);

    Map<DomainID, Integer> bags = new LinkedHashMap<>();
    for (DonationRequest request : requests) {
        bags.put(request.getId(), 0);
    }

    for (Donation donation : donations) {
        for (DonationRequest request : requests) {
            int given = bags.get(request.getId());
            boolean hasRoom = given < request.getGoalBloodBags();
            if (hasRoom && request.acceptsDonation(donation, asOfDate)) {
                bags.put(request.getId(), given + 1);
                break;
            }
        }
    }

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (DonationRequest request : requests) {
        int given = bags.get(request.getId());
        snapshot.put(
                request.getId(),
                new DonationRequestFulfillmentStatusRecord(
                        given, given >= request.getGoalBloodBags()));
    }
    return snapshot;
}
