package bloodmatch.domain.shared.valueObjects;

import java.util.Map;
import java.util.Set;
import java.util.Objects;

public class BloodType {

        private static final Map<String, BloodType> TYPES = Map.of(
                        "O-", new BloodType("O-"),
                        "O+", new BloodType("O+"),
                        "A-", new BloodType("A-"),
                        "A+", new BloodType("A+"),
                        "B-", new BloodType("B-"),
                        "B+", new BloodType("B+"),
                        "AB-", new BloodType("AB-"),
                        "AB+", new BloodType("AB+"));

        private final String type;

        private BloodType(String type) {
                this.type = type;
        }


        //private   
        public static BloodType of(String type) {

                if (type == null)
                        throw new IllegalArgumentException("Blood type cannot be null");

                BloodType bloodType = TYPES.get(type.toUpperCase());

                if (bloodType == null)
                        throw new IllegalArgumentException("Invalid blood type");

                return bloodType;
        }

        public String getType() {
                return type;
        }

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
        
        @Override
        public boolean equals(Object o) {
                if (this == o) {
                        return true;
                }
                if (!(o instanceof BloodType)) {
                        return false;
                }
                BloodType bloodType = (BloodType) o;
                return Objects.equals(type, bloodType.type);
        }

        @Override
        public int hashCode() {
                return Objects.hash(type);
        }
}