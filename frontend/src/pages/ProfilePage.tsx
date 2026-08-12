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
import { DonorBadgesSection } from "../components/profile/DonorBadgesSection";

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
  const [avatarIcon, setAvatarIcon] = useState<string>(() => {
    return localStorage.getItem("bloodmatch_avatar_icon") || "water_drop";
  });

  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [isSavingDonor, setIsSavingDonor] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  function handleSelectAvatarIcon(iconId: string) {
    setAvatarIcon(iconId);
    localStorage.setItem("bloodmatch_avatar_icon", iconId);
    setFeedback("Ícone do perfil atualizado com sucesso.");
  }

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

  async function handleSaveNameValue(newName: string) {
    if (!partyId) {
      return;
    }

    const trimmed = newName.trim();
    if (!trimmed) {
      setErrorMessage("Informe um nome válido.");
      return;
    }

    setFeedback(null);
    setErrorMessage(null);

    try {
      const result = await updatePartyName(partyId, trimmed);
      setDisplayName(result.name);
      setFeedback("Nome atualizado com sucesso.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível atualizar o nome."));
    }
  }

  async function handleSaveAllPreferences(event: React.FormEvent) {
    event.preventDefault();
    if (!partyId) {
      return;
    }

    const parsedWeight = Number(weight);
    if (!bloodType || Number.isNaN(parsedWeight) || parsedWeight <= 0) {
      setErrorMessage("Informe tipo sanguíneo e um peso corporal válido.");
      return;
    }

    if (!distanceValid) {
      setErrorMessage("Informe um raio máximo de geolocalização válido.");
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
      const distanceResult = await updateDonorRecommendationDistance(partyId, parsedDistance);
      setMaxDistanceInKm(String(distanceResult.maxDistanceInKm));
      setFeedback("Configurações e preferências atualizadas com sucesso.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível salvar as configurações."));
    } finally {
      setIsSavingDonor(false);
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
              avatarIcon={avatarIcon}
              onSelectAvatarIcon={handleSelectAvatarIcon}
              onSaveName={handleSaveNameValue}
            />
          )}

          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}

          {!isLoadingProfile && (
            <>
              {canAccessDonorArea ? (
                <>
                  {/* Card Único de Configurações e Preferências */}
                  <ProfileSection
                    icon="tune"
                    title="Configurações & Preferências de Doador"
                    description="Gerencie seu tipo sanguíneo, peso corporal e raio máximo de geolocalização."
                  >
                    <form className="space-y-8" onSubmit={handleSaveAllPreferences}>
                      {/* Bloco 1: Dados de Saúde & Elegibilidade */}
                      <div className="space-y-4">
                        <div className="flex items-center gap-2 border-b border-surface-container-high pb-2">
                          <span className="material-symbols-outlined text-primary text-lg">bloodtype</span>
                          <h3 className="font-headline text-sm font-extrabold text-on-surface uppercase tracking-wider">
                            Saúde & Elegibilidade
                          </h3>
                        </div>

                        <div>
                          <p className={labelClass}>Tipo Sanguíneo</p>
                          <div className="grid grid-cols-4 gap-2.5 sm:grid-cols-8">
                            {bloodTypes.map((type) => (
                              <button
                                key={type}
                                type="button"
                                onClick={() => setBloodType(type)}
                                className={`flex flex-col items-center justify-center rounded-xl p-3 transition-all ${
                                  bloodType === type
                                    ? "bg-primary text-white shadow-md ring-2 ring-primary/30"
                                    : "bg-surface-container-low text-primary hover:bg-[#fff2f0] border border-surface-container-high"
                                }`}
                              >
                                <span className="font-headline text-base sm:text-lg font-black">{type}</span>
                              </button>
                            ))}
                          </div>
                        </div>

                        <div className="max-w-xs">
                          <label htmlFor="profile-weight" className={labelClass}>
                            Peso Corporal (kg)
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
                      </div>

                      {/* Bloco 2: Alcance & Geolocalização */}
                      <div className="space-y-4 pt-2">
                        <div className="flex items-center justify-between border-b border-surface-container-high pb-2">
                          <div className="flex items-center gap-2">
                            <span className="material-symbols-outlined text-primary text-lg">radar</span>
                            <h3 className="font-headline text-sm font-extrabold text-on-surface uppercase tracking-wider">
                              Raio de Geolocalização
                            </h3>
                          </div>
                          <span className="font-headline text-xl font-black text-primary">
                            {distanceValid ? `${parsedDistance} km` : "—"}
                          </span>
                        </div>

                        <p className="text-xs text-text-secondary">
                          Pedidos e hemocentros fora desse raio não aparecerão nas suas recomendações.
                        </p>

                        <div>
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

                          <div className="mt-3 flex flex-wrap gap-2">
                            {DISTANCE_PRESETS.map((preset) => (
                              <button
                                key={preset}
                                type="button"
                                onClick={() => setMaxDistanceInKm(String(preset))}
                                className={`rounded-xl px-3.5 py-1.5 text-xs font-bold transition-all ${
                                  Number(maxDistanceInKm) === preset
                                    ? "bg-primary text-white shadow-xs"
                                    : "bg-surface-container-low text-secondary border border-surface-container-high hover:bg-surface-container-high"
                                }`}
                              >
                                {preset} km
                              </button>
                            ))}
                          </div>
                        </div>
                      </div>

                      {/* Botão de Salvar Unificado */}
                      <div className="pt-4 border-t border-surface-container-high flex justify-end">
                        <AppButton
                          type="submit"
                          variant="danger"
                          disabled={isSavingDonor || !distanceValid}
                          className="px-8 py-3 text-sm font-bold shadow-md"
                        >
                          <span className="material-symbols-outlined text-base mr-1">save</span>
                          {isSavingDonor ? "Salvando..." : "Salvar Configurações"}
                        </AppButton>
                      </div>
                    </form>
                  </ProfileSection>

                  <DonorBadgesSection
                    bloodType={bloodType}
                    livesImpacted={livesImpacted}
                    daysRemaining={daysRemaining}
                    lastDonationDate={lastDonationDate}
                  />
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
