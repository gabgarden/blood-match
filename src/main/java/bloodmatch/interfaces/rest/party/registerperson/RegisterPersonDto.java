package bloodmatch.interfaces.rest.party.registerperson;

import java.time.LocalDate;

public record RegisterPersonDto(
    String name,
    String cpf,
    LocalDate birthDate,
    String email,
    String password,
    String passwordConfirmation,
    String street,
    String city,
    String state,
    String zipCode) {
}
