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

    Map<DomainID, BigDecimal> bags = new LinkedHashMap<>();
    for (DonationRequest request : requestsOldestFirst) {
        bags.put(request.getId(), BigDecimal.ZERO);
    }

    List<DonationPortion> portions = new ArrayList<>();
    for (Donation donation : donationsOldestFirst) {
        portions.add(new DonationPortion(donation, BigDecimal.ONE));
    }

    // 1a passagem: cada solicitacao recebe ate o seu teto proporcional
    List<DonationPortion> leftovers = distribute(
            requestsOldestFirst, portions, bags,
            proportionalLimits(requestsOldestFirst, asOfDate),
            asOfDate);

    // 2a passagem: as sobras voltam em FIFO, agora ate a meta cheia
    distribute(
            requestsOldestFirst, leftovers, bags,
            fullGoalLimits(requestsOldestFirst),
            asOfDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (DonationRequest request : requestsOldestFirst) {
        BigDecimal given = bags.get(request.getId());
        BigDecimal goal = BigDecimal.valueOf(request.getGoalBloodBags());
        snapshot.put(
                request.getId(),
                new DonationRequestFulfillmentStatusRecord(
                        given, given.compareTo(goal) >= 0));
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

private static List<DonationPortion> distribute(
        List<DonationRequest> requestsOldestFirst,
        List<DonationPortion> portions,
        Map<DomainID, BigDecimal> bags,
        Map<DomainID, BigDecimal> limits,
        LocalDate asOfDate) {
    List<DonationPortion> unallocated = new ArrayList<>();
    for (DonationPortion portion : portions) {
        for (DonationRequest request : requestsOldestFirst) {
            if (portion.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (request.acceptsDonation(portion.getDonation(), asOfDate)) {
                BigDecimal given = bags.get(request.getId());
                BigDecimal limit = limits.get(request.getId());
                BigDecimal capacity = limit.subtract(given);
                if (capacity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal allocation = portion.getRemainingQuantity().min(capacity);
                    bags.put(request.getId(), given.add(allocation));
                    portion.deduct(allocation);
                }
            }
        }
        if (portion.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            unallocated.add(portion);
        }
    }
    return unallocated;
}
