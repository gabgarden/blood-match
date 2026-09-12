package bloodmatch.domain.party;

import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;

public abstract class Party extends DomainObject {

    private String name;
    private PhoneNumber phoneNumber;
    private Address address;

    protected Party(String name, PhoneNumber phoneNumber) {
        this.id = DomainID.generate();
       
        setName(name);
        setPhoneNumber(phoneNumber);
    }

    public String getName() {
        return name;
    }
    public final void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }
    public final void setPhoneNumber(PhoneNumber phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("PhoneNumber cannot be empty");
        }
        this.phoneNumber = phoneNumber;
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Name cannot be empty");

        this.name = newName;
    }

    public void changeAddress(Address newAddress) {
        this.address = newAddress;
    }

    public void changePhoneNumber(PhoneNumber newPhoneNumber) {
        if (newPhoneNumber == null)
            throw new IllegalArgumentException("PhoneNumber cannot be empty");
            
        this.phoneNumber = newPhoneNumber;
    }
}