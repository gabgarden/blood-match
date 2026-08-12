public boolean canDonateTo(BloodType receiver) {
    if (receiver == null)
        throw new IllegalArgumentException("Receiver blood type cannot be null");

    return switch (type) {
        case "O-" -> true;
        case "O+" ->
            Set.of("O+", "A+", "B+", "AB+").contains(receiver.type);
        case "A-" ->
            Set.of("A-", "A+", "AB-", "AB+").contains(receiver.type);
        case "A+" ->
            Set.of("A+", "AB+").contains(receiver.type);
        case "B-" ->
            Set.of("B-", "B+", "AB-", "AB+").contains(receiver.type);
        case "B+" ->
            Set.of("B+", "AB+").contains(receiver.type);
        case "AB-" ->
            Set.of("AB-", "AB+").contains(receiver.type);
        case "AB+" ->
            receiver.type.equals("AB+");
        default ->
            throw new IllegalStateException("Unexpected blood type");
    };
}
