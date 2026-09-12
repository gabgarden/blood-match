import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { MobileBottomNav } from "../components/dashboard/MobileBottomNav";
import { RequestCard } from "../components/requests/RequestCard";
import { AppButton, FullPageLoading, InlineAlert, Modal } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { AccessDenied } from "../components/AccessDenied";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasAdminRole, hasRequesterRole } from "../routes/roleRouting";
import {
  deleteDonationRequest,
  fetchUserDonationRequests,
  notifyDonationRequest,
  type UserDonationRequestCard,
} from "../services/donationService";
import { extractApiErrorMessage } from "../utils/apiError";

type StatusFilter = "all" | "active" | "goal" | "expired";

const STATUS_FILTERS: { id: StatusFilter; label: string }[] = [
  { id: "all", label: "Todas" },
  { id: "active", label: "Ativas" },
  { id: "goal", label: "Meta atingida" },
  { id: "expired", label: "Expiradas" },
];

function matchesStatus(request: UserDonationRequestCard, filter: StatusFilter): boolean {
  if (filter === "all") {
    return true;
  }
  if (filter === "goal") {
    return request.goalReached;
  }
  if (filter === "expired") {
    return request.expired && !request.goalReached;
  }
  return request.active && !request.goalReached && !request.expired;
}

export default function RequestsPage() {
  const { roles, partyId, logout } = useAuth();
  const [selectedBloodType, setSelectedBloodType] = useState("Todos");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [requestCards, setRequestCards] = useState<UserDonationRequestCard[]>([]);
  const [isLoadingRequests, setIsLoadingRequests] = useState(false);
  const [requestsError, setRequestsError] = useState<string | null>(null);
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyRequestId, setBusyRequestId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  const normalizedRoles = useMemo(() => roles, [roles]);
  const isResolvingRoles = useRoleResolution(normalizedRoles);
  const canAccessRequesterArea = hasRequesterRole(normalizedRoles);
  const canAccessAdminArea = hasAdminRole(normalizedRoles);

  useEffect(() => {
    if (!partyId || (!canAccessRequesterArea && !canAccessAdminArea)) {
      setRequestCards([]);
      return;
    }

    const currentPartyId = partyId;
    let isCancelled = false;

    async function run() {
      setIsLoadingRequests(true);
      setRequestsError(null);

      try {
        const items = await fetchUserDonationRequests(currentPartyId);
        if (!isCancelled) {
          setRequestCards(items);
        }
      } catch (error) {
        if (!isCancelled) {
          setRequestsError(extractApiErrorMessage(error, "Não foi possível carregar suas requisições agora."));
          setRequestCards([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoadingRequests(false);
        }
      }
    }

    run();

    return () => {
      isCancelled = true;
    };
  }, [partyId, canAccessRequesterArea, canAccessAdminArea]);

  async function handleNotify(requestId: string) {
    setActionFeedback(null);
    setActionError(null);
    setBusyRequestId(requestId);

    try {
      const message = await notifyDonationRequest(requestId);
      setActionFeedback(message);
    } catch (error) {
      setActionError(extractApiErrorMessage(error, "Não foi possível notificar doadores."));
    } finally {
      setBusyRequestId(null);
    }
  }

  async function confirmDelete() {
    if (!pendingDeleteId) {
      return;
    }

    const requestId = pendingDeleteId;
    setPendingDeleteId(null);
    setActionFeedback(null);
    setActionError(null);
    setBusyRequestId(requestId);

    const requestToDelete = requestCards.find((r) => r.id === requestId);
    if (!requestToDelete) {
      setBusyRequestId(null);
      return;
    }

    try {
      await deleteDonationRequest(requestId, requestToDelete.version);
      setRequestCards((current) => current.filter((request) => request.id !== requestId));
      setActionFeedback("Requisição removida.");
    } catch (error) {
      setActionError(extractApiErrorMessage(error, "Não foi possível remover a requisição."));
    } finally {
      setBusyRequestId(null);
    }
  }

  const availableBloodTypes = useMemo(() => {
    const dynamicTypes = Array.from(
      new Set(
        requestCards
          .map((request) => request.bloodType)
          .filter((bloodType) => bloodType && bloodType !== "-"),
      ),
    ).sort();

    return ["Todos", ...dynamicTypes];
  }, [requestCards]);

  const summary = useMemo(() => {
    const active = requestCards.filter((r) => r.active && !r.goalReached && !r.expired).length;
    const goalReached = requestCards.filter((r) => r.goalReached).length;
    const expired = requestCards.filter((r) => r.expired && !r.goalReached).length;
    const bagsRemaining = requestCards
      .filter((r) => r.active && !r.goalReached && !r.expired)
      .reduce((sum, r) => sum + r.remainingBloodBags, 0);

    return { active, goalReached, expired, bagsRemaining, total: requestCards.length };
  }, [requestCards]);

  const filteredRequests = useMemo(() => {
    return requestCards.filter((request) => {
      const bloodOk = selectedBloodType === "Todos" || request.bloodType === selectedBloodType;
      return bloodOk && matchesStatus(request, statusFilter);
    });
  }, [requestCards, selectedBloodType, statusFilter]);

  const pendingDeleteRequest = requestCards.find((request) => request.id === pendingDeleteId) ?? null;

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (!canAccessRequesterArea && !canAccessAdminArea) {
    return (
      <main className="min-h-screen bg-surface p-6">
        <div className="mx-auto max-w-3xl">
          <AccessDenied title="Área de Requisições indisponível" />
        </div>
      </main>
    );
  }


  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="requests" />
      <DonorDashboardTopbar title="Suas Requisições" onLogout={logout} />

      <main className="pt-20 px-4 pb-24 lg:ml-64 lg:px-8 lg:pb-10">
        <div className="mx-auto max-w-[1400px] space-y-6">
          <section className="relative overflow-hidden rounded-[2rem] border border-surface-container-high bg-white p-6 lg:p-8">
            <div className="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-[#fff2f0]" />
            <div className="absolute -left-10 bottom-0 h-28 w-28 rounded-full bg-[#eaf3f7] opacity-80" />

            <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.2em] text-secondary">Solicitações</p>
                <h1 className="mt-1 font-headline text-3xl font-extrabold tracking-tight text-on-surface">
                  Suas Requisições
                </h1>
                <p className="mt-2 max-w-xl text-sm text-text-secondary">
                  Acompanhe o progresso das coletas, ajuste prazos e meta, e avise doadores elegíveis.
                </p>
              </div>

              <Link to="/requests/new">
                <AppButton className="w-full gap-2 px-5 lg:w-auto" variant="danger">
                  <span className="material-symbols-outlined text-sm">add</span>
                  Nova Requisição
                </AppButton>
              </Link>
            </div>

            <div className="relative z-10 mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Ativas</p>
                <p className="mt-1 font-headline text-2xl font-black text-primary">{summary.active}</p>
              </div>
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Meta atingida</p>
                <p className="mt-1 font-headline text-2xl font-black text-on-surface">{summary.goalReached}</p>
              </div>
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Expiradas</p>
                <p className="mt-1 font-headline text-2xl font-black text-on-surface">{summary.expired}</p>
              </div>
              <div className="rounded-2xl bg-surface-container-low px-4 py-4">
                <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Bolsas faltando</p>
                <p className="mt-1 font-headline text-2xl font-black text-on-surface">{summary.bagsRemaining}</p>
              </div>
            </div>
          </section>

          <section className="rounded-[2rem] border border-surface-container-high bg-white p-4 sm:p-5">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex flex-wrap gap-2">
                {STATUS_FILTERS.map((filter) => (
                  <button
                    key={filter.id}
                    type="button"
                    onClick={() => setStatusFilter(filter.id)}
                    className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-colors ${
                      statusFilter === filter.id
                        ? "bg-primary text-white"
                        : "bg-surface-container-high text-secondary hover:bg-surface-container-highest"
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <span className="text-[10px] font-bold uppercase tracking-wider text-secondary">Tipo</span>
                {availableBloodTypes.map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setSelectedBloodType(type)}
                    className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-colors ${
                      selectedBloodType === type
                        ? "bg-[#fff2f0] text-primary"
                        : "bg-surface-container-low text-secondary hover:bg-surface-container-high"
                    }`}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>
          </section>

          {actionFeedback && <InlineAlert tone="success" message={actionFeedback} />}
          {actionError && <InlineAlert tone="error" message={actionError} />}
          {requestsError && <InlineAlert tone="error" message={requestsError} />}

          {isLoadingRequests && (
            <div className="rounded-[2rem] border border-surface-container-high bg-white p-10 text-center">
              <span className="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
              <p className="mt-3 text-sm text-text-secondary">Carregando suas requisições...</p>
            </div>
          )}

          {!isLoadingRequests && !requestsError && filteredRequests.length > 0 && (
            <section className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
              {filteredRequests.map((request) => (
                <RequestCard
                  key={request.id}
                  request={request}
                  isBusy={busyRequestId === request.id}
                  onNotify={handleNotify}
                  onDelete={setPendingDeleteId}
                  onUpdated={(requestId, patch) => {
                    setRequestCards((current) =>
                      current.map((item) => (item.id === requestId ? { ...item, ...patch } : item)),
                    );
                    setActionFeedback("Requisição atualizada.");
                    setActionError(null);
                  }}
                />
              ))}
            </section>
          )}

          {!isLoadingRequests && !requestsError && filteredRequests.length === 0 && (
            <section className="rounded-[2rem] border border-dashed border-surface-container-highest bg-white px-6 py-14 text-center">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-[#fff2f0] text-primary">
                <span className="material-symbols-outlined text-3xl">
                  {summary.total === 0 ? "add_circle" : "filter_alt_off"}
                </span>
              </div>
              <h2 className="mt-4 font-headline text-2xl font-extrabold text-on-surface">
                {summary.total === 0 ? "Nenhuma requisição ainda" : "Nenhum resultado com esses filtros"}
              </h2>
              <p className="mx-auto mt-2 max-w-md text-sm text-text-secondary">
                {summary.total === 0
                  ? "Publique um pedido de doação para começar a acompanhar o progresso da coleta."
                  : "Ajuste o status ou o tipo sanguíneo para ver outras requisições."}
              </p>
              {summary.total === 0 ? (
                <Link to="/requests/new" className="mt-6 inline-flex">
                  <AppButton variant="danger" className="gap-2 px-5">
                    <span className="material-symbols-outlined text-sm">add</span>
                    Criar primeira requisição
                  </AppButton>
                </Link>
              ) : (
                <AppButton
                  type="button"
                  variant="secondary"
                  className="mt-6 px-5"
                  onClick={() => {
                    setStatusFilter("all");
                    setSelectedBloodType("Todos");
                  }}
                >
                  Limpar filtros
                </AppButton>
              )}
            </section>
          )}
        </div>
      </main>

      <Modal
        open={Boolean(pendingDeleteRequest)}
        title="Remover requisição?"
        description={
          pendingDeleteRequest
            ? `A solicitação de ${pendingDeleteRequest.bloodType} em ${pendingDeleteRequest.bloodCenterName} será removida permanentemente.`
            : undefined
        }
        onClose={() => setPendingDeleteId(null)}
      >
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <AppButton type="button" variant="secondary" onClick={() => setPendingDeleteId(null)}>
            Cancelar
          </AppButton>
          <AppButton type="button" variant="danger" onClick={confirmDelete}>
            Remover
          </AppButton>
        </div>
      </Modal>

      <MobileBottomNav activeItem="requests" />
    </div>
  );
}
