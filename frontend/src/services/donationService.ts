import { api } from "../api/client";

type DonationHistoryApiItem = {
  donationId: string;
  date: string | null;
  location: string;
};

export type DonationHistoryEntry = {
  id: string;
  location: string;
  donationDate: string | null;
  status: string;
};

type RequestUrgency = "Crítica" | "Média" | "Baixa";

type UserDonationRequestApiItem = {
  requestId?: string;
  bloodTypeNeeded?: string;
  dateRequested?: string;
  dateLimit?: string | null;
  active?: boolean;
  expired?: boolean;
  bloodCenterName?: string;
  bloodCenterPhoneNumber?: string;
  urgency?: string;
  goalBloodBags?: number | string;
  fulfilledBloodBags?: number | string;
  remainingBloodBags?: number | string;
  goalReached?: boolean;
};

export type UserDonationRequestCard = {
  id: string;
  bloodType: string;
  bloodCenterName: string;
  bloodCenterPhoneNumber: string | null;
  urgency: RequestUrgency;
  goalBloodBags: number;
  fulfilledBloodBags: number;
  remainingBloodBags: number;
  goalReached: boolean;
  active: boolean;
  expired: boolean;
  dateRequested: string | null;
  dateLimit: string | null;
  deadlineLabel: string;
  dateRequestedLabel: string;
};

export type CreateDonationRequestPayload = {
  partyId: string;
  organizationId: string;
  bloodTypeNeeded: string;
  goalBloodBags: number;
  dateLimit: string;
  urgency: "LOW" | "MEDIUM" | "CRITICAL";
  directedTo?: string;
};

export type CreatePendingDonationPayload = {
  organizationId: string;
  personId: string;
  expectedDate: string;
};

export type CreateCompletedDonationPayload = {
  personId: string;
  organizationId: string;
  donationDate: string;
};

function toNumber(value: number | string | null | undefined, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim().length > 0) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }

  return fallback;
}

function normalizeDonation(item: DonationHistoryApiItem, index: number): DonationHistoryEntry {
  return {
    id: item.donationId || `donation-${index}`,
    location: item.location || "Local nao informado",
    donationDate: item.date,
    // O contrato de histórico não devolve status; ações de concluir/reagendar validam no backend.
    status: "REGISTERED",
  };
}

function normalizeRequestUrgency(value: string | undefined): RequestUrgency {
  const normalized = value?.normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim().toUpperCase();

  if (normalized === "CRITICAL" || normalized === "CRITICA") {
    return "Crítica";
  }

  if (normalized === "MEDIUM" || normalized === "MEDIA") {
    return "Média";
  }

  return "Baixa";
}

function formatDateLabel(input: string | null | undefined): string {
  if (!input || input === "null") {
    return "Não informado";
  }

  const parsed = new Date(input);
  if (Number.isNaN(parsed.getTime())) {
    return input;
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(parsed);
}

function normalizeUserDonationRequest(item: UserDonationRequestApiItem, index: number): UserDonationRequestCard {
  const id = item.requestId ?? `request-${index}`;
  const fulfilledBloodBags = toNumber(item.fulfilledBloodBags);
  const goalBloodBags = toNumber(item.goalBloodBags);
  const remainingFromApi = item.remainingBloodBags;
  const remainingBloodBags =
    remainingFromApi == null
      ? Math.max(goalBloodBags - fulfilledBloodBags, 0)
      : toNumber(remainingFromApi);

  return {
    id,
    bloodType: item.bloodTypeNeeded ?? "-",
    bloodCenterName: item.bloodCenterName ?? "Hemocentro não informado",
    bloodCenterPhoneNumber: item.bloodCenterPhoneNumber?.trim() || null,
    urgency: normalizeRequestUrgency(item.urgency),
    goalBloodBags,
    fulfilledBloodBags,
    remainingBloodBags,
    goalReached: item.goalReached === true,
    active: item.active !== false && item.expired !== true,
    expired: item.expired === true,
    dateRequested: item.dateRequested ?? null,
    dateLimit: item.dateLimit ?? null,
    deadlineLabel: formatDateLabel(item.dateLimit),
    dateRequestedLabel: formatDateLabel(item.dateRequested),
  };
}

export async function fetchDonorDonationHistory(personId: string): Promise<DonationHistoryEntry[]> {
  const response = await api.get<DonationHistoryApiItem[]>(`/donors/${personId}/donations`);
  return response.data.map(normalizeDonation);
}

export async function fetchUserDonationRequests(partyId: string): Promise<UserDonationRequestCard[]> {
  const response = await api.get<UserDonationRequestApiItem[]>(`/donation-requests/${partyId}`);
  return response.data.map(normalizeUserDonationRequest);
}

export async function createDonationRequest(payload: CreateDonationRequestPayload) {
  const response = await api.post("/donation-requests", payload);
  return response.data;
}

export async function createPendingDonation(payload: CreatePendingDonationPayload) {
  const response = await api.post("/donations/create-pending", payload);
  return response.data;
}

export async function createCompletedDonation(payload: CreateCompletedDonationPayload) {
  const response = await api.post("/donations/completed", payload);
  return response.data;
}

export async function notifyDonationRequest(requestId: string): Promise<string> {
  const response = await api.post<{ message?: string }>(`/donation-requests/${requestId}/notify`);
  return response.data?.message ?? "Notificações enviadas aos doadores elegíveis.";
}

export async function deleteDonationRequest(requestId: string): Promise<void> {
  await api.delete(`/donation-requests/${requestId}`);
}

export async function updateDonationRequestDateLimit(requestId: string, newDateLimit: string) {
  const response = await api.patch("/donation-requests/date-limit", { requestId, newDateLimit });
  return response.data;
}

export async function updateDonationRequestGoalBloodBags(requestId: string, newGoalBloodBags: number) {
  const response = await api.patch("/donation-requests/goal-blood-bags", { requestId, newGoalBloodBags });
  return response.data;
}

export async function completeDonation(donationId: string, completionDate: string) {
  const response = await api.patch<{ id: string; completionDate: string; status: string }>("/donations/complete", {
    donationId,
    completionDate,
  });
  return response.data;
}

export async function rescheduleDonation(donationId: string, newExpectedDate: string) {
  const response = await api.patch<{ id: string; expectedDate: string; status: string }>("/donations/reschedule", {
    donationId,
    newExpectedDate,
  });
  return response.data;
}

export const externalDonationCreatePath = "/donations/external/new";
