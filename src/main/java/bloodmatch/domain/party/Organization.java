package bloodmatch.domain.party;

import bloodmatch.domain.shared.valueObjects.CNPJ;

public class Organization extends Party {

    private CNPJ cnpj;

    public Organization(String name, CNPJ cnpj) {
        super(name);
        
        if (cnpj == null) {
            throw new IllegalArgumentException("CNPJ cannot be null");

        }
        this.cnpj = cnpj;
    }

    public CNPJ getCnpj() {
        return cnpj;
    }
}
