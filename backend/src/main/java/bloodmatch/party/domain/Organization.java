package bloodmatch.party.domain;

import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;

public class Organization extends Party {

    private CNPJ cnpj;

    public Organization(String name, PhoneNumber phonenumber, CNPJ cnpj) {
        super(name, phonenumber);
        
        if (cnpj == null) {
            throw new IllegalArgumentException("CNPJ cannot be null");

        }
        this.cnpj = cnpj;
    }

    public CNPJ getCnpj() {
        return cnpj;
    }
}
