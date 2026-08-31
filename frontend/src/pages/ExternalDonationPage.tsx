import { useMemo, useState } from "react";
import { Navigate } from "react-router-dom";
import { AccessDenied } from "../components/AccessDenied";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { MobileBottomNav } from "../components/dashboard/MobileBottomNav";
import {
  AppCard,
  AppButton,
  BloodCenterSearchField,
  FullPageLoading,
  InlineAlert,
  type BloodCenterSelection,
} from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasAdminRole, hasDonorRole, hasRequesterRole } from "../routes/roleRouting";
import { createDonation } from "../services/donationService";
import { extractApiErrorMessage } from "../utils/apiError";

type DonationMode = "pending" | "completed";

export default function ExternalDonationPage() {
  const { roles, partyId, logout } = useAuth();

  const normalizedRoles = useMemo(() => roles, [roles]);
  const isResolvingRoles = useRoleResolution(normalizedRoles);
  const canAccessDonorDashboard = hasDonorRole(normalizedRoles);
  const canAccessRequesterArea = hasRequesterRole(normalizedRoles);
  const canAccessAdminArea = hasAdminRole(normalizedRoles);
  const isRequesterOnly = canAccessRequesterArea && !canAccessDonorDashboard && !canAccessAdminArea;

  const [mode, setMode] = useState<DonationMode>("completed");
  const [bloodCenter, setBloodCenter] = useState<BloodCenterSelection | null>(null);
  const [donationDate, setDonationDate] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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
          <AccessDenied title="Área de doação externa indisponível" />
        </div>
      </main>
    );
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!partyId) {
      setErrorMessage("Usuário não identificado. Faça login novamente.");
      return;
    }

    if (!bloodCenter?.organizationId) {
      setErrorMessage("Selecione o hemocentro onde a doação ocorreu ou será realizada.");
      return;
    }

    if (!donationDate) {
      setErrorMessage("Informe a data da doação.");
      return;
    }

    setIsSubmitting(true);

    try {
      if (mode === "pending") {
        await createDonation({
          personId: partyId,
          organizationId: bloodCenter.organizationId,
          intendedDate: donationDate,
        });
        setSuccessMessage(`Doação pendente registrada em ${bloodCenter.name}.`);
      } else {
        await createDonation({
          personId: partyId,
          organizationId: bloodCenter.organizationId,
          donationDate,
        });
        setSuccessMessage(`Doação concluída registrada em ${bloodCenter.name}.`);
      }
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível registrar a doação."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="external-donation" />
      <DonorDashboardTopbar title="Registrar Doação Externa" onLogout={logout} />

      <main className="pt-20 px-4 pb-24 lg:ml-64 lg:px-8 lg:pb-8">
        <div className="mx-auto max-w-[900px]">
          <AppCard className="p-8 lg:p-10 bg-white border-0">
            <h1 className="font-headline text-3xl font-extrabold tracking-tight text-on-surface">
              Registrar Doação Externa
            </h1>
            <p className="mt-3 text-sm text-text-secondary">
              Busque o hemocentro pelo nome e registre uma doação pendente ou já concluída.
            </p>

            {successMessage && <InlineAlert className="mt-4" tone="success" message={successMessage} />}
            {errorMessage && <InlineAlert className="mt-4" tone="error" message={errorMessage} />}

            <form className="mt-8 space-y-8" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <label className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider">
                  Tipo de registro
                </label>
                <div className="flex bg-surface-container-high p-1 rounded-xl gap-1">
                  <button
                    type="button"
                    onClick={() => setMode("completed")}
                    className={`flex-1 py-3 text-xs font-bold rounded-lg transition-all ${
                      mode === "completed"
                        ? "bg-white text-primary shadow-sm"
                        : "bg-transparent text-secondary hover:bg-surface-container-low"
                    }`}
                  >
                    Concluída
                  </button>
                  <button
                    type="button"
                    onClick={() => setMode("pending")}
                    className={`flex-1 py-3 text-xs font-bold rounded-lg transition-all ${
                      mode === "pending"
                        ? "bg-white text-primary shadow-sm"
                        : "bg-transparent text-secondary hover:bg-surface-container-low"
                    }`}
                  >
                    Pendente / Agendada
                  </button>
                </div>
              </div>

              <BloodCenterSearchField
                value={bloodCenter}
                onChange={setBloodCenter}
                required
                disabled={isSubmitting}
              />

              <div className="space-y-2">
                <label className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider">
                  {mode === "pending" ? "Data esperada" : "Data da doação"}
                </label>
                <input
                  type="date"
                  value={donationDate}
                  onChange={(event) => setDonationDate(event.target.value)}
                  className="w-full bg-surface-container-highest border-none rounded-xl p-4 focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all"
                  required
                />
              </div>

              <AppButton type="submit" variant="danger" disabled={isSubmitting || !bloodCenter} fullWidth className="py-5">
                {isSubmitting
                  ? "Salvando..."
                  : mode === "pending"
                    ? "Agendar doação"
                    : "Registrar doação concluída"}
              </AppButton>
            </form>
          </AppCard>
        </div>
      </main>

      <MobileBottomNav activeItem="external-donation" />
    </div>
  );
}
