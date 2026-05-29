package bloodmatch.domain.party;

import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;

public abstract class Party extends DomainObject {

    private String name;
    private Email email;
    private PhoneNumber phoneNumber;
    private Address address;

    protected Party(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        this.id = DomainID.generate();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Name cannot be empty");

        this.name = newName;
    }

}