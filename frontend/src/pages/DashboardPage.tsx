import { useMemo } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { RecommendationCard } from "../components/dashboard/RecommendationCard";
import { useAuth } from "../context/AuthContext";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { DonorHeroSection } from "../components/dashboard/DonorHeroSection";
import { LastDonationCard } from "../components/dashboard/LastDonationCard";
import { DonationHistory } from "../components/dashboard/DonationHistory";
import { useDonorDashboard } from "../hooks/useDonorDashboard";
import { FullPageLoading, InlineAlert } from "../components/ui";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasAdminRole, hasDonorRole, hasRequesterRole } from "../routes/roleRouting";
import { externalDonationCreatePath } from "../services/donationService";

export default function DonorDashboardPage() {
  const navigate = useNavigate();
  const { roles, partyId, logout } = useAuth();

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
    displayName,
    donorBloodType,
    daysRemaining,
    livesImpacted,
    lastDonationDate,
    lastDonationHospitalName,
    lastDonationId,
    donationHistory,
    isLoadingDonationHistory,
    donationHistoryError,
    reloadDonationHistory,
    acceptDonation,
  } = useDonorDashboard({ partyId, hasDonorRole: canAccessDonorDashboard });

  const waitingDays = Math.max(daysRemaining, 0);
  const isEligibleToDonate = waitingDays <= 0;
  const featuredRecommendations = useMemo(() => recommendations.slice(0, 3), [recommendations]);
  const criticalCount = useMemo(
    () => recommendations.filter((item) => item.urgency === "CRITICAL").length,
    [recommendations],
  );

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  function handleCreateExternalDonation() {
    navigate(externalDonationCreatePath);
  }

  if (isRequesterOnly) {
    return <Navigate to="/requests" replace />;
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="donor-dashboard" />
      <DonorDashboardTopbar title="Dashboard" onLogout={logout} />

      <main className="pt-20 px-4 pb-10 lg:ml-64 lg:px-8">
        <div className="mx-auto max-w-[1400px] space-y-6">
          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}

          {canAccessDonorDashboard && (
            <>
              <section className="grid grid-cols-12 gap-6">
                <DonorHeroSection
                  userName={displayName}
                  bloodType={donorBloodType}
                  daysRemaining={daysRemaining}
                  livesImpacted={livesImpacted}
                  recommendationCount={isEligibleToDonate ? recommendations.length : 0}
                />
                <LastDonationCard
                  lastDonationDate={lastDonationDate}
                  lastDonationHospitalName={lastDonationHospitalName}
                  hasDonation={!!lastDonationId}
                  onCreateExternalDonation={handleCreateExternalDonation}
                />
              </section>

              <section className="space-y-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="font-headline text-2xl font-extrabold tracking-tight text-on-surface">
                        Recomendações
                      </h2>
                      {isEligibleToDonate && criticalCount > 0 && (
                        <span className="rounded-lg bg-[#fff2f0] px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-primary">
                          {criticalCount} crítica{criticalCount === 1 ? "" : "s"}
                        </span>
                      )}
                    </div>
                    <p className="mt-1 text-sm text-text-secondary">
                      {isEligibleToDonate
                        ? `Pedidos compatíveis com ${donorBloodType}, priorizados por proximidade e urgência.`
                        : `Você volta a receber recomendações em ${waitingDays} ${waitingDays === 1 ? "dia" : "dias"}.`}
                    </p>
                  </div>

                  {isEligibleToDonate && recommendations.length > 0 && (
                    <Link
                      to="/dashboard/recommendations"
                      className="inline-flex items-center gap-1 text-sm font-bold text-primary hover:underline"
                    >
                      Ver todas
                      <span className="material-symbols-outlined text-base">chevron_right</span>
                    </Link>
                  )}
                </div>

                {isEligibleToDonate && isLoadingRecommendations && (
                  <div className="rounded-[2rem] border border-surface-container-high bg-white p-10 text-center">
                    <span className="material-symbols-outlined animate-spin text-3xl text-primary">
                      progress_activity
                    </span>
                    <p className="mt-3 text-sm text-text-secondary">Buscando recomendações...</p>
                  </div>
                )}

                {isEligibleToDonate && !isLoadingRecommendations && featuredRecommendations.length > 0 && (
                  <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
                    {featuredRecommendations.map((recommendation) => (
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
                        onAccept={acceptDonation}
                      />
                    ))}
                  </div>
                )}

                {isEligibleToDonate && !isLoadingRecommendations && featuredRecommendations.length === 0 && (
                  <div className="rounded-[2rem] border border-dashed border-surface-container-highest bg-white px-6 py-12 text-center">
                    <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-container-low text-secondary">
                      <span className="material-symbols-outlined text-2xl">travel_explore</span>
                    </div>
                    <p className="mt-4 font-headline text-lg font-bold text-on-surface">
                      Nenhuma recomendação no momento
                    </p>
                    <p className="mx-auto mt-1 max-w-md text-sm text-text-secondary">
                      Assim que houver pedidos compatíveis no seu raio, eles aparecem aqui.
                    </p>
                  </div>
                )}

                {!isEligibleToDonate && (
                  <div className="rounded-[2rem] border border-surface-container-high bg-white p-6 sm:p-8">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                      <div className="flex items-start gap-3">
                        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-surface-container-low text-secondary">
                          <span className="material-symbols-outlined">hourglass_top</span>
                        </div>
                        <div>
                          <p className="font-headline text-lg font-bold text-on-surface">Intervalo de segurança</p>
                          <p className="mt-1 text-sm text-text-secondary">
                            Faltam <span className="font-bold text-on-surface">{waitingDays}</span>{" "}
                            {waitingDays === 1 ? "dia" : "dias"} para você voltar a doar.
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                <DonationHistory
                  items={donationHistory}
                  isLoading={isLoadingDonationHistory}
                  errorMessage={donationHistoryError}
                  onChanged={() => {
                    void reloadDonationHistory();
                  }}
                />
              </section>
            </>
          )}

          {!canAccessDonorDashboard && canAccessAdminArea && (
            <section className="rounded-[2rem] border border-surface-container-high bg-white p-6 sm:p-8">
              <h2 className="font-headline text-xl font-extrabold text-on-surface">Acesso administrativo</h2>
              <p className="mt-2 text-sm text-text-secondary">
                Use o menu lateral para gerenciar requisições e demais áreas liberadas ao seu perfil.
              </p>
            </section>
          )}
        </div>
      </main>
    </div>
  );
}
