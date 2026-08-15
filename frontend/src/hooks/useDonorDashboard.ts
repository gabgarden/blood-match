import { useEffect, useState } from "react";
import { api } from "../api/client";
import { fetchDonorHeroSummary } from "../services/partyService";
import {
  createPendingDonation,
  fetchDonorDonationHistory,
  type DonationHistoryEntry,
} from "../services/donationService";
import { extractApiErrorMessage } from "../utils/apiError";

export type Recommendation = {
  id: string;
  organizationId: string | null;
  bloodCenterName: string;
  bloodTypeNeeded: string;
  dateLimit: string;
  urgency: "LOW" | "MEDIUM" | "CRITICAL";
  distanceInKm: number | null;
  goalBloodBags: number | null;
  fulfilledBloodBags: number | null;
  goalReached: boolean;
  latitude: number | null;
  longitude: number | null;
};

type RecommendationApiItem = {
  requestId?: string;
  organizationId?: string;
  bloodCenterId?: string;
  bloodTypeNeeded?: string;
  dateLimit?: string | null;
  bloodCenterName?: string;
  urgency?: string;
  distanceInKm?: number | string;
  goalBloodBags?: number | string;
  fulfilledBloodBags?: number | string;
  goalReached?: boolean;
  latitude?: number | string | null;
  longitude?: number | string | null;
};

function toNumberOrNull(value: number | string | null | undefined): number | null {
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

function normalizeUrgency(value: string | undefined): "LOW" | "MEDIUM" | "CRITICAL" {
  const normalized = value?.trim().toUpperCase();

  if (normalized === "CRITICAL") {
    return "CRITICAL";
  }

  if (normalized === "MEDIUM") {
    return "MEDIUM";
  }

  return "LOW";
}

function normalizeRecommendation(item: RecommendationApiItem, index: number): Recommendation {
  return {
    id: item.requestId ?? `recommendation-${index}`,
    organizationId: item.organizationId ?? item.bloodCenterId ?? null,
    bloodCenterName: item.bloodCenterName ?? "Hemocentro sem nome",
    bloodTypeNeeded: item.bloodTypeNeeded ?? "-",
    dateLimit: item.dateLimit ?? "",
    urgency: normalizeUrgency(item.urgency),
    distanceInKm: toNumberOrNull(item.distanceInKm),
    goalBloodBags: toNumberOrNull(item.goalBloodBags),
    fulfilledBloodBags: toNumberOrNull(item.fulfilledBloodBags),
    goalReached: item.goalReached === true,
    latitude: toNumberOrNull(item.latitude),
    longitude: toNumberOrNull(item.longitude),
  };
}

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}



type DonorDashboardParams = {
  partyId: string | null;
  hasDonorRole: boolean;
};

export function useDonorDashboard({ partyId, hasDonorRole }: DonorDashboardParams) {
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [isLoadingRecommendations, setIsLoadingRecommendations] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState("Carregando...");
  const [donorBloodType, setDonorBloodType] = useState("-");
  const [daysRemaining, setDaysRemaining] = useState(0);
  const [livesImpacted, setLivesImpacted] = useState(0);
  const [lastDonationDate, setLastDonationDate] = useState<string | null>(null);
  const [lastDonationHospitalName, setLastDonationHospitalName] = useState<string | null>(null);
  const [lastDonationId, setLastDonationId] = useState<string | null>(null);
  const [donationHistory, setDonationHistory] = useState<DonationHistoryEntry[]>([]);
  const [isLoadingDonationHistory, setIsLoadingDonationHistory] = useState(false);
  const [donationHistoryError, setDonationHistoryError] = useState<string | null>(null);

  useEffect(() => {
    if (!partyId || !hasDonorRole) {
      return;
    }

    const currentPartyId = partyId;

    async function loadProfileData() {
      try {
        const summary = await fetchDonorHeroSummary(currentPartyId);

        setDisplayName(summary?.donorName?.trim() || "Seu perfil");
        setDonorBloodType(summary?.bloodType || "-");
        setDaysRemaining(typeof summary?.daysRemaining === "number" ? summary.daysRemaining : 0);
        setLivesImpacted(typeof summary?.livesImpacted === "number" ? summary.livesImpacted : 0);
        setLastDonationDate(summary?.lastDonationDate ?? null);
      } catch {
        setDisplayName("Seu perfil");
      }
    }

    loadProfileData();
  }, [partyId, hasDonorRole]);

  useEffect(() => {
    if (!hasDonorRole || !partyId) {
      setDonationHistory([]);
      setLastDonationId(null);
      setLastDonationHospitalName(null);
      return;
    }

    const personId = partyId;
    let cancelled = false;

    async function loadDonationHistory() {
      setIsLoadingDonationHistory(true);
      setDonationHistoryError(null);

      try {
        const history = await fetchDonorDonationHistory(personId);
        if (cancelled) {
          return;
        }

        setDonationHistory(history);
        setLastDonationId(history[0]?.id ?? null);
        setLastDonationHospitalName(history[0]?.location ?? null);
      } catch (error) {
        if (cancelled) {
          return;
        }

        setLastDonationId(null);
        setDonationHistoryError(
          extractApiErrorMessage(error, "Não foi possível carregar o histórico de doações agora."),
        );
      } finally {
        if (!cancelled) {
          setIsLoadingDonationHistory(false);
        }
      }
    }

    loadDonationHistory();

    return () => {
      cancelled = true;
    };
  }, [hasDonorRole, partyId]);

  async function reloadDonationHistory() {
    if (!hasDonorRole || !partyId) {
      setDonationHistory([]);
      setLastDonationId(null);
      setLastDonationHospitalName(null);
      return;
    }

    setIsLoadingDonationHistory(true);
    setDonationHistoryError(null);

    try {
      const history = await fetchDonorDonationHistory(partyId);
      setDonationHistory(history);
      setLastDonationId(history[0]?.id ?? null);
      setLastDonationHospitalName(history[0]?.location ?? null);
    } catch (error) {
      setLastDonationId(null);
      setDonationHistoryError(
        extractApiErrorMessage(error, "Não foi possível carregar o histórico de doações agora."),
      );
    } finally {
      setIsLoadingDonationHistory(false);
    }
  }

  useEffect(() => {
    if (!hasDonorRole || !partyId) {
      return;
    }

    const currentPartyId = partyId;

    async function loadRecommendations() {
      setIsLoadingRecommendations(true);
      setErrorMessage(null);

      try {
        const response = await api.get<RecommendationApiItem[]>("/donation-requests/recommendations", {
          params: { personId: currentPartyId, includeNonEligible: true },
        });

        setRecommendations(response.data.map(normalizeRecommendation));
      } catch (error) {
        setErrorMessage(extractApiErrorMessage(error, "Não foi possível carregar as recomendações agora."));
      } finally {
        setIsLoadingRecommendations(false);
      }
    }

    loadRecommendations();
  }, [hasDonorRole, partyId]);

  const hasPendingDonation = donationHistory.some(
    (item) => item.status === "REGISTERED" || item.status === "PENDING",
  );

  async function acceptDonation(requestId: string, expectedDate?: string, expectedTime?: string): Promise<boolean> {
    if (!partyId) {
      return false;
    }

    if (hasPendingDonation) {
      setErrorMessage(
        "Você já possui uma doação agendada. Conclua ou reagende a doação atual antes de criar um novo agendamento.",
      );
      return false;
    }

    const recommendation = recommendations.find((item) => item.id === requestId);
    if (!recommendation?.organizationId) {
      setErrorMessage(
        "Esta recomendação não inclui o hemocentro. Atualize a página ou tente novamente.",
      );
      return false;
    }

    try {
      setFeedback(null);
      setErrorMessage(null);
      await createPendingDonation({
        personId: partyId,
        organizationId: recommendation.organizationId,
        expectedDate: expectedDate || todayIsoDate(),
        expectedTime,
      });
      setFeedback("Doação pendente agendada com sucesso.");
      await reloadDonationHistory();
      return true;
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Erro ao processar o agendamento."));
      return false;
    }
  }

  return {
    recommendations,
    isLoadingRecommendations,
    feedback,
    errorMessage,
    displayName,
    donorBloodType,
    daysRemaining,
    livesImpacted,
    lastDonationDate,
    lastDonationHospitalName,
    lastDonationId,
    donationHistory,
    hasPendingDonation,
    isLoadingDonationHistory,
    donationHistoryError,
    reloadDonationHistory,
    acceptDonation,
  };
}
