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
        snapshot.putAll(allocateProportionalThenFifo(
                requestsAt(bloodCenter, requests),
                donationsAt(bloodCenter, allDonations),
                asOfDate));
    }
    return snapshot;
}

private static Map<DomainID, DonationRequestFulfillmentStatusRecord>
        allocateProportionalThenFifo(
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

    // 1a passagem: cada solicitacao recebe ate o seu teto proporcional
    List<Donation> leftovers = distribute(
            requestsOldestFirst, donationsOldestFirst, bags,
            proportionalLimits(requestsOldestFirst, asOfDate),
            asOfDate);

    // 2a passagem: as sobras voltam em FIFO, agora ate a meta cheia
    distribute(
            requestsOldestFirst, leftovers, bags,
            fullGoalLimits(requestsOldestFirst),
            asOfDate);

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

private static Map<DomainID, BigDecimal> proportionalLimits(
        List<DonationRequest> requests,
        LocalDate asOfDate) {
    Map<DomainID, BigDecimal> limits = new LinkedHashMap<>();
    for (DonationRequest request : requests) {
        limits.put(request.getId(), request.proportionalGoalAt(asOfDate));
    }
    return limits;
}

private static Map<DomainID, BigDecimal> fullGoalLimits(List<DonationRequest> requests) {
    Map<DomainID, BigDecimal> limits = new LinkedHashMap<>();
    for (DonationRequest request : requests) {
        limits.put(request.getId(), BigDecimal.valueOf(request.getGoalBloodBags()));
    }
    return limits;
}

private static List<Donation> distribute(
        List<DonationRequest> requestsOldestFirst,
        List<Donation> donationsOldestFirst,
        Map<DomainID, Integer> bags,
        Map<DomainID, BigDecimal> limits,
        LocalDate asOfDate) {
    List<Donation> unallocated = new ArrayList<>();
    for (Donation donation : donationsOldestFirst) {
        boolean allocated = false;
        for (DonationRequest request : requestsOldestFirst) {
            int given = bags.get(request.getId());
            BigDecimal limit = limits.get(request.getId());
            if (BigDecimal.valueOf(given).compareTo(limit) < 0 && request.acceptsDonation(donation, asOfDate)) {
                bags.put(request.getId(), given + 1);
                allocated = true;
                break;
            }
        }
        if (!allocated) {
            unallocated.add(donation);
        }
    }
    return unallocated;
}
