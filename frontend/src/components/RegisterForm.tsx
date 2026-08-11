import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { TypeToggle } from "./TypeToggle";
import { AccountCredentialsFields } from "./register/AccountCredentialsFields";
import { AddressFields } from "./register/AddressFields";
import { PartyIdentityFields } from "./register/PartyIdentityFields";
import { ProfileIntentFields } from "./register/ProfileIntentFields";
import type { PartyType } from "./register/PartyIdentityFields";
import {
  createBloodCenterProfile,
  createDonorProfile,
  createRequesterProfile,
} from "../services/profileService";
import { registerOrganization, registerPerson } from "../services/partyService";
import type { AddressFields as AddressPayload, ApiResponse } from "../types/party";
import { useAuth } from "../context/AuthContext";
import { authService } from "../services/authService";
import { AppButton, InlineAlert } from "./ui";
import { resolvePostLoginPath } from "../routes/roleRouting";
import { extractApiErrorMessage } from "../utils/apiError";

type RegisterFormState = {
  name: string;
  phoneNumber: string;
  cpf: string;
  cnpj: string;
  birthDate: string;
  email: string;
  password: string;
  confirmPassword: string;
  bloodType: string;
  weight: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
};

function getCreatedPartyId(response: ApiResponse): string | null {
  return response.id ?? null;
}

function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

function buildAddressPayload(form: RegisterFormState): AddressPayload | { error: string } | null {
  const street = form.street.trim();
  const city = form.city.trim();
  const state = form.state.trim().toUpperCase();
  const zipCode = onlyDigits(form.zipCode);

  const filled = [street, city, state, zipCode].filter((value) => value.length > 0);

  if (filled.length === 0) {
    return null;
  }

  if (filled.length !== 4) {
    return { error: "Preencha todos os campos de endereço (rua, cidade, estado e CEP) ou deixe todos vazios." };
  }

  if (state.length !== 2) {
    return { error: "Informe a UF com 2 letras (ex.: SP)." };
  }

  return { street, city, state, zipCode };
}

export default function RegisterForm() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [type, setType] = useState<PartyType>("person");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [form, setForm] = useState<RegisterFormState>({
    name: "",
    phoneNumber: "",
    cpf: "",
    cnpj: "",
    birthDate: "",
    email: "",
    password: "",
    confirmPassword: "",
    bloodType: "O+",
    weight: "",
    street: "",
    city: "",
    state: "",
    zipCode: "",
  });

  function handleInputChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleProfileFieldChange(event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleAccountTypeChange(nextType: PartyType) {
    setType(nextType);
  }

  async function createPersonProfiles(partyId: string) {
    const parsedWeight = Number(form.weight);
    if (!form.bloodType || Number.isNaN(parsedWeight) || parsedWeight <= 0) {
      throw new Error("Informe tipo sanguíneo e peso válido para criar os perfis iniciais.");
    }

    await createDonorProfile({
      personId: partyId,
      bloodType: form.bloodType,
      weight: parsedWeight,
    });

    await createRequesterProfile(partyId);
  }

  async function createOrganizationProfiles(partyId: string) {
    await createBloodCenterProfile(partyId);
    await createRequesterProfile(partyId);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    // Register flow: create party → login → register roles → re-login (JWT não atualiza no registro de papel).
    event.preventDefault();
    setErrorMessage(null);

    if (form.password !== form.confirmPassword) {
      setErrorMessage("Senha e confirmação de senha precisam ser iguais.");
      return;
    }

    if (!form.phoneNumber.trim()) {
      setErrorMessage("Informe o telefone.");
      return;
    }

    const address = buildAddressPayload(form);
    if (address && "error" in address) {
      setErrorMessage(address.error);
      return;
    }

    setIsSubmitting(true);

    try {
      // Garante que token antigo não contamine o fluxo público de cadastro.
      authService.logout();

      const normalizedEmail = form.email.trim();
      const phoneNumber = form.phoneNumber.trim();
      const addressFields = address ?? {};

      const response =
        type === "person"
          ? await registerPerson({
              name: form.name.trim(),
              phoneNumber,
              cpf: onlyDigits(form.cpf),
              birthDate: form.birthDate,
              email: normalizedEmail,
              password: form.password,
              passwordConfirmation: form.confirmPassword,
              ...addressFields,
            })
          : await registerOrganization({
              name: form.name.trim(),
              phoneNumber,
              cnpj: onlyDigits(form.cnpj),
              email: normalizedEmail,
              password: form.password,
              passwordConfirmation: form.confirmPassword,
              ...addressFields,
            });

      await login({ email: normalizedEmail, password: form.password });

      const createdPartyId = getCreatedPartyId(response);
      if (!createdPartyId) {
        setErrorMessage("Cadastro criado, mas não foi possível identificar o partyId para criar os perfis iniciais.");
        return;
      }

      if (type === "person") {
        await createPersonProfiles(createdPartyId);
      } else {
        await createOrganizationProfiles(createdPartyId);
      }

      const refreshedSession = await login({ email: normalizedEmail, password: form.password });
      navigate(resolvePostLoginPath(refreshedSession.roles), { replace: true });
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Erro ao registrar. Verifique os dados e tente novamente."));
    } finally {
      setIsSubmitting(false);
    }
  }

  const inputStyle =
    "w-full px-3 py-2 bg-white border border-gray-200 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent focus:outline-none transition-all placeholder:text-gray-300 text-sm";
  const labelStyle = "text-xs font-bold text-gray-500 uppercase tracking-wider mb-1 ml-1";

  return (
    <div className="w-full">
      <TypeToggle selected={type} onChange={handleAccountTypeChange} />

      {errorMessage && <InlineAlert className="mb-4" tone="error" message={errorMessage} />}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <PartyIdentityFields
          type={type}
          name={form.name}
          phoneNumber={form.phoneNumber}
          cpf={form.cpf}
          cnpj={form.cnpj}
          birthDate={form.birthDate}
          onChange={handleInputChange}
          inputStyle={inputStyle}
          labelStyle={labelStyle}
        />

        <AddressFields
          street={form.street}
          city={form.city}
          state={form.state}
          zipCode={form.zipCode}
          onChange={handleInputChange}
          inputStyle={inputStyle}
          labelStyle={labelStyle}
        />

        <AccountCredentialsFields
          email={form.email}
          password={form.password}
          confirmPassword={form.confirmPassword}
          onChange={handleInputChange}
          inputStyle={inputStyle}
          labelStyle={labelStyle}
        />

        <ProfileIntentFields
          accountType={type}
          bloodType={form.bloodType}
          weight={form.weight}
          onFieldChange={handleProfileFieldChange}
          labelStyle={labelStyle}
          inputStyle={inputStyle}
        />

        <AppButton type="submit" disabled={isSubmitting} fullWidth className="mt-1 shadow-md active:scale-[0.98]">
          {isSubmitting ? "Cadastrando..." : "Finalizar Cadastro"}
        </AppButton>
      </form>
    </div>
  );
}
