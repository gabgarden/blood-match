import { api } from "../api/client";

type CreateDonorPayload = {
  personId: string;
  bloodType: string;
  weight: number;
};

export async function createDonorProfile(payload: CreateDonorPayload) {
  const response = await api.post("/donors", payload);
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

export async function updateDonorProfile(payload: CreateDonorPayload) {
  const response = await api.patch<{ id: string }>("/donors/profile", payload);
  return response.data;
}

export async function updateDonorRecommendationDistance(personId: string, maxDistanceInKm: number) {
  const response = await api.patch<{ personId: string; maxDistanceInKm: number }>(
    "/donors/recommendation-distance",
    { personId, maxDistanceInKm },
  );
  return response.data;
}
