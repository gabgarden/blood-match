import { isAxiosError } from "axios";
import { api, publicApi } from "../api/client";
import type { ApiResponse, CreatePersonDTO, CreateOrganizationDTO } from "../types/party";

export const registerPerson = async (data: CreatePersonDTO) => {
  const response = await publicApi.post<ApiResponse>("/parties/persons", data);
  return response.data;
};

export const registerOrganization = async (data: CreateOrganizationDTO) => {
  const response = await publicApi.post<ApiResponse>("/parties/organizations", data);
  return response.data;
};

export async function updateParty(
  partyId: string,
  version: number,
  patch: { name?: string; phoneNumber?: string; address?: { street: string; city: string; state: string; zipCode?: string } }
) {
  const response = await api.patch<{ id: string; version: number; name?: string; phoneNumber?: string; address?: any }>("/parties", {
    partyId,
    version,
    ...patch,
  });
  return response.data;
}

type DonorSummaryResponse = {
  personId?: string;
  partyVersion?: number;
  donorVersion?: number;
  donorName?: string;
  phoneNumber?: string;
  bloodType?: string;
  address?: string;
  lastDonationDate?: string | null;
  daysRemaining?: number | null;
  livesImpacted?: number | null;
  weight?: number | null;
  weightUpdatedAt?: string | null;
};

export type DonorHeroSummary = {
  personId: string | null;
  partyVersion: number | null;
  donorVersion: number | null;
  donorName: string | null;
  phoneNumber: string | null;
  bloodType: string | null;
  address: string | null;
  daysRemaining: number | null;
  livesImpacted: number | null;
  lastDonationDate: string | null;
  weight: number | null;
  weightUpdatedAt: string | null;
};

function toFiniteNumberOrNull(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim().length > 0) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
}

export async function fetchDonorHeroSummary(personId: string): Promise<DonorHeroSummary | null> {
  try {
    const { data } = await api.get<DonorSummaryResponse>(`/donors/${personId}/summary`);

    return {
      personId: data.personId ?? personId,
      partyVersion: data.partyVersion ?? null,
      donorVersion: data.donorVersion ?? null,
      donorName: data.donorName ?? null,
      phoneNumber: data.phoneNumber ?? null,
      bloodType: data.bloodType ?? null,
      address: data.address ?? null,
      daysRemaining: typeof data.daysRemaining === "number" ? data.daysRemaining : null,
      livesImpacted: typeof data.livesImpacted === "number" ? data.livesImpacted : null,
      lastDonationDate: data.lastDonationDate ?? null,
      weight: toFiniteNumberOrNull(data.weight),
      weightUpdatedAt: data.weightUpdatedAt ?? null,
    };
  } catch (error) {
    // Workaround: backend às vezes responde 401 "Unauthorized" neste endpoint com JWT válido.
    if (isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 404)) {
      return null;
    }

    throw error;
  }
}
