export type AddressFields = {
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
};

export type CreatePersonDTO = {
  name: string;
  phoneNumber: string;
  cpf: string;
  birthDate: string;
  email: string;
  password: string;
  passwordConfirmation: string;
} & AddressFields;

export type CreateOrganizationDTO = {
  name: string;
  phoneNumber: string;
  cnpj: string;
  email: string;
  password: string;
  passwordConfirmation: string;
} & AddressFields;

export type ApiResponse = {
  id?: string;
  type?: string;
  error?: string;
  message?: string;
  emailConfirmationRequired?: boolean;
};
