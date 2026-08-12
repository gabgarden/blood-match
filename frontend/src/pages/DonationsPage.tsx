import { useMemo, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { DonationHistory } from "../components/dashboard/DonationHistory";
import { TodayDonationBanner } from "../components/dashboard/TodayDonationBanner";
import { FullPageLoading, InlineAlert } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useDonorDashboard } from "../hooks/useDonorDashboard";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasDonorRole, hasRequesterRole, hasAdminRole } from "../routes/roleRouting";
import { externalDonationCreatePath } from "../services/donationService";

export default function DonationsPage() {
  const navigate = useNavigate();
  const { roles, partyId, logout } = useAuth();
  const normalizedRoles = useMemo(() => roles, [roles]);
  const isResolvingRoles = useRoleResolution(normalizedRoles);
  const canAccessDonorArea = hasDonorRole(normalizedRoles);
  const canAccessRequesterArea = hasRequesterRole(normalizedRoles);
  const canAccessAdminArea = hasAdminRole(normalizedRoles);
  const isRequesterOnly = canAccessRequesterArea && !canAccessDonorArea && !canAccessAdminArea;

  const [activeGuideTab, setActiveGuideTab] = useState<"before" | "after" | "clt">("before");

  const {
    donorBloodType,
    daysRemaining,
    livesImpacted,
    donationHistory,
    isLoadingDonationHistory,
    donationHistoryError,
    reloadDonationHistory,
    feedback,
    errorMessage,
  } = useDonorDashboard({ partyId, hasDonorRole: canAccessDonorArea });

  const waitingDays = Math.max(daysRemaining, 0);
  const isEligibleToDonate = waitingDays <= 0;

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (isRequesterOnly) {
    return <Navigate to="/requests" replace />;
  }

  function handleCreateExternalDonation() {
    navigate(externalDonationCreatePath);
  }

  // Encontra a doação pendente/agendada ativa se houver
  const pendingDonation = donationHistory.find(
    (item) => item.status === "REGISTERED" || item.status === "PENDING" || item.status === "SCHEDULED"
  );

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="donations" />
      <DonorDashboardTopbar title="Minhas Doações" onLogout={logout} />

      <main className="pt-20 px-4 pb-10 lg:ml-64 lg:px-8">
        <div className="mx-auto max-w-5xl space-y-6">
          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}

          {/* Banner Hero das Doações */}
          <section className="relative overflow-hidden rounded-[2rem] bg-white border border-surface-container-high p-6 lg:p-8">
            <div className="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-[#fff2f0] pointer-events-none" />
            <div className="absolute -left-10 bottom-0 h-32 w-32 rounded-full bg-[#eaf3f7] opacity-80 pointer-events-none" />

            <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
              <div className="space-y-2">
                <div className="inline-flex items-center gap-1.5 rounded-full bg-[#fff2f0] px-3 py-1 text-xs font-bold text-primary">
                  <span className="material-symbols-outlined text-sm">water_drop</span>
                  Gestão de Doações
                </div>
                <h1 className="font-headline text-3xl font-extrabold tracking-tight text-on-surface">
                  Sua Jornada de Solidariedade
                </h1>
                <p className="text-sm text-text-secondary max-w-xl leading-relaxed">
                  Acompanhe doações agendadas, registre comprovantes externos e veja seu histórico de bolsas doadas.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3 shrink-0">
                <button
                  type="button"
                  onClick={handleCreateExternalDonation}
                  className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-bold text-white shadow-md hover:bg-[#920f16] transition-colors"
                >
                  <span className="material-symbols-outlined text-base">add</span>
                  Registrar Doação Externa
                </button>
              </div>
            </div>

            {/* Grid de Resumo Rápido */}
            <div className="relative z-10 mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="font-headline text-2xl font-black text-primary">{donorBloodType || "—"}</p>
                <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Tipo Sanguíneo</p>
              </div>
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="font-headline text-2xl font-black text-on-surface">{livesImpacted}</p>
                <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Vidas Salvas (Est.)</p>
              </div>
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="font-headline text-2xl font-black text-on-surface">{donationHistory.length}</p>
                <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Doações Registradas</p>
              </div>
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                {isEligibleToDonate ? (
                  <p className="font-headline text-lg font-black text-emerald-600 leading-tight">Pronto Hoje!</p>
                ) : (
                  <p className="font-headline text-2xl font-black text-on-surface">{waitingDays}</p>
                )}
                <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">
                  {isEligibleToDonate ? "Elegibilidade" : "Dias p/ Próxima"}
                </p>
              </div>
            </div>
          </section>

          {/* Doação Agendada Ativa (Caso Exista) */}
          {pendingDonation && (
            <section className="space-y-3">
              <h2 className="font-headline text-xl font-extrabold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">event_upcoming</span>
                Doação Agendada em Andamento
              </h2>
              <TodayDonationBanner
                hospitalName={pendingDonation.location}
                expectedDate={pendingDonation.donationDate ? pendingDonation.donationDate.slice(0, 10).split("-").reverse().join("/") : undefined}
                bloodType={donorBloodType}
              />
            </section>
          )}

          {/* Seção de Guia & Orientações Médicas */}
          <section className="rounded-[2rem] border border-surface-container-high bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mb-5">
              <div>
                <h3 className="font-headline text-xl font-extrabold text-on-surface flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-xl">medical_information</span>
                  Orientações Importantes ao Doador
                </h3>
                <p className="mt-1 text-xs text-text-secondary">
                  Informações essenciais para garantir uma doação segura e saudável.
                </p>
              </div>

              {/* Tabs do Guia */}
              <div className="flex items-center gap-1 rounded-xl bg-surface-container-low p-1 border border-surface-container-high text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setActiveGuideTab("before")}
                  className={`rounded-lg px-3 py-1.5 transition-all ${
                    activeGuideTab === "before" ? "bg-white text-primary shadow-xs" : "text-secondary hover:text-on-surface"
                  }`}
                >
                  Antes de Doar
                </button>
                <button
                  type="button"
                  onClick={() => setActiveGuideTab("after")}
                  className={`rounded-lg px-3 py-1.5 transition-all ${
                    activeGuideTab === "after" ? "bg-white text-primary shadow-xs" : "text-secondary hover:text-on-surface"
                  }`}
                >
                  Após Doar
                </button>
                <button
                  type="button"
                  onClick={() => setActiveGuideTab("clt")}
                  className={`rounded-lg px-3 py-1.5 transition-all ${
                    activeGuideTab === "clt" ? "bg-white text-primary shadow-xs" : "text-secondary hover:text-on-surface"
                  }`}
                >
                  Direito CLT
                </button>
              </div>
            </div>

            {/* Conteúdo da Tab Ativa */}
            {activeGuideTab === "before" && (
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="rounded-2xl bg-surface-container-low p-4 border border-surface-container-high">
                  <span className="material-symbols-outlined text-primary text-2xl mb-1">local_drinking</span>
                  <h4 className="font-headline text-sm font-bold text-on-surface">Hidratação abundante</h4>
                  <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                    Beba pelo menos 500ml de água antes de sair de casa para facilitar a coleta.
                  </p>
                </div>
                <div className="rounded-2xl bg-surface-container-low p-4 border border-surface-container-high">
                  <span className="material-symbols-outlined text-primary text-2xl mb-1">set_meal</span>
                  <h4 className="font-headline text-sm font-bold text-on-surface">Alimentação leve</h4>
                  <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                    Evite alimentos gordurosos (frituras, ovos, leites) nas 4h que antecedem a doação.
                  </p>
                </div>
                <div className="rounded-2xl bg-surface-container-low p-4 border border-surface-container-high">
                  <span className="material-symbols-outlined text-primary text-2xl mb-1">badge</span>
                  <h4 className="font-headline text-sm font-bold text-on-surface">Documento oficial</h4>
                  <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                    Apresente documento oficial com foto (RG, CNH, Passaporte ou Carteira de Trabalho).
                  </p>
                </div>
              </div>
            )}

            {activeGuideTab === "after" && (
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="rounded-2xl bg-surface-container-low p-4 border border-surface-container-high">
                  <span className="material-symbols-outlined text-emerald-600 text-2xl mb-1">self_improvement</span>
                  <h4 className="font-headline text-sm font-bold text-on-surface">Repouso inicial</h4>
                  <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                    Permaneça sentado e tome o lanche oferecido pelo hemocentro por pelo menos 15 minutos.
                  </p>
                </div>
                <div className="rounded-2xl bg-surface-container-low p-4 border border-surface-container-high">
                  <span className="material-symbols-outlined text-emerald-600 text-2xl mb-1">fitness_center</span>
                  <h4 className="font-headline text-sm font-bold text-on-surface">Evitar esforço pesado</h4>
                  <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                    Não pratique exercícios físicos pesados ou transporte de cargas nas 12h seguintes.
                  </p>
                </div>
                <div className="rounded-2xl bg-surface-container-low p-4 border border-surface-container-high">
                  <span className="material-symbols-outlined text-emerald-600 text-2xl mb-1">no_drinks</span>
                  <h4 className="font-headline text-sm font-bold text-on-surface">Bebidas e fumo</h4>
                  <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                    Não fume por 2h e evite bebidas alcoólicas por pelo menos 12h após doar.
                  </p>
                </div>
              </div>
            )}

            {activeGuideTab === "clt" && (
              <div className="rounded-2xl bg-blue-50/60 p-4 border border-blue-100 flex items-start gap-3">
                <span className="material-symbols-outlined text-blue-600 text-2xl shrink-0 mt-0.5">gavel</span>
                <div className="space-y-1">
                  <h4 className="font-headline text-sm font-extrabold text-blue-900">
                    Abono de Falta Garantido pela CLT (Art. 473)
                  </h4>
                  <p className="text-xs text-blue-800 leading-relaxed">
                    O trabalhador sob regime CLT pode se ausentar do trabalho por <strong>1 dia a cada 12 meses</strong> sem qualquer prejuízo no salário, desde que comprovada a doação de sangue voluntária emitida pelo hemocentro.
                  </p>
                </div>
              </div>
            )}
          </section>

          {/* Histórico Completo de Doações */}
          <DonationHistory
            items={donationHistory}
            isLoading={isLoadingDonationHistory}
            errorMessage={donationHistoryError}
            onChanged={() => {
              void reloadDonationHistory();
            }}
          />
        </div>
      </main>
    </div>
  );
}
