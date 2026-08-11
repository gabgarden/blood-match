package bloodmatch.domain.party;

import java.time.LocalDate;
import java.time.Period;

import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;

public class Person extends Party {

    private CPF cpf;
    private LocalDate birthDate;

    public Person(String name, PhoneNumber phonenumber, CPF cpf, LocalDate birthDate) {
        super(name, phonenumber);

        if (cpf == null) {
            throw new IllegalArgumentException("CPF cannot be null");
        }
        if (birthDate == null){
            throw new IllegalArgumentException("Birth date cannot be null");
        }
        
        this.cpf = cpf;
        this.birthDate = birthDate;
    }

    public CPF getCpf() {
        return cpf;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public int getAge(LocalDate currentDate) {
        if (currentDate == null)
            throw new IllegalArgumentException("Current date cannot be null");

        return Period.between(birthDate, currentDate).getYears();
    }

}