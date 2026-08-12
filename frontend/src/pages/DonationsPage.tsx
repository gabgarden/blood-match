import { useMemo, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { DonationHistory, type DonationHistoryItem } from "../components/dashboard/DonationHistory";
import { AppButton, FullPageLoading, InlineAlert, Modal } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useDonorDashboard } from "../hooks/useDonorDashboard";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasDonorRole, hasRequesterRole, hasAdminRole } from "../routes/roleRouting";
import { completeDonation, rescheduleDonation, externalDonationCreatePath } from "../services/donationService";
import { extractApiErrorMessage } from "../utils/apiError";

function isPendingStatus(status: string): boolean {
  const normalized = status.trim().toUpperCase();
  return ["SCHEDULED", "PENDING", "AGENDADO", "EM_ANDAMENTO"].includes(normalized);
}

function formatDate(input: string | null): string {
  if (!input) return "Data não informada";
  const parsed = new Date(input);
  if (Number.isNaN(parsed.getTime())) return input;
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(parsed);
}

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

function createGoogleCalendarUrl(title: string, location: string, dateIso: string): string {
  const cleanDate = dateIso.replace(/-/g, "");
  const start = `${cleanDate}T090000Z`;
  const end = `${cleanDate}T110000Z`;
  const params = new URLSearchParams({
    action: "TEMPLATE",
    text: title,
    details: `Doação agendada via BloodMatch no ${location}.`,
    location,
    dates: `${start}/${end}`,
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

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
  
  // Modal de gerenciamento (Concluir / Reagendar)
  const [selectedPending, setSelectedPending] = useState<DonationHistoryItem | null>(null);
  const [manageMode, setManageMode] = useState<"complete" | "reschedule">("complete");
  const [manageDate, setManageDate] = useState(todayIsoDate());
  const [isManaging, setIsManaging] = useState(false);
  const [manageError, setManageError] = useState<string | null>(null);
  const [manageFeedback, setManageFeedback] = useState<string | null>(null);

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

  // Encontra apenas doações genuinamente agendadas/pendentes (ex: PENDING, SCHEDULED)
  const pendingDonation = donationHistory.find((item) => isPendingStatus(item.status));

  function openManageModal(item: DonationHistoryItem, mode: "complete" | "reschedule") {
    setSelectedPending(item);
    setManageMode(mode);
    setManageDate(item.donationDate?.slice(0, 10) || todayIsoDate());
    setManageError(null);
    setManageFeedback(null);
  }

  async function handleManageSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedPending || !manageDate) return;

    setIsManaging(true);
    setManageError(null);
    setManageFeedback(null);

    try {
      if (manageMode === "complete") {
        await completeDonation(selectedPending.id, manageDate);
        setManageFeedback("Doação marcada como concluída com sucesso!");
      } else {
        await rescheduleDonation(selectedPending.id, manageDate);
        setManageFeedback("Data da doação reagendada com sucesso!");
      }

      await reloadDonationHistory();
      setTimeout(() => setSelectedPending(null), 800);
    } catch (err) {
      setManageError(extractApiErrorMessage(err, "Não foi possível processar a alteração."));
    } finally {
      setIsManaging(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="donations" />
      <DonorDashboardTopbar title="Minhas Doações" onLogout={logout} />

      <main className="pt-20 px-4 pb-10 lg:ml-64 lg:px-8">
        <div className="mx-auto max-w-5xl space-y-6">
          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}
          {manageFeedback && !selectedPending && <InlineAlert tone="success" message={manageFeedback} />}

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
                  Acompanhe doações agendadas, gerencie datas, registre comprovantes e consulte o histórico de bolsas doadas.
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

          {/* Doação Agendada Ativa (Se houver agendamento pendente) */}
          <section className="space-y-3">
            <h2 className="font-headline text-xl font-extrabold text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-xl">event_upcoming</span>
              Doação Agendada em Andamento
            </h2>

            {pendingDonation ? (
              <div className="overflow-hidden rounded-[2rem] border border-amber-200 bg-gradient-to-r from-amber-50/80 via-white to-amber-50/80 p-6 shadow-sm">
                <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
                  <div className="flex items-start gap-4">
                    <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-amber-500 text-white shadow-md">
                      <span className="material-symbols-outlined text-3xl">event_available</span>
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="rounded-full bg-amber-100 px-3 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-amber-800 border border-amber-200">
                          Agendamento Ativo
                        </span>
                        <span className="text-xs font-bold text-secondary">
                          Tipo {donorBloodType}
                        </span>
                      </div>

                      <h3 className="font-headline text-xl font-extrabold text-on-surface">
                        {pendingDonation.location}
                      </h3>

                      <p className="text-sm font-semibold text-text-secondary">
                        Data Prevista: <strong className="text-on-surface font-black">{formatDate(pendingDonation.donationDate)}</strong>
                      </p>
                    </div>
                  </div>

                  {/* Ações da Doação Agendada */}
                  <div className="flex flex-wrap items-center gap-2 shrink-0">
                    <button
                      type="button"
                      onClick={() => openManageModal(pendingDonation, "complete")}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-bold text-white shadow-sm hover:bg-emerald-700 transition-colors"
                    >
                      <span className="material-symbols-outlined text-base">check_circle</span>
                      Concluir Doação
                    </button>

                    <button
                      type="button"
                      onClick={() => openManageModal(pendingDonation, "reschedule")}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-white border border-surface-container-high px-4 py-2.5 text-xs font-bold text-on-surface hover:bg-surface-container-low transition-colors shadow-xs"
                    >
                      <span className="material-symbols-outlined text-base text-secondary">edit_calendar</span>
                      Reagendar
                    </button>

                    {pendingDonation.donationDate && (
                      <a
                        href={createGoogleCalendarUrl(
                          `Doação de Sangue (${donorBloodType})`,
                          pendingDonation.location,
                          pendingDonation.donationDate.slice(0, 10)
                        )}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 rounded-xl bg-blue-50 text-blue-700 border border-blue-100 px-3 py-2.5 text-xs font-bold hover:bg-blue-100 transition-colors"
                        title="Adicionar ao Google Agenda"
                      >
                        <span className="material-symbols-outlined text-base">calendar_add_on</span>
                        Google Agenda
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="rounded-[2rem] border border-dashed border-surface-container-highest bg-white p-6 text-center">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-container-low text-secondary">
                  <span className="material-symbols-outlined text-2xl">event_busy</span>
                </div>
                <p className="mt-3 font-headline text-base font-bold text-on-surface">
                  Nenhum agendamento pendente
                </p>
                <p className="mt-1 text-xs text-text-secondary">
                  Você está sem agendamentos ativos no momento. Acesse a aba <strong>Dashboard</strong> para encontrar recomendações compatíveis!
                </p>
              </div>
            )}
          </section>

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
                  <span className="material-symbols-outlined text-primary text-2xl mb-1">water_drop</span>
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

      {/* Modal de Concluir / Reagendar doação agendada */}
      <Modal
        open={!!selectedPending}
        onClose={() => setSelectedPending(null)}
        title={manageMode === "complete" ? "Concluir Doação Agendada" : "Reagendar Data da Doação"}
        description={selectedPending ? `${selectedPending.location}` : undefined}
      >
        <form className="space-y-4" onSubmit={handleManageSubmit}>
          {manageError && <InlineAlert tone="error" message={manageError} />}
          {manageFeedback && <InlineAlert tone="success" message={manageFeedback} />}

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-secondary">
              {manageMode === "complete" ? "Data real de conclusão" : "Nova data prevista"}
            </label>
            <input
              type="date"
              value={manageDate}
              onChange={(e) => setManageDate(e.target.value)}
              className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all"
              required
            />
          </div>

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <AppButton
              type="button"
              variant="secondary"
              onClick={() => setSelectedPending(null)}
              disabled={isManaging}
            >
              Cancelar
            </AppButton>
            <AppButton type="submit" variant="danger" disabled={isManaging}>
              {isManaging ? "Salvando..." : manageMode === "complete" ? "Confirmar Conclusão" : "Confirmar Reagendamento"}
            </AppButton>
          </div>
        </form>
      </Modal>
    </div>
  );
}
