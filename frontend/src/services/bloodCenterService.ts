import { api } from "../api/client";

export type BloodCenterSearchResult = {
  organizationId: string;
  name: string;
  city: string | null;
  state: string | null;
};

export async function searchBloodCenters(
  query: string,
  limit = 10,
): Promise<BloodCenterSearchResult[]> {
  const trimmed = query.trim();
  if (trimmed.length < 2) {
    return [];
  }

  const response = await api.get<BloodCenterSearchResult[]>("/blood-centers/search", {
    params: { q: trimmed, limit },
  });

  return response.data.map((item) => ({
    organizationId: item.organizationId,
    name: item.name,
    city: item.city ?? null,
    state: item.state ?? null,
  }));
}
