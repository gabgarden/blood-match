import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { MobileBottomNav } from "../components/dashboard/MobileBottomNav";
import { FullPageLoading, InlineAlert, type BloodCenterSelection } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { CreateRequestForm } from "../components/requests/CreateRequestForm";
import { RequestPreview } from "../components/requests/RequestPreview";
import { RequestGuide } from "../components/requests/RequestGuide";
import { AccessDenied } from "../components/AccessDenied";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasAdminRole, hasRequesterRole } from "../routes/roleRouting";
import { createDonationRequest } from "../services/donationService";
import { extractApiErrorMessage } from "../utils/apiError";

function mapUrgencyToApi(urgency: "BAIXA" | "MÉDIA" | "CRÍTICA"): "LOW" | "MEDIUM" | "CRITICAL" {
  if (urgency === "CRÍTICA") {
    return "CRITICAL";
  }

  if (urgency === "MÉDIA") {
    return "MEDIUM";
  }

  return "LOW";
}

export default function NewRequestPage() {
  const navigate = useNavigate();
  const { partyId, roles, logout } = useAuth();

  const isResolvingRoles = useRoleResolution(roles);
  const canAccessRequesterArea = hasRequesterRole(roles);
  const canAccessAdminArea = hasAdminRole(roles);

  const [bloodType, setBloodType] = useState("");
  const [quantity, setQuantity] = useState(0);
  const [urgency, setUrgency] = useState<"BAIXA" | "MÉDIA" | "CRÍTICA">("CRÍTICA");
  const [bloodCenter, setBloodCenter] = useState<BloodCenterSelection | null>(null);
  const [description, setDescription] = useState("");
  const [dateLimit, setDateLimit] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (!canAccessRequesterArea && !canAccessAdminArea) {
    return (
      <main className="min-h-screen bg-surface p-6">
        <div className="mx-auto max-w-3xl">
          <AccessDenied title="Área de Nova Requisição indisponível" />
        </div>
      </main>
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsLoading(true);

    try {
      if (!partyId) {
        throw new Error("Usuário não identificado. Faça login novamente.");
      }

      if (!bloodCenter?.organizationId) {
        throw new Error("Selecione o hemocentro de destino.");
      }

      if (quantity <= 0) {
        throw new Error("A quantidade de bolsas deve ser maior que zero.");
      }

      await createDonationRequest({
        partyId,
        organizationId: bloodCenter.organizationId,
        bloodTypeNeeded: bloodType,
        goalBloodBags: quantity,
        dateLimit,
        urgency: mapUrgencyToApi(urgency),
        directedTo: description.trim() || undefined,
      });

      setSuccessMessage("Requisição publicada com sucesso!");

      setTimeout(() => {
        navigate("/requests");
      }, 1500);
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Erro ao publicar requisição"));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="new-request" />
      <DonorDashboardTopbar title="Nova Requisição" onLogout={logout} />

      <main className="pt-20 px-4 pb-24 lg:ml-64 lg:px-8 lg:pb-16">
        <div className="max-w-5xl mx-auto">
          {successMessage && <InlineAlert tone="success" message={successMessage} className="mb-6" />}

          <div className="grid grid-cols-12 gap-8">
            <div className="col-span-12 lg:col-span-7">
              <CreateRequestForm
                bloodType={bloodType}
                onBloodTypeChange={setBloodType}
                quantity={quantity}
                onQuantityChange={setQuantity}
                urgency={urgency}
                onUrgencyChange={setUrgency}
                bloodCenter={bloodCenter}
                onBloodCenterChange={setBloodCenter}
                description={description}
                onDescriptionChange={setDescription}
                dateLimit={dateLimit}
                onDateLimitChange={setDateLimit}
                onSubmit={handleSubmit}
                isLoading={isLoading}
                errorMessage={errorMessage}
              />
            </div>

            <div className="col-span-12 lg:col-span-5 flex flex-col gap-8">
              <RequestPreview
                bloodType={bloodType}
                quantity={quantity}
                hospital={bloodCenter?.name || "Hemocentro não informado"}
                urgency={urgency}
              />
              <RequestGuide />
            </div>
          </div>
        </div>
      </main>

      <MobileBottomNav activeItem="new-request" />
    </div>
  );
}
