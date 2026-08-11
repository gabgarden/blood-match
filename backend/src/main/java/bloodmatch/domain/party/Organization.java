package bloodmatch.domain.party;

import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;

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
