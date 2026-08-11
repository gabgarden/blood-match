import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router-dom";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { ProfileHero } from "../components/profile/ProfileHero";
import { ProfileSection } from "../components/profile/ProfileSection";
import { AppButton, FullPageLoading, InlineAlert } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useRoleResolution } from "../hooks/useRoleResolution";
import {
  hasBloodCenterRole,
  hasDonorRole,
  hasRequesterRole,
  resolvePostLoginPath,
} from "../routes/roleRouting";
import { fetchDonorHeroSummary, updatePartyName } from "../services/partyService";
import { updateDonorProfile, updateDonorRecommendationDistance } from "../services/profileService";
import { extractApiErrorMessage } from "../utils/apiError";

const bloodTypes = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

const fieldClass =
  "w-full bg-surface-container-highest border-none rounded-xl p-4 focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all placeholder:text-gray-400";

const labelClass = "block font-label text-sm font-semibold text-secondary uppercase tracking-wider mb-2";

const DISTANCE_PRESETS = [10, 20, 30, 50, 100];

export default function ProfilePage() {
  const { roles, partyId, logout } = useAuth();
  const isResolvingRoles = useRoleResolution(roles);
  const canAccessDonorArea = hasDonorRole(roles);
  const canAccessRequesterArea = hasRequesterRole(roles);
  const canAccessBloodCenter = hasBloodCenterRole(roles);

  const [displayName, setDisplayName] = useState("");
  const [bloodType, setBloodType] = useState("O+");
  const [weight, setWeight] = useState("");
  const [maxDistanceInKm, setMaxDistanceInKm] = useState("30");
  const [phoneNumber, setPhoneNumber] = useState<string | null>(null);
  const [address, setAddress] = useState<string | null>(null);
  const [livesImpacted, setLivesImpacted] = useState<number | null>(null);
  const [daysRemaining, setDaysRemaining] = useState<number | null>(null);
  const [lastDonationDate, setLastDonationDate] = useState<string | null>(null);

  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [isSavingName, setIsSavingName] = useState(false);
  const [isSavingDonor, setIsSavingDonor] = useState(false);
  const [isSavingDistance, setIsSavingDistance] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const hasAnyRole = useMemo(
    () => canAccessDonorArea || canAccessRequesterArea || canAccessBloodCenter || roles.length > 0,
    [canAccessDonorArea, canAccessRequesterArea, canAccessBloodCenter, roles.length],
  );

  const parsedDistance = Number(maxDistanceInKm);
  const distanceValid = !Number.isNaN(parsedDistance) && parsedDistance > 0;

  useEffect(() => {
    if (!partyId) {
      setIsLoadingProfile(false);
      return;
    }

    const currentPartyId = partyId;
    let cancelled = false;

    async function load() {
      setIsLoadingProfile(true);
      setErrorMessage(null);

      try {
        if (canAccessDonorArea) {
          const summary = await fetchDonorHeroSummary(currentPartyId);
          if (!cancelled && summary) {
            setDisplayName(summary.donorName?.trim() || "");
            if (summary.bloodType) {
              setBloodType(summary.bloodType);
            }
            setPhoneNumber(summary.phoneNumber);
            setAddress(summary.address);
            setLivesImpacted(summary.livesImpacted);
            setDaysRemaining(summary.daysRemaining);
            setLastDonationDate(summary.lastDonationDate);
          }
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(extractApiErrorMessage(error, "Não foi possível carregar o perfil."));
        }
      } finally {
        if (!cancelled) {
          setIsLoadingProfile(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [partyId, canAccessDonorArea]);

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (!hasAnyRole) {
    return <Navigate to={resolvePostLoginPath(roles)} replace />;
  }

  async function handleSaveName(event: React.FormEvent) {
    event.preventDefault();
    if (!partyId) {
      return;
    }

    const newName = displayName.trim();
    if (!newName) {
      setErrorMessage("Informe um nome válido.");
      return;
    }

    setIsSavingName(true);
    setFeedback(null);
    setErrorMessage(null);

    try {
      const result = await updatePartyName(partyId, newName);
      setDisplayName(result.name);
      setFeedback("Nome atualizado com sucesso.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível atualizar o nome."));
    } finally {
      setIsSavingName(false);
    }
  }

  async function handleSaveDonorProfile(event: React.FormEvent) {
    event.preventDefault();
    if (!partyId) {
      return;
    }

    const parsedWeight = Number(weight);
    if (!bloodType || Number.isNaN(parsedWeight) || parsedWeight <= 0) {
      setErrorMessage("Informe tipo sanguíneo e um peso válido.");
      return;
    }

    setIsSavingDonor(true);
    setFeedback(null);
    setErrorMessage(null);

    try {
      await updateDonorProfile({
        personId: partyId,
        bloodType,
        weight: parsedWeight,
      });
      setFeedback("Dados de doação atualizados.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível atualizar o perfil de doador."));
    } finally {
      setIsSavingDonor(false);
    }
  }

  async function handleSaveDistance(event: React.FormEvent) {
    event.preventDefault();
    if (!partyId) {
      return;
    }

    if (!distanceValid) {
      setErrorMessage("Informe uma distância máxima válida em km.");
      return;
    }

    setIsSavingDistance(true);
    setFeedback(null);
    setErrorMessage(null);

    try {
      const result = await updateDonorRecommendationDistance(partyId, parsedDistance);
      setMaxDistanceInKm(String(result.maxDistanceInKm));
      setFeedback("Raio de recomendações atualizado.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível atualizar a distância."));
    } finally {
      setIsSavingDistance(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="profile" />
      <DonorDashboardTopbar title="Meu Perfil" onLogout={logout} />

      <main className="pt-20 px-4 pb-10 lg:ml-64 lg:px-8">
        <div className="mx-auto max-w-4xl space-y-6">
          {isLoadingProfile ? (
            <div className="rounded-[2rem] border border-surface-container-high bg-white p-10 text-center">
              <span className="material-symbols-outlined animate-spin text-primary text-3xl">progress_activity</span>
              <p className="mt-3 text-sm text-text-secondary">Carregando seu perfil...</p>
            </div>
          ) : (
            <ProfileHero
              displayName={displayName}
              bloodType={canAccessDonorArea ? bloodType : null}
              phoneNumber={phoneNumber}
              address={address}
              roles={roles}
              livesImpacted={livesImpacted}
              daysRemaining={daysRemaining}
              lastDonationDate={lastDonationDate}
              isDonor={canAccessDonorArea}
            />
          )}

          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}

          {!isLoadingProfile && (
            <>
              <ProfileSection
                icon="badge"
                title="Identidade"
                description="Nome exibido na plataforma e nas comunicações."
              >
                <form className="space-y-5" onSubmit={handleSaveName}>
                  <div>
                    <label htmlFor="profile-name" className={labelClass}>
                      Nome completo
                    </label>
                    <div className="relative">
                      <input
                        id="profile-name"
                        className={`${fieldClass} pr-12`}
                        value={displayName}
                        onChange={(event) => setDisplayName(event.target.value)}
                        placeholder="Como você deseja ser chamado"
                        required
                      />
                      <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 material-symbols-outlined text-sm text-gray-400">
                        person
                      </span>
                    </div>
                  </div>
                  <AppButton type="submit" variant="danger" disabled={isSavingName} className="px-6">
                    {isSavingName ? "Salvando..." : "Salvar nome"}
                  </AppButton>
                </form>
              </ProfileSection>

              {canAccessDonorArea ? (
                <>
                  <ProfileSection
                    icon="bloodtype"
                    title="Dados de doação"
                    description="Tipo sanguíneo e peso usados para elegibilidade e matching."
                  >
                    <form className="space-y-6" onSubmit={handleSaveDonorProfile}>
                      <div>
                        <p className={labelClass}>Tipo sanguíneo</p>
                        <div className="grid grid-cols-4 gap-3">
                          {bloodTypes.map((type) => (
                            <button
                              key={type}
                              type="button"
                              onClick={() => setBloodType(type)}
                              className={`flex items-center justify-center rounded-xl p-3 transition-all ${
                                bloodType === type
                                  ? "bg-primary text-white shadow-md"
                                  : "bg-surface-container-low text-primary hover:bg-[#fff2f0]"
                              }`}
                            >
                              <span className="font-headline text-lg font-black">{type}</span>
                            </button>
                          ))}
                        </div>
                      </div>

                      <div className="max-w-xs">
                        <label htmlFor="profile-weight" className={labelClass}>
                          Peso (kg)
                        </label>
                        <div className="relative">
                          <input
                            id="profile-weight"
                            className={`${fieldClass} pr-12`}
                            type="number"
                            min="1"
                            step="0.1"
                            value={weight}
                            onChange={(event) => setWeight(event.target.value)}
                            placeholder="Ex: 72.5"
                            required
                          />
                          <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-gray-400">
                            kg
                          </span>
                        </div>
                      </div>

                      <AppButton type="submit" variant="danger" disabled={isSavingDonor} className="px-6">
                        {isSavingDonor ? "Salvando..." : "Salvar dados de doação"}
                      </AppButton>
                    </form>
                  </ProfileSection>

                  <ProfileSection
                    icon="radar"
                    title="Raio de recomendações"
                    description="Pedidos fora desse raio não aparecem nas suas recomendações."
                  >
                    <form className="space-y-6" onSubmit={handleSaveDistance}>
                      <div>
                        <div className="mb-3 flex items-end justify-between gap-3">
                          <label htmlFor="profile-distance" className={labelClass + " mb-0"}>
                            Distância máxima
                          </label>
                          <p className="font-headline text-2xl font-black text-primary">
                            {distanceValid ? `${parsedDistance} km` : "—"}
                          </p>
                        </div>

                        <input
                          id="profile-distance"
                          type="range"
                          min={5}
                          max={150}
                          step={5}
                          value={distanceValid ? parsedDistance : 30}
                          onChange={(event) => setMaxDistanceInKm(event.target.value)}
                          className="w-full accent-[#ae131a]"
                        />

                        <div className="mt-4 flex flex-wrap gap-2">
                          {DISTANCE_PRESETS.map((preset) => (
                            <button
                              key={preset}
                              type="button"
                              onClick={() => setMaxDistanceInKm(String(preset))}
                              className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-colors ${
                                Number(maxDistanceInKm) === preset
                                  ? "bg-primary text-white"
                                  : "bg-surface-container-high text-secondary hover:bg-surface-container-highest"
                              }`}
                            >
                              {preset} km
                            </button>
                          ))}
                        </div>
                      </div>

                      <AppButton type="submit" variant="danger" disabled={isSavingDistance || !distanceValid} className="px-6">
                        {isSavingDistance ? "Salvando..." : "Salvar raio"}
                      </AppButton>
                    </form>
                  </ProfileSection>
                </>
              ) : (
                <ProfileSection
                  icon="info"
                  title="Perfil de doador"
                  description="Esta conta ainda não possui papel de doador. As preferências de doação ficam disponíveis após o cadastro como doador."
                >
                  <div className="rounded-xl bg-surface-container-low px-4 py-3 text-sm text-text-secondary">
                    Papéis atuais:{" "}
                    <span className="font-semibold text-on-surface">
                      {roles.length > 0 ? roles.join(", ") : "nenhum"}
                    </span>
                  </div>
                </ProfileSection>
              )}
            </>
          )}
        </div>
      </main>
    </div>
  );
}
