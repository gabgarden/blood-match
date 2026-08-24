public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
        BloodCenter bloodCenter,
        LocalDate asOfDate) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    List<DonationRequest> requests = requestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId),
            asOfDate);
    if (requests.isEmpty()) {
        return Map.of();
    }
    return allocateLoaded(requests, asOfDate);
}

private Map<DomainID, DonationRequestFulfillmentStatusRecord> allocateLoaded(
        List<DonationRequest> requests,
        LocalDate asOfDate) {
    List<Donation> allDonations =
            donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(
                    organizationIdsOf(bloodCentersOf(requests)),
                    startOfPool(requests, asOfDate),
                    asOfDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (BloodCenter bloodCenter : bloodCentersOf(requests)) {
        snapshot.putAll(allocateFifo(
                requestsAt(bloodCenter, requests),
                donationsAt(bloodCenter, allDonations),
                asOfDate));
    }
    return snapshot;
}

private static Map<DomainID, DonationRequestFulfillmentStatusRecord> allocateFifo(
        List<DonationRequest> requests,
        List<Donation> donations,
        LocalDate asOfDate) {
    List<DonationRequest> requestsOldestFirst = sorted(requests, OLDEST_REQUEST_FIRST);
    List<Donation> donationsOldestFirst = sorted(
            donations.stream().filter(Donation::isCompleted).toList(),
            OLDEST_DONATION_FIRST);

    Map<DomainID, Integer> bags = new LinkedHashMap<>();
    for (DonationRequest request : requestsOldestFirst) {
        bags.put(request.getId(), 0);
    }

    for (Donation donation : donationsOldestFirst) {
        for (DonationRequest request : requestsOldestFirst) {
            int given = bags.get(request.getId());
            boolean hasRoom = given < request.getGoalBloodBags();
            if (hasRoom && request.acceptsDonation(donation, asOfDate)) {
                bags.put(request.getId(), given + 1);
                break;
            }
        }
    }

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (DonationRequest request : requestsOldestFirst) {
        int given = bags.get(request.getId());
        snapshot.put(
                request.getId(),
                new DonationRequestFulfillmentStatusRecord(
                        given, given >= request.getGoalBloodBags()));
    }
    return snapshot;
}
