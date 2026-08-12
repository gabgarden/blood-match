public Map<DomainID, DonationRequestFulfillmentStatusRecord> calculate(
        List<DonationRequest> requests,
        List<Donation> donations,
        LocalDate currentDate) {

    requests = new ArrayList<>(requests);
    requests.sort(
        Comparator.comparing(DonationRequest::getDateRequested)
            .thenComparing(request -> request.getId().getValue()));

    donations = new ArrayList<>(donations);
    donations.sort(
        Comparator.comparing(Donation::getDonationDate)
            .thenComparing(donation -> donation.getId().getValue()));

    Map<DomainID, Integer> fulfilled = initialize(requests);
    Map<DomainID, List<DonationRequest>> requestsByBloodCenter = requests.stream()
        .collect(Collectors.groupingBy(request -> request.getBloodCenter().getId()));

    distributeDonations(requestsByBloodCenter, donations, currentDate, fulfilled);
    return buildResult(requests, fulfilled);
}

private void distributeDonations(
        Map<DomainID, List<DonationRequest>> requestsByBloodCenter,
        List<Donation> donations,
        LocalDate currentDate,
        Map<DomainID, Integer> fulfilled) {

    for (Donation donation : donations) {
        List<DonationRequest> requestsAtBloodCenter = requestsByBloodCenter.getOrDefault(
            donation.getBloodCenter().getId(),
            List.of());

        for (DonationRequest request : requestsAtBloodCenter) {
            if (!request.acceptsDonation(donation, currentDate))
                continue;

            int currentFulfilled = fulfilled.get(request.getId());
            if (currentFulfilled >= request.getGoalBloodBags())
                continue;

            fulfilled.put(request.getId(), currentFulfilled + 1);
            break;
        }
    }
}
