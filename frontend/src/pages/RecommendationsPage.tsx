import { useState, useMemo } from "react";
import { Link, Navigate } from "react-router-dom";
import { AccessDenied } from "../components/AccessDenied";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { RecommendationCard } from "../components/dashboard/RecommendationCard";
import { ScheduleDonationModal } from "../components/dashboard/ScheduleDonationModal";
import { FullPageLoading, InlineAlert } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useDonorDashboard, type Recommendation } from "../hooks/useDonorDashboard";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasAdminRole, hasDonorRole, hasRequesterRole } from "../routes/roleRouting";

export default function RecommendationsPage() {
  const { roles, partyId, logout } = useAuth();
  const [schedulingRecommendation, setSchedulingRecommendation] = useState<Recommendation | null>(null);
  const [isSubmittingSchedule, setIsSubmittingSchedule] = useState(false);

  const normalizedRoles = useMemo(() => roles, [roles]);
  const isResolvingRoles = useRoleResolution(normalizedRoles);
  const canAccessDonorDashboard = hasDonorRole(normalizedRoles);
  const canAccessRequesterArea = hasRequesterRole(normalizedRoles);
  const canAccessAdminArea = hasAdminRole(normalizedRoles);
  const isRequesterOnly = canAccessRequesterArea && !canAccessDonorDashboard && !canAccessAdminArea;

  const {
    recommendations,
    isLoadingRecommendations,
    feedback,
    errorMessage,
    donorBloodType,
    daysRemaining,
    acceptDonation,
  } = useDonorDashboard({ partyId, hasDonorRole: canAccessDonorDashboard });

  const waitingDays = Math.max(daysRemaining, 0);
  const isEligibleToDonate = waitingDays <= 0;
  const criticalCount = useMemo(
    () => recommendations.filter((item) => item.urgency === "CRITICAL").length,
    [recommendations],
  );

  function handleOpenScheduleModal(requestId: string) {
    const rec = recommendations.find((item) => item.id === requestId);
    if (rec) {
      setSchedulingRecommendation(rec);
    }
  }

  async function handleConfirmSchedule(requestId: string, expectedDate: string) {
    setIsSubmittingSchedule(true);
    try {
      await acceptDonation(requestId, expectedDate);
      setSchedulingRecommendation(null);
    } finally {
      setIsSubmittingSchedule(false);
    }
  }

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (isRequesterOnly) {
    return <Navigate to="/requests" replace />;
  }

  if (!canAccessDonorDashboard) {
    return (
      <main className="min-h-screen bg-surface p-6">
        <div className="mx-auto max-w-3xl">
          <AccessDenied title="Área de recomendações indisponível" />
        </div>
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="donor-dashboard" />
      <DonorDashboardTopbar title="Recomendações" onLogout={logout} />

      <main className="pt-20 px-4 pb-10 lg:ml-64 lg:px-8">
        <div className="mx-auto max-w-[1400px] space-y-6">
          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}

          <section className="relative overflow-hidden rounded-[2rem] border border-surface-container-high bg-white p-6 lg:p-8">
            <div className="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-[#fff2f0]" />
            <div className="relative z-10 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <Link
                  to="/dashboard"
                  className="inline-flex items-center gap-1 text-xs font-bold uppercase tracking-wider text-secondary hover:text-primary"
                >
                  <span className="material-symbols-outlined text-sm">arrow_back</span>
                  Dashboard
                </Link>
                <h1 className="mt-2 font-headline text-3xl font-extrabold tracking-tight text-on-surface">
                  Todas as recomendações
                </h1>
                <p className="mt-2 text-sm text-text-secondary">
                  {isEligibleToDonate
                    ? `Compatíveis com ${donorBloodType}${criticalCount > 0 ? ` · ${criticalCount} crítica${criticalCount === 1 ? "" : "s"}` : ""}.`
                    : `Você não está elegível para doar. Aguarde ${waitingDays} ${waitingDays === 1 ? "dia" : "dias"}.`}
                </p>
              </div>

              {isEligibleToDonate && (
                <div className="rounded-2xl bg-surface-container-low px-4 py-3 text-center">
                  <p className="font-headline text-2xl font-black text-primary">{recommendations.length}</p>
                  <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Matches</p>
                </div>
              )}
            </div>
          </section>

          {isEligibleToDonate && isLoadingRecommendations && (
            <div className="rounded-[2rem] border border-surface-container-high bg-white p-10 text-center">
              <span className="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
              <p className="mt-3 text-sm text-text-secondary">Carregando recomendações...</p>
            </div>
          )}

          {isEligibleToDonate && !isLoadingRecommendations && recommendations.length > 0 && (
            <section className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
              {recommendations.map((recommendation) => (
                <RecommendationCard
                  key={recommendation.id}
                  id={recommendation.id}
                  name={recommendation.bloodCenterName}
                  bloodTypeNeeded={recommendation.bloodTypeNeeded}
                  dateLimit={recommendation.dateLimit}
                  urgency={recommendation.urgency}
                  distanceInKm={recommendation.distanceInKm}
                  goalBloodBags={recommendation.goalBloodBags}
                  fulfilledBloodBags={recommendation.fulfilledBloodBags}
                  goalReached={recommendation.goalReached}
                  onAccept={handleOpenScheduleModal}
                />
              ))}
            </section>
          )}

          {isEligibleToDonate && !isLoadingRecommendations && recommendations.length === 0 && (
            <section className="rounded-[2rem] border border-dashed border-surface-container-highest bg-white px-6 py-14 text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-surface-container-low text-secondary">
                <span className="material-symbols-outlined text-3xl">travel_explore</span>
              </div>
              <h2 className="mt-4 font-headline text-2xl font-extrabold text-on-surface">
                Nenhuma recomendação disponível
              </h2>
              <p className="mx-auto mt-2 max-w-md text-sm text-text-secondary">
                Não há pedidos compatíveis no seu raio agora. Você pode ajustar a distância máxima no perfil.
              </p>
              <Link to="/profile" className="mt-6 inline-flex text-sm font-bold text-primary hover:underline">
                Ir para o perfil
              </Link>
            </section>
          )}

          {!isEligibleToDonate && (
            <section className="rounded-[2rem] border border-surface-container-high bg-white p-6 sm:p-8">
              <div className="flex items-start gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-surface-container-low text-secondary">
                  <span className="material-symbols-outlined">hourglass_top</span>
                </div>
                <div>
                  <p className="font-headline text-lg font-bold text-on-surface">Intervalo de segurança</p>
                  <p className="mt-1 text-sm text-text-secondary">
                    Faltam {waitingDays} {waitingDays === 1 ? "dia" : "dias"} para você voltar a doar.
                  </p>
                </div>
              </div>
            </section>
          )}
        </div>
      </main>

      <ScheduleDonationModal
        isOpen={!!schedulingRecommendation}
        recommendation={schedulingRecommendation}
        onClose={() => setSchedulingRecommendation(null)}
        onConfirm={handleConfirmSchedule}
        isSubmitting={isSubmittingSchedule}
      />
    </div>
  );
}
