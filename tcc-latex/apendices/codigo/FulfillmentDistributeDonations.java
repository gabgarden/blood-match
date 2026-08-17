public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
        BloodCenter bloodCenter,
        LocalDate startDate,
        LocalDate endDate,
        List<DonationRequest> requests,
        List<Donation> donations) {

    List<DonationRequest> centerRequests = requests.stream()
        .filter(request -> sameBloodCenter(request.getBloodCenter(), bloodCenter))
        .sorted(REQUEST_ORDER)
        .toList();

    List<Donation> centerDonations = donations.stream()
        .filter(donation -> sameBloodCenter(donation.getBloodCenter(), bloodCenter))
        .filter(donation -> inRange(donation.getDonationDate(), startDate, endDate))
        .sorted(DONATION_ORDER)
        .toList();

    Map<DomainID, Integer> fulfilled = new LinkedHashMap<>();
    for (DonationRequest request : centerRequests) {
        fulfilled.put(request.getId(), 0);
    }

    for (Donation donation : centerDonations) {
        for (DonationRequest request : centerRequests) {
            if (!request.acceptsDonation(donation, endDate))
                continue;
            int current = fulfilled.get(request.getId());
            if (current >= request.getGoalBloodBags())
                continue;
            fulfilled.put(request.getId(), current + 1);
            break;
        }
    }

    return buildResult(centerRequests, fulfilled);
}
