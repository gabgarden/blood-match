import { api } from "../api/client";

type CreateDonorPayload = {
  personId: string;
  bloodType: string;
  weight: number;
  lastDonationDate?: string;
};

export async function createDonorProfile(payload: CreateDonorPayload) {
  const body: CreateDonorPayload = {
    personId: payload.personId,
    bloodType: payload.bloodType,
    weight: payload.weight,
  };

  if (payload.lastDonationDate) {
    body.lastDonationDate = payload.lastDonationDate;
  }

  const response = await api.post("/donors", body);
  return response.data;
}

export async function createRequesterProfile(partyId: string) {
  const response = await api.post("/requesters", { partyId });
  return response.data;
}

export async function createBloodCenterProfile(organizationId: string) {
  const response = await api.post("/blood-centers", { organizationId });
  return response.data;
}

export async function updateDonor(payload: { personId: string; version: number; bloodType?: string; weight?: number; maxDistanceInKm?: number }) {
  const response = await api.patch<{ id: string; version: number }>("/donors", payload);
  return response.data;
}
